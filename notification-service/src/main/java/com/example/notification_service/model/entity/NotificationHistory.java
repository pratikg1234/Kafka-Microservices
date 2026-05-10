package com.example.notification_service.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_history", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationHistory {
    
    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String notificationId;
    
    @Column(nullable = false, columnDefinition = "CHAR(36)")
    private String userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private NotificationType notificationType;
    
    @Column(nullable = false, length = 150)
    private String recipient; // Email or phone number
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;
    
    @Column(nullable = false)
    private Integer attempts;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column
    private LocalDateTime sentAt;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    private void generateId() {
        if (this.notificationId == null) {
            this.notificationId = UUID.randomUUID().toString();
        }
        if (this.attempts == null) {
            this.attempts = 0;
        }
    }
    
    public enum NotificationType {
        otp_email, otp_sms, birthday_wish, general
    }
    
    public enum NotificationStatus {
        sent, failed, pending, retry
    }
}
