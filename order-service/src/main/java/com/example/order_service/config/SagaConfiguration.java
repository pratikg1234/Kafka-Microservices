package com.example.order_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for Saga Pattern features
 * 
 * Enables:
 * - Scheduled DLQ retry processing
 * - Compensation processing
 * - Saga monitoring
 */
@Configuration
@EnableScheduling
public class SagaConfiguration {
    
    /**
     * Saga execution configuration constants
     */
    public static class SagaProperties {
        public static final int MAX_RETRIES = 3;
        public static final long RETRY_DELAY_MS = 1000; // 1 second initial delay
        public static final long MAX_RETRY_DELAY_MS = 60000; // 60 seconds max
        public static final int RETRY_SCHEDULER_INTERVAL_MS = 300000; // 5 minutes
        public static final int HEALTH_CHECK_INTERVAL_MS = 60000; // 1 minute
        public static final int ACTIVE_SAGA_THRESHOLD = 100;
    }
}
