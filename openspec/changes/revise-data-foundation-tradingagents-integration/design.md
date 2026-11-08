# Design

## Context

参见 `proposal.md` 的 Why。本设计在以下已核查事实和约束上工作：

- 后端源码根为 `D:\workspace\quant\RuoYi-Vue-Plus`，已有 `quantpf-domain`、`quantpf-marketdata`、`quantpf-research`、`quantpf-job` 等模块；当前模块一大部分仍是骨架，已有 Paper 域必须受回归保护。
- 前端源码根为 `D:\workspace\quant\plus-ui`，采用 Vue 3、Element Plus、TypeScript 和若依动态菜单；尚无完整 `src/views/quant` 与 `src/api/quant` 实现。
- 当前根 OpenSpec 没有已归档主规格；模块一已有四个在途 change，分别覆盖数据库、代码、UI 与整合交付。本设计作为 V5/TradingAgents 引入后的统一复核入口，实施时须消除而不是复制它们之间的冲突。
- Spring Boot 是唯一权威业务事实源；Dify 是模型、Prompt、RAG 与固定 Workflow 编排层；TradingAgents 是由 Dify 按需调用的独立多智能体研究引擎。
- 模块一的确定性数据能力不得依赖 AI 可用性，AI 不得直连 QuantPF MySQL、Redis、DuckDB、MinIO 管理接口、Paper 事实库或交易写 API。
- 一期采用 REST/OpenAPI、SSE 和结构化 JSON；MCP 不作为三方生产主通信协议。

## Goals / Non-Goals

**Goals:**

- 为模块一形成可独立实施的数据库、用户界面和代码仓落地设计，明确现有表、演进表、新增表、字段、约束、索引和 AI 数据流。
- 把 TradingAgents 加在正确的位置：只增强获准材料的复杂研究、辩论和反方分析，不介入采集、数据质量、授权、点时、标的身份、日历、行情资格和交易执行。
- 统一 `Spring Boot → Dify → TradingAgents Adapter → Dify → Spring Boot` 的主链路、运行关联、错误、幂等、安全和审计。
- 为管理员、审核用户和普通研究用户定义可验证的页面入口与操作流程。
- 将设计映射到两个实际代码仓的目录、关键类、接口、配置、契约与测试。

**Non-Goals:**

- 不在本 change 编写 Java、Vue、SQL、Docker Compose、Dify DSL 或 TradingAgents Python 实现。
- 不引入 LIVE/QMT/CTP、真实下单、自动开平仓、分钟/Tick 用户页面或二期自主 Agent 循环。
- 不让 Dify Dataset 取代 MySQL/DuckDB/MinIO，不让 TradingAgents 保存 QuantPF 权威事实。
- 不修改若依 `sys_*` 底座表的既有语义，也不改写历史 Flyway 文件。

## Decisions

### Decision 1：TradingAgents 只作为 Dify 按需编排的研究工具

标准生产链路：

```text
Vue/浏览器
→ Spring Boot：鉴权、任务、输入快照、幂等、权威上下文
→ Dify Workflow：Prompt、模型、RAG、路由
→〔复杂任务才调用〕TradingAgents Adapter（HTTPS REST/JSON）
→ TradingAgents Engine：多角色分析与辩论
→ Dify：保留观点冲突并汇总、Schema初检
→ Spring Boot：完整校验、AI_DRAFT落库、人工审核/状态推进
```

简单事实抽取、摘要、格式转换和确定性状态解释不调用 TradingAgents。Spring Boot 直连 Adapter 仅允许健康、状态、取消、运维和显式审批的降级/离线任务，并必须使用不同 `execution_route`。选择此方案是为了避免两套 Prompt、模型、引用规则和审计链；替代方案“Spring Boot 默认直调 TradingAgents”被否决。

### Decision 2：存储按权威性和数据体量分层

| 存储 | 权威内容 | 禁止内容 |
|:---|:---|:---|
| MySQL | 数据源/授权/质量/点时、标的/日历、任务、AI运行索引、草稿验证、研究事件与审计 | 大体量逐笔、向量、明文密钥 |
| DuckDB | K线、预留Tick及本地分析型历史数据 | 业务状态、账户、订单、AI Workflow状态 |
| MinIO/OSS | 原始快照、材料正文、超阈值Dify/TradingAgents原始响应、导出和附件 | 未授权公开链接、可执行密钥 |
| Redis | 幂等短锁、限流、Nonce、防重放、短期任务进度缓存 | 权威任务状态、永久审计 |
| Dify Dataset/向量库 | 获准文本知识和引用元数据 | 行情、因子、概率、订单、持仓、风险事实 |
| TradingAgents内部存储 | 运行临时状态/缓存 | QuantPF权威材料、事实和交易状态 |

### Decision 3：MySQL 采用若依兼容的统一数据规范

所有新增 `quantpf_*` 业务表使用应用生成的有符号 `BIGINT` 雪花主键，不使用 `AUTO_INCREMENT`。枚举字段使用 `VARCHAR` 加应用枚举校验；关键不变量使用 `CHECK`（以目标 MySQL 8 实测为准）。时间统一 `DATETIME(6)` 并明确业务时区；Hash 使用 `CHAR(64)`；正文/大JSON超过阈值转 MinIO。每张业务表列尾固定：

```text
create_by BIGINT NULL
create_time DATETIME(6) NOT NULL
update_by BIGINT NULL
update_time DATETIME(6) NOT NULL
remark VARCHAR(500) NULL
```

逻辑删除只用于适合配置类对象，快照、质量、运行、草稿、证据和审计记录禁止物理删除或逻辑覆盖。外键是否创建物理约束由迁移压测决定，但字段类型、唯一约束和 Repository 校验必须一致。

## 核心章节一：数据库详细设计

### 1. 表清单与状态

#### 1.1 若依底座复用表

| 表 | 用途 | 本变更处理 |
|:---|:---|:---|
| `sys_user`、`sys_role`、`sys_user_role` | 用户、角色和权限主体 | 复用，不向AI发送密码、Token、邮箱/手机号 |
| `sys_menu` | QuantPF动态菜单和权限码 | 由菜单契约增量生成，不手工绕过契约 |
| `sys_dict_type`、`sys_dict_data` | UI枚举标签 | 只用于展示，领域枚举仍由代码/Schema校验 |
| `sys_config` | 非敏感功能开关 | 禁止存Dify/TradingAgents/API明文密钥 |
| `sys_oper_log` | 通用操作日志 | 关键业务审计另写 `quantpf_audit_record` |
| `sys_oss`、`sys_oss_config` | MinIO/OSS对象引用和配置 | 保存大对象引用；AI只获得已授权片段 |
| `sys_notice`、`sys_message` | 数据质量/研究运行通知 | 不放完整材料与凭据 |

#### 1.2 现有表兼容演进

