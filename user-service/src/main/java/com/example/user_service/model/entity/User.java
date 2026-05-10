package com.example.user_service.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String userId;
    
    @Column(unique = true, nullable = false, length = 100)
    private String username;
    
    @Column(nullable = false, length = 255)
    private String password;
    
    @Column(nullable = false, length = 150)
    private String email;
    
    @Column(nullable = false, length = 20)
    private String phoneNumber;
    
    @Column(nullable = false)
    private LocalDate dateOfBirth;
    
    @Column(nullable = false)
    private Integer accountAge; // Days since account creation
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus accountStatus;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    private void generateId() {
        if (this.userId == null) {
            this.userId = UUID.randomUUID().toString();
        }
        if (this.accountStatus == null) {
            this.accountStatus = AccountStatus.ACTIVE;
        }
    }
    
    public enum AccountStatus {
        ACTIVE, INACTIVE, DELETED
    }
}
