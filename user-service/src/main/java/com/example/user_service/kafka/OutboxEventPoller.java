package com.example.user_service.kafka;

import com.example.user_service.model.entity.OutboxEvent;
import com.example.user_service.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class OutboxEventPoller {
    
    private static final String KAFKA_TOPIC = "user-events";
    
    @Autowired
    private OutboxEventRepository outboxEventRepository;
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Transactional
    public void pollAndPublishUnpublishedEvents() {
        log.debug("Polling for unpublished outbox events");
        
        try {

            log.info("trying to findby published false method again");
            List<OutboxEvent> unpublishedEvents = outboxEventRepository.findByPublishedFalseOrderByCreatedAtAsc();
            log.info("pass!!");
            if (unpublishedEvents.isEmpty()) {
                log.debug("No unpublished events found");
                return;
            }
            
            log.info("Found {} unpublished events", unpublishedEvents.size());
            
            for (OutboxEvent event : unpublishedEvents) {
                try {
                    publishEventToKafka(event);
                    
                    // Mark as published
                    event.setPublished(true);
                    event.setPublishedAt(LocalDateTime.now());
                    log.info("showing event type = {}", event.getEventType());
                    outboxEventRepository.save(event);
                    
                    log.info("Event published to Kafka - EventId: {}, Type: {}", event.getEventId(), event.getEventType());
                } catch (Exception e) {
                    log.error("Failed to publish event to Kafka - EventId: {}, Error: {}", event.getEventId(), e.getMessage());
                    // Continue with next event (skip on error)
                }
            }
        } catch (Exception e) {
            log.error("Error occurred while polling outbox events", e);
        }
    }
    
    private void publishEventToKafka(OutboxEvent event) {
        log.debug("Publishing event to Kafka - EventId: {}, Type: {}", event.getEventId(), event.getEventType());
        
        String message = String.format(
                "{\"eventId\":\"%s\",\"eventType\":\"%s\",\"aggregateId\":\"%s\",\"payload\":%s,\"timestamp\":\"%s\"}",
                event.getEventId(),
                event.getEventType(),
                event.getAggregateId(),
                event.getPayload(),
                LocalDateTime.now()
        );
        
        kafkaTemplate.send(KAFKA_TOPIC, event.getAggregateId(), message);
    }
}
