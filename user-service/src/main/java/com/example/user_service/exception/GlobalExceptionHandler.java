package com.example.user_service.exception;

import com.example.user_service.model.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.builder()
                        .success(false)
                        .message(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build());
    }
    
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        log.error("User already exists: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.builder()
                        .success(false)
                        .message(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        log.error("Validation error occurred");
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .data(errors)
                        .timestamp(LocalDateTime.now())
                        .build());
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneralException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.builder()
                        .success(false)
                        .message("An unexpected error occurred")
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}
