package com.example.user_service.repository;

import com.example.user_service.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByPhoneNumber(String phoneNumber);
    
    Page<User> findByAccountStatus(User.AccountStatus status, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE MONTH(u.dateOfBirth) = MONTH(CURDATE()) AND DAY(u.dateOfBirth) = DAY(CURDATE()) AND u.accountStatus = 'ACTIVE'")
    List<User> findUsersBirthdayToday();
    
    @Query("SELECT u FROM User u WHERE u.accountStatus = 'ACTIVE'")
    List<User> findAllActiveUsers(Pageable pageable);
    
    long countByAccountStatus(User.AccountStatus status);
}
