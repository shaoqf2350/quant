-- =====================================================================
-- QuantPF 模块一「数据与标的基础」：公司行为 / 期货合约事件
-- 对应《详设_1_数据与标的基础详细设计方案.md》§13
-- 业务表主键统一 pk_id BIGINT（Hutool Snowflake 生成）
-- 注：instrument_id 引用既有表 quantpf_instrument.id（bigint unsigned，须同类型）
-- =====================================================================

-- 股票公司行为（§13.1）：除权除息/分红/配股/停复牌/重组/ST/退市/财报披露/问询
CREATE TABLE quantpf_corporate_action (
    pk_id          BIGINT        NOT NULL COMMENT '主键（雪花ID）',
    instrument_id  BIGINT UNSIGNED NOT NULL COMMENT '标的ID（关联 quantpf_instrument.id）',
    action_type    VARCHAR(32)   NOT NULL COMMENT '行为类型（CorporateActionType）',
    action_code    VARCHAR(64)   NOT NULL COMMENT '行为业务编码',
    effective_date DATETIME      NULL COMMENT '生效时间',
    announce_date  DATETIME      NULL COMMENT '公告时间',
    status         VARCHAR(32)   NOT NULL DEFAULT 'DISCOVERED' COMMENT '事件状态（LifecycleEventStatus）',
    detail         VARCHAR(1000) NULL COMMENT '明细（JSON：比例/价格/影响）',
    source_id      BIGINT        NULL COMMENT '来源数据源ID',
    version        VARCHAR(32)   NOT NULL DEFAULT '1.0' COMMENT '版本',
    create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (pk_id),
    UNIQUE KEY uk_corporate_action_code (action_code),
    KEY idx_corporate_action_instrument_date (instrument_id, effective_date),
    CONSTRAINT fk_corporate_action_instrument FOREIGN KEY (instrument_id) REFERENCES quantpf_instrument (id),
    CONSTRAINT ck_corporate_action_status CHECK (status IN ('DISCOVERED','VERIFIED','SCHEDULED','EFFECTIVE','EXPIRED','SUPERSEDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='股票公司行为表';

-- 期货合约生命周期/制度事件（§13.2）：上市/保证金/涨跌停/时段/限仓/最后交易日/交割/移仓/摘牌
CREATE TABLE quantpf_contract_event (
    pk_id          BIGINT        NOT NULL COMMENT '主键（雪花ID）',
    instrument_id  BIGINT UNSIGNED NOT NULL COMMENT '合约ID（关联 quantpf_instrument.id）',
    event_type     VARCHAR(32)   NOT NULL COMMENT '事件类型（ContractEventType）',
    event_code     VARCHAR(64)   NOT NULL COMMENT '事件业务编码',
    effective_date DATETIME      NULL COMMENT '生效时间',
    status         VARCHAR(32)   NOT NULL DEFAULT 'DISCOVERED' COMMENT '事件状态（LifecycleEventStatus）',
    detail         VARCHAR(1000) NULL COMMENT '明细（JSON：调整前后值）',
    source_id      BIGINT        NULL COMMENT '来源数据源ID',
    version        VARCHAR(32)   NOT NULL DEFAULT '1.0' COMMENT '版本',
    create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (pk_id),
    UNIQUE KEY uk_contract_event_code (event_code),
    KEY idx_contract_event_instrument_date (instrument_id, effective_date),
    CONSTRAINT fk_contract_event_instrument FOREIGN KEY (instrument_id) REFERENCES quantpf_instrument (id),
    CONSTRAINT ck_contract_event_status CHECK (status IN ('DISCOVERED','VERIFIED','SCHEDULED','EFFECTIVE','EXPIRED','SUPERSEDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='期货合约生命周期与制度事件表';