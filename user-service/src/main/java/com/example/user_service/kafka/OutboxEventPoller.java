package com.example.user_service.kafka;

import com.example.user_service.model.entity.OutboxEvent;
import com.example.user_service.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Transactional
    public void pollAndPublishUnpublishedEvents() {
        log.debug("Polling for unpublished outbox events");
        
        try {
            List<OutboxEvent> unpublishedEvents = outboxEventRepository.findByPublishedFalseOrderByCreatedAtAsc();
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
        
        try {
            ObjectNode messageNode = objectMapper.createObjectNode();
            messageNode.put("eventId", event.getEventId());
            messageNode.put("eventType", event.getEventType().name());
            messageNode.put("aggregateId", event.getAggregateId());
            messageNode.set("payload", objectMapper.readTree(event.getPayload()));
            messageNode.put("timestamp", LocalDateTime.now().toString());
            
            String message = objectMapper.writeValueAsString(messageNode);
            kafkaTemplate.send(KAFKA_TOPIC, event.getAggregateId(), message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize outbox event: " + event.getEventId(), e);
        }
    }
}
