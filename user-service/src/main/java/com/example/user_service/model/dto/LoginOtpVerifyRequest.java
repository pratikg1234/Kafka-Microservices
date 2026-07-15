package com.example.user_service.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginOtpVerifyRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "OTP code is required")
    private String otpCode;
}
