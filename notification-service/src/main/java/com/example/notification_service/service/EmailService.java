package com.example.notification_service.service;

import com.example.notification_service.model.entity.NotificationHistory;
import com.example.notification_service.repository.NotificationHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private NotificationHistoryRepository notificationHistoryRepository;
    
    @Value("${notification.from-email}")
    private String fromEmail;
    
    public void sendOtpEmail(String toEmail, String otpCode, String userId) {
        log.info("Sending OTP email to: {}", toEmail);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your OTP Code");
            
            String htmlContent = buildOtpEmailTemplate(otpCode);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
            // Log in notification history
            logNotification(userId, toEmail, NotificationHistory.NotificationType.otp_email,
                          NotificationHistory.NotificationStatus.sent);
            
            log.info("OTP email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", toEmail, e);
            logNotification(userId, toEmail, NotificationHistory.NotificationType.otp_email,
                          NotificationHistory.NotificationStatus.failed);
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }
    
    public void sendBirthdayWishEmail(String toEmail, String username, int age, String userId) {
        log.info("Sending birthday wish email to: {}", toEmail);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Happy Birthday!");
            
            String htmlContent = buildBirthdayEmailTemplate(username, age);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
            // Log in notification history
            logNotification(userId, toEmail, NotificationHistory.NotificationType.birthday_wish,
                          NotificationHistory.NotificationStatus.sent);
            
            log.info("Birthday wish email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send birthday wish email to: {}", toEmail, e);
            logNotification(userId, toEmail, NotificationHistory.NotificationType.birthday_wish,
                          NotificationHistory.NotificationStatus.failed);
            throw new RuntimeException("Failed to send birthday wish email", e);
        }
    }
    
    private String buildOtpEmailTemplate(String otpCode) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<h2>Your OTP Code</h2>" +
                "<p>Your One-Time Password is:</p>" +
                "<h1 style='color: #007bff; font-size: 32px;'>" + otpCode + "</h1>" +
                "<p>This code is valid for 2 minutes.</p>" +
                "<p>If you did not request this code, please ignore this email.</p>" +
                "<hr>" +
                "<p style='color: #666; font-size: 12px;'>Do not share this code with anyone.</p>" +
                "</body>" +
                "</html>";
    }
    
    private String buildBirthdayEmailTemplate(String username, int age) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<h2>🎉 Happy Birthday, " + username + "! 🎉</h2>" +
                "<p>Wishing you a wonderful birthday filled with joy, laughter, and happiness!</p>" +
                "<p>Today marks your special day as you turn <strong>" + age + "</strong> years young.</p>" +
                "<p>May this year bring you endless opportunities and amazing memories.</p>" +
                "<hr>" +
                "<p style='color: #666; font-size: 12px;'>Thank you for being with us!</p>" +
                "</body>" +
                "</html>";
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
