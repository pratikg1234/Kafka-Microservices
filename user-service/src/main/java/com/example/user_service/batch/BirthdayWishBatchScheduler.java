package com.example.user_service.batch;

import com.example.user_service.model.entity.User;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.service.OutboxPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
public class BirthdayWishBatchScheduler {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private OutboxPublisher outboxPublisher;
    
    @Scheduled(cron = "0 0 6 * * ?") // 6 AM UTC daily
    public void sendBirthdayWisheDaily() {
        log.info("Starting scheduled batch job to send birthday wishes");
        try {
            List<User> birthdayUsers = userRepository.findUsersBirthdayToday();
            
            if (birthdayUsers.isEmpty()) {
                log.info("No users have birthday today");
                return;
            }
            
            log.info("Found {} users with birthday today", birthdayUsers.size());
            
            for (User user : birthdayUsers) {
                try {
                    outboxPublisher.publishBirthdayWishEvent(user.getUserId(), user.getEmail());
                } catch (Exception e) {
                    log.error("Failed to publish birthday wish event for user: {}", user.getUserId(), e);
                    // Continue with next user on error
                }
            }
            
            log.info("Birthday wish batch job completed successfully");
        } catch (Exception e) {
            log.error("Error occurred in birthday wish batch job", e);
        }
    }
}
