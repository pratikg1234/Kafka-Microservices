package com.example.order_service.saga.repository;

import com.example.order_service.saga.entity.SagaExecution;
import com.example.order_service.saga.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for SagaExecution entity.
 */
@Repository
public interface SagaExecutionRepository extends JpaRepository<SagaExecution, Long> {

    /**
     * Find saga execution by order ID
     */
    Optional<SagaExecution> findByOrderId(Long orderId);

    /**
     * Find all failed sagas that can be retried
     */
    @Query("SELECT s FROM SagaExecution s WHERE s.sagaState = :state AND s.retryCount < s.maxRetries")
    List<SagaExecution> findFailedSagasForRetry(@Param("state") SagaState state);

    /**
     * Find all sagas requiring compensation
     */
    @Query("SELECT s FROM SagaExecution s WHERE s.requiresCompensation = true AND s.sagaState = :state")
    List<SagaExecution> findSagasRequiringCompensation(@Param("state") SagaState state);

    /**
     * Find sagas that failed after a certain time (for alerting)
     */
    @Query("SELECT s FROM SagaExecution s WHERE s.sagaState = :state AND s.completedAt > :since")
    List<SagaExecution> findRecentFailedSagas(@Param("state") SagaState state, @Param("since") LocalDateTime since);

    /**
     * Count active sagas
     */
    @Query("SELECT COUNT(s) FROM SagaExecution s WHERE s.sagaState NOT IN ('SAGA_COMPLETED', 'SAGA_FAILED', 'SAGA_COMPENSATED')")
    long countActiveSagas();
}
