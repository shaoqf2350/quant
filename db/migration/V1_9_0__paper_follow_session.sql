CREATE TABLE quantpf_paper_follow_session (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    session_id VARCHAR(128) NOT NULL,
    account_id BIGINT UNSIGNED NOT NULL,
    strategy_id BIGINT UNSIGNED NOT NULL,
    mode VARCHAR(16) NOT NULL DEFAULT 'PAPER',
    status VARCHAR(16) NOT NULL,
    start_request_id VARCHAR(128) NOT NULL,
    stop_request_id VARCHAR(128) NULL,
    started_by VARCHAR(128) NULL,
    stopped_by VARCHAR(128) NULL,
    started_at DATETIME(6) NOT NULL,
    stopped_at DATETIME(6) NULL,
    stop_reason VARCHAR(512) NULL,
    revision BIGINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    active_slot VARCHAR(256) GENERATED ALWAYS AS (
        CASE WHEN status = 'RUNNING'
             THEN CONCAT(account_id, ':', strategy_id, ':', mode)
             ELSE NULL
        END
    ) STORED,
    PRIMARY KEY (id),
    UNIQUE KEY uk_quantpf_follow_session_id (session_id),
    UNIQUE KEY uk_quantpf_follow_start_request (start_request_id),
    UNIQUE KEY uk_quantpf_follow_stop_request (stop_request_id),
    UNIQUE KEY uk_quantpf_follow_active_slot (active_slot),
    KEY idx_quantpf_follow_account_strategy_status (account_id, strategy_id, status, updated_at),
    CONSTRAINT fk_quantpf_follow_account FOREIGN KEY (account_id) REFERENCES quantpf_trading_account (id),
    CONSTRAINT fk_quantpf_follow_strategy FOREIGN KEY (strategy_id) REFERENCES quantpf_strategy (id),
    CONSTRAINT ck_quantpf_follow_mode CHECK (mode = 'PAPER'),
    CONSTRAINT ck_quantpf_follow_status CHECK (status IN ('RUNNING', 'STOPPED')),
    CONSTRAINT ck_quantpf_follow_running_fields CHECK (
        (status = 'RUNNING' AND stopped_at IS NULL AND stop_request_id IS NULL)
        OR
        (status = 'STOPPED' AND stopped_at IS NOT NULL AND stop_request_id IS NOT NULL)
    ),
    CONSTRAINT ck_quantpf_follow_time_order CHECK (stopped_at IS NULL OR started_at <= stopped_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;