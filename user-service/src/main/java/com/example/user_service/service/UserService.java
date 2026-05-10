package com.example.user_service.service;

import com.example.user_service.model.dto.CreateUserRequest;
import com.example.user_service.model.dto.UpdateUserRequest;
import com.example.user_service.model.dto.UserResponse;
import com.example.user_service.model.dto.PaginatedResponse;
import com.example.user_service.model.entity.User;
import com.example.user_service.model.entity.UserAuditLog;
import com.example.user_service.exception.UserAlreadyExistsException;
import com.example.user_service.exception.ResourceNotFoundException;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.repository.UserAuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;

@Slf4j
@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserAuditLogRepository auditLogRepository;
    
    @Autowired
    private OutboxPublisher outboxPublisher;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    public UserResponse createUser(CreateUserRequest request, String ipAddress) {
        log.info("Creating new user with username: {}", request.getUsername());
        
        // Check if user already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException("Username already exists: " + request.getUsername());
        }
        
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Email already registered: " + request.getEmail());
        }
        
        if (userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new UserAlreadyExistsException("Phone number already registered: " + request.getPhoneNumber());
        }
        
        // Create user
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .dateOfBirth(request.getDateOfBirth())
                .accountAge(0)
                .build();
        
        User savedUser = userRepository.save(user);
        
        // Audit log
//        createAuditLog(savedUser.getUserId(), UserAuditLog.AuditAction.CREATED, ipAddress);
        
        // Publish OTP_REQUESTED event
        outboxPublisher.publishOtpRequestedEvent(savedUser.getUserId(), savedUser.getEmail(), savedUser.getPhoneNumber());
        
        log.info("User created successfully with ID: {}", savedUser.getUserId());
        return mapToUserResponse(savedUser);
    }
    
    public UserResponse getUserById(String userId) {
        log.info("Fetching user with ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        return mapToUserResponse(user);
    }
    
    public PaginatedResponse<UserResponse> getAllUsers(int pageNumber, int pageSize) {
        log.info("Fetching all users - page: {}, size: {}", pageNumber, pageSize);
        
        if (pageSize > 20) {
            pageSize = 20; // Max page size limit
        }
        
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<User> usersPage = userRepository.findByAccountStatus(User.AccountStatus.ACTIVE, pageable);
        
        return PaginatedResponse.<UserResponse>builder()
                .content(usersPage.getContent().stream().map(this::mapToUserResponse).toList())
                .pageNumber(usersPage.getNumber())
                .pageSize(usersPage.getSize())
                .totalElements(usersPage.getTotalElements())
                .totalPages(usersPage.getTotalPages())
                .last(usersPage.isLast())
                .build();
    }
    
    public UserResponse updateUser(String userId, UpdateUserRequest request, String ipAddress) {
        log.info("Updating user with ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        // Update email with OTP verification
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new UserAlreadyExistsException("Email already registered: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
            outboxPublisher.publishOtpRequestedEvent(userId, request.getEmail(), user.getPhoneNumber());
            createAuditLog(userId, UserAuditLog.AuditAction.EMAIL_UPDATED, ipAddress);
        }
        
        // Update phone with OTP verification
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().equals(user.getPhoneNumber())) {
            if (userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
                throw new UserAlreadyExistsException("Phone number already registered: " + request.getPhoneNumber());
            }
            user.setPhoneNumber(request.getPhoneNumber());
            outboxPublisher.publishOtpRequestedEvent(userId, user.getEmail(), request.getPhoneNumber());
            createAuditLog(userId, UserAuditLog.AuditAction.PHONE_UPDATED, ipAddress);
        }
        
        User updatedUser = userRepository.save(user);
        log.info("User updated successfully with ID: {}", userId);
        return mapToUserResponse(updatedUser);
    }
    
    public void resetPassword(String userId, String newPassword, String ipAddress) {
        log.info("Resetting password for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        // Publish OTP_REQUESTED event for verification
        outboxPublisher.publishPasswordResetRequestedEvent(userId, user.getEmail(), user.getPhoneNumber());
        
        createAuditLog(userId, UserAuditLog.AuditAction.PASSWORD_RESET_REQUESTED, ipAddress);
        log.info("Password reset event published for user ID: {}", userId);
    }
    
    public void confirmPasswordReset(String userId, String newPassword) {
        log.info("Confirming password reset for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        log.info("Password reset confirmed for user ID: {}", userId);
    }
    
    public void updateAccountAgeForAllUsers() {
        log.info("Starting batch update of account age for all users");
        
        Pageable pageable = PageRequest.of(0, 500);
        Page<User> usersBatch = userRepository.findAll(pageable);
        
        while (usersBatch.hasContent()) {
            usersBatch.forEach(user -> {
                int accountAge = Period.between(user.getCreatedAt().toLocalDate(), LocalDate.now()).getDays();
                user.setAccountAge(accountAge);
            });
            
            userRepository.saveAll(usersBatch.getContent());
            log.info("Updated {} users' account age", usersBatch.getNumberOfElements());
            
            if (usersBatch.isLast()) {
                break;
            }
            usersBatch = userRepository.findAll(PageRequest.of(usersBatch.getNumber() + 1, 500));
        }
        
        log.info("Batch update of account age completed");
    }
    
    public void deleteUser(String userId, String ipAddress) {
        log.info("Deleting user with ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        user.setAccountStatus(User.AccountStatus.DELETED);
        userRepository.save(user);
        
        createAuditLog(userId, UserAuditLog.AuditAction.DELETED, ipAddress);
        log.info("User deleted successfully with ID: {}", userId);
    }
    
    private UserResponse mapToUserResponse(User user) {
        int age = Period.between(user.getDateOfBirth(), LocalDate.now()).getYears();
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .dateOfBirth(user.getDateOfBirth())
                .age(age)
                .accountAge(user.getAccountAge())
                .accountStatus(user.getAccountStatus().name())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
    
    private void createAuditLog(String userId, UserAuditLog.AuditAction action, String ipAddress) {
        UserAuditLog auditLog = UserAuditLog.builder()
                .userId(userId)
                .action(action)
                .ipAddress(ipAddress)
                .build();
        auditLogRepository.save(auditLog);
    }
}
