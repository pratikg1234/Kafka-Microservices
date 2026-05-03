package com.example.order_service.service;

import com.example.order_service.dto.CreateOrderRequest;
import com.example.order_service.model.entity.Order;
import com.example.order_service.model.entity.OrderItem;
import com.example.order_service.model.entity.OutboxEvent;
import com.example.order_service.model.enums.OrderStatus;
import com.example.order_service.repository.OrderRepository;
import com.example.order_service.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository,
                        OutboxEventRepository outboxRepository,
                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Long createOrder(CreateOrderRequest request) {

        // 1. Create Order
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setStatus(OrderStatus.PENDING);
        order.setAmount(BigDecimal.ZERO);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        List<OrderItem> items = request.getItems().stream().map(i -> {
            OrderItem item = new OrderItem();
            item.setProductId(i.getProductId());
            item.setQuantity(i.getQuantity());
            item.setOrder(order);
            return item;
        }).toList();

        order.setItems(items);

        Order savedOrder = orderRepository.save(order);

        // 2. Create Outbox Event
        try {
            String eventId = UUID.randomUUID().toString();

            Map<String, Object> payload = Map.of(
                    "eventId", eventId,
                    "orderId", savedOrder.getId(),
                    "userId", savedOrder.getUserId(),
                    "items", request.getItems()
            );

            OutboxEvent event = new OutboxEvent();
            event.setEventId(eventId);
            event.setAggregateType("ORDER");
            event.setAggregateId(savedOrder.getId());
            event.setEventType("order.created");
            event.setPayload(objectMapper.writeValueAsString(payload));
            event.setStatus("NEW");
            event.setCreatedAt(LocalDateTime.now());

            outboxRepository.save(event);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create outbox event", e);
        }

        return savedOrder.getId();
    }

    public void markOrderConfirmed(Long orderId) {
        updateStatus(orderId, OrderStatus.COMPLETED);
    }

    public void markOrderFailed(Long orderId) {
        updateStatus(orderId, OrderStatus.CANCELLED);
    }

    private void updateStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() == status) {
            log.info("Order {} already in status {}, skipping", orderId, status);
            return;
        }

        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        log.info("Order {} updated to {}", orderId, status);
    }
}