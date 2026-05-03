package com.example.order_service.outbox;

import com.example.order_service.model.entity.OutboxEvent;
import com.example.order_service.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    @Scheduled(fixedDelay = 3000)
    public void publishEvents() {

        List<OutboxEvent> events =
                outboxRepository.findTop10ByStatusOrderByCreatedAtAsc("NEW");

        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(
                        event.getEventType(),
                        event.getAggregateId().toString(),
                        event.getPayload()
                ).get();

                event.setStatus("SENT");
                outboxRepository.save(event);

            } catch (Exception e) {
                log.error("Failed to publish event {}", event.getEventId(), e);
                event.setStatus("FAILED");
                outboxRepository.save(event);
            }
        }
    }
}