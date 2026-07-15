package com.example.order_service.saga.entity;

import com.example.order_service.saga.SagaState;
import com.example.order_service.saga.SagaStep;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity to track the execution state of a saga.
 * Stores saga execution history for auditing and recovery purposes.
 */
@Entity
@Table(name = "saga_executions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SagaExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    @Enumerated(EnumType.STRING)
    private SagaState sagaState;

    @Column(columnDefinition = "JSON")
    private String stepStates; // JSON: {"ORDER_CREATION": "COMPLETED", "INVENTORY_RESERVATION": "FAILED"}

    private int retryCount;

    private int maxRetries = 3;

    private String errorMessage;

    private String errorStackTrace;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime lastRetryAt;

    @Column(columnDefinition = "JSON")
    private String payloadSnapshot; // Store original request payload for retry

    private boolean requiresCompensation;

    /**
     * Initialize saga execution for an order
     */
    public void initializeForOrder(Long orderId, String payload) {
        this.orderId = orderId;
        this.sagaState = SagaState.STARTED;
        this.stepStates = "{}";
        this.retryCount = 0;
        this.maxRetries = 3;
        this.startedAt = LocalDateTime.now();
        this.payloadSnapshot = payload;
        this.requiresCompensation = false;
    }

    /**
     * Update step state
     */
    public void updateStepState(String stepName, SagaStep step) {
        // Parse existing JSON, update, and store
        // In real implementation, use ObjectMapper
        this.stepStates = updateJsonField(this.stepStates, stepName, step.name());
    }

    /**
     * Mark saga as completed
     */
    public void markCompleted() {
        this.sagaState = SagaState.SAGA_COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Mark saga as failed and trigger compensation
     */
    public void markFailedAndCompensate(String errorMsg, String stackTrace) {
        this.sagaState = SagaState.SAGA_FAILED;
        this.errorMessage = errorMsg;
        this.errorStackTrace = stackTrace;
        this.completedAt = LocalDateTime.now();
        this.requiresCompensation = true;
    }

    /**
     * Check if saga can be retried
     */
    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    /**
     * Increment retry count and update last retry time
     */
    public void incrementRetry() {
        this.retryCount++;
        this.lastRetryAt = LocalDateTime.now();
    }

    /**
     * Mark compensation as started
     */
    public void markCompensationStarted() {
        this.sagaState = SagaState.SAGA_COMPENSATING;
    }

    /**
     * Mark compensation as completed
     */
    public void markCompensationCompleted() {
        this.sagaState = SagaState.SAGA_COMPENSATED;
        this.completedAt = LocalDateTime.now();
        this.requiresCompensation = false;
    }

    /**
     * Helper method to update JSON field (simplified, use ObjectMapper in production)
     */
    private String updateJsonField(String json, String key, String value) {
        // Simplified implementation
        return json.replace("}", ",\"" + key + "\":\"" + value + "\"}");
    }
}
