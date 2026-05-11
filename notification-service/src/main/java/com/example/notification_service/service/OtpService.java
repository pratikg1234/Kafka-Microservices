package com.example.notification_service.service;

import com.example.notification_service.model.entity.OtpRecord;
import com.example.notification_service.repository.OtpRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Transactional
public class OtpService {

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${otp.expiry-minutes:2}")
    private int otpExpiryMinutes;

    @Value("${otp.max-attempts:3}")
    private int maxAttempts;

    @Value("${otp.max-per-hour:5}")
    private int maxPerHour;

    private static final int OTP_LENGTH = 6;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generateOtp(String userId, OtpRecord.OtpType type) {
        log.info("Generating OTP for user: {} with type: {}", userId, type);

        // Check rate limit
        String rateLimitKey = "otp:rate_limit:" + userId;
        String value = redisTemplate.opsForValue().get(rateLimitKey);
        long count = value != null ? Long.parseLong(value) : 0;

        if (count >= maxPerHour) {
            log.warn("Rate limit exceeded for user: {}", userId);
            throw new RuntimeException("Too many OTP requests. Please try again later.");
        }

        // Generate OTP
        String otpCode = generateRandomOtp();

        // Save OTP to database
        OtpRecord otpRecord = OtpRecord.builder()
                .userId(userId)
                .otpCode(otpCode)
                .otpType(type)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .verified(false)
                .verificationAttempts(0)
                .build();

        otpRepository.save(otpRecord);

        // Update rate limit in Redis
        if (count == 0) {
            redisTemplate.opsForValue().set(rateLimitKey, "1", 1, TimeUnit.HOURS);
        } else {
            redisTemplate.opsForValue().increment(rateLimitKey);
        }

        log.info("OTP generated successfully for user: {}, type: {}", userId, type);
        return otpCode;
    }

    public boolean verifyOtp(String userId, String otpCode) {
        log.info("Verifying OTP for user: {}", userId);

        OtpRecord otpRecord = otpRepository.findByUserIdAndOtpCode(userId, otpCode)
                .orElse(null);

        if (otpRecord == null) {
            log.warn("OTP not found or invalid for user: {}", userId);
            return false;
        }

        // Check if already verified
        if (otpRecord.getVerified()) {
            log.warn("OTP already used for user: {}", userId);
            return false;
        }

        // Increment attempts
        otpRecord.setVerificationAttempts(otpRecord.getVerificationAttempts() + 1);

        // Check if expired
        if (LocalDateTime.now().isAfter(otpRecord.getExpiresAt())) {
            otpRepository.save(otpRecord);
            log.warn("OTP expired for user: {}", userId);
            return false;
        }

        // Check attempts
        if (otpRecord.getVerificationAttempts() > maxAttempts) {
            otpRepository.save(otpRecord);
            log.warn("Max verification attempts exceeded for user: {}", userId);
            return false;
        }

        // Mark as verified
        otpRecord.setVerified(true);
        otpRepository.save(otpRecord);

        log.info("OTP verified successfully for user: {}", userId);
        return true;
    }

    public void cleanupExpiredOtps() {
        log.info("Cleaning up expired OTPs");
        otpRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("Expired OTPs cleanup completed");
    }

    private String generateRandomOtp() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(SECURE_RANDOM.nextInt(10));
        }
        return otp.toString();
    }
}
