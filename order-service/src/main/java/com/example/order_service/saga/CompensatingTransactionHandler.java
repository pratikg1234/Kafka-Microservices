package com.example.order_service.saga;

import com.example.order_service.model.enums.OrderStatus;
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
 * CompensatingTransactionHandler handles rollback operations when a saga fails.
 * 
 * Compensating Transactions:
 * - Cancel Order
 * - Release Reserved Inventory
 * - Reverse Payments
 * - Send Notification to Customer
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CompensatingTransactionHandler {

    private final OrderService orderService;
    private final SagaExecutionRepository sagaRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Compensate order creation - reverses the order creation
     */
    @Transactional
    public void compensateOrderCreation(Long orderId, Long sagaId) {
        log.info("Starting compensation for OrderId: {}, SagaId: {}", orderId, sagaId);

        try {
            // Step 1: Mark saga as compensating
            SagaExecution sagaExecution = sagaRepository.findById(sagaId)
                    .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

            sagaExecution.markCompensationStarted();
            sagaRepository.save(sagaExecution);

            // Step 2: Cancel the order
            log.info("Cancelling order: {}", orderId);
            orderService.markOrderFailed(orderId);

            // Step 3: Publish event to release reserved inventory
            log.info("Publishing inventory release event for OrderId: {}", orderId);
            publishInventoryReleaseEvent(orderId, sagaId);

            // Step 4: Publish notification event
            log.info("Publishing compensation notification for OrderId: {}", orderId);
            publishCompensationNotification(orderId, sagaId);

            // Step 5: Mark compensation as completed
            sagaExecution.markCompensationCompleted();
            sagaRepository.save(sagaExecution);

            log.info("Compensation completed successfully for OrderId: {}", orderId);

        } catch (Exception e) {
            log.error("Error during compensation for OrderId: {}", orderId, e);
            // Log for manual intervention
            logCompensationError(sagaId, e);
        }
    }

    /**
     * Publish inventory release event - instructs inventory service to release reserved stock
     */
    private void publishInventoryReleaseEvent(Long orderId, Long sagaId) {
        try {
            Map<String, Object> payload = Map.of(
                    "orderId", orderId,
                    "sagaId", sagaId,
                    "action", "RELEASE_RESERVATION",
                    "timestamp", System.currentTimeMillis()
            );

            kafkaTemplate.send(
                    "order.inventory.release",
                    orderId.toString(),
                    objectMapper.writeValueAsString(payload)
            );

            log.info("Inventory release event published for OrderId: {}", orderId);

        } catch (Exception e) {
            log.error("Failed to publish inventory release event for OrderId: {}", orderId, e);
            throw new RuntimeException("Failed to publish inventory release", e);
        }
    }

    /**
     * Publish notification event - informs customer of order cancellation
     */
    private void publishCompensationNotification(Long orderId, Long sagaId) {
        try {
            Map<String, Object> payload = Map.of(
                    "orderId", orderId,
                    "sagaId", sagaId,
                    "eventType", "ORDER_CANCELLED_SAGA_FAILURE",
                    "message", "Your order could not be fulfilled due to inventory constraints",
                    "timestamp", System.currentTimeMillis()
            );

            kafkaTemplate.send(
                    "order.notifications",
                    orderId.toString(),
                    objectMapper.writeValueAsString(payload)
            );

            log.info("Compensation notification published for OrderId: {}", orderId);

        } catch (Exception e) {
            log.error("Failed to publish compensation notification for OrderId: {}", orderId, e);
            // Don't throw - notification failure shouldn't prevent compensation
        }
    }

    /**
     * Handle payment compensation if payment was processed
     */
    @Transactional
    public void compensatePayment(Long orderId, Long sagaId, String transactionId) {
        log.info("Starting payment compensation for OrderId: {}, TransactionId: {}", orderId, transactionId);

        try {
            Map<String, Object> payload = Map.of(
                    "orderId", orderId,
                    "sagaId", sagaId,
                    "transactionId", transactionId,
                    "action", "REFUND",
                    "timestamp", System.currentTimeMillis()
            );

            kafkaTemplate.send(
                    "order.payment.refund",
                    orderId.toString(),
                    objectMapper.writeValueAsString(payload)
            );

            log.info("Payment refund event published for OrderId: {}", orderId);

        } catch (Exception e) {
            log.error("Failed to compensate payment for OrderId: {}", orderId, e);
            // Log for manual intervention - payment compensation failure is critical
            logCompensationError(sagaId, e);
        }
    }

    /**
     * Log compensation errors for alerting and manual intervention
     */
    private void logCompensationError(Long sagaId, Exception exception) {
        log.error("CRITICAL: Compensation failed for SagaId: {}. Manual intervention required.", sagaId);
        log.error("Error: {}", exception.getMessage());

        // In production, integrate with alerting system:
        // - Send to Slack/PagerDuty
        // - Store in alert database
        // - Create ticket in issue tracking system
    }

    /**
     * Check and process sagas requiring compensation (after service restart)
     */
    @Transactional
    public void processCompensationQueue() {
        log.info("Processing compensation queue");

        try {
            var failedSagas = sagaRepository.findSagasRequiringCompensation(SagaState.SAGA_FAILED);

            for (SagaExecution saga : failedSagas) {
                log.info("Processing compensation for SagaId: {}, OrderId: {}", saga.getId(), saga.getOrderId());
                compensateOrderCreation(saga.getOrderId(), saga.getId());
            }

        } catch (Exception e) {
            log.error("Error processing compensation queue", e);
        }
    }
}
