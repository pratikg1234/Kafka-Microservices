package com.example.order_service.consumer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DLTConsumer {

    private final Counter dltMessagesCounter;

    public DLTConsumer(MeterRegistry meterRegistry) {
        this.dltMessagesCounter = Counter.builder("kafka.dlt.messages.received")
                .description("Dead letter topic messages received")
                .register(meterRegistry);
    }

    @KafkaListener(topics = "inventory.reserved.dlt", groupId = "order-dlt-group")
    public void handleReservedDLT(String message) {
        dltMessagesCounter.increment();
        log.error("DLT message received on inventory.reserved.dlt: {}", message);
    }

    @KafkaListener(topics = "inventory.failed.dlt", groupId = "order-dlt-group")
    public void handleFailedDLT(String message) {
        dltMessagesCounter.increment();
        log.error("DLT message received on inventory.failed.dlt: {}", message);
    }
}
