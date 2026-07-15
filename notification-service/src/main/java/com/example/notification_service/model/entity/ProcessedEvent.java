package com.example.notification_service.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "processed_events", indexes = {
    @Index(name = "idx_event_id", columnList = "event_id", unique = true),
    @Index(name = "idx_processed_at", columnList = "processed_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEvent {
    
    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String processedEventId;
    
    @Column(nullable = false, unique = true, columnDefinition = "CHAR(36)")
    private String eventId; // Idempotency key
    
    @Column(nullable = false, length = 100)
    private String eventType;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime processedAt;
    
    @PrePersist
    private void generateId() {
        if (this.processedEventId == null) {
            this.processedEventId = UUID.randomUUID().toString();
        }
    }
}
