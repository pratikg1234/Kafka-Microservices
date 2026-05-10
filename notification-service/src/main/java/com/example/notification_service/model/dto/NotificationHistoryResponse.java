package com.example.notification_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationHistoryResponse {
    
    private String notificationId;
    private String userId;
    private String notificationType;
    private String recipient;
    private String status;
    private Integer attempts;
    private String errorMessage;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
