package com.example.user_service.batch;

import com.example.user_service.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AccountAgeBatchScheduler {
    
    @Autowired
    private UserService userService;
    
    @Scheduled(cron = "0 0 2 * * ?") // 2 AM UTC daily
    public void updateAccountAgeDaily() {
        log.info("Starting scheduled batch job to update account age for all users");
        try {
            userService.updateAccountAgeForAllUsers();
            log.info("Account age batch job completed successfully");
        } catch (Exception e) {
            log.error("Error occurred in account age batch job", e);
        }
    }
}
