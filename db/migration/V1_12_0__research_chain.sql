-- =====================================================================
-- QuantPF 模块一「数据与标的基础」：新闻研究链（跨模块一/二边界）
-- 对应《详设_1_数据与标的基础详细设计方案.md》§10
-- 链路：新闻材料 → ResearchTask → Dify/TradingAgents 草稿 → Spring Boot 校验 → ResearchEvent + Evidence → 发布
-- 业务表主键统一 pk_id BIGINT（Hutool Snowflake 生成）
-- =====================================================================

-- 研究任务（§10.2）：冻结点时上下文，驱动 Dify/TradingAgents 分析
CREATE TABLE quantpf_research_task (
    pk_id            BIGINT        NOT NULL COMMENT '主键（雪花ID）',
    task_code        VARCHAR(64)   NOT NULL COMMENT '研究任务业务ID',
    material_ids     VARCHAR(1000) NULL COMMENT '研究材料ID集合（逗号分隔）',
    subject_refs     VARCHAR(1000) NULL COMMENT '标的/合约/现货引用',
    task_type        VARCHAR(32)   NOT NULL COMMENT '任务类型（NEWS_ANALYSIS/DEEP_RESEARCH/RISK_REVIEW）',
    snapshot_id      BIGINT        NULL COMMENT '点时快照ID（冻结点时上下文）',
    workflow_version VARCHAR(32)   NULL COMMENT 'Dify Workflow 版本',
    prompt_version   VARCHAR(32)   NULL COMMENT 'Prompt 版本',
    status           VARCHAR(32)   NOT NULL DEFAULT 'NEW' COMMENT '任务状态（ResearchTaskStatus）',
    idempotency_key  VARCHAR(128)  NOT NULL COMMENT '幂等键',
    run_id           VARCHAR(128)  NULL COMMENT 'Dify/Agent 运行ID',
    error_code       VARCHAR(64)   NULL COMMENT '异常码',
    create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (pk_id),
    UNIQUE KEY uk_research_task_code (task_code),
    UNIQUE KEY uk_research_task_idempotency (idempotency_key),
    KEY idx_research_task_status_time (status, create_time),
    CONSTRAINT ck_research_task_status CHECK (status IN ('NEW','RUNNING','SUCCEEDED','FAILED','CANCELED','RETRYING'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究任务表';

-- AI 研究草稿（§10.3）：Dify/TradingAgents 原始输出 + 版本 + 校验状态（非事实主表）
CREATE TABLE quantpf_ai_research_draft (
    pk_id                  BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    research_task_id       BIGINT       NOT NULL COMMENT '研究任务ID（关联 quantpf_research_task.pk_id）',
    run_id                 VARCHAR(128) NULL COMMENT '运行ID',
    model_version          VARCHAR(64)  NULL COMMENT '模型版本',
    summary                TEXT         NULL COMMENT '新闻/材料摘要',
    fact_candidates        TEXT         NULL COMMENT '事实候选（JSON）',
    impact_scope_candidate VARCHAR(500) NULL COMMENT '影响范围候选',
    impact_level_candidate VARCHAR(500) NULL COMMENT '重大程度候选',
    direction_candidate    VARCHAR(500) NULL COMMENT '方向候选（利多/利空/中性/双向/不确定）',
    subject_candidates     TEXT         NULL COMMENT '实体/行业/商品候选（JSON）',
    citations              TEXT         NULL COMMENT '原文引用（JSON）',
    counter_questions      TEXT         NULL COMMENT '反方问题（JSON）',
    warnings               TEXT         NULL COMMENT '警告',
    raw_response           MEDIUMTEXT   NULL COMMENT '原始 AI 响应（保留）',
    validation_status      VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '校验状态（AiDraftValidationStatus）',
    create_time            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (pk_id),
    KEY idx_ai_draft_task (research_task_id),
    KEY idx_ai_draft_validation (validation_status),
    CONSTRAINT fk_ai_draft_task FOREIGN KEY (research_task_id) REFERENCES quantpf_research_task (pk_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 研究草稿表（非事实主表）';

-- 研究事件（§10.4）：Spring Boot 校验后的事件主表 + 版本
CREATE TABLE quantpf_research_event (
    pk_id           BIGINT        NOT NULL COMMENT '主键（雪花ID）',
    event_code      VARCHAR(64)   NOT NULL COMMENT '研究事件业务ID',
    event_type      VARCHAR(64)   NULL COMMENT '事件类型候选',
    impact_scope    VARCHAR(32)   NULL COMMENT '影响层级（OVERALL/INDUSTRY/COMPANY/COMMODITY）',
    impact_level    VARCHAR(32)   NULL COMMENT '重大程度（NORMAL/IMPORTANT/MAJOR_RISK）',
    direction       VARCHAR(32)   NULL COMMENT '方向（ResearchDirection）',
    subject_type    VARCHAR(32)   NULL COMMENT '主体类型',
    subject_id      BIGINT        NULL COMMENT '主体ID',
    status          VARCHAR(32)   NOT NULL DEFAULT 'NEW' COMMENT '事件状态（ResearchEventStatus）',
    version         VARCHAR(32)   NOT NULL DEFAULT '1.0' COMMENT '版本',
    source_refs     VARCHAR(1000) NULL COMMENT '来源/材料引用',
    idempotency_key VARCHAR(128)  NOT NULL COMMENT '幂等键',
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (pk_id),
    UNIQUE KEY uk_research_event_code (event_code),
    UNIQUE KEY uk_research_event_idempotency (idempotency_key),
    KEY idx_research_event_status (status, create_time),
    KEY idx_research_event_subject (subject_type, subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='核验后的研究事件主表';

-- 事件证据（§19.1）：原文/来源/发布时间/哈希/事实观点类型
CREATE TABLE quantpf_event_evidence (
    pk_id             BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    research_event_id BIGINT       NOT NULL COMMENT '研究事件ID（关联 quantpf_research_event.pk_id）',
    evidence_type     VARCHAR(32)  NULL COMMENT '证据类型（FACT/OPINION/COUNTER）',
    source_id         BIGINT       NULL COMMENT '来源数据源ID',
    source_ref        VARCHAR(255) NULL COMMENT '来源引用',
    published_at      DATETIME     NULL COMMENT '原文发布时间',
    content_hash      CHAR(64)     NULL COMMENT '内容哈希',
    quote_text        TEXT         NULL COMMENT '原文片段引用',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (pk_id),
    KEY idx_event_evidence_event (research_event_id),
    CONSTRAINT fk_event_evidence_event FOREIGN KEY (research_event_id) REFERENCES quantpf_research_event (pk_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究事件证据链表';