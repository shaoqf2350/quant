CREATE TABLE quantpf_research_news (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    external_id VARCHAR(128) NOT NULL,
    title VARCHAR(512) NOT NULL,
    published_at DATETIME(6) NULL,
    source VARCHAR(128) NOT NULL,
    content_hash CHAR(64) NOT NULL,
    content JSON NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quantpf_news_external (external_id, source),
    UNIQUE KEY uk_quantpf_news_hash (content_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quantpf_financial_statement (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    instrument_id BIGINT UNSIGNED NOT NULL,
    report_period DATE NOT NULL,
    statement_type VARCHAR(32) NOT NULL,
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY',
    data JSON NOT NULL,
    source VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quantpf_fin_statement (instrument_id, report_period, statement_type, source),
    CONSTRAINT fk_quantpf_fin_statement_instrument FOREIGN KEY (instrument_id) REFERENCES quantpf_instrument (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quantpf_strategy_signal (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    strategy_id BIGINT UNSIGNED NULL,
    instrument_id BIGINT UNSIGNED NOT NULL,
    signal_type VARCHAR(32) NOT NULL,
    signal_value DECIMAL(24,8) NULL,
    signal_time DATETIME(6) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    payload JSON NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_quantpf_signal_instrument_time (instrument_id, signal_time),
    CONSTRAINT fk_quantpf_signal_instrument FOREIGN KEY (instrument_id) REFERENCES quantpf_instrument (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;