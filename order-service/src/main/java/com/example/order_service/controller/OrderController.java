package com.example.order_service.controller;

import com.example.order_service.dto.CreateOrderRequest;
import com.example.order_service.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;


@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        Long orderId = orderService.createOrder(request);
        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "status", "PENDING"
        ));
    }
}
