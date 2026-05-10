package com.example.user_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetRequest {
    
    @NotNull(message = "User ID is required")
    private String userId;
    
    @NotBlank(message = "New password is required")
    private String newPassword;
}
