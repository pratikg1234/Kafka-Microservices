package com.example.order_service.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public OrderItem() {
    }

    private String productId;

    public Long getId() {
        return id;
    }

    public OrderItem(Long id, String productId, Integer quantity, Order order) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.order = order;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    private Integer quantity;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
}
