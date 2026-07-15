package com.example.notification_service.repository;

import com.example.notification_service.model.entity.OtpRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpRecord, String> {
    
    Optional<OtpRecord> findByUserIdAndOtpCode(String userId, String otpCode);
    
    List<OtpRecord> findByUserIdAndVerifiedFalse(String userId);
    
    List<OtpRecord> findByUserIdAndExpiresAtBefore(String userId, LocalDateTime now);
    
    long countByUserIdAndCreatedAtAfter(String userId, LocalDateTime startTime);
    
    @Modifying
    @Transactional
    void deleteByExpiresAtBefore(LocalDateTime now);
}
