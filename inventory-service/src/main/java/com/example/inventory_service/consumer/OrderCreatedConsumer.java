package com.example.inventory_service.consumer;

import com.example.inventory_service.service.InventoryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderCreatedConsumer {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Timer messageProcessingTimer;
    private final Counter messagesReceivedCounter;
    private final Counter messagesFailedCounter;

    public OrderCreatedConsumer(InventoryService inventoryService,
                                ObjectMapper objectMapper,
                                KafkaTemplate<String, String> kafkaTemplate,
                                MeterRegistry meterRegistry) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.messageProcessingTimer = Timer.builder("kafka.consumer.processing.duration")
                .tag("topic", "order.created")
                .description("Time to process order.created messages")
                .register(meterRegistry);
        this.messagesReceivedCounter = Counter.builder("kafka.consumer.messages.received")
                .tag("topic", "order.created")
                .description("Messages received on order.created")
                .register(meterRegistry);
        this.messagesFailedCounter = Counter.builder("kafka.consumer.messages.failed")
                .tag("topic", "order.created")
                .description("Failed messages on order.created")
                .register(meterRegistry);
    }

    @KafkaListener(topics = "order.created", groupId = "inventory-group")
    public void consume(String message) {
        messagesReceivedCounter.increment();

        messageProcessingTimer.record(() -> {
            try {
                JsonNode json = objectMapper.readTree(message);
                JsonNode items = json.get("items");
                Long orderId = json.get("orderId").asLong();

                log.info("Processing order.created: orderId={}, itemCount={}", orderId, items.size());

                boolean allReserved = true;

                for (JsonNode item : items) {
                    String productId = item.get("productId").asText();
                    int quantity = item.get("quantity").asInt();

                    boolean success = inventoryService.reserveStock(orderId, productId, quantity);
                    if (!success) {
                        log.warn("Reservation failed: orderId={}, productId={}", orderId, productId);
                        allReserved = false;
                        break;
                    }
                }
                if (allReserved) {
                    log.info("All items reserved: orderId={}", orderId);
                    kafkaTemplate.send("inventory.reserved", message);
                } else {
                    log.warn("Reservation failed for order: orderId={}", orderId);
                    kafkaTemplate.send("inventory.failed", message);
                }

            } catch (Exception e) {
                messagesFailedCounter.increment();
                log.error("Error processing order.created", e);
            }
        });
    }
}
