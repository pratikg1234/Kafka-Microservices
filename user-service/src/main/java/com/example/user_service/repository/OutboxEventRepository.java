package com.example.user_service.repository;

import com.example.user_service.model.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    
    List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc();
    
    List<OutboxEvent> findByPublishedFalseAndEventTypeOrderByCreatedAtAsc(OutboxEvent.EventType eventType);
    
    long countByPublishedFalse();
}