| 表 | 主要现状/问题 | 演进决定 | 关键索引 |
|:---|:---|:---|:---|
| `quantpf_instrument` | 已有标的但身份/版本字段不足，历史主键可能自增 | 补 `instrument_kind/market_code/timezone/currency/future_product_id/source_id/identity_status/current_version_id/version`；移除新写自增 | UK(`instrument_code`,`exchange_code`,`instrument_kind`)，IX(`product_type`,`status`)，IX(`current_version_id`) |
| `quantpf_trading_calendar` | 日期、开闭市、时段混合 | 回填新日历分层表后冻结只读；不再新写 | 保留兼容查询索引 |
| `quantpf_market_bar` | MySQL K线与DuckDB分层冲突 | 对拍回填DuckDB后冻结只读 | 迁移期IX(`instrument_id`,`bar_time`) |
| `quantpf_research_news` | 缺授权、可见时间、快照/材料引用 | 补 `source_id/snapshot_id/material_id/published_at/available_at/effective_at/observed_at/revised_at/quality_status/authorization_status` | UK(`source_id`,`content_hash`)，IX(`available_at`)，IX(`material_id`) |
| `quantpf_financial_statement` | 缺点时与修订 | 补 `published_at/available_at/revision_no/snapshot_id/quality_status/authorization_status/current_flag` | UK(`instrument_id`,`report_period`,`statement_type`,`revision_no`)，IX(`available_at`) |
| `quantpf_audit_record` | 粗粒度detail JSON | 兼容增加 `trace_id/source_type/source_id/schema_version/snapshot_id`，敏感内容清洗 | IX(`trace_id`)，IX(`biz_type`,`biz_id`,`create_time`) |

#### 1.3 新增/补齐表域

| 表域 | 表 |
|:---|:---|
| 供应商与接口 | `quantpf_vendor`、`quantpf_data_product`、`quantpf_data_interface`、`quantpf_data_field`、`quantpf_data_source` |
| 来源组与采集 | `quantpf_source_group`、`quantpf_source_group_member`、`quantpf_collection_task`、`quantpf_collection_attempt`、`quantpf_data_raw_snapshot`、`quantpf_data_observation` |
| 质量、点时与资格 | `quantpf_data_quality_issue`、`quantpf_data_point_in_time`、`quantpf_data_use_eligibility` |
| 标的与日历 | `quantpf_future_product`、`quantpf_instrument_version`、`quantpf_instrument_relation`、`quantpf_trading_calendar_def`、`quantpf_trading_calendar_day`、`quantpf_trading_session_template`、`quantpf_instrument_calendar_binding`、`quantpf_calendar_source_snapshot`、`quantpf_calendar_conflict`、`quantpf_fx_session_rule` |
| 生命周期与行情摘要 | `quantpf_corporate_action`、`quantpf_contract_event`、`quantpf_market_data_readiness`、`quantpf_market_quote_snapshot`、`quantpf_market_state`、`quantpf_capacity_assessment`、`quantpf_quant_feature_snapshot` |
| 商品/现货预留 | `quantpf_spot_quote`、`quantpf_supply_demand_observation`、`quantpf_basis_term_structure` |
| 研究材料与AI | `quantpf_research_material`、`quantpf_research_material_relation`、`quantpf_research_task`、`quantpf_research_task_run`、`quantpf_dify_workflow_registry`、`quantpf_dify_invocation`、`quantpf_trading_agents_run`、`quantpf_trading_agents_agent_view`、`quantpf_ai_research_draft`、`quantpf_ai_validation_result`、`quantpf_research_event`、`quantpf_event_evidence` |
| Schema元数据 | `quantpf_schema_version`、`quantpf_schema_marker` |
| DuckDB | `qpf_schema_version`、`qpf_kline_bar`、`qpf_tick_data` |

### 2. 数据源、采集与治理表字段

以下字段表均省略统一五个尾字段，但DDL必须实际包含。

