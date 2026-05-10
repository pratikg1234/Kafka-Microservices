package com.example.user_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    
    private String userId;
    private String username;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private Integer age;
    private Integer accountAge;
    private String accountStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
