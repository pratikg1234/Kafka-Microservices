package com.example.order_service.saga;

import com.example.order_service.saga.entity.SagaExecution;
import com.example.order_service.saga.repository.SagaExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * DLQRetryScheduler manages retry logic for messages in Dead Letter Queues.
 * 
 * Features:
 * - Automatic retry of failed sagas
 * - Exponential backoff strategy
 * - Max retry limits
 * - Metrics on DLQ message count
 * - Manual intervention triggers
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DLQRetryScheduler {

    private final SagaExecutionRepository sagaRepository;
    private final SagaOrchestrator sagaOrchestrator;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Periodically check for failed sagas and attempt retry
     * Runs every 5 minutes
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    @Transactional
    public void retryFailedSagas() {
        log.info("Starting DLQ retry scheduler");

        try {
            // Find all failed sagas that can be retried
            List<SagaExecution> failedSagas = sagaRepository.findFailedSagasForRetry(SagaState.SAGA_FAILED);

            if (failedSagas.isEmpty()) {
                log.debug("No failed sagas to retry");
                return;
            }

            log.info("Found {} failed sagas to retry", failedSagas.size());

            for (SagaExecution saga : failedSagas) {
                retryFailedSaga(saga);
            }

        } catch (Exception e) {
            log.error("Error in retry scheduler", e);
        }
    }

    /**
     * Retry a specific failed saga with exponential backoff
     */
    @Transactional
    public void retryFailedSaga(SagaExecution saga) {
        log.info("Retrying SagaId: {} (Attempt: {}/{})", 
                saga.getId(), 
                saga.getRetryCount() + 1, 
                saga.getMaxRetries());

        try {
            // Calculate exponential backoff delay
            long delayMs = calculateBackoffDelay(saga.getRetryCount());
            long lastRetryAge = System.currentTimeMillis() - saga.getLastRetryAt().getTime();

            // Only retry if enough time has passed
            if (lastRetryAge < delayMs) {
                log.debug("SagaId: {} not ready for retry yet. Waiting {} ms more", 
                        saga.getId(), 
                        delayMs - lastRetryAge);
                return;
            }

            // Retry the saga
            saga.incrementRetry();
            sagaRepository.save(saga);

            // Re-process the saga - reconstruct from payload snapshot
            // In real implementation, you'd deserialize and retry
            log.info("SagaId: {} retry initiated. Current attempt: {}/{}", 
                    saga.getId(), 
                    saga.getRetryCount(), 
                    saga.getMaxRetries());

            // Publish retry event
            publishRetryEvent(saga);

        } catch (Exception e) {
            log.error("Error retrying SagaId: {}", saga.getId(), e);
        }
    }

    /**
     * Calculate exponential backoff delay (1s, 2s, 4s, 8s, ...)
     */
    private long calculateBackoffDelay(int retryCount) {
        long baseDelayMs = 1000; // 1 second
        long maxDelayMs = 60000;  // 60 seconds max
        long delay = baseDelayMs * (long) Math.pow(2, retryCount);
        return Math.min(delay, maxDelayMs);
    }

    /**
     * Monitor DLQ health and alert if too many messages
     * Runs every 1 minute
     */
    @Scheduled(fixedDelay = 60000) // 1 minute
    public void monitorDLQHealth() {
        log.debug("Monitoring DLQ health");

        try {
            long failedSagaCount = sagaRepository.countActiveSagas();
            long threshold = 100; // Alert if more than 100 active sagas

            if (failedSagaCount > threshold) {
                log.warn("WARNING: High number of active sagas detected: {}. Threshold: {}", 
                        failedSagaCount, 
                        threshold);
                
                // Send alert
                sendDLQHealthAlert(failedSagaCount, threshold);
            }

        } catch (Exception e) {
            log.error("Error monitoring DLQ health", e);
        }
    }

    /**
     * Get count of messages in DLQ pending retry
     */
    public long getDLQPendingCount() {
        try {
            return sagaRepository.findFailedSagasForRetry(SagaState.SAGA_FAILED).size();
        } catch (Exception e) {
            log.error("Error getting DLQ pending count", e);
            return -1;
        }
    }

    /**
     * Get count of messages that exceeded max retries (dead)
     */
    public long getDeadLetterCount() {
        try {
            var deadLetters = sagaRepository.findFailedSagasForRetry(SagaState.SAGA_FAILED)
                    .stream()
                    .filter(saga -> saga.getRetryCount() >= saga.getMaxRetries())
                    .count();
            return deadLetters;
        } catch (Exception e) {
            log.error("Error getting dead letter count", e);
            return -1;
        }
    }

    /**
     * Publish retry event for saga
     */
    private void publishRetryEvent(SagaExecution saga) {
        try {
            String retryEvent = String.format(
                    "{\"sagaId\": %d, \"orderId\": %d, \"retryCount\": %d, \"action\": \"RETRY\"}",
                    saga.getId(),
                    saga.getOrderId(),
                    saga.getRetryCount()
            );

            kafkaTemplate.send(
                    "saga.retry",
                    saga.getId().toString(),
                    retryEvent
            );

            log.info("Retry event published for SagaId: {}", saga.getId());

        } catch (Exception e) {
            log.error("Failed to publish retry event for SagaId: {}", saga.getId(), e);
        }
    }

    /**
     * Send DLQ health alert
     */
    private void sendDLQHealthAlert(long failedCount, long threshold) {
        log.error("ALERT: DLQ health threshold exceeded. Failed sagas: {}, Threshold: {}", 
                failedCount, threshold);

        // In production, send to:
        // - Slack channel
        // - PagerDuty alert
        // - Datadog/New Relic metrics
        // - Alert database
    }

    /**
     * Manual trigger to retry all dead letter messages
     */
    @Transactional
    public void manualRetryAllDLQ() {
        log.warn("Manual retry triggered for all DLQ messages");

        try {
            List<SagaExecution> deadLetters = sagaRepository.findFailedSagasForRetry(SagaState.SAGA_FAILED);

            for (SagaExecution saga : deadLetters) {
                // Reset retry count to allow retry
                saga.setRetryCount(0);
                saga.setMaxRetries(3);
                sagaRepository.save(saga);
                
                retryFailedSaga(saga);
            }

            log.info("Manual retry completed for {} DLQ messages", deadLetters.size());

        } catch (Exception e) {
            log.error("Error in manual DLQ retry", e);
        }
    }
}
