package com.example.order_service.consumer;

import com.example.order_service.model.entity.ProcessedEvent;
import com.example.order_service.repository.ProcessedEventRepository;
import com.example.order_service.service.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {

    private final ObjectMapper objectMapper;
    private final OrderService orderService;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(topics = "inventory.reserved", groupId = "order-group")
    public void handleReserved(String message) {
        process(message, true);
    }

    @KafkaListener(topics = "inventory.failed", groupId = "order-group")
    public void handleFailed(String message) {
        process(message, false);
    }

    private void process(String message, boolean success) {
        try {
            JsonNode json = objectMapper.readTree(message);

            String eventId = json.get("eventId").asText();
            Long orderId = json.get("orderId").asLong();

            if (processedEventRepository.existsById(eventId)) {
                log.info("Duplicate event ignored: {}", eventId);
                return;
            }

            log.info("Processing eventId={} for orderId={}", eventId, orderId);

            if (success) {
                orderService.markOrderConfirmed(orderId);
            } else {
                orderService.markOrderFailed(orderId);
            }

            processedEventRepository.save(new ProcessedEvent(eventId));

        } catch (Exception e) {
            log.error("Error processing inventory event", e);
            throw new RuntimeException("fail");
        }
    }
}