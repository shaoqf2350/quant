CREATE TABLE quantpf_strategy (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    strategy_code VARCHAR(64) NOT NULL,
    strategy_name VARCHAR(128) NOT NULL,
    version VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    definition JSON NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quantpf_strategy_code_version (strategy_code, version),
    CONSTRAINT ck_quantpf_strategy_status CHECK (status IN ('DRAFT','ACTIVE','DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quantpf_backtest_run (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    strategy_id BIGINT UNSIGNED NOT NULL,
    run_id VARCHAR(128) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    initial_capital DECIMAL(24,8) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'CREATED',
    result JSON NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_quantpf_backtest_run_id (run_id),
    KEY idx_quantpf_backtest_strategy (strategy_id),
    CONSTRAINT fk_quantpf_backtest_strategy FOREIGN KEY (strategy_id) REFERENCES quantpf_strategy (id),
    CONSTRAINT ck_quantpf_backtest_status CHECK (status IN ('CREATED','RUNNING','SUCCEEDED','FAILED','CANCELED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quantpf_backtest_result (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    backtest_run_id BIGINT UNSIGNED NOT NULL,
    metric_code VARCHAR(64) NOT NULL,
    metric_value DECIMAL(30,12) NULL,
    metric_json JSON NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quantpf_backtest_metric (backtest_run_id, metric_code),
    CONSTRAINT fk_quantpf_backtest_result_run FOREIGN KEY (backtest_run_id) REFERENCES quantpf_backtest_run (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;