| 表 | 字段（类型与约束） | 索引/约束 | AI交互 |
|:---|:---|:---|:---|
| `quantpf_vendor` | `id BIGINT PK`；`vendor_code VARCHAR(64) NN`；`name VARCHAR(128) NN`；`account_scope VARCHAR(32) NN`；`contract_ref VARCHAR(128)`；`status VARCHAR(24) NN`；`service_start/service_end DATETIME(6)`；`terms_url VARCHAR(512)`；`notes JSON` | UK(`vendor_code`)；IX(`status`,`service_end`) | 禁止直读，只传授权摘要 |
| `quantpf_data_product` | `id`; `vendor_id BIGINT NN`; `product_code/name VARCHAR(128) NN`; `package_ref VARCHAR(128)`; `quota_limit DECIMAL(24,8)`; `quota_unit VARCHAR(32)`; `history_from/history_to DATE`; `latency_ms INT`; `save_scope/share_scope VARCHAR(32)`; `status VARCHAR(24)`; `expires_at DATETIME(6)` | UK(`vendor_id`,`product_code`)；IX(`status`,`expires_at`) | 不传合同/账号 |
| `quantpf_data_interface` | `id`; `product_id BIGINT NN`; `interface_code/name VARCHAR(128) NN`; `endpoint_ref VARCHAR(256)`; `transport VARCHAR(16)`; `request_schema_version/response_schema_version VARCHAR(32)`; `frequency_limit/concurrency_limit INT`; `permission_status/verification_status/status VARCHAR(24)`; `raw_doc_ref VARCHAR(512)` | UK(`product_id`,`interface_code`,`response_schema_version`)；IX(`status`,`verification_status`) | 仅传能力摘要，隐藏端点凭据 |
| `quantpf_data_field` | `id`; `interface_id BIGINT NN`; `field_code/name VARCHAR(128) NN`; `data_type/unit/currency/timezone VARCHAR(32)`; `nullable_flag/required_flag/save_allowed/share_allowed TINYINT NN`; `source_type/availability_status VARCHAR(24)`; `raw_field_name VARCHAR(128)`; `description VARCHAR(1000)` | UK(`interface_id`,`field_code`,`schema_version`)；IX(`availability_status`) | 构建字段白名单 |
| `quantpf_data_source` | `id`; `source_code/name VARCHAR(128) NN`; `vendor_id/product_id/interface_id BIGINT`; `source_level VARCHAR(8)`; `source_kind VARCHAR(32)`; `authorization_status/health_status/status VARCHAR(24)`; `usage_scope JSON`; `coverage JSON`; `priority INT`; `last_success_at/last_failure_at DATETIME(6)`; `failure_count INT`; `fallback_source_id BIGINT`; `version INT NN` | UK(`source_code`)；IX(`source_kind`,`status`,`priority`)；IX(`health_status`,`last_success_at`) | 只传获准来源ID、质量与用途 |
| `quantpf_source_group` | `id`; `group_code/name VARCHAR(128) NN`; `subject_type VARCHAR(32)`; `verification_policy JSON NN`; `min_independent_sources INT`; `status VARCHAR(24)`; `version INT` | UK(`group_code`)；IX(`subject_type`,`status`) | 可传核验规则摘要 |
| `quantpf_source_group_member` | `id`; `group_id/source_id BIGINT NN`; `role VARCHAR(24)`; `independence_group VARCHAR(64)`; `priority INT`; `valid_from/valid_to DATETIME(6)`; `status VARCHAR(24)` | UK(`group_id`,`source_id`,`valid_from`)；IX(`source_id`,`status`) | 约束“转载不算独立证据” |
| `quantpf_collection_task` | `id`; `task_key VARCHAR(128) NN`; `source_id BIGINT NN`; `task_type VARCHAR(32)`; `schedule_ref VARCHAR(128)`; `request_hash CHAR(64)`; `status VARCHAR(24)`; `planned_at/started_at/finished_at DATETIME(6)`; `attempt_count INT`; `idempotency_key VARCHAR(192) NN`; `error_code VARCHAR(64)` | UK(`idempotency_key`)；IX(`status`,`planned_at`)；IX(`source_id`,`started_at`) | AI不参与 |
| `quantpf_collection_attempt` | `id`; `task_id BIGINT NN`; `attempt_no INT NN`; `status VARCHAR(24)`; `http_status INT`; `started_at/finished_at DATETIME(6)`; `bytes BIGINT`; `snapshot_id BIGINT`; `error_code/message VARCHAR(64/1000)` | UK(`task_id`,`attempt_no`)；IX(`status`,`started_at`) | AI不参与 |
| `quantpf_data_raw_snapshot` | `id`; `source_id/interface_id BIGINT`; `external_key VARCHAR(256)`; `content_hash CHAR(64) NN`; `content_type VARCHAR(128)`; `object_ref VARCHAR(512)`; `inline_payload MEDIUMTEXT`; `payload_size BIGINT`; `published_at/observed_at/available_at DATETIME(6)`; `authorization_status/quality_status VARCHAR(24)`; `schema_version VARCHAR(32)` | UK(`source_id`,`content_hash`)；IX(`available_at`)；CHECK对象引用与行内载荷至少一个存在 | 仅由Spring Boot裁剪后供给 |
| `quantpf_data_observation` | `id`; `snapshot_id/source_id BIGINT NN`; `observation_key VARCHAR(192)`; `observed_at DATETIME(6)`; `value_hash CHAR(64)`; `status VARCHAR(24)` | UK(`source_id`,`observation_key`,`observed_at`)；IX(`snapshot_id`) | AI不参与 |
| `quantpf_data_quality_issue` | `id`; `issue_key VARCHAR(192)`; `object_type VARCHAR(32)`; `object_id BIGINT`; `dimension VARCHAR(32)`; `severity/status VARCHAR(24)`; `detected_at/resolved_at DATETIME(6)`; `detector VARCHAR(64)`; `details JSON`; `impact_scope JSON`; `version INT` | UK(`issue_key`)；IX(`status`,`severity`,`detected_at`)；IX(`object_type`,`object_id`) | 可将问题摘要传入，不允许AI关闭 |
| `quantpf_data_point_in_time` | `id`; `object_type VARCHAR(32)`; `object_id/version_id BIGINT NN`; `published_at/available_at/effective_at/revised_at DATETIME(6)`; `valid_from/valid_to DATETIME(6)`; `current_flag TINYINT` | UK(`object_type`,`object_id`,`version_id`)；IX(`available_at`,`current_flag`) | 决定可见版本 |
| `quantpf_data_use_eligibility` | `id`; `object_type/object_id`; `usage_type VARCHAR(32)`; `eligibility_status VARCHAR(24)`; `reason_codes JSON`; `evaluated_at/expires_at DATETIME(6)`; `policy_version VARCHAR(32)`; `input_hash CHAR(64)` | UK(`object_type`,`object_id`,`usage_type`,`policy_version`)；IX(`usage_type`,`eligibility_status`,`expires_at`) | 调用前硬门禁 |

### 3. 标的、日历、行情与生命周期表字段

