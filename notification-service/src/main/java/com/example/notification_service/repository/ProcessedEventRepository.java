package com.example.notification_service.repository;

import com.example.notification_service.model.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
    
    Optional<ProcessedEvent> findByEventId(String eventId);
    
    boolean existsByEventId(String eventId);
}
