package com.example.order_service.controller;

import com.example.order_service.dto.CreateOrderRequest;
import com.example.order_service.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;


@RestController
@RequestMapping("/orders")
@Slf4j
public class OrderController {

    private final OrderService orderService;
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        log.info("Received order creation request: userId={}, items={}", request.getUserId(), request.getItems().size());
        Long orderId = orderService.createOrder(request);
        log.info("Order created: orderId={}", orderId);
        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "status", "PENDING"
        ));
    }
}