| 表 | 字段（类型与约束） | 索引/约束 | AI交互 |
|:---|:---|:---|:---|
| `quantpf_future_product` | `id BIGINT PK`; `exchange_code/product_code/name VARCHAR(64/128) NN`; `currency VARCHAR(16)`; `contract_multiplier DECIMAL(24,8)`; `price_tick DECIMAL(24,8)`; `delivery_method VARCHAR(32)`; `timezone VARCHAR(64)`; `status VARCHAR(24)` | UK(`exchange_code`,`product_code`)；IX(`status`) | 只传展示和研究摘要 |
| `quantpf_instrument_version` | `id`; `instrument_id BIGINT NN`; `version_no INT NN`; `name/symbol VARCHAR(128)`; `instrument_kind/product_type/status VARCHAR(32)`; `market_code/exchange_code/timezone/currency`; `attributes JSON`; `source_id/snapshot_id BIGINT`; `valid_from DATETIME(6) NN`; `valid_to DATETIME(6)` | UK(`instrument_id`,`version_no`)；IX(`instrument_id`,`valid_from`,`valid_to`) | Dify消费固定版本引用，不改身份 |
| `quantpf_instrument_relation` | `id`; `from_instrument_id/to_instrument_id BIGINT NN`; `relation_type VARCHAR(32)`; `valid_from/valid_to DATETIME(6)`; `source_id BIGINT`; `status VARCHAR(24)` | UK(`from_instrument_id`,`to_instrument_id`,`relation_type`,`valid_from`)；IX(`to_instrument_id`,`relation_type`) | 可传关系摘要 |
| `quantpf_trading_calendar_def` | `id`; `calendar_code/name VARCHAR(128) NN`; `market_code/exchange_code VARCHAR(32)`; `timezone VARCHAR(64) NN`; `source_group_id BIGINT`; `status VARCHAR(24)`; `version INT` | UK(`calendar_code`)；IX(`market_code`,`status`) | 只解释确定性结果 |
| `quantpf_trading_calendar_day` | `id`; `calendar_id BIGINT NN`; `trade_date DATE NN`; `open_flag TINYINT NN`; `previous/next_trade_date DATE`; `source_snapshot_id BIGINT`; `verification_status VARCHAR(24)`; `version INT` | UK(`calendar_id`,`trade_date`,`version`)；IX(`trade_date`,`open_flag`) | 不允许AI决定开闭市 |
| `quantpf_trading_session_template` | `id`; `template_code VARCHAR(128) NN`; `timezone VARCHAR(64)`; `sessions JSON NN`; `night_session_trade_date_rule VARCHAR(32)`; `valid_from/valid_to DATETIME(6)`; `status VARCHAR(24)` | UK(`template_code`,`valid_from`)；IX(`status`) | 只传已计算时段 |
| `quantpf_instrument_calendar_binding` | `id`; `instrument_id/calendar_id/session_template_id BIGINT NN`; `valid_from DATETIME(6) NN`; `valid_to DATETIME(6)`; `priority INT`; `status VARCHAR(24)` | UK(`instrument_id`,`valid_from`)；IX(`calendar_id`,`status`) | AI不修改 |
| `quantpf_calendar_source_snapshot` | `id`; `calendar_id/source_id/snapshot_id BIGINT NN`; `trade_date DATE NN`; `open_flag TINYINT`; `raw_value JSON`; `observed_at DATETIME(6)` | UK(`calendar_id`,`source_id`,`trade_date`,`snapshot_id`)；IX(`trade_date`) | 可作为冲突材料引用 |
| `quantpf_calendar_conflict` | `id`; `calendar_id BIGINT`; `trade_date DATE`; `left_snapshot_id/right_snapshot_id BIGINT`; `status/severity VARCHAR(24)`; `resolution VARCHAR(32)`; `resolved_by BIGINT`; `resolved_at DATETIME(6)`; `reason VARCHAR(1000)`; `version INT` | UK(`calendar_id`,`trade_date`,`left_snapshot_id`,`right_snapshot_id`)；IX(`status`,`trade_date`) | TradingAgents可解释冲突，不能裁决 |
| `quantpf_fx_session_rule` | `id`; `instrument_id/market_code`; `timezone`; `week_rule JSON`; `holiday_policy JSON`; `dst_policy JSON`; `valid_from/valid_to`; `status` | UK(`instrument_id`,`valid_from`)；IX(`status`) | AI只读摘要 |
| `quantpf_corporate_action` | `id`; `instrument_id BIGINT`; `action_type VARCHAR(32)`; `announcement/effective/record/pay_date`; `ratio/amount DECIMAL(24,8)`; `currency`; `snapshot_id/source_id`; `status/version` | UK(`instrument_id`,`action_type`,`effective_date`,`version`)；IX(`effective_date`,`status`) | 只传已治理事件 |
| `quantpf_contract_event` | `id`; `instrument_id BIGINT`; `event_type`; `event_time`; `last_trade_date/delivery_start/end DATE`; `source_id/snapshot_id`; `status/version` | UK(`instrument_id`,`event_type`,`event_time`,`version`)；IX(`event_time`,`status`) | 不生成移仓订单 |
| `quantpf_market_data_readiness` | `id`; `instrument_id BIGINT`; `trade_date DATE`; `timeframe`; `expected_at/received_at/verified_at`; `readiness_status`; `quality_status`; `strategy_ready/paper_ready TINYINT`; `reason_codes JSON`; `input_hash` | UK(`instrument_id`,`trade_date`,`timeframe`)；IX(`readiness_status`,`trade_date`) | Dify只解释 |
| `quantpf_market_quote_snapshot` | `id`; `instrument_id`; `quote_time`; `last/bid/ask DECIMAL(24,8)`; `bid_size/ask_size/volume/turnover/open_interest DECIMAL(30,8)`; `source_snapshot_id`; `quality_status` | UK(`instrument_id`,`quote_time`,`source_snapshot_id`)；IX(`quote_time`) | 仅传聚合，非原始盘口 |
| `quantpf_market_state` | `id`; `market_code`; `state_time`; `state_type`; `state_value`; `reason_codes JSON`; `source_snapshot_refs JSON`; `valid_to`; `version` | UK(`market_code`,`state_time`,`state_type`,`version`)；IX(`state_type`,`state_time`) | AI不重新计算 |
| `quantpf_capacity_assessment` | `id`; `instrument_id`; `as_of_time`; `capacity_status`; `adv/turnover/open_interest DECIMAL(30,8)`; `estimated_impact_bps DECIMAL(12,6)`; `method_version`; `input_hash`; `valid_to` | UK(`instrument_id`,`as_of_time`,`method_version`)；IX(`capacity_status`,`as_of_time`) | 仅解释，不决定仓位 |
| `quantpf_quant_feature_snapshot` | `id`; `instrument_id`; `as_of_time`; `timeframe`; `feature_version`; `features JSON`; `source_hash`; `quality_status`; `available_at`; `valid_to` | UK(`instrument_id`,`as_of_time`,`timeframe`,`feature_version`)；IX(`available_at`,`quality_status`) | 只传授权子集 |
| `quantpf_spot_quote` | `id`; `spot_subject_id`; `quote_time`; `region/grade/unit/currency`; `price`; `source_id/snapshot_id`; `quality_status` | UK(`spot_subject_id`,`quote_time`,`source_id`) | 只读解释 |
| `quantpf_supply_demand_observation` | `id`; `subject_id`; `metric_code`; `period_start/end`; `value/unit`; `region/grade`; `source_id/snapshot_id`; `available_at`; `quality_status` | UK(`subject_id`,`metric_code`,`period_end`,`source_id`,`snapshot_id`)；IX(`available_at`) | 提供结构化供需摘要 |
| `quantpf_basis_term_structure` | `id`; `subject_id`; `as_of_time`; `spot_quote_id`; `near/far_instrument_id`; `basis/near_far_spread`; `structure_type`; `method_version`; `input_hash` | UK(`subject_id`,`as_of_time`,`method_version`)；IX(`structure_type`,`as_of_time`) | Dify不重算基差 |

### 4. 研究、Dify 与 TradingAgents 表字段

