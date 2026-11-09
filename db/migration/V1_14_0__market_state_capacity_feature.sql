-- =====================================================================
-- QuantPF 模块一「数据与标的基础」：市场状态 / 容量评估 / 量价特征预留
-- 对应《详设_1_数据与标的基础详细设计方案.md》§14 / §15 / §23
-- 业务表主键统一 pk_id BIGINT（Hutool Snowflake 生成）
-- 注：instrument_id 引用既有表 quantpf_instrument.id（bigint unsigned）
-- =====================================================================

-- 市场状态快照（§14.4）：确定性规则产生（TREND_UP/RANGE/HIGH_VOLATILITY/...），Dify 只能解释
CREATE TABLE quantpf_market_state (
    pk_id             BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    instrument_id     BIGINT UNSIGNED NULL COMMENT '标的ID（NULL=市场级，关联 quantpf_instrument.id）',
    calendar_id       BIGINT       NULL COMMENT '日历ID（关联 quantpf_calendar_def.pk_id）',
    market_local_date DATE         NOT NULL COMMENT '市场本地日期',
    state_code        VARCHAR(32)  NOT NULL COMMENT '市场状态（MarketStateCode）',
    state_reason      VARCHAR(500) NULL COMMENT '状态原因',
    version           VARCHAR(32)  NOT NULL DEFAULT '1.0' COMMENT '版本',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (pk_id),
    KEY idx_market_state_instrument_date (instrument_id, market_local_date),
    KEY idx_market_state_date (market_local_date),
    CONSTRAINT fk_market_state_instrument FOREIGN KEY (instrument_id) REFERENCES quantpf_instrument (id),
    CONSTRAINT fk_market_state_calendar FOREIGN KEY (calendar_id) REFERENCES quantpf_calendar_def (pk_id),
    CONSTRAINT ck_market_state_code CHECK (state_code IN ('TREND_UP','TREND_DOWN','RANGE','HIGH_VOLATILITY','LOW_LIQUIDITY','SYSTEMIC_RISK','DATA_UNAVAILABLE','SPECIAL_RULE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='市场状态快照表';

-- 容量/冲击成本评估（§15）：候选进入交易计划前判断能否成交/滑点/止损/退出
CREATE TABLE quantpf_capacity_assessment (
    pk_id             BIGINT        NOT NULL COMMENT '主键（雪花ID）',
    instrument_id     BIGINT UNSIGNED NOT NULL COMMENT '标的ID（关联 quantpf_instrument.id）',
    assess_date       DATE          NOT NULL COMMENT '评估日期',
    capacity_status   VARCHAR(32)   NOT NULL DEFAULT 'UNKNOWN' COMMENT '容量状态（CapacityStatus）',
    avg_turnover      DECIMAL(30,8) NULL COMMENT '日均成交额',
    avg_volume        DECIMAL(30,8) NULL COMMENT '日均成交量',
    bid_ask_spread    DECIMAL(20,8) NULL COMMENT '买卖价差',
    turnover_rate     DECIMAL(20,8) NULL COMMENT '换手率',
    float_market_cap  DECIMAL(30,8) NULL COMMENT '流通市值',
    planned_ratio     DECIMAL(20,8) NULL COMMENT '计划金额占成交额比例',
    slippage_estimate DECIMAL(20,8) NULL COMMENT '预计滑点',
    detail            VARCHAR(1000) NULL COMMENT '明细（JSON：主力量/持仓/夜盘流动性/交割临近）',
    create_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (pk_id),
    KEY idx_capacity_instrument_date (instrument_id, assess_date),
    CONSTRAINT fk_capacity_instrument FOREIGN KEY (instrument_id) REFERENCES quantpf_instrument (id),
    CONSTRAINT ck_capacity_status CHECK (capacity_status IN ('SUFFICIENT','LIMITED','HIGH_IMPACT','UNTRADEABLE','UNKNOWN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易容量与冲击成本评估表';

-- 量价特征快照（§23）：一期预留（收益/波动/相对强弱/成交量分位/价格位置 + 版本 + 点时）
CREATE TABLE quantpf_quant_feature_snapshot (
    pk_id                     BIGINT        NOT NULL COMMENT '主键（雪花ID）',
    instrument_id             BIGINT UNSIGNED NOT NULL COMMENT '标的ID（关联 quantpf_instrument.id）',
    market_local_date         DATE          NOT NULL COMMENT '市场本地日期',
    timeframe                 VARCHAR(16)   NOT NULL COMMENT '周期（30M/DAY/WEEK/MONTH）',
    feature_definition_version VARCHAR(32)  NOT NULL COMMENT '特征定义版本',
    source_snapshot_id        BIGINT        NULL COMMENT '源快照ID',
    point_in_time_id          BIGINT        NULL COMMENT '点时版本ID',
    return_value              DECIMAL(20,8) NULL COMMENT '收益（标准化）',
    volatility                DECIMAL(20,8) NULL COMMENT '波动',
    relative_strength         DECIMAL(20,8) NULL COMMENT '相对强弱',
    volume_percentile         DECIMAL(20,8) NULL COMMENT '成交量分位',
    price_position            DECIMAL(20,8) NULL COMMENT '价格位置',
    status                    VARCHAR(32)   NOT NULL DEFAULT 'CALCULATED' COMMENT '状态',
    create_time               DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (pk_id),
    UNIQUE KEY uk_quant_feature_snapshot (instrument_id, market_local_date, timeframe, feature_definition_version),
    KEY idx_quant_feature_date (market_local_date),
    CONSTRAINT fk_quant_feature_instrument FOREIGN KEY (instrument_id) REFERENCES quantpf_instrument (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='量价特征快照表（二期时机优化预留）';