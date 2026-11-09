CREATE TABLE quantpf_job_execution_audit (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    job_name VARCHAR(128) NOT NULL,
    execution_id VARCHAR(128) NOT NULL,
    scheduler_type VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    status VARCHAR(16) NOT NULL,
    request_id VARCHAR(128) NULL,
    started_at DATETIME(6) NULL,
    finished_at DATETIME(6) NULL,
    error_code VARCHAR(64) NULL,
    error_message VARCHAR(1024) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quantpf_job_execution (execution_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quantpf_audit_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    request_id VARCHAR(128) NULL,
    actor_id VARCHAR(128) NULL,
    action VARCHAR(128) NOT NULL,
    aggregate_type VARCHAR(64) NULL,
    aggregate_id VARCHAR(128) NULL,
    result VARCHAR(16) NOT NULL,
    detail JSON NULL,
    occurred_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_quantpf_audit_request (request_id),
    KEY idx_quantpf_audit_aggregate (aggregate_type, aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;