package com.example.order_service.saga;

import lombok.Getter;

/**
 * Represents a step in the saga execution flow.
 * Used to track the state of each saga step for compensating transactions.
 */
@Getter
public enum SagaStep {
    PENDING("Waiting to execute"),
    IN_PROGRESS("Currently executing"),
    COMPLETED("Successfully completed"),
    FAILED("Failed during execution"),
    COMPENSATING("Rolling back changes"),
    COMPENSATED("Changes rolled back");

    private final String description;

    SagaStep(String description) {
        this.description = description;
    }
}
