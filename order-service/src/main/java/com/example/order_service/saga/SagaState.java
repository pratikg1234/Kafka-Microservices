package com.example.order_service.saga;

import lombok.Getter;

/**
 * Represents the overall state of a saga execution.
 */
@Getter
public enum SagaState {
    STARTED("Saga execution started"),
    ORDER_CREATION_IN_PROGRESS("Creating order"),
    ORDER_CREATED("Order created successfully"),
    INVENTORY_RESERVATION_IN_PROGRESS("Reserving inventory"),
    INVENTORY_RESERVED("Inventory reserved successfully"),
    PAYMENT_PROCESSING_IN_PROGRESS("Processing payment"),
    PAYMENT_COMPLETED("Payment completed successfully"),
    SAGA_COMPLETED("Saga completed successfully"),
    SAGA_FAILED("Saga failed"),
    SAGA_COMPENSATING("Rolling back saga"),
    SAGA_COMPENSATED("Saga rolled back");

    private final String description;

    SagaState(String description) {
        this.description = description;
    }
}
