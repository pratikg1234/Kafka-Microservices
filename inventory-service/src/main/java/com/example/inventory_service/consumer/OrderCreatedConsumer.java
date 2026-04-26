package com.example.inventory_service.consumer;

import com.example.inventory_service.service.InventoryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedConsumer {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = "order.created", groupId = "inventory-group")
    public void consume(String message) {

        try {
            log.info("Full message: {}", message);

            JsonNode json = objectMapper.readTree(message);

            log.info("Parsed JSON: {}", json);

            JsonNode items = json.get("items");

            log.info("Items node: {}", items);

            Long orderId = json.get("orderId").asLong();

            boolean allReserved = true;

            for (JsonNode item : items) {
                String productId = item.get("productId").asText();
                int quantity = item.get("quantity").asInt();

                boolean success = inventoryService.reserveStock(orderId, productId, quantity);
                log.info("Product {} reserve result: {}", productId, success);
                if (!success) {
                    allReserved = false;
                    break;
                }
            }
            if (allReserved) {
                kafkaTemplate.send("inventory.reserved", message);
            } else {
                kafkaTemplate.send("inventory.failed", message);
            }

        } catch (Exception e) {
            log.error("Error processing order.created", e);
        }
    }
}
