CREATE TABLE quantpf_trading_account (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    account_code VARCHAR(64) NOT NULL,
    account_name VARCHAR(128) NOT NULL,
    account_type VARCHAR(16) NOT NULL DEFAULT 'PAPER',
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY',
    initial_cash DECIMAL(24,8) NOT NULL DEFAULT 0,
    available_cash DECIMAL(24,8) NOT NULL DEFAULT 0,
    frozen_cash DECIMAL(24,8) NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quantpf_account_code (account_code),
    CONSTRAINT ck_quantpf_account_type CHECK (account_type = 'PAPER'),
    CONSTRAINT ck_quantpf_account_status CHECK (status IN ('ACTIVE','HALTED','CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quantpf_cash_ledger (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    account_id BIGINT UNSIGNED NOT NULL,
    ledger_type VARCHAR(32) NOT NULL,
    amount DECIMAL(24,8) NOT NULL,
    balance_after DECIMAL(24,8) NOT NULL,
    reference_type VARCHAR(32) NULL,
    reference_id VARCHAR(128) NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quantpf_cash_ledger_idempotency (idempotency_key),
    KEY idx_quantpf_cash_ledger_account_time (account_id, occurred_at),
    CONSTRAINT fk_quantpf_cash_ledger_account FOREIGN KEY (account_id) REFERENCES quantpf_trading_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quantpf_position (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    account_id BIGINT UNSIGNED NOT NULL,
    instrument_id BIGINT UNSIGNED NOT NULL,
    quantity DECIMAL(24,8) NOT NULL DEFAULT 0,
    available_quantity DECIMAL(24,8) NOT NULL DEFAULT 0,
    frozen_quantity DECIMAL(24,8) NOT NULL DEFAULT 0,
    average_cost DECIMAL(24,8) NOT NULL DEFAULT 0,
    realized_pnl DECIMAL(24,8) NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quantpf_position_account_instrument (account_id, instrument_id),
    CONSTRAINT fk_quantpf_position_account FOREIGN KEY (account_id) REFERENCES quantpf_trading_account (id),
    CONSTRAINT fk_quantpf_position_instrument FOREIGN KEY (instrument_id) REFERENCES quantpf_instrument (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;