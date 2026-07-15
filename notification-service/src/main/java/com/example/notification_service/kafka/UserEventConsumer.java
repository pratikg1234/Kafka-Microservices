package com.example.notification_service.kafka;

import com.example.notification_service.model.dto.OtpRequestedEvent;
import com.example.notification_service.model.dto.BirthdayWishEvent;
import com.example.notification_service.model.dto.UserRegisteredEvent;
import com.example.notification_service.model.entity.ProcessedEvent;
import com.example.notification_service.repository.ProcessedEventRepository;
import com.example.notification_service.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class UserEventConsumer {
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private ProcessedEventRepository processedEventRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static final String KAFKA_TOPIC = "user-events";
    
    @KafkaListener(topics = KAFKA_TOPIC, groupId = "notification-service-group")
    @Transactional
    public void consumeUserEvent(String message) {
        log.debug("Consuming message from kafka: {}", message);
        
        try {
            // Parse message to determine event type
            var messageJson = objectMapper.readTree(message);
            String eventId = messageJson.get("eventId").asText();
            String eventType = messageJson.get("eventType").asText();
            
            // Check for duplicate (idempotency)
            if (processedEventRepository.existsByEventId(eventId)) {
                log.warn("Event already processed: {}", eventId);
                return;
            }
            
            switch (eventType) {
                case "USER_REGISTERED" -> handleUserRegisteredEvent(message, eventId);
                case "OTP_REQUESTED", "PASSWORD_RESET_REQUESTED" -> handleOtpRequestedEvent(message, eventId);
                case "BIRTHDAY_WISH_REQUESTED" -> handleBirthdayWishEvent(message, eventId);
                default -> log.warn("Unknown event type: {}", eventType);
            }
            
        } catch (Exception e) {
            log.error("Error processing Kafka message. EventId extraction attempted. Message: {}", message, e);
            throw e; // Re-throw to trigger Kafka retry/DLQ mechanism
        }
    }
    
    private void handleUserRegisteredEvent(String message, String eventId) throws Exception {
        log.info("Processing USER_REGISTERED event: {}", eventId);
        
        var messageJson = objectMapper.readTree(message);
        String payloadStr = messageJson.get("payload").toString();
        var payloadJson = objectMapper.readTree(payloadStr);
        
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .eventId(eventId)
                .eventType(messageJson.get("eventType").asText())
                .userId(messageJson.get("aggregateId").asText())
                .username(payloadJson.get("username").asText())
                .email(payloadJson.get("email").asText())
                .build();
        
        notificationService.handleUserRegisteredEvent(event);
        
        markEventAsProcessed(eventId, "USER_REGISTERED");
    }
    
    private void handleOtpRequestedEvent(String message, String eventId) throws Exception {
        log.info("Processing OTP_REQUESTED event: {}", eventId);
        
        var messageJson = objectMapper.readTree(message);
        String payloadStr = messageJson.get("payload").toString();
        
        OtpRequestedEvent event = OtpRequestedEvent.builder()
                .eventId(eventId)
                .eventType(messageJson.get("eventType").asText())
                .userId(messageJson.get("aggregateId").asText())
                .email(objectMapper.readTree(payloadStr).get("email").asText())
                .phoneNumber(objectMapper.readTree(payloadStr).get("phoneNumber").asText())
                .notificationType(objectMapper.readTree(payloadStr).get("notificationType").asText())
                .build();
        
        notificationService.handleOtpRequestedEvent(event);
        
        // Mark as processed
        markEventAsProcessed(eventId, "OTP_REQUESTED");
    }
    
    private void handleBirthdayWishEvent(String message, String eventId) throws Exception {
        log.info("Processing BIRTHDAY_WISH_REQUESTED event: {}", eventId);
        
        var messageJson = objectMapper.readTree(message);
        String payloadStr = messageJson.get("payload").toString();
        var payloadJson = objectMapper.readTree(payloadStr);
        
        BirthdayWishEvent event = BirthdayWishEvent.builder()
                .eventId(eventId)
                .eventType(messageJson.get("eventType").asText())
                .userId(messageJson.get("aggregateId").asText())
                .email(payloadJson.get("email").asText())
                .username(payloadJson.has("username") ? payloadJson.get("username").asText() : null)
                .age(payloadJson.has("age") ? payloadJson.get("age").asInt() : null)
                .build();
        
        notificationService.handleBirthdayWishEvent(event);
        
        // Mark as processed
        markEventAsProcessed(eventId, "BIRTHDAY_WISH_REQUESTED");
    }
    
    private void markEventAsProcessed(String eventId, String eventType) {
        ProcessedEvent processedEvent = ProcessedEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .build();
        
        processedEventRepository.save(processedEvent);
        log.info("Event marked as processed: {}", eventId);
    }
}