| 表 | 字段（类型与约束） | 索引/约束 | 写入者与边界 |
|:---|:---|:---|:---|
| `quantpf_research_material` | `id`; `material_key VARCHAR(192) NN`; `material_type/title`; `source_id/snapshot_id`; `object_ref`; `content_hash CHAR(64)`; `language`; `published_at/available_at/effective_at`; `authorization_status/quality_status`; `version_no`; `previous_material_id`; `current_flag` | UK(`material_key`,`version_no`)；UK(`source_id`,`content_hash`)；IX(`available_at`,`authorization_status`) | Spring Boot；AI只读授权版本 |
| `quantpf_research_material_relation` | `id`; `from_material_id/to_material_id`; `relation_type`; `independence_group`; `confidence_source`; `status` | UK(`from_material_id`,`to_material_id`,`relation_type`)；IX(`to_material_id`,`relation_type`) | Spring Boot确定转载/修订关系 |
| `quantpf_research_task` | `id`; `task_key`; `task_type/trigger_type`; `material_refs/event_refs/subject_refs/instrument_refs JSON`; `snapshot_id`; `published_at/available_at/as_of_time`; `authorization_status/quality_status/use_eligibility`; `research_question TEXT`; `period`; `workflow_version/prompt_version/schema_version`; `execution_route VARCHAR(32)`; `status`; `idempotency_key`; `owner_user_id`; `version INT` | UK(`idempotency_key`)；IX(`status`,`create_time`)；IX(`task_type`,`as_of_time`) | Spring Boot创建，AI不改状态 |
| `quantpf_research_task_run` | `id`; `task_id`; `run_no`; `status`; `invocation_id`; `retry_of`; `started_at/finished_at`; `error_code/message`; `output_draft_id`; `audit_ref`; `execution_route` | UK(`task_id`,`run_no`)；UK(`invocation_id`)；IX(`status`,`started_at`) | Spring Boot运行编排 |
| `quantpf_dify_workflow_registry` | `id`; `workflow_key`; `dify_app_ref`; `workflow_version`; `model_ref/model_version`; `prompt_version`; `input_schema_version/output_schema_version`; `knowledge_release`; `status`; `timeout_seconds`; `max_input_bytes/max_output_bytes`; `published_at`; `approved_by`; `environment` | UK(`workflow_key`,`workflow_version`,`environment`)；IX(`status`,`environment`) | 不保存API Key |
| `quantpf_dify_invocation` | `id`; `invocation_id`; `task_id/task_run_id`; `workflow_registry_id`; `dify_run_id`; `request_hash/response_hash`; `request_ref/response_ref`; `response_mode`; `status/http_status`; `started_at/finished_at`; `token_usage JSON`; `error_code/message`; `retry_of`; `idempotency_key` | UK(`invocation_id`)；UK(`idempotency_key`)；IX(`task_id`,`status`)；IX(`dify_run_id`) | 仅Spring Boot Adapter写 |
| `quantpf_trading_agents_run` | `id`; `ta_run_id VARCHAR(128) NN`; `task_id/task_run_id/dify_invocation_id BIGINT NN`; `research_mode VARCHAR(32)`; `adapter_version/engine_version/schema_version VARCHAR(32)`; `execution_route VARCHAR(32)`; `input_hash CHAR(64)`; `request_ref/response_ref VARCHAR(512)`; `status VARCHAR(24)`; `max_rounds/actual_rounds INT`; `started_at/finished_at`; `token_usage/cost_usage JSON`; `error_code/message`; `retry_of BIGINT`; `idempotency_key VARCHAR(192)` | UK(`ta_run_id`)；UK(`idempotency_key`)；IX(`task_id`,`status`)；IX(`dify_invocation_id`)；CHECK(`max_rounds` BETWEEN 1 AND 2一期) | Adapter返回，Spring Boot经Dify结果/管理查询登记；不含权威事实 |
| `quantpf_trading_agents_agent_view` | `id`; `ta_run_id BIGINT NN`; `agent_role VARCHAR(64) NN`; `sequence_no INT`; `status VARCHAR(24)`; `view_payload JSON`; `citation_refs JSON`; `model_ref/model_version`; `started_at/finished_at`; `error_code`; `content_hash CHAR(64)` | UK(`ta_run_id`,`agent_role`,`sequence_no`)；IX(`ta_run_id`,`status`) | 保留原始角色观点，不作为事实 |
| `quantpf_ai_research_draft` | `id`; `task_run_id`; `invocation_id`; `trading_agents_run_id`; `raw_response_ref`; `structured_payload JSON`; `schema_status/citation_status/time_visibility_status/authorization_status/subject_status/conflict_status/overall_status`; `failure_reasons JSON`; `model/workflow/prompt/schema/knowledge versions`; `content_hash` | UK(`invocation_id`)；IX(`overall_status`,`create_time`)；IX(`trading_agents_run_id`) | Spring Boot写；固定AI_DRAFT语义 |
| `quantpf_ai_validation_result` | `id`; `draft_id`; `validation_type`; `status`; `rule_version`; `details JSON`; `blocking_flag TINYINT`; `validated_at`; `validator_type`; `validator_id`; `input_hash` | UK(`draft_id`,`validation_type`,`rule_version`)；IX(`status`,`blocking_flag`) | 记录每层校验，禁止覆盖 |
| `quantpf_research_event` | `id`; `event_key/event_type`; `impact_scope/impact_level`; `direction_candidate/verified_direction`; `status`; `material_id/draft_id`; `version_no`; `previous_event_id/published_event_id`; `valid_from/valid_to`; `published_at/available_at`; `verified_by`; `verification_reason` | UK(`event_key`,`version_no`)；IX(`status`,`published_at`)；IX(`event_type`,`available_at`) | 只由Spring Boot发布 |
| `quantpf_event_evidence` | `id`; `event_id`; `evidence_type`; `material_id/snapshot_id/source_id`; `quote_text/quote_location`; `structured_field_ref`; `content_hash`; `published_at/available_at`; `verification_status`; `independence_group` | UK(`event_id`,`content_hash`,`quote_location`)；IX(`event_id`)；IX(`source_id`,`independence_group`) | AI引用先待核验 |

`quantpf_trading_agents_run` 和 `quantpf_trading_agents_agent_view` 是本次复核新增的明确设计，用来解决原有 Dify 草稿表无法表达多角色、部分成功和独立运行版本的问题；它们不授予 TradingAgents 数据库连接，记录仍由 Spring Boot 写入。

### 5. DuckDB 与对象存储

| 表 | 字段 | 约束/索引 |
|:---|:---|:---|
| `qpf_schema_version` | `version VARCHAR(32) PK`; `description VARCHAR(256) NN`; `installed_at TIMESTAMP`; `checksum CHAR(64)` | PK；UK(`checksum`,`version`) |
| `qpf_kline_bar` | `instrument_id BIGINT NN`; `instrument_code/exchange_code`; `timeframe VARCHAR(8)`; `bar_time TIMESTAMP`; `market_local_date DATE`; `timezone`; OHLC `DECIMAL(24,8)`; `volume/turnover/open_interest DECIMAL(30,8)`; `adjust_flag`; `source_code`; `source_snapshot_id/point_in_time_id BIGINT`; `import_batch_id`; `quality_status`; `ingested_at` | UK(`instrument_id`,`timeframe`,`bar_time`,`adjust_flag`,`source_code`)；IX(`instrument_id`,`timeframe`,`bar_time`)；IX(`market_local_date`)；CHECK(high>=low) |
| `qpf_tick_data` | `instrument_id`; `instrument_code/exchange_code`; `tick_time`; `market_local_date`; `timezone`; `last/bid/ask_price`; `bid/ask_volume`; `volume/turnover/open_interest`; `source_code/source_snapshot_id`; `quality_status`; `ingested_at` | UK(`instrument_id`,`tick_time`,`source_code`)；IX(`instrument_id`,`tick_time`)；一期仅结构预留 |

MinIO对象键建议：`quantpf/{env}/{objectType}/{yyyy}/{MM}/{id}/{sha256}`。MySQL保存 `object_ref/content_hash/payload_size/encryption_key_ref/retention_until`；下载必须经Spring Boot授权和审计，禁止生成长期公共URL。

### 6. Dify/TradingAgents 数据交互与事务

```text
事务A：Spring Boot校验授权和点时 → 保存ResearchTask/InputSnapshot/TaskRun(COMMITTED)
事务外：调用Dify SSE → Dify按需调用TradingAgents Adapter
事务B：保存DifyInvocation、TradingAgentsRun/AgentView、原始响应引用和AiDraft
事务C：逐项写AiValidationResult → 更新Draft总体状态
事务D：仅在校验/人工门禁通过后创建ResearchEvent/Evidence并发布领域事件
```

任何外部HTTP调用不得包在长数据库事务中。原始响应先落受控对象并计算Hash，再以唯一幂等键提交数据库；数据库提交失败可根据运行ID重建，外部重试不得创建第二个业务事件。

## 核心章节二：用户操作界面说明

### 1. 页面结构、路由和角色

