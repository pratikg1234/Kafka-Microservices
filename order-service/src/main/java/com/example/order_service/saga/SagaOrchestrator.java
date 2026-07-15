package com.example.order_service.saga;

import com.example.order_service.dto.CreateOrderRequest;
import com.example.order_service.model.entity.Order;
import com.example.order_service.saga.entity.SagaExecution;
import com.example.order_service.saga.repository.SagaExecutionRepository;
import com.example.order_service.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * SagaOrchestrator manages the distributed saga execution.
 * 
 * Saga Flow:
 * 1. Create Order (LOCAL)
 * 2. Reserve Inventory (via Kafka event)
 * 3. Process Payment (via Kafka event)
 * 4. Complete Saga
 * 
 * If any step fails:
 * - Mark saga as failed
 * - Trigger compensating transactions
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SagaOrchestrator {

    private final OrderService orderService;
    private final SagaExecutionRepository sagaRepository;
    private final CompensatingTransactionHandler compensationHandler;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Start a new saga for order creation
     */
    @Transactional
    public Long startOrderCreationSaga(CreateOrderRequest request) {
        log.info("Starting Saga for Order Creation");

        try {
            // Step 1: Create Saga Execution Record
            SagaExecution sagaExecution = new SagaExecution();
            sagaExecution.initializeForOrder(null, objectMapper.writeValueAsString(request));
            SagaExecution savedSaga = sagaRepository.save(sagaExecution);

            log.info("Saga created with ID: {}", savedSaga.getId());

            // Step 2: Create Order (Local Transaction)
            sagaExecution.updateStepState("ORDER_CREATION", SagaStep.IN_PROGRESS);
            sagaRepository.save(sagaExecution);

            Long orderId = orderService.createOrder(request);
            sagaExecution.setOrderId(orderId);

            sagaExecution.updateStepState("ORDER_CREATION", SagaStep.COMPLETED);
            sagaExecution.setSagaState(SagaState.ORDER_CREATED);
            sagaRepository.save(sagaExecution);

            log.info("Order created successfully. OrderId: {}, SagaId: {}", orderId, savedSaga.getId());

            // Step 3: Publish event for Inventory Reservation
            sagaExecution.updateStepState("INVENTORY_RESERVATION", SagaStep.IN_PROGRESS);
            sagaExecution.setSagaState(SagaState.INVENTORY_RESERVATION_IN_PROGRESS);
            sagaRepository.save(sagaExecution);

            publishInventoryReservationEvent(orderId, request, savedSaga.getId());

            return orderId;

        } catch (Exception e) {
            log.error("Saga failed during execution", e);
            throw new RuntimeException("Saga execution failed", e);
        }
    }

    /**
     * Handle inventory reservation success
     */
    @Transactional
    public void handleInventoryReserved(Long orderId, Long sagaId) {
        log.info("Inventory reserved successfully for OrderId: {}", orderId);

        try {
            SagaExecution sagaExecution = sagaRepository.findById(sagaId)
                    .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

            sagaExecution.updateStepState("INVENTORY_RESERVATION", SagaStep.COMPLETED);
            sagaExecution.setSagaState(SagaState.INVENTORY_RESERVED);
            sagaRepository.save(sagaExecution);

            // In a real scenario, you'd call Payment Service here
            // For now, we'll complete the saga
            completeSaga(sagaId);

        } catch (Exception e) {
            log.error("Error handling inventory reserved for SagaId: {}", sagaId, e);
            handleSagaFailure(sagaId, e);
        }
    }

    /**
     * Handle inventory reservation failure
     */
    @Transactional
    public void handleInventoryReservationFailed(Long orderId, Long sagaId) {
        log.error("Inventory reservation failed for OrderId: {}", orderId);

        try {
            SagaExecution sagaExecution = sagaRepository.findById(sagaId)
                    .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

            sagaExecution.updateStepState("INVENTORY_RESERVATION", SagaStep.FAILED);
            sagaExecution.markFailedAndCompensate(
                    "Inventory reservation failed",
                    "Product not available or insufficient stock"
            );
            sagaRepository.save(sagaExecution);

            // Trigger compensating transactions
            compensationHandler.compensateOrderCreation(orderId, sagaId);

        } catch (Exception e) {
            log.error("Error handling inventory reservation failure for SagaId: {}", sagaId, e);
        }
    }

    /**
     * Complete saga successfully
     */
    @Transactional
    public void completeSaga(Long sagaId) {
        log.info("Completing Saga: {}", sagaId);

        SagaExecution sagaExecution = sagaRepository.findById(sagaId)
                .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

        sagaExecution.markCompleted();
        sagaRepository.save(sagaExecution);

        log.info("Saga completed successfully: {}", sagaId);
    }

    /**
     * Handle saga failure - mark for compensation and retry
     */
    @Transactional
    public void handleSagaFailure(Long sagaId, Exception exception) {
        log.error("Saga failed: {}", sagaId, exception);

        SagaExecution sagaExecution = sagaRepository.findById(sagaId)
                .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

        sagaExecution.markFailedAndCompensate(
                exception.getMessage(),
                getStackTrace(exception)
        );

        if (sagaExecution.canRetry()) {
            log.info("Saga will be retried. Retry count: {}/{}", 
                    sagaExecution.getRetryCount(), 
                    sagaExecution.getMaxRetries());
            sagaExecution.incrementRetry();
        } else {
            log.error("Saga max retries exceeded. Compensation will proceed.");
        }

        sagaRepository.save(sagaExecution);

        // Trigger compensation
        if (sagaExecution.getOrderId() != null) {
            compensationHandler.compensateOrderCreation(sagaExecution.getOrderId(), sagaId);
        }
    }

    /**
     * Publish inventory reservation event
     */
    private void publishInventoryReservationEvent(Long orderId, CreateOrderRequest request, Long sagaId) {
        try {
            Map<String, Object> payload = Map.of(
                    "orderId", orderId,
                    "sagaId", sagaId,
                    "userId", request.getUserId(),
                    "items", request.getItems()
            );

            kafkaTemplate.send("order.inventory.reserve", orderId.toString(), objectMapper.writeValueAsString(payload));

            log.info("Inventory reservation event published for OrderId: {}", orderId);

        } catch (Exception e) {
            log.error("Failed to publish inventory reservation event", e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    /**
     * Helper to convert exception stack trace to string
     */
    private String getStackTrace(Exception exception) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : exception.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}
