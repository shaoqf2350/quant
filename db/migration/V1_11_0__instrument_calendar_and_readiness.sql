-- =====================================================================
-- QuantPF 模块一「数据与标的基础」：标的身份版本 / 交易日历 / 日线就绪
-- 对应《详设_1_数据与标的基础详细设计方案.md》§11 / §12
-- 业务表主键统一 pk_id BIGINT（Hutool Snowflake 生成）
-- 注：既有 quantpf_instrument / quantpf_trading_calendar 表维持原结构（主键 id），
--     本迁移新增的日历定义表命名 quantpf_calendar_def 以避免与既有 quantpf_trading_calendar 冲突。
-- =====================================================================

-- 日历定义（§12.3.1 TradingCalendar）：市场/交易所/产品/经纪商级别 + 市场本地时区 + 版本
CREATE TABLE quantpf_calendar_def (
    pk_id          BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    calendar_code  VARCHAR(64)  NOT NULL COMMENT '日历编码（CN_STOCK_SSE/CN_FUTURE_SHFE/HK_STOCK_HKEX/US_STOCK_GENERIC）',
    calendar_name  VARCHAR(128) NULL COMMENT '日历名称',
    market_type    VARCHAR(16)  NOT NULL COMMENT '市场类型（STOCK/FUTURE/FX/SPOT/INDEX）',
    region_code    VARCHAR(16)  NULL COMMENT '地区（CN/HK/US/GLOBAL）',
    venue_code     VARCHAR(32)  NULL COMMENT '交易场所（SSE/SZSE/HKEX/SHFE/...）',
    timezone       VARCHAR(64)  NOT NULL DEFAULT 'Asia/Shanghai' COMMENT '市场本地时区',
    currency       VARCHAR(8)   NULL COMMENT '币种',
    calendar_level VARCHAR(16)  NOT NULL DEFAULT 'MARKET' COMMENT '级别（MARKET/EXCHANGE/PRODUCT/INSTRUMENT/BROKER）',
    source_policy  VARCHAR(255) NULL COMMENT '来源策略',
    effective_from DATE         NULL COMMENT '生效开始',
    effective_to   DATE         NULL COMMENT '生效结束',
    status         VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    version        VARCHAR(32)  NOT NULL DEFAULT '1.0' COMMENT '版本',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    remark         VARCHAR(500) NULL COMMENT '备注',
    PRIMARY KEY (pk_id),
    UNIQUE KEY uk_calendar_def_code (calendar_code),
    KEY idx_calendar_def_market_venue (market_type, venue_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='市场/交易所/产品/经纪商交易日历定义表';

-- 交易日记录（§12.3.2 TradingCalendarDay）：市场本地日期 + 开闭市 + 前后交易日 + 质量状态
CREATE TABLE quantpf_calendar_day (
    pk_id              BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    calendar_id        BIGINT       NOT NULL COMMENT '日历ID（关联 quantpf_calendar_def.pk_id）',
    market_local_date  DATE         NOT NULL COMMENT '市场本地日期',
    is_open            TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否开市（标准布尔）',
    day_status         VARCHAR(32)  NOT NULL DEFAULT 'OPEN' COMMENT '日状态（OPEN/CLOSED/SPECIAL_OPEN/SPECIAL_CLOSED/UNKNOWN/CONFLICT）',
    previous_trade_date DATE        NULL COMMENT '上一交易日',
    next_trade_date    DATE         NULL COMMENT '下一交易日',
    holiday_code       VARCHAR(64)  NULL COMMENT '节假日编码',
    special_reason     VARCHAR(255) NULL COMMENT '特殊原因（半日市/提前收市）',
    source_id          BIGINT       NULL COMMENT '来源数据源ID',
    source_record_hash CHAR(64)     NULL COMMENT '来源记录哈希',
    quality_status     VARCHAR(32)  NOT NULL DEFAULT 'PENDING_REVIEW' COMMENT '质量状态（DataQualityStatus）',
    version            VARCHAR(32)  NOT NULL DEFAULT '1.0' COMMENT '版本',
    create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (pk_id),
    UNIQUE KEY uk_calendar_day_date (calendar_id, market_local_date),
    KEY idx_calendar_day_prev (calendar_id, previous_trade_date),
    CONSTRAINT fk_calendar_day_calendar FOREIGN KEY (calendar_id) REFERENCES quantpf_calendar_def (pk_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易日记录表（市场本地日期）';

-- 交易时段模板（§12.3.3）：预留午休/集合竞价/夜盘/跨日/半日市
CREATE TABLE quantpf_calendar_session (
    pk_id                 BIGINT      NOT NULL COMMENT '主键（雪花ID）',
    calendar_id           BIGINT      NOT NULL COMMENT '日历ID',
    session_type          VARCHAR(16) NOT NULL COMMENT '时段类型（REGULAR/NIGHT/AUCTION/SPECIAL）',
    sequence_no           INT         NOT NULL DEFAULT 0 COMMENT '顺序号',
    start_local_time      TIME        NOT NULL COMMENT '本地开始时间',
    end_local_time        TIME        NOT NULL COMMENT '本地结束时间',
    crosses_midnight      TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否跨自然日（夜盘）',
    trading_day_assignment VARCHAR(32) NULL COMMENT '交易日归属（夜盘归属下一交易日）',
    is_tradable           TINYINT(1)  NOT NULL DEFAULT 1 COMMENT '是否可交易',
    version               VARCHAR(32) NOT NULL DEFAULT '1.0' COMMENT '版本',
    create_time           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (pk_id),
    UNIQUE KEY uk_calendar_session_seq (calendar_id, session_type, sequence_no),
    CONSTRAINT fk_calendar_session_calendar FOREIGN KEY (calendar_id) REFERENCES quantpf_calendar_def (pk_id),
    CONSTRAINT ck_calendar_session_type CHECK (session_type IN ('REGULAR','NIGHT','AUCTION','SPECIAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易时段模板表';

-- 标的交易属性版本（§11.7）：最小单位/涨跌停/保证金/时段/交割等变化必须有版本
CREATE TABLE quantpf_instrument_version (
    pk_id              BIGINT        NOT NULL COMMENT '主键（雪花ID）',
    instrument_id      BIGINT UNSIGNED NOT NULL COMMENT '标的ID（关联 quantpf_instrument.id）',
    instrument_version VARCHAR(32)   NOT NULL COMMENT '标的版本号',
    valid_from         DATETIME      NOT NULL COMMENT '生效开始时间',
    valid_until        DATETIME      NULL COMMENT '生效结束时间（NULL=当前有效）',
    change_reason      VARCHAR(255)  NULL COMMENT '变更原因',
    source_ref         VARCHAR(255)  NULL COMMENT '来源引用',
    tick_size          DECIMAL(20,8) NULL COMMENT '最小价格变动',
    lot_size           DECIMAL(20,8) NULL COMMENT '最小交易单位',
    margin_rate        DECIMAL(20,8) NULL COMMENT '保证金率',
    price_limit        VARCHAR(64)   NULL COMMENT '涨跌停规则',
    trading_sessions   VARCHAR(1000) NULL COMMENT '交易时段（JSON）',
    delisting_rule     VARCHAR(255)  NULL COMMENT '上市/停牌/退市状态规则',
    create_time        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (pk_id),
    UNIQUE KEY uk_instrument_version (instrument_id, instrument_version),
    KEY idx_instrument_version_valid (instrument_id, valid_from, valid_until),
    CONSTRAINT fk_instrument_version_instrument FOREIGN KEY (instrument_id) REFERENCES quantpf_instrument (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标的交易属性版本表';

-- 标的/日历绑定（§12.3.4）：最终可交易资格叠加
CREATE TABLE quantpf_instrument_calendar_binding (
    pk_id              BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    instrument_id      BIGINT UNSIGNED NOT NULL COMMENT '标的/合约/外汇品种ID',
    calendar_id        BIGINT       NOT NULL COMMENT '日历ID',
    binding_type       VARCHAR(16)  NOT NULL DEFAULT 'DEFAULT' COMMENT '绑定类型（DEFAULT/OVERRIDE）',
    valid_from         DATETIME     NULL COMMENT '有效开始',
    valid_to           DATETIME     NULL COMMENT '有效结束',
    tradability_status VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE' COMMENT '可交易状态（ACTIVE/SUSPENDED/EXPIRED）',
    source             VARCHAR(128) NULL COMMENT '来源',
    reason             VARCHAR(255) NULL COMMENT '原因',
    version            VARCHAR(32)  NOT NULL DEFAULT '1.0' COMMENT '版本',
    create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (pk_id),
    UNIQUE KEY uk_instrument_calendar_binding (instrument_id, calendar_id),
    KEY idx_binding_calendar (calendar_id),
    CONSTRAINT fk_binding_instrument FOREIGN KEY (instrument_id) REFERENCES quantpf_instrument (id),
    CONSTRAINT fk_binding_calendar FOREIGN KEY (calendar_id) REFERENCES quantpf_calendar_def (pk_id),
    CONSTRAINT ck_binding_type CHECK (binding_type IN ('DEFAULT','OVERRIDE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标的/合约/外汇品种与日历绑定表';

-- 日线数据就绪（§12.3.5）：交易日、数据应到/已到/校验/策略可用/Paper可用分隔
CREATE TABLE quantpf_market_data_readiness (
    pk_id                  BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    calendar_id            BIGINT       NOT NULL COMMENT '日历ID',
    market_local_date      DATE         NOT NULL COMMENT '市场本地日期',
    expected_flag          TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否应到',
    received_flag          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已到',
    validated_flag         TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否校验通过',
    strategy_ready_flag    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '策略是否可用',
    paper_ready_flag       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT 'Paper 是否可新增风险',
    last_valid_snapshot_id BIGINT       NULL COMMENT '最后一次有效快照ID',
    block_reason           VARCHAR(255) NULL COMMENT '阻断原因',
    version                VARCHAR(32)  NOT NULL DEFAULT '1.0' COMMENT '版本',
    create_time            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (pk_id),
    UNIQUE KEY uk_readiness_calendar_date (calendar_id, market_local_date),
    CONSTRAINT fk_readiness_calendar FOREIGN KEY (calendar_id) REFERENCES quantpf_calendar_def (pk_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日线数据就绪状态表';