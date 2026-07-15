package com.example.user_service.repository;

import com.example.user_service.model.entity.UserAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, String> {
    
    List<UserAuditLog> findByUserIdOrderByTimestampDesc(String userId);
    
    List<UserAuditLog> findByUserIdAndAction(String userId, UserAuditLog.AuditAction action);
}
