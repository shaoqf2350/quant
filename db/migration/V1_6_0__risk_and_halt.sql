CREATE TABLE quantpf_risk_rule (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(128) NOT NULL,
    rule_type VARCHAR(32) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    definition JSON NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quantpf_risk_rule_code (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quantpf_risk_snapshot (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    account_id BIGINT UNSIGNED NULL,
    risk_status VARCHAR(16) NOT NULL,
    risk_score DECIMAL(12,6) NULL,
    reason VARCHAR(512) NULL,
    snapshot_at DATETIME(6) NOT NULL,
    data JSON NULL,
    PRIMARY KEY (id),
    KEY idx_quantpf_risk_snapshot_account_time (account_id, snapshot_at),
    CONSTRAINT fk_quantpf_risk_snapshot_account FOREIGN KEY (account_id) REFERENCES quantpf_trading_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quantpf_trading_halt (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    scope VARCHAR(16) NOT NULL,
    target_id VARCHAR(128) NULL,
    reason VARCHAR(512) NOT NULL,
    halted_by VARCHAR(128) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    halted_at DATETIME(6) NOT NULL,
    resumed_at DATETIME(6) NULL,
    resume_reason VARCHAR(512) NULL,
    PRIMARY KEY (id),
    KEY idx_quantpf_halt_active_scope (active, scope, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;