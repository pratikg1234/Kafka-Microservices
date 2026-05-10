package com.example.user_service.controller;

import com.example.user_service.model.dto.*;
import com.example.user_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/v1/users")
@Tag(name = "User Management", description = "APIs for user management")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<UserResponse>> registerUser(
            @Valid @RequestBody CreateUserRequest request,
            HttpServletRequest httpRequest) {
        log.info("Register user request received for username: {}", request.getUsername());
        
        String ipAddress = getClientIpAddress(httpRequest);
        UserResponse userResponse = userService.createUser(request, ipAddress);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<UserResponse>builder()
                        .success(true)
                        .message("User registered successfully. OTP has been sent to email and phone.")
                        .data(userResponse)
                        .timestamp(LocalDateTime.now())
                        .correlationId(MDC.get("correlationId"))
                        .build());
    }
    
    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable String userId) {
        log.info("Get user request for userId: {}", userId);
        
        UserResponse userResponse = userService.getUserById(userId);
        
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("User retrieved successfully")
                .data(userResponse)
                .timestamp(LocalDateTime.now())
                .correlationId(MDC.get("correlationId"))
                .build());
    }
    
    @GetMapping
    @Operation(summary = "Get all users with pagination")
    public ResponseEntity<ApiResponse<PaginatedResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "20") int pageSize) {
        log.info("Get all users request - page: {}, size: {}", pageNumber, pageSize);
        
        PaginatedResponse<UserResponse> response = userService.getAllUsers(pageNumber, pageSize);
        
        return ResponseEntity.ok(ApiResponse.<PaginatedResponse<UserResponse>>builder()
                .success(true)
                .message("Users retrieved successfully")
                .data(response)
                .timestamp(LocalDateTime.now())
                .correlationId(MDC.get("correlationId"))
                .build());
    }
    
    @PutMapping("/{userId}")
    @Operation(summary = "Update user details")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRequest request,
            HttpServletRequest httpRequest) {
        log.info("Update user request for userId: {}", userId);
        
        String ipAddress = getClientIpAddress(httpRequest);
        UserResponse userResponse = userService.updateUser(userId, request, ipAddress);
        
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("User updated successfully. OTP has been sent for verification if email or phone was changed.")
                .data(userResponse)
                .timestamp(LocalDateTime.now())
                .correlationId(MDC.get("correlationId"))
                .build());
    }
    
    @PostMapping("/{userId}/reset-password")
    @Operation(summary = "Request password reset with OTP")
    public ResponseEntity<ApiResponse<Object>> requestPasswordReset(
            @PathVariable String userId,
            HttpServletRequest httpRequest) {
        log.info("Password reset request for userId: {}", userId);
        
        String ipAddress = getClientIpAddress(httpRequest);
        userService.resetPassword(userId, "", ipAddress);
        
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Password reset OTP has been sent to your email and phone.")
                .timestamp(LocalDateTime.now())
                .correlationId(MDC.get("correlationId"))
                .build());
    }
    
    @PostMapping("/{userId}/confirm-password-reset")
    @Operation(summary = "Confirm password reset with new password")
    public ResponseEntity<ApiResponse<Object>> confirmPasswordReset(
            @PathVariable String userId,
            @Valid @RequestBody PasswordResetRequest request) {
        log.info("Confirm password reset for userId: {}", userId);
        
        userService.confirmPasswordReset(userId, request.getNewPassword());
        
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Password reset successfully")
                .timestamp(LocalDateTime.now())
                .correlationId(MDC.get("correlationId"))
                .build());
    }
    
    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete a user account")
    public ResponseEntity<ApiResponse<Object>> deleteUser(
            @PathVariable String userId,
            HttpServletRequest httpRequest) {
        log.info("Delete user request for userId: {}", userId);
        
        String ipAddress = getClientIpAddress(httpRequest);
        userService.deleteUser(userId, ipAddress);
        
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("User deleted successfully")
                .timestamp(LocalDateTime.now())
                .correlationId(MDC.get("correlationId"))
                .build());
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0];
        }
        return request.getRemoteAddr();
    }
}
