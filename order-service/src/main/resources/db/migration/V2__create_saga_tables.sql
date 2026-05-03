-- Migration script to create saga_executions table
-- Run this script during database initialization

CREATE TABLE IF NOT EXISTS saga_executions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT,
    saga_state VARCHAR(50) NOT NULL,
    step_states JSON,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    error_message TEXT,
    error_stack_trace LONGTEXT,
    started_at DATETIME NOT NULL,
    completed_at DATETIME,
    last_retry_at DATETIME,
    payload_snapshot LONGTEXT NOT NULL,
    requires_compensation BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_order_id (order_id),
    INDEX idx_saga_state (saga_state),
    INDEX idx_requires_compensation (requires_compensation),
    CONSTRAINT check_saga_state CHECK (saga_state IN (
        'STARTED',
        'ORDER_CREATION_IN_PROGRESS',
        'ORDER_CREATED',
        'INVENTORY_RESERVATION_IN_PROGRESS',
        'INVENTORY_RESERVED',
        'PAYMENT_PROCESSING_IN_PROGRESS',
        'PAYMENT_COMPLETED',
        'SAGA_COMPLETED',
        'SAGA_FAILED',
        'SAGA_COMPENSATING',
        'SAGA_COMPENSATED'
    ))
);

-- Create table for saga step history (optional, for detailed auditing)
CREATE TABLE IF NOT EXISTS saga_step_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    saga_id BIGINT NOT NULL,
    step_name VARCHAR(100) NOT NULL,
    step_state VARCHAR(50) NOT NULL,
    executed_at DATETIME NOT NULL,
    duration_ms BIGINT,
    
    FOREIGN KEY (saga_id) REFERENCES saga_executions(id),
    INDEX idx_saga_id (saga_id),
    INDEX idx_step_name (step_name)
);

-- Create table for DLT archive (for long-term auditing)
CREATE TABLE IF NOT EXISTS dlt_message_archive (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    topic VARCHAR(255) NOT NULL,
    order_id BIGINT,
    saga_id BIGINT,
    message LONGTEXT NOT NULL,
    failure_reason TEXT,
    retry_count INT DEFAULT 0,
    archived_at DATETIME NOT NULL,
    resolved BOOLEAN DEFAULT false,
    resolved_at DATETIME,
    resolution_notes TEXT,
    
    INDEX idx_topic (topic),
    INDEX idx_order_id (order_id),
    INDEX idx_saga_id (saga_id),
    INDEX idx_resolved (resolved)
);
