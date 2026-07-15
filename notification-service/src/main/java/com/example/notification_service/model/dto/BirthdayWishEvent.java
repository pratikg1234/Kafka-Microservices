package com.example.notification_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BirthdayWishEvent {
    
    private String eventId;
    private String eventType;
    private String aggregateId; // userId
    private String userId;
    private String email;
    private String username;
    private Integer age;
    private Long timestamp;
}
