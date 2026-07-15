package com.example.order_service.saga.controller;

import com.example.order_service.saga.DLQRetryScheduler;
import com.example.order_service.saga.SagaOrchestrator;
import com.example.order_service.saga.repository.SagaExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for Saga Management and Monitoring
 */
@RestController
@RequestMapping("/saga")
@Slf4j
@RequiredArgsConstructor
public class SagaManagementController {

    private final SagaOrchestrator sagaOrchestrator;
    private final DLQRetryScheduler dlqRetryScheduler;
    private final SagaExecutionRepository sagaRepository;

    /**
     * Get DLQ metrics
     */
    @GetMapping("/dlq/metrics")
    public ResponseEntity<?> getDLQMetrics() {
        long pendingCount = dlqRetryScheduler.getDLQPendingCount();
        long deadCount = dlqRetryScheduler.getDeadLetterCount();

        return ResponseEntity.ok(Map.of(
                "pendingRetryCount", pendingCount,
                "deadLetterCount", deadCount,
                "totalFailedMessages", pendingCount + deadCount
        ));
    }

    /**
     * Manually trigger retry for all DLQ messages
     */
    @PostMapping("/dlq/retry-all")
    public ResponseEntity<?> retryAllDLQ() {
        log.warn("Manual trigger: Retrying all DLQ messages");
        dlqRetryScheduler.manualRetryAllDLQ();

        return ResponseEntity.ok(Map.of(
                "status", "DLQ retry triggered",
                "message", "All DLQ messages will be retried with exponential backoff"
        ));
    }

    /**
     * Get saga execution details by saga ID
     */
    @GetMapping("/execution/{sagaId}")
    public ResponseEntity<?> getSagaExecution(@PathVariable Long sagaId) {
        return sagaRepository.findById(sagaId)
                .map(saga -> ResponseEntity.ok(Map.of(
                        "sagaId", saga.getId(),
                        "orderId", saga.getOrderId(),
                        "sagaState", saga.getSagaState().toString(),
                        "retryCount", saga.getRetryCount(),
                        "maxRetries", saga.getMaxRetries(),
                        "requiresCompensation", saga.isRequiresCompensation(),
                        "startedAt", saga.getStartedAt(),
                        "completedAt", saga.getCompletedAt(),
                        "errorMessage", saga.getErrorMessage()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get saga execution by order ID
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getSagaByOrder(@PathVariable Long orderId) {
        return sagaRepository.findByOrderId(orderId)
                .map(saga -> ResponseEntity.ok(Map.of(
                        "sagaId", saga.getId(),
                        "orderId", saga.getOrderId(),
                        "sagaState", saga.getSagaState().toString(),
                        "retryCount", saga.getRetryCount(),
                        "requiresCompensation", saga.isRequiresCompensation()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get health status
     */
    @GetMapping("/health")
    public ResponseEntity<?> getHealth() {
        long activeSagas = sagaRepository.countActiveSagas();
        String status = activeSagas < 100 ? "HEALTHY" : "DEGRADED";

        return ResponseEntity.ok(Map.of(
                "status", status,
                "activeSagas", activeSagas,
                "dlqMetrics", Map.of(
                        "pending", dlqRetryScheduler.getDLQPendingCount(),
                        "dead", dlqRetryScheduler.getDeadLetterCount()
                )
        ));
    }
}
