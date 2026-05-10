package com.example.notification_service.service;

import com.example.notification_service.model.entity.OtpRecord;
import com.example.notification_service.repository.OtpRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Transactional
public class OtpService {
    
    @Autowired
    private OtpRepository otpRepository;
    
//    @Autowired
//    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final int OTP_EXPIRY_MINUTES = 2;
    private static final int OTP_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 10;
    private static final int MAX_PER_HOUR = 20;
    
    public String generateOtp(String userId, OtpRecord.OtpType type) {
        log.info("Generating OTP for user: {} with type: {}", userId, type);
        
        // Check rate limit
        String rateLimitKey = "otp:rate_limit:" + userId;
        String value = redisTemplate.opsForValue().get(rateLimitKey);
//        Long count = (Long) redisTemplate.opsForValue().get(rateLimitKey);
//
//        if (count != null && count >= MAX_PER_HOUR) {
//            log.warn("Rate limit exceeded for user: {}", userId);
//            throw new RuntimeException("Too many OTP requests. Please try again later.");
//        }

        long count = value != null ? Long.parseLong(value) : 0;

        if (count >= MAX_PER_HOUR) {
            log.warn("Rate limit exceeded for user: {}", userId);
            throw new RuntimeException("Too many OTP requests");
        }
        
        // Generate OTP
        String otpCode = generateRandomOtp();
        
        // Save OTP to database
        OtpRecord otpRecord = OtpRecord.builder()
                .userId(userId)
                .otpCode(otpCode)
                .otpType(type)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .verified(false)
                .verificationAttempts(0)
                .build();
        
        otpRepository.save(otpRecord);
        
        // Update rate limit in Redis
//        if (count == null) {
//            redisTemplate.opsForValue().set(rateLimitKey, 1L, 1, TimeUnit.HOURS);
//        } else {
//            redisTemplate.opsForValue().increment(rateLimitKey);
//        }

        if (count == 0) {
            redisTemplate.opsForValue()
                    .set(rateLimitKey, "1", 1, TimeUnit.HOURS);
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
        
        // Check if expired
        if (LocalDateTime.now().isAfter(otpRecord.getExpiresAt())) {
            log.warn("OTP expired for user: {}", userId);
            return false;
        }
        
        // Check attempts
        if (otpRecord.getVerificationAttempts() >= MAX_ATTEMPTS) {
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
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }
}
