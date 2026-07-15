package com.example.notification_service.service;

import com.example.notification_service.model.dto.BirthdayWishEvent;
import com.example.notification_service.model.dto.OtpRequestedEvent;
import com.example.notification_service.model.dto.UserRegisteredEvent;
import com.example.notification_service.model.entity.OtpRecord;
import com.example.notification_service.repository.NotificationHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class NotificationService {

    @Autowired
    private OtpService otpService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TwilioSmsService smsService;

    @Autowired
    private NotificationHistoryRepository notificationHistoryRepository;

    @Value("${notification.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${notification.retry.initial-delay-ms:1000}")
    private long initialDelayMs;

    @Value("${notification.retry.max-delay-ms:10000}")
    private long maxDelayMs;

    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        log.info("Handling USER_REGISTERED event for user: {}", event.getUserId());
        
        String userId = event.getUserId();
        String email = event.getEmail();
        String username = event.getUsername() != null ? event.getUsername() : email.split("@")[0];
        
        executeWithRetry(() -> emailService.sendWelcomeEmail(email, username, userId),
                "Welcome email to " + email);
        
        log.info("Welcome notification sent successfully for user: {}", userId);
    }
    
    public void handleOtpRequestedEvent(OtpRequestedEvent event) {
        log.info("Handling OTP_REQUESTED event for user: {}", event.getUserId());

        String userId = event.getUserId();
        String email = event.getEmail();
        String phoneNumber = event.getPhoneNumber();
        String notificationType = event.getNotificationType();

        // Generate OTPs
        if ("BOTH".equalsIgnoreCase(notificationType) || "EMAIL_ONLY".equalsIgnoreCase(notificationType)) {
            String emailOtp = otpService.generateOtp(userId, OtpRecord.OtpType.email);
            executeWithRetry(() -> emailService.sendOtpEmail(email, emailOtp, userId), "OTP email to " + email);
        }

        if ("BOTH".equalsIgnoreCase(notificationType) || "PHONE_ONLY".equalsIgnoreCase(notificationType)) {
            String phoneOtp = otpService.generateOtp(userId, OtpRecord.OtpType.phone);
            executeWithRetry(() -> smsService.sendOtpSms(phoneNumber, phoneOtp, userId), "OTP SMS to " + phoneNumber);
        }

        log.info("OTP notification sent successfully for user: {}", userId);
    }

    public void handleBirthdayWishEvent(BirthdayWishEvent event) {
        log.info("Handling BIRTHDAY_WISH_REQUESTED event for user: {}", event.getUserId());

        String userId = event.getUserId();
        String email = event.getEmail();
        String username = event.getUsername() != null ? event.getUsername() : email.split("@")[0];
        int age = event.getAge() != null ? event.getAge() : 0;

        executeWithRetry(() -> emailService.sendBirthdayWishEmail(email, username, age, userId),
                "Birthday wish to " + email);

        log.info("Birthday wish sent successfully for user: {}", userId);
    }

    private void executeWithRetry(Runnable action, String description) {
        int attempt = 0;
        long delay = initialDelayMs;

        while (attempt < maxRetryAttempts) {
            try {
                action.run();
                return;
            } catch (Exception e) {
                attempt++;
                if (attempt >= maxRetryAttempts) {
                    log.error("Failed to execute '{}' after {} attempts", description, maxRetryAttempts, e);
                    throw e;
                }
                log.warn("Attempt {}/{} failed for '{}'. Retrying in {}ms...",
                        attempt, maxRetryAttempts, description, delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
                delay = Math.min(delay * 2, maxDelayMs);
            }
        }
    }
}
