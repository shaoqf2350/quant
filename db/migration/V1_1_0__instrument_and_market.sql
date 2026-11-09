CREATE TABLE quantpf_instrument (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    instrument_code VARCHAR(64) NOT NULL,
    product_type VARCHAR(16) NOT NULL,
    exchange_code VARCHAR(32) NOT NULL,
    instrument_name VARCHAR(128) NOT NULL,
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY',
    tick_size DECIMAL(20,8) NULL,
    lot_size DECIMAL(20,8) NOT NULL DEFAULT 1,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    listed_date DATE NULL,
    delisted_date DATE NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quantpf_instrument_code_exchange (instrument_code, exchange_code),
    CONSTRAINT ck_quantpf_instrument_product_type CHECK (product_type IN ('STOCK','FUTURE')),
    CONSTRAINT ck_quantpf_instrument_status CHECK (status IN ('ACTIVE','SUSPENDED','DELISTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quantpf_trading_calendar (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    market_code VARCHAR(32) NOT NULL,
    trading_date DATE NOT NULL,
    is_open TINYINT(1) NOT NULL DEFAULT 1,
    session_open TIME NULL,
    session_close TIME NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quantpf_calendar_market_date (market_code, trading_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quantpf_market_bar (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    instrument_id BIGINT UNSIGNED NOT NULL,
    timeframe VARCHAR(16) NOT NULL,
    bar_time DATETIME(6) NOT NULL,
    open_price DECIMAL(24,8) NOT NULL,
    high_price DECIMAL(24,8) NOT NULL,
    low_price DECIMAL(24,8) NOT NULL,
    close_price DECIMAL(24,8) NOT NULL,
    volume DECIMAL(30,8) NULL,
    turnover DECIMAL(30,8) NULL,
    source VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quantpf_market_bar (instrument_id, timeframe, bar_time),
    CONSTRAINT fk_quantpf_market_bar_instrument FOREIGN KEY (instrument_id) REFERENCES quantpf_instrument (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;