| 页面 | 路由 | 主要角色 | 核心操作 | AI入口 |
|:---|:---|:---|:---|:---|
| 量化工作台 | `/quant/dashboard` | 全部 | 查看数据健康、市场日期、待办和AI服务状态 | 进入研究任务，不直接调用AI |
| 数据源与来源组 | `/quant/market/data-source` | 数据管理员 | 登记、测试、启停、优先级、授权和来源组 | 无TradingAgents；可查看影响说明 |
| 数据质量/快照 | `/quant/market/data-quality` | 管理员/审核员 | 查看问题、原始快照、点时版本、重试/复核 | 对复杂来源冲突可发起“辅助解释”，不自动裁决 |
| 标的中心 | `/quant/market/instrument` | 全部/管理员 | 查询身份、版本、关系、合约生命周期 | 仅解释已治理身份，不允许AI改标的 |
| 交易日历 | `/quant/market/calendar` | 全部/审核员 | 查看开闭市、时段、来源冲突并审核 | 可解释冲突；最终日历由人工/规则确认 |
| 行情与就绪 | `/quant/market` | 全部 | 日线、数据日期、来源、质量、资格 | Dify解释量价状态，不调用TA处理确定性缺失 |
| 研究材料详情 | `/quant/research/material/:id` | 研究/审核 | 查看正文、授权、版本、引用 | “提取事实”“分析影响”“深度研究/多空辩论” |
| 研究任务 | `/quant/research/task` | 研究/审核 | 创建、查看、取消、重试、补材料 | 展示Dify与TA阶段状态 |
| AI草稿与辩论 | `/quant/research/draft/:id` | 研究/审核 | 查看角色观点、共识、争议、引用和校验 | 提交审核/补证据，不执行交易 |
| 研究事件与证据 | `/quant/research/event/:id` | 研究/审核 | 查看核验事实、版本、证据链 | 解释已确认事件 |
| 研究助手 | `/quant/research/assistant` | 有权限用户 | 受控问答、创建任务意图 | 只通过Spring Boot，操作意图转确认表单 |

权限建议：`quant:data-source:*`、`quant:data-quality:*`、`quant:instrument:*`、`quant:calendar:*`、`quant:research:task:*`、`quant:research:draft:review`、`quant:research:debate`、`quant:research:event:*`。前端隐藏按钮不是安全边界，Controller必须再次校验Sa-Token权限与owner/tenant scope。

### 2. 管理员操作流程

```text
数据源页面新增来源
→ 填写供应商/产品/接口/授权用途（密钥一次性提交）
→ 服务端连接测试与字段能力登记
→ PENDING_CONFIRMATION
→ 管理员确认授权/保存权/频率
→ ENABLED
→ XXL-JOB采集
→ 快照、质量、点时和用途资格页面可追踪
```

失败时保留表单非敏感项，密钥不回显；未购买或未实测接口保持 `PENDING_CONFIRMATION`，不得用于正式研究、回测或Paper。

### 3. 审核用户操作流程

```text
质量待办/日历冲突/AI草稿
→ 对比来源、材料版本、引用与运行版本
→ 查看TradingAgents各角色观点（如存在）
→ 选择确认/驳回/补证据/重跑
→ 必填审核理由
→ Spring Boot校验expectedVersion
→ 保存审核记录并重新计算资格/发布获准事件
```

审核不得删除冲突来源或覆盖旧版本；并发版本变化时返回“内容已更新”，保留用户填写理由。

### 4. 普通研究用户操作流程与Dify触发位置

- 新闻/材料详情的“提取事实”：调用 `NEWS_FACT_EXTRACTION`，默认不调用TradingAgents。
- “分析影响”：调用 `NEWS_IMPACT_ANALYSIS`；遇复杂冲突可由Workflow条件路由到TradingAgents。
- “深度研究/多空辩论”：显式需要 `quant:research:debate`，调用 `TRADING_AGENTS_DEBATE`。
- 行情页“解释当前状态”：调用 `PRICE_VOLUME_EXPLANATION`，只解释Spring Boot已计算字段。
- 研究助手：先由Spring Boot查询授权事实，再交Dify组织回答；操作意图必须转确认表单。

任务详情分四层显示：①确定性输入快照；②Dify Workflow状态；③TradingAgents角色/轮次状态（按需）；④Spring Boot校验与人工审核。中间Token片段不标为最终事实。

### 5. 页面状态和异常文案

| 内部状态 | 页面文案 | 可用操作 |
|:---|:---|:---|
| `ACCEPTED/QUEUED` | 已提交，等待分析 | 取消、稍后查看 |
| `RUNNING_DIFY` | 正在整理材料 | 查看输入版本 |
| `RUNNING_TRADING_AGENTS` | 正在进行多视角研究 | 查看已完成角色，不显示最终结论 |
| `PARTIAL` | 已完成部分分析，缺少部分角色/证据 | 查看缺失项、补材料、重试 |
| `AI_DRAFT/VALIDATING` | AI草稿已生成，系统正在核验 | 等待、查看运行信息 |
| `WAITING_REVIEW` | 草稿待人工审核 | 审核角色确认/驳回 |
| `AUTHORIZATION_BLOCKED` | 当前材料授权不允许用于该分析 | 联系管理员 |
| `CITATION_MISSING` | 引用不足，不能升级为事实 | 补证据 |
| `TA_UNAVAILABLE` | 多智能体研究暂不可用 | 稍后重试或使用明确标注的单模型草稿 |
| `DIFY_UNAVAILABLE` | AI研究暂不可用，数据功能正常 | 查看结构化数据/已有结果 |
| `FAILED` | 分析未完成 | 查看原因、按幂等关系重试 |

所有草稿固定显示“AI生成、尚非交易建议”；页面不得提供从草稿直接创建订单的捷径。

## 核心章节三：代码仓库落地指导

### 1. 仓库布局与模块依赖

#### 后端：`D:\workspace\quant\RuoYi-Vue-Plus`

```text
RuoYi-Vue-Plus/
├─ db/migration/                         # Flyway权威迁移（不是新建sql根）
├─ script/sql/                           # 幂等字典/演示/运维种子，不存版本DDL
├─ contract/
│  ├─ openapi/                           # REST请求/响应Schema
│  ├─ events/                            # 领域事件Schema
│  ├─ error-codes/                       # 稳定错误码
│  └─ menu/quantpf-menu-facts.yaml       # 菜单权限事实源
├─ quantpf-modules/
│  ├─ quantpf-domain/src/main/java/com/quantpf/domain/
│  │  ├─ entity/                         # Instrument等领域聚合/值对象
│  │  ├─ repository/                     # Repository端口
│  │  └─ persistence/{record,mapper}/     # Record与MyBatis Mapper
│  ├─ quantpf-marketdata/src/main/java/com/quantpf/marketdata/
│  │  ├─ application/                    # 采集、质量、标的、日历、行情用例
│  │  ├─ infrastructure/{mysql,duckdb}/  # Repository与DuckDB Adapter
│  │  └─ scheduler/                      # XXL-JOB Handler薄适配器
│  ├─ quantpf-research/src/main/java/com/quantpf/research/
│  │  ├─ application/                    # 研究任务/草稿/验证用例
│  │  ├─ dify/                           # Dify REST/SSE适配器
│  │  ├─ tradingagents/                  # TA Adapter客户端/结果映射
│  │  └─ validation/                     # 引用、时间、授权、身份、安全校验
│  └─ quantpf-common/                    # ID、Actor、审计、错误、JSON契约
└─ ruoyi-admin/src/main/
   ├─ java/com/ruoyi/web/controller/     # 按实际包结构核对；量化Controller
   └─ resources/application*.yml         # 仅环境占位符，不提交真实密钥
```

