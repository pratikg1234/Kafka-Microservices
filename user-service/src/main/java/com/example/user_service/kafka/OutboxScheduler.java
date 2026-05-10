package com.example.user_service.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OutboxScheduler {
    
    @Autowired
    private OutboxEventPoller outboxEventPoller;
    
    @Scheduled(fixedDelay = 5000) // Run every 5 seconds
    public void pollAndPublish() {
        log.debug("Running outbox polling scheduler");
        outboxEventPoller.pollAndPublishUnpublishedEvents();
    }
}
