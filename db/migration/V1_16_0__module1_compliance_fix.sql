-- =====================================================================
-- QuantPF 模块一「数据与标的基础」：代码审查合规修正迁移
-- 依据《详设_1_数据与标的基础详细设计方案_代码落地指导文档.md》§5/§9/§10/§12 与审查结论：
--   1. 研究任务状态机补充 PENDING_REVIEW（AI 草稿校验未过/适配器异常时任务转待复核，不判失败）；
--   2. 交易日记录补充 is_open 原始值列（§12.2.6：原始值与标准化值同时保存，解析失败不得默认开市）
--      及版本字段（valid_from/valid_to/current_flag，§12.3.2）；
--   3. 交易时段模板补充市场本地时区与生效区间（§12.3.3）；
--   4. 数据源表补充供应商商务字段（service_scope/fee_info/health_status，§5.1）；
--   5. 原始快照补充来源组维度索引 idx_raw_snapshot_group_time（§5.2）。
-- =====================================================================

-- 1. 研究任务状态机扩展：PENDING_REVIEW（待人工复核，详设_1 §10.4 / 详设_0 §5.7 降级语义）
ALTER TABLE quantpf_research_task
    DROP CONSTRAINT ck_research_task_status;
ALTER TABLE quantpf_research_task
    ADD CONSTRAINT ck_research_task_status
    CHECK (status IN ('NEW','RUNNING','PENDING_REVIEW','SUCCEEDED','FAILED','CANCELED','RETRYING'));

-- 2. 交易日记录：is_open 原始值 + 版本管理字段（§12.2.6 / §12.3.2）
ALTER TABLE quantpf_calendar_day
    ADD COLUMN is_open_raw VARCHAR(8) NULL COMMENT '来源 is_open 原始值（字符串/整数原样保留，解析失败置 INVALID）' AFTER is_open,
    ADD COLUMN valid_from DATETIME NULL COMMENT '有效开始' AFTER version,
    ADD COLUMN valid_to DATETIME NULL COMMENT '有效结束' AFTER valid_from,
    ADD COLUMN current_flag TINYINT(1) NOT NULL DEFAULT 1 COMMENT '当前有效版本标记' AFTER valid_to;

-- 3. 交易时段模板：市场本地时区 + 生效区间（§12.3.3）
ALTER TABLE quantpf_calendar_session
    ADD COLUMN market_timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai' COMMENT '市场本地时区' AFTER session_type,
    ADD COLUMN effective_from DATE NULL COMMENT '生效开始',
    ADD COLUMN effective_to DATE NULL COMMENT '生效结束';

-- 4. 数据源：供应商商务字段（§5.1 供应商登记）
ALTER TABLE quantpf_data_source
    ADD COLUMN service_scope VARCHAR(255) NULL COMMENT '服务周期与共享/商业使用范围' AFTER contract_ref,
    ADD COLUMN fee_info VARCHAR(255) NULL COMMENT '费用说明' AFTER service_scope,
    ADD COLUMN health_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '健康状态（DataSourceStatus）' AFTER primary_source;

-- 5. 原始快照：来源组维度时间索引（§5.2 idx_raw_snapshot_group_time）
CREATE INDEX idx_raw_snapshot_group_time ON quantpf_data_raw_snapshot (source_group_id, observed_at);