目标源码已经有 `com.quantpf.domain` 与业务模块骨架；Controller的实际包路径施工前须以现有 `ruoyi-admin/src/main/java` 核查，勿照文档臆造包名。依赖方向为 `domain ← marketdata/research ← admin`；domain不得依赖Web、Dify SDK、XXL-JOB或TradingAgents实现。

#### 前端：`D:\workspace\quant\plus-ui`

```text
src/views/quant/
├─ dashboard/index.vue
├─ market/{data-source,data-quality,instrument,calendar,index}.vue
└─ research/{material,task,draft,event,assistant}.vue
src/api/quant/
├─ dataSource/index.ts
├─ dataQuality/index.ts
├─ snapshot/index.ts
├─ instrument/index.ts
├─ calendar/index.ts
├─ market/index.ts
├─ researchTask/index.ts
├─ dify/index.ts
├─ tradingAgents/index.ts
└─ researchEvent/index.ts
src/components/quant/
├─ AiRunTimeline.vue
├─ AgentRoleViews.vue
├─ EvidenceDrawer.vue
├─ DataFreshnessBadge.vue
└─ GuidedTask.vue
```

遵循现有API封装、权限指令、动态路由和Element Plus组件模式；菜单来自后端 `contract/menu/quantpf-menu-facts.yaml`，页面中不拼接Dify Host/Key。

### 2. 关键实体、服务与接口职责

| 类/接口建议 | 模块路径 | 职责 |
|:---|:---|:---|
| `Vendor`、`DataSource`、`SourceGroup` | `quantpf-domain/entity` | 来源治理聚合、不变量和状态 |
| `ResearchMaterial`、`ResearchTask` | `quantpf-domain/entity` | 授权材料快照与研究任务状态 |
| `AiInvocation`、`TradingAgentsRun`、`AgentView` | `quantpf-domain/entity` | 跨引擎运行关系、部分角色状态与版本 |
| `ResearchDraft`、`ValidationResult` | `quantpf-domain/entity` | AI草稿与逐项校验结果 |
| `DataSourceApplicationService` | `quantpf-marketdata/application` | 数据源维护、测试、启停和权限/授权守卫 |
| `CollectionApplicationService` | `quantpf-marketdata/application` | 采集计划、去重、重试、快照提交 |
| `QualityEligibilityService` | `quantpf-marketdata/application` | 质量、点时、用途资格和门禁计算 |
| `InstrumentCalendarService` | `quantpf-marketdata/application` | 标的版本、日历、冲突和市场就绪 |
| `DifyWorkflowClient` | `quantpf-research/dify` | Workflow API、认证、SSE、限时/限流 |
| `DifyEventAdapter` | `quantpf-research/dify` | 将原始SSE事件映射为内部事件 |
| `TradingAgentsAdapterClient` | `quantpf-research/tradingagents` | 受控REST/JSON调用、异步Run查询、版本兼容 |
| `ResearchContextAssembler` | `quantpf-research/application` | 从授权/点时快照组装最小上下文并去敏 |
| `ResearchOutputValidator` | `quantpf-research/validation` | Schema、引用范围、时间、授权、主体和冲突校验 |
| `ResearchTaskApplicationService` | `quantpf-research/application` | 幂等创建、取消、重试、结果合并、草稿保存 |
| `ResearchTaskController` | `ruoyi-admin` Controller | 用户API门面；只调用应用服务，不直写Mapper |
| `DifyCallbackController`（可选） | `ruoyi-admin` internal Controller | HMAC验签、防重放、仅投递校验队列 |
| `CollectionJobHandler` | `quantpf-job`/marketdata | XXL-JOB薄入口，调用采集应用服务 |

### 3. API与通信契约

#### QuantPF用户API（候选路径，最终以OpenAPI为准）

```text
POST /api/quant/research/tasks
GET  /api/quant/research/tasks/{taskId}
POST /api/quant/research/tasks/{taskId}/cancel
POST /api/quant/research/tasks/{taskId}/retry
GET  /api/quant/research/tasks/{taskId}/runs
GET  /api/quant/research/drafts/{draftId}
POST /api/quant/research/drafts/{draftId}/review
GET  /api/quant/market/data-readiness
POST /api/quant/data-sources/{sourceId}/test
POST /internal/quant/ai/dify/callback  # 可选
```

所有命令带 `requestId`、`Idempotency-Key`；修改版本对象带`expectedVersion`；响应包含业务ID、状态、可读摘要、运行阶段、证据和下一步。错误码至少包括 `AUTHORIZATION_BLOCKED`、`DATA_NOT_READY`、`DIFY_UNAVAILABLE`、`TRADING_AGENTS_UNAVAILABLE`、`OUTPUT_INVALID`、`CITATION_MISSING`、`STALE_VERSION`、`RATE_LIMITED`。

#### Dify客户端约定

- Workflow Service API：`POST {baseUrl}/v1/workflows/run`；`Authorization: Bearer`；`response_mode=streaming`；`Content-Type: application/json`；服务端WebClient消费SSE。
- 传入稳定脱敏的内部`user`、`workflowKey`解析出的已发布App、统一Envelope、最小点时上下文、材料引用和授权结论；禁止由用户提交任意App/Workflow ID。
- 调用前提交事务保存`invocationId`；网络调用事务外执行；完成后关联`difyRunId`并保存原始响应Hash/对象引用。
- 重试只针对可恢复错误，保持幂等键；Schema、授权、引用失败不重试为成功；超时后查询/恢复原run，不能盲目重复创建。
- SSE事件统一映射内部 `AiRunEvent`；UI通过Spring Boot任务状态端点或服务端推送接收，不暴露Dify API Key。

#### TradingAgents Adapter约定

```http
POST /internal/v1/trading-agents/runs
GET  /internal/v1/trading-agents/runs/{runId}
GET  /internal/v1/trading-agents/runs/{runId}/result
POST /internal/v1/trading-agents/runs/{runId}/cancel
```

使用内网HTTPS、服务认证、OpenAPI版本、Idempotency-Key、连接/读取超时、最大轮次、大小限制、限流和熔断。Dify正常Workflow发起研究Run；Spring Boot只在管理或显式降级Route调用同一Adapter。响应必须含`runId/status/agentViews/citations/unknowns/warnings/schemaVersion/engineVersion`。一期MCP非必需且禁作绕过HTTP Adapter的通道。

### 4. 新增配置项

配置放`application.yml`占位符及部署密钥环境变量/Secret Manager；敏感值绝不提交Git。建议：

