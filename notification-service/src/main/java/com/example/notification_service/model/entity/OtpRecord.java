package com.example.notification_service.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "otp_records", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpRecord {
    
    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String otpId;
    
    @Column(nullable = false, columnDefinition = "CHAR(36)")
    private String userId;
    
    @Column(nullable = false, length = 10)
    private String otpCode;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OtpType otpType;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(nullable = false)
    private Boolean verified;
    
    @Column(nullable = false)
    private Integer verificationAttempts;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    private void generateId() {
        if (this.otpId == null) {
            this.otpId = UUID.randomUUID().toString();
        }
        if (this.verified == null) {
            this.verified = false;
        }
        if (this.verificationAttempts == null) {
            this.verificationAttempts = 0;
        }
    }
    
    public enum OtpType {
        email, phone
    }
}
