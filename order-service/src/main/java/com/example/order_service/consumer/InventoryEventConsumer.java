package com.example.order_service.consumer;

import com.example.order_service.model.entity.ProcessedEvent;
import com.example.order_service.repository.ProcessedEventRepository;
import com.example.order_service.service.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InventoryEventConsumer {

    private final ObjectMapper objectMapper;
    private final OrderService orderService;
    private final ProcessedEventRepository processedEventRepository;
    private final Counter inventoryReservedCounter;
    private final Counter inventoryFailedCounter;
    private final Counter duplicateEventsCounter;

    public InventoryEventConsumer(ObjectMapper objectMapper,
                                   OrderService orderService,
                                   ProcessedEventRepository processedEventRepository,
                                   MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
        this.processedEventRepository = processedEventRepository;
        this.inventoryReservedCounter = Counter.builder("kafka.consumer.messages.received")
                .tag("topic", "inventory.reserved")
                .description("Inventory reserved events received")
                .register(meterRegistry);
        this.inventoryFailedCounter = Counter.builder("kafka.consumer.messages.received")
                .tag("topic", "inventory.failed")
                .description("Inventory failed events received")
                .register(meterRegistry);
        this.duplicateEventsCounter = Counter.builder("kafka.consumer.messages.duplicate")
                .description("Duplicate events ignored")
                .register(meterRegistry);
    }

    @KafkaListener(topics = "inventory.reserved", groupId = "order-group")
    public void handleReserved(String message) {
        inventoryReservedCounter.increment();
        process(message, true);
    }

    @KafkaListener(topics = "inventory.failed", groupId = "order-group")
    public void handleFailed(String message) {
        inventoryFailedCounter.increment();
        process(message, false);
    }

    private void process(String message, boolean success) {
        try {
            JsonNode json = objectMapper.readTree(message);

            String eventId = json.get("eventId").asText();
            Long orderId = json.get("orderId").asLong();

            if (processedEventRepository.existsById(eventId)) {
                duplicateEventsCounter.increment();
                log.info("Duplicate event ignored: eventId={}, orderId={}", eventId, orderId);
                return;
            }

            log.info("Processing inventory event: eventId={}, orderId={}, success={}", eventId, orderId, success);

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