package com.example.notification_service.controller;

import com.example.notification_service.model.dto.ApiResponse;
import com.example.notification_service.model.dto.NotificationHistoryResponse;
import com.example.notification_service.model.entity.NotificationHistory;
import com.example.notification_service.model.entity.OtpRecord;
import com.example.notification_service.repository.NotificationHistoryRepository;
import com.example.notification_service.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/notifications")
@Tag(name = "Notification Management", description = "APIs for OTP and notification management")
public class NotificationController {

    @Autowired
    private OtpService otpService;

    @Autowired
    private NotificationHistoryRepository notificationHistoryRepository;

    private static final int MAX_PAGE_SIZE = 20;

    // ========== OTP Endpoints ==========

    @PostMapping("/otp/generate")
    @Operation(summary = "Generate OTP for a user")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateOtp(
            @RequestParam String userId,
            @RequestParam String type) {
        log.info("Generate OTP request for user: {}, type: {}", userId, type);

        OtpRecord.OtpType otpType = OtpRecord.OtpType.valueOf(type.toLowerCase());
        otpService.generateOtp(userId, otpType);

        return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                .success(true)
                .message("OTP generated successfully")
                .data(Map.of("userId", userId, "type", type))
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Verify OTP for a user")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOtp(
            @RequestParam String userId,
            @RequestParam String otpCode) {
        log.info("Verify OTP request for user: {}", userId);

        boolean verified = otpService.verifyOtp(userId, otpCode);

        if (verified) {
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("OTP verified successfully")
                    .data(Map.of("verified", true))
                    .timestamp(LocalDateTime.now())
                    .build());
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("OTP verification failed. Invalid or expired code.")
                    .data(Map.of("verified", false))
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    // ========== Notification History Endpoints ==========

    @GetMapping("/history/{userId}")
    @Operation(summary = "Get notification history for a user")
    public ResponseEntity<ApiResponse<List<NotificationHistoryResponse>>> getNotificationHistory(
            @PathVariable String userId) {
        log.info("Fetching notification history for user: {}", userId);

        List<NotificationHistoryResponse> history = notificationHistoryRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.<List<NotificationHistoryResponse>>builder()
                .success(true)
                .message("Notification history retrieved successfully")
                .data(history)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/history/{userId}/status/{status}")
    @Operation(summary = "Get notification history by status")
    public ResponseEntity<ApiResponse<Page<NotificationHistoryResponse>>> getNotificationHistoryByStatus(
            @PathVariable String userId,
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching notification history for user: {} with status: {}", userId, status);

        int pageSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, pageSize);

        NotificationHistory.NotificationStatus notificationStatus =
                NotificationHistory.NotificationStatus.valueOf(status.toLowerCase());

        Page<NotificationHistoryResponse> history = notificationHistoryRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userId, notificationStatus, pageable)
                .map(this::mapToResponse);

        return ResponseEntity.ok(ApiResponse.<Page<NotificationHistoryResponse>>builder()
                .success(true)
                .message("Notification history retrieved successfully")
                .data(history)
                .timestamp(LocalDateTime.now())
                .build());
    }

    private NotificationHistoryResponse mapToResponse(NotificationHistory entity) {
        return NotificationHistoryResponse.builder()
                .notificationId(entity.getNotificationId())
                .userId(entity.getUserId())
                .notificationType(entity.getNotificationType().name())
                .recipient(entity.getRecipient())
                .status(entity.getStatus().name())
                .attempts(entity.getAttempts())
                .errorMessage(entity.getErrorMessage())
                .sentAt(entity.getSentAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
