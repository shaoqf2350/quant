-- =====================================================================
-- QuantPF 模块一「数据与标的基础」：数据源 / 来源组 / 采集任务 / 原始快照 / 数据质量
-- 对应《详设_1_数据与标的基础详细设计方案.md》§5 / §6 / §7 / §8 / §9
-- 业务表主键统一 pk_id BIGINT（Hutool Snowflake 生成，应用层填充，不用自增）
-- =====================================================================

-- 数据源/供应商/数据产品登记表（§5.1）
CREATE TABLE quantpf_data_source (
    pk_id           BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    source_code     VARCHAR(64)  NOT NULL COMMENT '数据源编码（如 TUSHARE_PRO）',
    source_name     VARCHAR(128) NOT NULL COMMENT '数据源名称',
    vendor_name     VARCHAR(128) NULL COMMENT '供应商组织名',
    account_type    VARCHAR(32)  NULL COMMENT '账号类型',
    contract_ref    VARCHAR(128) NULL COMMENT '合同/订阅标识',
    expiry_date     DATE         NULL COMMENT '到期日',
    primary_source  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否主源（1是/0否）',
    status          VARCHAR(32)  NOT NULL DEFAULT 'DRAFT' COMMENT '生命周期状态（DataSourceStatus）',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    remark          VARCHAR(500) NULL COMMENT '备注',
    PRIMARY KEY (pk_id),
    UNIQUE KEY uk_data_source_code (source_code),
    CONSTRAINT ck_data_source_status CHECK (status IN ('DRAFT','PENDING_CONFIRMATION','ACTIVE','DEGRADED','EXPIRING','EXPIRED','SUSPENDED','RETIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据源/供应商/数据产品登记表';

-- 数据源接口目录表（§5.2，接口级下钻）
CREATE TABLE quantpf_data_source_interface (
    pk_id                BIGINT        NOT NULL COMMENT '主键（雪花ID）',
    source_id            BIGINT        NOT NULL COMMENT '数据源ID（关联 quantpf_data_source.pk_id）',
    product_code         VARCHAR(64)   NULL COMMENT '数据产品编码',
    endpoint_name        VARCHAR(64)   NOT NULL COMMENT '接口名（如 trade_cal / fut_trade_cal）',
    field_summary        VARCHAR(1000) NULL COMMENT '字段摘要',
    history_from         DATE          NULL COMMENT '历史数据起始',
    history_to           DATE          NULL COMMENT '历史数据截止',
    update_frequency     VARCHAR(32)   NULL COMMENT '更新频率（DAILY/MINUTELY/ON_DEMAND）',
    latency              VARCHAR(32)   NULL COMMENT '延迟（REALTIME/DELAYED_1D...）',
    quota_limit          VARCHAR(128)  NULL COMMENT '调用额度与并发限制',
    authorization_status VARCHAR(32)   NOT NULL DEFAULT 'PENDING' COMMENT '授权状态',
    create_time          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (pk_id),
    UNIQUE KEY uk_data_interface_source_endpoint (source_id, endpoint_name),
    CONSTRAINT fk_data_interface_source FOREIGN KEY (source_id) REFERENCES quantpf_data_source (pk_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据源接口目录表';

-- 数据源字段级授权目录表（§5.2，下钻到字段，不可只存“已配置”粗粒度）
CREATE TABLE quantpf_data_source_field (
    pk_id         BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    interface_id  BIGINT       NOT NULL COMMENT '接口ID（关联 quantpf_data_source_interface.pk_id）',
    field_name    VARCHAR(64)  NOT NULL COMMENT '源字段名（保留原始拼写）',
    internal_name VARCHAR(64)  NOT NULL COMMENT '内部标准化字段名',
    data_type     VARCHAR(32)  NULL COMMENT '数据类型/来源语义',
    usage_scope   VARCHAR(255) NOT NULL COMMENT '授权用途（逗号分隔，如 DISPLAY_ALLOWED,RESEARCH_ALLOWED）',
    history_from  DATE         NULL COMMENT '历史范围起始',
    history_to    DATE         NULL COMMENT '历史范围截止',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (pk_id),
    UNIQUE KEY uk_data_field_interface_name (interface_id, field_name),
    CONSTRAINT fk_data_field_interface FOREIGN KEY (interface_id) REFERENCES quantpf_data_source_interface (pk_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据源字段级授权目录表';

-- 来源组与交叉核验配置表（§6）
CREATE TABLE quantpf_source_group (
    pk_id             BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    group_code        VARCHAR(64)  NOT NULL COMMENT '来源组编码（NEWS_POLICY/MARKET_PRICE/TRADING_RULE...）',
    group_name        VARCHAR(128) NOT NULL COMMENT '来源组名称',
    primary_source_id BIGINT       NULL COMMENT '主源数据源ID',
    backup_source_ids VARCHAR(255) NULL COMMENT '备源数据源ID（逗号分隔）',
    verify_rule       VARCHAR(500) NULL COMMENT '交叉核验规则（发布时间/原文/转载/数值一致）',
    reprint_policy    VARCHAR(255) NULL COMMENT '转载/去重识别策略',
    status            VARCHAR(32)  NOT NULL DEFAULT 'CONFIGURED' COMMENT '来源组状态（SourceGroupStatus）',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (pk_id),
    UNIQUE KEY uk_source_group_code (group_code),
    KEY idx_source_group_primary (primary_source_id),
    CONSTRAINT fk_source_group_primary FOREIGN KEY (primary_source_id) REFERENCES quantpf_data_source (pk_id),
    CONSTRAINT ck_source_group_status CHECK (status IN ('CONFIGURED','HEALTHY','PARTIALLY_AVAILABLE','CONFLICTED','DEGRADED','UNAVAILABLE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='来源组与交叉核验配置表';

-- 数据采集任务表（§7.4 状态机）
CREATE TABLE quantpf_data_collection_task (
    pk_id           BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    task_code       VARCHAR(64)  NOT NULL COMMENT '采集任务编码',
    source_id       BIGINT       NULL COMMENT '数据源ID',
    source_group_id BIGINT       NULL COMMENT '来源组ID',
    task_type       VARCHAR(32)  NOT NULL COMMENT '采集类型（NEWS/KLINE/CALENDAR/FINANCIAL/RULE）',
    request_params  TEXT         NULL COMMENT '请求参数（JSON，保留原始值）',
    status          VARCHAR(32)  NOT NULL DEFAULT 'CREATED' COMMENT '任务状态（CollectionTaskStatus）',
    success_scope   VARCHAR(500) NULL COMMENT '成功范围（PARTIAL_SUCCESS 时必须列明）',
    failure_scope   VARCHAR(500) NULL COMMENT '失败范围',
    error_code      VARCHAR(64)  NULL COMMENT '异常码',
    retry_count     INT          NOT NULL DEFAULT 0 COMMENT '重试次数',
    started_at      DATETIME     NULL COMMENT '开始时间',
    finished_at     DATETIME     NULL COMMENT '结束时间',
    scheduler_type  VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '调度类型（PENDING/XXL_JOB）',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (pk_id),
    UNIQUE KEY uk_collection_task_code (task_code),
    KEY idx_collection_task_status_time (status, create_time),
    CONSTRAINT ck_collection_task_status CHECK (status IN ('CREATED','RUNNING','SUCCEEDED','PARTIAL_SUCCESS','FAILED','RETRYING','CANCELED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据采集任务表';

-- 原始数据快照表（§7.2/§7.3 原始响应优先 + 哈希去重）
CREATE TABLE quantpf_data_raw_snapshot (
    pk_id                   BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    snapshot_id             VARCHAR(64)  NOT NULL COMMENT '快照业务ID',
    source_id               BIGINT       NULL COMMENT '数据源ID',
    source_group_id         BIGINT       NULL COMMENT '来源组ID',
    external_id             VARCHAR(128) NULL COMMENT '外部ID',
    request_time            DATETIME     NULL COMMENT '请求时间',
    observed_at             DATETIME     NULL COMMENT '系统观察时间',
    published_at            DATETIME     NULL COMMENT '来源发布时间',
    available_at            DATETIME     NULL COMMENT '系统可获得时间',
    content_hash            CHAR(64)     NOT NULL COMMENT '内容哈希（SHA-256，去重键）',
    normalized_title_hash   CHAR(64)     NULL COMMENT '标准化标题哈希（新闻/文档去重）',
    raw_content_reference   VARCHAR(500) NULL COMMENT '原始内容引用路径（对象存储/大文本）',
    response_status         VARCHAR(32)  NULL COMMENT '响应状态',
    parser_version          VARCHAR(32)  NOT NULL DEFAULT '1.0' COMMENT '解析器版本',
    response_schema_version VARCHAR(32)  NULL COMMENT '来源响应 Schema 版本',
    authorization_status    VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '授权状态',
    idempotency_key         VARCHAR(128) NOT NULL COMMENT '幂等键（source+externalId/哈希）',
    create_time             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (pk_id),
    UNIQUE KEY uk_raw_snapshot_snapshot_id (snapshot_id),
    UNIQUE KEY uk_raw_snapshot_idempotency (idempotency_key),
    KEY idx_raw_snapshot_source_content (source_id, content_hash),
    KEY idx_raw_snapshot_observed (observed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='原始数据快照表（保留原始响应与哈希）';

-- 数据质量问题记录表（§9.5 生命周期）
CREATE TABLE quantpf_data_quality_issue (
    pk_id           BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    issue_code      VARCHAR(64)  NOT NULL COMMENT '问题编码',
    snapshot_id     BIGINT       NULL COMMENT '关联原始快照ID',
    source_id       BIGINT       NULL COMMENT '数据源ID',
    subject_type    VARCHAR(32)  NULL COMMENT '影响对象类型（MATERIAL/MARKET_DATA/INSTRUMENT/CALENDAR）',
    subject_id      BIGINT       NULL COMMENT '影响对象ID',
    dimension       VARCHAR(32)  NOT NULL COMMENT '质量维度（COMPLETENESS/VALIDITY/IDENTITY/TIME/AUTHORIZATION/CONSISTENCY/TIMELINESS/REVISION）',
    quality_status  VARCHAR(32)  NOT NULL DEFAULT 'PENDING_REVIEW' COMMENT '数据质量状态（DataQualityStatus §9.2）',
    issue_status    VARCHAR(32)  NOT NULL DEFAULT 'OPEN' COMMENT '问题生命周期状态（DataQualityIssueStatus §9.5）',
    severity        VARCHAR(32)  NOT NULL DEFAULT 'MEDIUM' COMMENT '严重级别（LOW/MEDIUM/HIGH/CRITICAL）',
    business_impact VARCHAR(500) NULL COMMENT '业务影响（影响哪些功能）',
    handler         VARCHAR(64)  NULL COMMENT '处理人',
    action_taken    VARCHAR(500) NULL COMMENT '处理动作',
    basis_ref       VARCHAR(500) NULL COMMENT '处理依据',
    fix_version     VARCHAR(32)  NULL COMMENT '修复版本',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (pk_id),
    UNIQUE KEY uk_data_quality_issue_code (issue_code),
    KEY idx_data_quality_subject (subject_type, subject_id),
    KEY idx_data_quality_status (issue_status, quality_status),
    CONSTRAINT ck_data_quality_qstatus CHECK (quality_status IN ('AVAILABLE','DELAYED','PARTIAL','CONFLICTED','UNAUTHORIZED','STALE','INVALID','PENDING_REVIEW')),
    CONSTRAINT ck_data_quality_istatus CHECK (issue_status IN ('OPEN','ANALYZING','WAITING_SOURCE','WAITING_HUMAN','RESOLVED','VERIFIED','CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据质量问题记录表';