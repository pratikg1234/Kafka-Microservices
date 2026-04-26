package com.example.order_service.dto;

import java.util.List;

public class CreateOrderRequest {
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }

    private Long userId;
    private List<OrderItemRequest> items;
}
