-- V1__initial_schema.sql
-- Initial database schema for notification-service

CREATE TABLE IF NOT EXISTS otp_records (
    otp_id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    otp_type VARCHAR(20) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_attempts INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_user_id (user_id),
    KEY idx_created_at (created_at),
    KEY idx_otp_code (otp_code),
    KEY idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notification_history (
    notification_id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    notification_type VARCHAR(100) NOT NULL,
    recipient VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    error_message TEXT,
    sent_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_user_id (user_id),
    KEY idx_status (status),
    KEY idx_created_at (created_at),
    KEY idx_notification_type (notification_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS processed_events (
    processed_event_id CHAR(36) PRIMARY KEY,
    event_id CHAR(36) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    processed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_event_id (event_id),
    KEY idx_processed_at (processed_at),
    KEY idx_event_type (event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
