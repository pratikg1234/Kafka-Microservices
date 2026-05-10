package com.example.notification_service.service;

import com.example.notification_service.model.dto.BirthdayWishEvent;
import com.example.notification_service.model.dto.OtpRequestedEvent;
import com.example.notification_service.model.entity.NotificationHistory;
import com.example.notification_service.model.entity.OtpRecord;
import com.example.notification_service.repository.NotificationHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    public void handleOtpRequestedEvent(OtpRequestedEvent event) {
        log.info("Handling OTP_REQUESTED event for user: {}", event.getUserId());
        
        try {
            String userId = event.getUserId();
            String email = event.getEmail();
            String phoneNumber = event.getPhoneNumber();
            String notificationType = event.getNotificationType();
            
            // Generate OTPs
            String emailOtp = otpService.generateOtp(userId, OtpRecord.OtpType.email);
            String phoneOtp = otpService.generateOtp(userId, OtpRecord.OtpType.phone);
            
            // Send based on notification type
            if ("BOTH".equalsIgnoreCase(notificationType) || "EMAIL_ONLY".equalsIgnoreCase(notificationType)) {
                emailService.sendOtpEmail(email, emailOtp, userId);
            }
            
            if ("BOTH".equalsIgnoreCase(notificationType) || "PHONE_ONLY".equalsIgnoreCase(notificationType)) {
                smsService.sendOtpSms(phoneNumber, phoneOtp, userId);
            }
            
            log.info("OTP notification sent successfully for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to handle OTP_REQUESTED event for user: {}", event.getUserId(), e);
            // Don't throw - log for now, retry mechanism can be added later
        }
    }
    
    public void handleBirthdayWishEvent(BirthdayWishEvent event) {
        log.info("Handling BIRTHDAY_WISH_REQUESTED event for user: {}", event.getUserId());
        
        try {
            String userId = event.getUserId();
            String email = event.getEmail();
            
            // Extract username from email (before @)
            String username = email.split("@")[0];
            
            // Calculate age based on current date (in production, should come from user service)
            int age = calculateAgeFromEvent(event);
            
            // Send birthday email
            emailService.sendBirthdayWishEmail(email, username, age, userId);
            
            log.info("Birthday wish sent successfully for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to handle BIRTHDAY_WISH_REQUESTED event for user: {}", event.getUserId(), e);
        }
    }
    
    private int calculateAgeFromEvent(BirthdayWishEvent event) {
        // In a real scenario, age would be passed in the event or calculated from DOB
        // For now, returning 0 as a placeholder
        return 0;
    }
}
