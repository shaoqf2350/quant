-- =====================================================================
-- QuantPF 模块一「数据与标的基础」补齐：点时版本 / 日历来源快照 / 日历冲突 / 外汇时段规则
-- 对应《详设_1_数据与标的基础详细设计方案.md》§8.4 / §12.2.2 / §12.2.5 / §12.2.6
-- 业务表主键统一 pk_id BIGINT（Hutool Snowflake 生成）
-- =====================================================================

-- 点时数据版本（§8.3/§8.4）：决策时点可见版本，任何修订保留旧版本
CREATE TABLE quantpf_data_point_in_time (
    pk_id             BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    subject_type      VARCHAR(32)  NOT NULL COMMENT '对象类型（MATERIAL/MARKET_DATA/INSTRUMENT/CALENDAR）',
    subject_id        BIGINT       NOT NULL COMMENT '对象ID',
    snapshot_version  VARCHAR(32)  NOT NULL COMMENT '快照版本',
    decision_time     DATETIME     NOT NULL COMMENT '决策时点',
    visible_at        DATETIME     NULL COMMENT '点时可获得时间',
    effective_at      DATETIME     NULL COMMENT '业务生效时间',
    revised_at        DATETIME     NULL COMMENT '修订时间',
    revision_reason   VARCHAR(500) NULL COMMENT '修订原因（不静默覆盖）',
    revision_source   VARCHAR(128) NULL COMMENT '修订来源',
    payload_ref       VARCHAR(500) NULL COMMENT '点时数据引用',
    affected_objects  VARCHAR(500) NULL COMMENT '受影响对象',
    need_recompute    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否需重算研究/指标/回测',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (pk_id),
    UNIQUE KEY uk_point_in_time_subject_version (subject_type, subject_id, snapshot_version),
    KEY idx_point_in_time_decision_time (decision_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点时数据版本表';

-- 日历来源原始快照（§12.2.6）：保留每个接口原请求/响应/参数
CREATE TABLE quantpf_calendar_source_snapshot (
    pk_id                   BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    source_name             VARCHAR(64)  NOT NULL COMMENT '来源名（TUSHARE_PRO）',
    endpoint_name           VARCHAR(64)  NOT NULL COMMENT '接口名（trade_cal/fut_trade_cal/hk_tradecal/us_tradecal/fx_obasic/fx_daily）',
    request_params          TEXT         NULL COMMENT '请求参数（保留原始值）',
    response_schema_version VARCHAR(32)  NULL COMMENT '响应 Schema 版本',
    raw_payload_hash        CHAR(64)     NULL COMMENT '原始载荷哈希',
    raw_payload_ref         VARCHAR(500) NULL COMMENT '原始载荷引用',
    retrieved_at            DATETIME     NULL COMMENT '抓取时间',
    source_observed_at      DATETIME     NULL COMMENT '来源观察时间',
    permission_profile      VARCHAR(255) NULL COMMENT '账号/权限说明（不含 Token 明文）',
    create_time             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (pk_id),
    UNIQUE KEY uk_calendar_snapshot_hash (source_name, endpoint_name, raw_payload_hash),
    KEY idx_calendar_snapshot_retrieved (retrieved_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日历来源原始快照表';

-- 日历冲突与人工覆盖审核（§12.2.2 冲突不静默择一）
CREATE TABLE quantpf_calendar_conflict (
    pk_id             BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    conflict_code     VARCHAR(64)  NOT NULL COMMENT '冲突编码',
    calendar_id       BIGINT       NOT NULL COMMENT '日历ID（关联 quantpf_calendar_def.pk_id）',
    market_local_date DATE         NULL COMMENT '涉及市场本地日期',
    conflict_type     VARCHAR(64)  NOT NULL COMMENT '冲突类型（SOURCE_CONFLICT/MANUAL_OVERRIDE/TYPE_MISMATCH/GFEX_GAP）',
    source_a          VARCHAR(255) NULL COMMENT '来源A（记录/哈希）',
    source_b          VARCHAR(255) NULL COMMENT '来源B（记录/哈希）',
    description       VARCHAR(500) NULL COMMENT '冲突描述',
    status            VARCHAR(32)  NOT NULL DEFAULT 'OPEN' COMMENT '状态（CalendarConflictStatus）',
    override_decision VARCHAR(500) NULL COMMENT '人工覆盖结论（处理人/依据/生效版本）',
    impact_scope      VARCHAR(500) NULL COMMENT '影响范围',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (pk_id),
    UNIQUE KEY uk_calendar_conflict_code (conflict_code),
    KEY idx_calendar_conflict_calendar (calendar_id, market_local_date),
    CONSTRAINT fk_calendar_conflict_calendar FOREIGN KEY (calendar_id) REFERENCES quantpf_calendar_def (pk_id),
    CONSTRAINT ck_calendar_conflict_status CHECK (status IN ('OPEN','REVIEWED','OVERRIDDEN','CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易日历冲突与人工覆盖审核表';

-- 外汇/海外经纪商品时段规则（§12.2.5）：不伪造统一外汇日历，按品种/经纪商时段规则建模（一期 schema 预留）
CREATE TABLE quantpf_fx_session_rule (
    pk_id              BIGINT        NOT NULL COMMENT '主键（雪花ID）',
    ts_code            VARCHAR(64)   NOT NULL COMMENT 'Tushare/FX 品种代码',
    classify           VARCHAR(32)   NOT NULL COMMENT '品种分类（FX_FOREX/FX_INDEX/FX_COMMODITY/FX_METAL/FX_BUND/FX_CRYPTO/FX_BASKET）',
    exchange           VARCHAR(64)   NULL COMMENT '交易商/交易所',
    trading_hours      VARCHAR(255)  NULL COMMENT '交易时段原文',
    break_time         VARCHAR(255)  NULL COMMENT '休市时段原文',
    normalized_sessions VARCHAR(1000) NULL COMMENT '标准化时段规则（JSON）',
    timezone           VARCHAR(64)   NOT NULL DEFAULT 'GMT' COMMENT '来源时区',
    valid_from         DATE          NULL COMMENT '生效开始',
    valid_to           DATE          NULL COMMENT '生效结束',
    version            VARCHAR(32)   NOT NULL DEFAULT '1.0' COMMENT '版本',
    create_time        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (pk_id),
    UNIQUE KEY uk_fx_session_rule (ts_code, version),
    KEY idx_fx_session_classify (classify)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外汇/海外经纪商品时段规则表';