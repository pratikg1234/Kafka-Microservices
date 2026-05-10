package com.example.notification_service.service;

import com.example.notification_service.model.entity.NotificationHistory;
import com.example.notification_service.repository.NotificationHistoryRepository;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
public class TwilioSmsService {
    
    @Autowired
    private NotificationHistoryRepository notificationHistoryRepository;
    
    @Value("${twilio.account-sid}")
    private String accountSid;
    
    @Value("${twilio.auth-token}")
    private String authToken;
    
    @Value("${twilio.phone-number}")
    private String twilioPhoneNumber;

    public void sendOtpSms(String phoneNumber, String otpCode, String userId) {
        log.info("Sending OTP SMS to: {}", phoneNumber);

        try {
            Twilio.init(accountSid, authToken);

            String messageBody =
                    "Your OTP code is: " + otpCode +
                            ". Valid for 2 minutes. Do not share this code with anyone.";

            Message message = Message.creator(
                    new PhoneNumber(phoneNumber),      // To
                    new PhoneNumber(twilioPhoneNumber),// From
                    messageBody
            ).create();

            logNotification(
                    userId,
                    phoneNumber,
                    NotificationHistory.NotificationType.otp_sms,
                    NotificationHistory.NotificationStatus.sent
            );

            log.info(
                    "OTP SMS sent successfully to: {} with SID: {}",
                    phoneNumber,
                    message.getSid()
            );

        } catch (Exception e) {
            log.error("Failed to send OTP SMS to: {}", phoneNumber, e);

            logNotification(
                    userId,
                    phoneNumber,
                    NotificationHistory.NotificationType.otp_sms,
                    NotificationHistory.NotificationStatus.failed
            );

            throw new RuntimeException("Failed to send OTP SMS", e);
        }
    }
    
    private void logNotification(String userId, String recipient, 
                               NotificationHistory.NotificationType type,
                               NotificationHistory.NotificationStatus status) {
        NotificationHistory history = NotificationHistory.builder()
                .userId(userId)
                .recipient(recipient)
                .notificationType(type)
                .status(status)
                .attempts(1)
                .sentAt(status == NotificationHistory.NotificationStatus.sent ? LocalDateTime.now() : null)
                .build();
        
        notificationHistoryRepository.save(history);
    }
}
