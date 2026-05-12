package com.example.user_service.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_audit_log", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAuditLog {
    
    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String auditId;
    
    @Column(name = "user_id", nullable = false, columnDefinition = "CHAR(36)")
    private String userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditAction action;
    
    @Column(columnDefinition = "JSON")
    private String changedFields; // JSON string of changed fields
    
    @Column(nullable = false)
    private String ipAddress;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
    
    @PrePersist
    private void generateId() {
        if (this.auditId == null) {
            this.auditId = UUID.randomUUID().toString();
        }
    }
    
    public enum AuditAction {
        CREATED, UPDATED, DELETED, LOGIN, LOGIN_FAILED, LOGIN_OTP_VERIFIED, OTP_REQUESTED, PASSWORD_RESET_REQUESTED, PASSWORD_RESET_CONFIRMED, EMAIL_UPDATED, PHONE_UPDATED
    }
}
