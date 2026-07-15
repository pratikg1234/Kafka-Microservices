package com.example.order_service.config;

import com.example.order_service.repository.OutboxEventRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class OutboxHealthIndicator implements HealthIndicator {

    private final OutboxEventRepository outboxRepository;
    private static final int PENDING_THRESHOLD = 50;

    public OutboxHealthIndicator(OutboxEventRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Override
    public Health health() {
        long pendingCount = outboxRepository.findTop10ByStatusOrderByCreatedAtAsc("NEW").size();
        long failedCount = outboxRepository.findTop10ByStatusOrderByCreatedAtAsc("FAILED").size();

        if (pendingCount >= PENDING_THRESHOLD) {
            return Health.down()
                    .withDetail("pendingEvents", pendingCount)
                    .withDetail("failedEvents", failedCount)
                    .withDetail("reason", "Outbox backlog exceeds threshold")
                    .build();
        }

        return Health.up()
                .withDetail("pendingEvents", pendingCount)
                .withDetail("failedEvents", failedCount)
                .build();
    }
}
