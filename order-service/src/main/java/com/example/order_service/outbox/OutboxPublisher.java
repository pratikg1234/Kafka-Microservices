package com.example.order_service.outbox;

import com.example.order_service.model.entity.OutboxEvent;
import com.example.order_service.repository.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.annotation.Observed;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Counter eventsPublishedCounter;
    private final Counter eventsFailedCounter;
    private final Timer publishBatchTimer;

    public OutboxPublisher(OutboxEventRepository outboxRepository,
                           KafkaTemplate<String, String> kafkaTemplate,
                           MeterRegistry meterRegistry) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.eventsPublishedCounter = Counter.builder("outbox.events.published")
                .description("Total outbox events published to Kafka")
                .register(meterRegistry);
        this.eventsFailedCounter = Counter.builder("outbox.events.failed")
                .description("Total outbox events that failed to publish")
                .register(meterRegistry);
        this.publishBatchTimer = Timer.builder("outbox.publish.duration")
                .description("Time taken per outbox publish cycle")
                .register(meterRegistry);
        Gauge.builder("outbox.events.pending", outboxRepository,
                        repo -> repo.findTop10ByStatusOrderByCreatedAtAsc("NEW").size())
                .description("Current pending outbox events")
                .register(meterRegistry);
    }

    @Transactional
    @Scheduled(fixedDelay = 3000)
    @Observed(name = "outbox.publish", contextualName = "outbox-publish-cycle")
    public void publishEvents() {

        publishBatchTimer.record(() -> {
            List<OutboxEvent> events =
                    outboxRepository.findTop10ByStatusOrderByCreatedAtAsc("NEW");

            if (!events.isEmpty()) {
                log.info("Publishing {} outbox events", events.size());
            }

            for (OutboxEvent event : events) {
                try {
                    kafkaTemplate.send(
                            event.getEventType(),
                            event.getAggregateId().toString(),
                            event.getPayload()
                    ).get();

                    event.setStatus("SENT");
                    outboxRepository.save(event);
                    eventsPublishedCounter.increment();

                } catch (Exception e) {
                    log.error("Failed to publish event {}", event.getEventId(), e);
                    event.setStatus("FAILED");
                    outboxRepository.save(event);
                    eventsFailedCounter.increment();
                }
            }
        });
    }
}