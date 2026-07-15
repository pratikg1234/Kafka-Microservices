package com.example.notification_service.repository;

import com.example.notification_service.model.entity.NotificationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, String> {
    
    List<NotificationHistory> findByUserIdOrderByCreatedAtDesc(String userId);
    
    Page<NotificationHistory> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, NotificationHistory.NotificationStatus status, Pageable pageable);
    
    long countByUserIdAndNotificationType(String userId, NotificationHistory.NotificationType type);
}
