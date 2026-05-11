package com.example.notification_service.scheduler;

import com.example.notification_service.service.OtpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OtpCleanupScheduler {

    @Autowired
    private OtpService otpService;

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void cleanupExpiredOtps() {
        log.debug("Running scheduled OTP cleanup");
        try {
            otpService.cleanupExpiredOtps();
        } catch (Exception e) {
            log.error("Error during OTP cleanup", e);
        }
    }
}