```yaml
quantpf:
  ai:
    enabled: ${QUANTPF_AI_ENABLED:true}
    max-input-bytes: ${QUANTPF_AI_MAX_INPUT_BYTES:1048576}
    max-output-bytes: ${QUANTPF_AI_MAX_OUTPUT_BYTES:4194304}
  dify:
    enabled: ${QUANTPF_DIFY_ENABLED:true}
    base-url: ${QUANTPF_DIFY_BASE_URL:http://dify-api:5001}
    api-key: ${DIFY_APP_API_KEY:}
    environment: ${QUANTPF_DIFY_ENVIRONMENT:dev}
    workflow-registry-cache-seconds: ${QUANTPF_DIFY_REGISTRY_CACHE_SECONDS:60}
    connect-timeout-ms: ${QUANTPF_DIFY_CONNECT_TIMEOUT_MS:5000}
    read-timeout-ms: ${QUANTPF_DIFY_READ_TIMEOUT_MS:180000}
    max-attempts: ${QUANTPF_DIFY_MAX_ATTEMPTS:3}
    max-concurrency: ${QUANTPF_DIFY_MAX_CONCURRENCY:2}
    callback:
      enabled: ${QUANTPF_DIFY_CALLBACK_ENABLED:false}
      signing-secret: ${DIFY_CALLBACK_SIGNING_SECRET:}
      allowed-clock-skew-seconds: ${QUANTPF_DIFY_CALLBACK_CLOCK_SKEW_SECONDS:300}
  trading-agents:
    enabled: ${QUANTPF_TRADING_AGENTS_ENABLED:true}
    base-url: ${QUANTPF_TRADING_AGENTS_BASE_URL:http://trading-agents-adapter:8080}
    service-token: ${TRADING_AGENTS_SERVICE_TOKEN:}
    api-version: ${QUANTPF_TRADING_AGENTS_API_VERSION:v1}
    connect-timeout-ms: ${QUANTPF_TA_CONNECT_TIMEOUT_MS:3000}
    read-timeout-ms: ${QUANTPF_TA_READ_TIMEOUT_MS:120000}
    max-rounds: ${QUANTPF_TA_MAX_ROUNDS:2}
    max-polls: ${QUANTPF_TA_MAX_POLLS:20}
    poll-interval-ms: ${QUANTPF_TA_POLL_INTERVAL_MS:3000}
    circuit-breaker:
      failure-threshold: ${QUANTPF_TA_CB_FAILURE_THRESHOLD:5}
      open-seconds: ${QUANTPF_TA_CB_OPEN_SECONDS:60}
  storage:
    minio:
      endpoint: ${QUANTPF_MINIO_ENDPOINT:}
      bucket: ${QUANTPF_MINIO_BUCKET:quantpf-research}
      access-key: ${MINIO_ACCESS_KEY:}
      secret-key: ${MINIO_SECRET_KEY:}
```

生产启动校验：启用AI但无密钥时禁止匿名调用；dev/test/prod的App Key、Workflow版本、TA token、回调secret和模型供应商凭据分离；日志过滤Authorization、Token、正文敏感信息。

### 5. 菜单/API/权限映射

菜单新增项必须更新 `contract/menu/quantpf-menu-facts.yaml`，由既有生成/校验流程同步到数据库：

| 菜单 | 权限示例 | 页面API前缀 |
|:---|:---|:---|
| 数据源与来源组 | `quant:data-source:list/add/edit/test` | `/api/quant/data-sources` |
| 数据质量 | `quant:data-quality:list/review/resolve` | `/api/quant/data-quality` |
| 标的中心 | `quant:instrument:list/view` | `/api/quant/instruments` |
| 交易日历 | `quant:calendar:list/review` | `/api/quant/calendars` |
| 研究任务 | `quant:research:task:create/view/cancel/retry` | `/api/quant/research/tasks` |
| 深度研究/辩论 | `quant:research:debate` | 创建带`researchMode=DEBATE`的研究任务 |
| AI草稿审核 | `quant:research:draft:review` | `/api/quant/research/drafts` |
| 研究事件/证据 | `quant:research:event:view/publish` | `/api/quant/research/events` |

## Risks / Trade-offs

- **[Risk] 多文档和四个在途change产生重复/冲突** → 本change作为模块一统一实施入口，实施前生成差异矩阵；按数据库/代码/UI章节分别保留各源唯一权威，不重复复制矛盾字段。
- **[Risk] TradingAgents结果异构、部分失败或引用不稳定** → Adapter版本化Schema、按角色保存状态、提供PARTIAL，引用不通过阻断事实升级。
- **[Risk] Adapter与Dify双重重试放大负载** → 明确唯一重试责任、总尝试上限、幂等键、熔断和全局并发。
- **[Risk] 多智能体原文导致MySQL行膨胀或敏感内容泄漏** → 行内限长，超限MinIO加密引用，内容哈希/访问审计/保留期。
- **[Risk] MySQL现存自增unsigned/FK迁移造成停机或ID冲突** → 先备份和盘点FK图，以兼容视图/影子表/回填对拍分阶段切换，不改历史迁移。
- **[Risk] 普通用户把多空辩论当作交易决策** → UI固定标记AI_DRAFT，展示冲突、来源、风险和下一步，不暴露下单入口。
- **[Risk] 文件型DuckDB并发写和单机故障** → 单写入适配器、批量事务、持久化卷、校验备份；扩容前评估分析存储替代方案。
- **[Risk] 回调安全能力随Dify版本变化** → 默认SSE；回调仅在签名实现通过渗透/重放测试后启用。

## Migration Plan

1. 冻结模块一权威基线，逐项比对四个旧change与详设0/1/6；输出表/接口/页面/任务差异，不直接改旧change或历史迁移。
2. 在测试库盘点当前DDL、数据量、unsigned/autoincrement/FK、MyBatis ID生成和当前Paper表依赖；完成备份恢复演练。
3. 先部署兼容MySQL迁移及MySQL/DuckDB/MinIO存储结构，影子写入并核对Hash/行数/引用。
4. 部署Domain/Repository与数据服务，再发布API、错误码、事件和菜单契约；保持AI入口Feature Flag关闭。
5. 部署Dify Workflow注册和TradingAgents Adapter Stub；先验证单模型Workflow、再启用按需TA路由，限制测试数据和并发。
6. 发布前端页面/API，通过角色权限逐步开放数据治理、研究入口和审核页。
7. 以Mock/Stub完成端到端、安全、Paper回归和压力测试后，灰度真实授权来源；监测质量、延迟、部分成功、Token/费用和熔断。
8. 回滚时先关闭研究/TA Feature Flag和新菜单；保留已落库快照、原始响应与审计。数据库向前兼容迁移按恢复演练方案回滚或修复，不删除已被任务引用的历史版本。

## Open Questions

以下为不改变协议和数据边界的实施配置项，落地前由部署环境确定：

- 各角色权限码与组织内若依角色的最终映射；
- MinIO保留周期、加密KMS和原始响应法律/授权保留期限；
- TA异步轮询与任务队列的最终部署组件、吞吐和超时，以压测确定；
- Dify各环境的正式Workflow Key、模型供应商与模型版本；
- 当前生产库的历史ID转换窗口和不停机兼容策略，需结合现场DDL/数据量评审。
