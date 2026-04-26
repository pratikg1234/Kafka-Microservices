package com.example.order_service.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DLTConsumer {

    @KafkaListener(topics = "inventory.reserved.dlt", groupId = "order-dlt-group")
    public void handleReservedDLT(String message) {
        log.error("DLT (reserved) message: {}", message);
    }

    @KafkaListener(topics = "inventory.failed.dlt", groupId = "order-dlt-group")
    public void handleFailedDLT(String message) {
        log.error("DLT (failed) message: {}", message);
    }
}
