package com.example.user_service.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_published", columnList = "published"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_aggregate_id", columnList = "aggregate_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {
    
    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String eventId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private EventType eventType;
    
    @Column(nullable = false, columnDefinition = "CHAR(36)")
    private String aggregateId; // Usually userId
    
    @Column(columnDefinition = "JSON", nullable = false)
    private String payload;
    
    @Column(nullable = false)
    private Boolean published;
    
    @Column
    private LocalDateTime publishedAt;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    private void generateId() {
        if (this.eventId == null) {
            this.eventId = UUID.randomUUID().toString();
        }
        if (this.published == null) {
            this.published = false;
        }
    }
    
    public enum EventType {
        OTP_REQUESTED, USER_REGISTERED, PASSWORD_RESET_REQUESTED, BIRTHDAY_WISH_REQUESTED
    }
}
