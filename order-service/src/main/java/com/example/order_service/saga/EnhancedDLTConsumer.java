package com.example.order_service.saga;

import com.example.order_service.saga.entity.SagaExecution;
import com.example.order_service.saga.repository.SagaExecutionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Enhanced DLT Consumer with Saga Integration
 * 
 * Handles messages that failed after max retries and:
 * - Stores in saga execution for auditing
 * - Triggers compensation if needed
 * - Logs for manual intervention
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EnhancedDLTConsumer {

    private final SagaExecutionRepository sagaRepository;
    private final CompensatingTransactionHandler compensationHandler;
    private final ObjectMapper objectMapper;

    /**
     * Handle inventory reservation DLT messages
     */
    @KafkaListener(topics = "inventory.reserved.dlt", groupId = "order-dlt-group")
    public void handleReservedDLT(String message) {
        log.error("DLT Message (inventory.reserved): {}", message);

        try {
            JsonNode json = objectMapper.readTree(message);
            Long orderId = json.get("orderId").asLong();
            Long sagaId = json.get("sagaId").asLong();

            // Find related saga
            SagaExecution saga = sagaRepository.findById(sagaId)
                    .orElse(null);

            if (saga != null) {
                saga.markFailedAndCompensate(
                        "Message failed after max retries in inventory.reserved.dlt",
                        message
                );
                sagaRepository.save(saga);

                // Trigger compensation
                compensationHandler.compensateOrderCreation(orderId, sagaId);
            }

            // Log for manual investigation
            logDLTMessage("inventory.reserved.dlt", orderId, message);

        } catch (Exception e) {
            log.error("Error processing inventory.reserved.dlt", e);
        }
    }

    /**
     * Handle inventory failed DLT messages
     */
    @KafkaListener(topics = "inventory.failed.dlt", groupId = "order-dlt-group")
    public void handleFailedDLT(String message) {
        log.error("DLT Message (inventory.failed): {}", message);

        try {
            JsonNode json = objectMapper.readTree(message);
            Long orderId = json.get("orderId").asLong();
            Long sagaId = json.get("sagaId").asLong();

            // Find related saga
            SagaExecution saga = sagaRepository.findById(sagaId)
                    .orElse(null);

            if (saga != null) {
                saga.markFailedAndCompensate(
                        "Message failed after max retries in inventory.failed.dlt",
                        message
                );
                sagaRepository.save(saga);

                // Trigger compensation
                compensationHandler.compensateOrderCreation(orderId, sagaId);
            }

            // Log for manual investigation
            logDLTMessage("inventory.failed.dlt", orderId, message);

        } catch (Exception e) {
            log.error("Error processing inventory.failed.dlt", e);
        }
    }

    /**
     * Log DLT message for investigation
     */
    private void logDLTMessage(String topic, Long orderId, String message) {
        log.warn("=== DLT MESSAGE LOG ===");
        log.warn("Topic: {}", topic);
        log.warn("OrderId: {}", orderId);
        log.warn("Message: {}", message);
        log.warn("Timestamp: {}", System.currentTimeMillis());
        log.warn("Action Required: Manual investigation and potential retry or compensation");
        log.warn("=======================");

        // In production:
        // - Store in DLT archive database
        // - Create ticket in issue tracking
        // - Send Slack alert with details
        // - Store metrics for dashboard
    }
}
