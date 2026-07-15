package com.example.order_service.config;

import com.example.order_service.saga.SagaState;
import com.example.order_service.saga.repository.SagaExecutionRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class SagaHealthIndicator implements HealthIndicator {

    private final SagaExecutionRepository sagaRepository;
    private static final int ACTIVE_SAGA_THRESHOLD = 100;

    public SagaHealthIndicator(SagaExecutionRepository sagaRepository) {
        this.sagaRepository = sagaRepository;
    }

    @Override
    public Health health() {
        long activeSagas = sagaRepository.count();

        if (activeSagas >= ACTIVE_SAGA_THRESHOLD) {
            return Health.down()
                    .withDetail("activeSagas", activeSagas)
                    .withDetail("reason", "Too many active sagas - possible stuck executions")
                    .build();
        }

        return Health.up()
                .withDetail("activeSagas", activeSagas)
                .build();
    }
}
