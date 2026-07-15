package com.example.user_service.service;

import com.example.user_service.model.entity.OutboxEvent;
import com.example.user_service.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@Transactional
public class OutboxPublisher {
    
    @Autowired
    private OutboxEventRepository outboxEventRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public void publishUserRegisteredEvent(String userId, String username, String email) {
        log.info("Publishing USER_REGISTERED event for user: {}", userId);
        
        Map<String, String> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("username", username);
        payload.put("email", email);
        
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .eventType(OutboxEvent.EventType.USER_REGISTERED)
                    .aggregateId(userId)
                    .payload(objectMapper.writeValueAsString(payload))
                    .published(false)
                    .build();
            outboxEventRepository.save(event);
            log.info("USER_REGISTERED event saved to outbox for user: {}", userId);
        } catch (Exception e) {
            log.error("Error publishing USER_REGISTERED event for user: {}", userId, e);
            throw new RuntimeException("Failed to publish USER_REGISTERED event", e);
        }
    }
    
    public void publishOtpRequestedEvent(String userId, String email, String phoneNumber) {
        log.info("Publishing OTP_REQUESTED event for user: {}", userId);
        
        Map<String, String> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("email", email);
        payload.put("phoneNumber", phoneNumber);
        payload.put("notificationType", "BOTH"); // Send both EMAIL and SMS OTP
        
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .eventType(OutboxEvent.EventType.OTP_REQUESTED)
                    .aggregateId(userId)
                    .payload(objectMapper.writeValueAsString(payload))
                    .published(false)
                    .build();
            outboxEventRepository.save(event);

            log.info("OTP_REQUESTED event saved to outbox for user: {}", userId);
        } catch (Exception e) {
            log.error("Error publishing OTP_REQUESTED event for user: {}", userId, e);
            throw new RuntimeException("Failed to publish OTP_REQUESTED event", e);
        }
    }
    
    public void publishPasswordResetRequestedEvent(String userId, String email, String phoneNumber) {
        log.info("Publishing PASSWORD_RESET_REQUESTED event for user: {}", userId);
        
        Map<String, String> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("email", email);
        payload.put("phoneNumber", phoneNumber);
        payload.put("notificationType", "BOTH");
        
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .eventType(OutboxEvent.EventType.PASSWORD_RESET_REQUESTED)
                    .aggregateId(userId)
                    .payload(objectMapper.writeValueAsString(payload))
                    .published(false)
                    .build();
            
            outboxEventRepository.save(event);
            log.info("PASSWORD_RESET_REQUESTED event saved to outbox for user: {}", userId);
        } catch (Exception e) {
            log.error("Error publishing PASSWORD_RESET_REQUESTED event for user: {}", userId, e);
            throw new RuntimeException("Failed to publish PASSWORD_RESET_REQUESTED event", e);
        }
    }
    
    public void publishBirthdayWishEvent(String userId, String email, String username, LocalDate dateOfBirth) {
        log.info("Publishing BIRTHDAY_WISH_REQUESTED event for user: {}", userId);
        
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("email", email);
        payload.put("username", username);
        payload.put("age", age);
        
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .eventType(OutboxEvent.EventType.BIRTHDAY_WISH_REQUESTED)
                    .aggregateId(userId)
                    .payload(objectMapper.writeValueAsString(payload))
                    .published(false)
                    .build();
            
            outboxEventRepository.save(event);
            log.info("BIRTHDAY_WISH_REQUESTED event saved to outbox for user: {}", userId);
        } catch (Exception e) {
            log.error("Error publishing BIRTHDAY_WISH_REQUESTED event for user: {}", userId, e);
            throw new RuntimeException("Failed to publish BIRTHDAY_WISH_REQUESTED event", e);
        }
    }
}
