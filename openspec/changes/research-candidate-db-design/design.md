# Design

## Context

本设计承接 `proposal.md` 与五份 capability spec，把详设 2 的模块二【研究与候选发现】落到可施工的数据库边界。当前仓库基线为 MySQL + Flyway `V1_0_0`~`V1_9_0`（Paper 交易域）与模块一 `data-foundation-db-design` 规划的 `V1_10_0` 起新增表（治理/标的/日历/行情/研究入口）。其中模块一已冻结并权威定义下列 9 张研究入口表，本 change **只复用、不重建、不修改**：

- `quantpf_research_material`、`quantpf_research_material_relation`（材料与转载/修订关系）
- `quantpf_research_task`、`quantpf_research_task_run`（研究任务与运行）
- `quantpf_dify_workflow_registry`、`quantpf_dify_invocation`（Dify Workflow 注册与调用）
- `quantpf_ai_research_draft`（Dify 原始输出与分量校验状态，非事实主表）
- `quantpf_research_event`、`quantpf_event_evidence`（核验后的研究事件与证据）

模块二另引用模块一的 `quantpf_instrument`、`quantpf_instrument_version`、`quantpf_market_quote_snapshot`、`quantpf_market_state`、`quantpf_quant_feature_snapshot`、`quantpf_supply_demand_observation`、`quantpf_basis_term_structure`、`quantpf_spot_quote`、`quantpf_data_point_in_time`、`quantpf_data_use_eligibility`，作为身份/行情/因子/点时/用途资格的权威键。

设计约束（全部承接模块一、与详设 1/2 及 ADR 一致）：

- **全部是 MySQL 业务表现**，模块二不新增 DuckDB 表。研究结论、因子、评分、研究卡、审核、交接均属"业务/分析数据"，按 ADR-9 统一存 MySQL。Dify 数据库/向量库不得成为任何这些表的权威源。
- 时间以 `DATETIME(6)` 保存 UTC 瞬时值，同时保留市场本地日期字段；价格/金额/比例/评分用 `DECIMAL`，不用浮点作为主值；状态、版本、来源、时点、自然键全部实体化，`JSON` 只承载传导链、条件、证据引用集合、分项评分等解释性/集合性内容，且必须配合可检索的状态/摘要列。
- **统一主键与末尾日志字段（全库规范，模块二新表强制遵守）**：主键 `id BIGINT NOT NULL PRIMARY KEY`，禁止 `AUTO_INCREMENT`；MyBatis-Plus 实体主键用 `IdentifierGenerator`，业务侧需主动发号的业务 ID（`handoff_key`、`card_key`、`screening_key` 等）用 Hutool `Snowflake` 或应用生成的稳定字符串，两者共用同一套 `workerId/datacenterId` 配置防碰撞。每张表列末尾固定追加五列：`create_by BIGINT NULL`、`create_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`、`update_by BIGINT NULL`、`update_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)`、`remark VARCHAR(500) NULL`。
- 幂等键统一 `VARCHAR(128)` 并建唯一约束；跨模块引用（模块三策略评估、模块四验证结果）一律逻辑外键（BIGINT 稳定引用，不建物理 FK），仅同库模块二内部表之间建物理 FK。
- 与上游基线的对应：总纲第 8 章（事实—解释—预测链）、第 11/12 章（筛选/概率/漂移）、第 18 章（状态机/评分卡/硬否决/反确认偏误）、第 20 章（表单/领域对象）、第 21/22 章（股票/商品研究维度）、第 25 章（Dify 边界）；详设 0 第 6/7/8/12/13 章；详设 2 第 10–18 章（校验门禁/事实/研究链/四类入口/评估/研究卡/审核/状态机）。冲突时先更新上级文档，不由实现者兼容两套语义。

## Goals / Non-Goals

**Goals:**

- 形成可直接拆解为 Flyway 迁移、种子数据与契约任务的表级设计，覆盖事实核验、研究链、资产研究、候选筛选/评分/研究卡、审核/交接五个能力域。
- 明确与模块一 9 张研究入口表、模块三 `quantpf_strategy*`、模块四验证结果的 FK/逻辑引用边界，杜绝"同义重复建表"。
- 让每条研究结论能回放（点时快照 + 口径版本 + 披露/推断标识）；每个候选能解释（硬门禁逐项 + 分项评分 + 淘汰原因）；每次交接幂等可回执。
- 让 Dify 只能读受控结构化结果、只产草稿，绝不写事实/研究链/评分/研究卡/交接主表。

**Non-Goals:**

- 不实现 Java Entity/Mapper/Repository、Dify 适配器、筛选/评分引擎、API、XXL-JOB Handler 或前端页面；只冻结持久化、约束、契约与验收任务。
- 不实现模块三策略评估/观察池/预警、模块四组合风险/回测/概率/复盘的权威表（其主表由对应模块 change 定形）；本设计只预留稳定引用。
- 不改变总纲/详设已定的评分权重、概率口径、状态语义、资产边界或 Dify/Spring Boot 职责；不触碰模块一历史迁移。
- 不复制若依用户/角色/菜单/字典/通知表，不创建真实交易表。

## Decisions

### 1. 存储分层与表命名

模块二全部落 MySQL。表统一 `quantpf_` 前缀；模块二专属 new 表共 **13 张**，按能力域分五组：

| 分组（capability） | 表 |
|---|---|
| 事实核验 `fact-verification` | `quantpf_research_claim`、`quantpf_research_evidence_link` |
| 研究链 `research-chain` | `quantpf_research_thesis`、`quantpf_research_prediction`、`quantpf_research_counter_evidence` |
| 资产研究 `asset-research` | `quantpf_fundamental_factor_snapshot`、`quantpf_event_exposure`、`quantpf_commodity_research_snapshot` |
| 候选筛选 `candidate-screening` | `quantpf_screening_run`、`quantpf_candidate_assessment`、`quantpf_research_card` |
| 审核交接 `review-handoff` | `quantpf_research_review`、`quantpf_candidate_handoff` |

`quantpf_schema_marker`、Flyway 元数据、若依 `sys_*` 与模块一已冻结表不属于本 change 的新表范围。

### 2. 复用表映射（不重建）

**若依底座复用**（只保存稳定 ID，Dify 不得访问）：

| 表 | 复用字段 | 模块二用途 |
|---|---|---|
| `sys_user` | `user_id BIGINT PK`、`user_name`、`status` | 审核人/操作者/候选人归属 owner；Dify 不传密码/Token |
| `sys_role` | `role_id`、`role_key`、`status` | 普通研究/研究审核用户角色来源；Dify 不读取 |
| `sys_menu` | `menu_id`、`perms`、`status` | 研究/候选/审核菜单权限码 `quant:*` |
| `sys_dict_type` / `sys_dict_data` | 字典类型/值/状态 | 候选原因码、研究状态、证据类型、审核动作、交接状态展示翻译；主状态仍由 CHECK 约束 |
| `sys_message` / `sys_notice` | 通知 ID、接收人、状态 | 研究任务/候选/交接站内通知；不放凭据与完整原文 |

**模块一 9 张研究入口表复用**（模块二通过 FK/稳定引用关联，不复制）：

| 模块一表 | 模块二引用字段 | 模块二用途 |
|---|---|---|
| `quantpf_research_task` / `_task_run` | `task_id`、`task_run_id` | 主张/假设/预测/评估/研究卡的任务归属 |
| `quantpf_ai_research_draft` | `draft_id` | 事实候选/传导链/反方/预测草稿的来源链 |
| `quantpf_research_event` / `event_evidence` | `event_id`、`event_evidence_id` | 事件驱动研究的证据引用 |
| `quantpf_research_material` | `material_id` | 主张/证据的材料来源 |
| `quantpf_data_point_in_time` | `point_in_time_id` | 因子/结论/评估的点时回放 |

**模块三/四逻辑引用**（不建物理 FK）：

| 引用 | 模块二字段 | 目标（目标表由对应模块 change 定形） |
|---|---|---|
| 策略评估/信号 | `candidate_handoff.handoff_payload` 内的 `strategy_evaluation_ref` | 模块三 `quantpf_strategy_evaluation`（未建） |
| 验证结果 | `research_prediction.sample_result_ref`、`research_card.sample_result_ref` | 模块四验证域结果（`quantpf_backtest_result` 已存在，或未来 `quantpf_probability_result`） |

### 3. 全库统一主键与末尾日志字段（模块二新表强制）

每张模块二新表：`id BIGINT NOT NULL PRIMARY KEY`（雪花，应用侧生成），列末尾固定追加 `create_by / create_time / update_by / update_time / remark` 五列（顺序与类型见 Context）。下文各表字段栏只列业务字段，五列默认追加不重复写出。`created_at/updated_at` 不得作为新表规范日志列。`create_by/update_by` 系统任务可用约定系统用户 ID 或 NULL，不得伪造普通用户。

### 4. 表级详细设计：事实核验与证据链

状态 CHAR 与 CHECK：`verification_status` = `PENDING_VERIFICATION / VERIFIED / CONFLICTED / REVISED / INVALIDATED / EXPIRED`；`evidence_type` = `PRIMARY_SOURCE / INDEPENDENT_SECONDARY / DERIVED_DATA / AI_EXTRACTED / USER_ANNOTATION`。

| 表 | 字段（类型；约束） | 索引/唯一键 | Dify 交互 |
|---|---|---|---|
| `quantpf_research_claim` | `id BIGINT PK`；`claim_hash CHAR(64) NOT NULL`；`draft_id BIGINT NULL`；`task_id BIGINT NOT NULL`；`material_id BIGINT NULL`；`event_id BIGINT NULL`；`claim_type VARCHAR(32) NOT NULL`；`statement VARCHAR(2000) NOT NULL`；`subject_ref VARCHAR(128)`；`source_ref VARCHAR(128)`；`quote_text TEXT`；`quote_location VARCHAR(256)`；`structured_field_ref VARCHAR(256)`；`published_at/available_at DATETIME(6)`；`valid_from/valid_to DATETIME(6)`；`snapshot_id BIGINT`；`verification_status VARCHAR(32) NOT NULL`；`quality_status VARCHAR(24)`；`authorization_status VARCHAR(24)`；`conflict_status VARCHAR(24)`；`verification_method VARCHAR(32)`；`verifier_user_id BIGINT`；`review_reason VARCHAR(500)`；`version_no INT NOT NULL`；`previous_claim_id BIGINT`；`current_flag TINYINT`；**末尾追加五日志字段** | `UK(claim_hash)`；`IX(task_id)`；`IX(subject_ref)`；`IX(verification_status, create_time)` | Dify 输出的事实候选先落在 `ai_research_draft`，通过校验后由 Spring Boot 升级为本表 `VERIFIED`；Dify 不得直写本表，仅可读已核验主张用于问答证据 |
| `quantpf_research_evidence_link` | `id BIGINT PK`；`claim_id BIGINT NOT NULL`(FK claim)；`evidence_type VARCHAR(32) NOT NULL`；`material_id BIGINT`；`event_evidence_id BIGINT`；`source_id BIGINT`；`snapshot_id BIGINT`；`quote_text TEXT`；`quote_location VARCHAR(256)`；`structured_field_ref VARCHAR(256)`；`independence_group VARCHAR(64)`；`verification_status VARCHAR(24)`；**末尾追加五日志字段** | `UK(claim_id, material_id, quote_location, evidence_type)`；`IX(claim_id)`；`IX(source_id, independence_group)`；`IX(event_evidence_id)` | 引用由 Spring Boot 绑定并校验；Dify 输出引用先作 `AI_EXTRACTED/待核验`，不得伪装外部证据；用户备注单独入 `USER_ANNOTATION` |

### 5. 表级详细设计：研究链

| 表 | 字段（类型；约束） | 索引/唯一键 | Dify 交互 |
|---|---|---|---|
| `quantpf_research_thesis` | `id BIGINT PK`；`thesis_key VARCHAR(128) NOT NULL`；`task_id BIGINT NOT NULL`；`event_id BIGINT`；`draft_id BIGINT`；`asset_type VARCHAR(16) NOT NULL`(STOCK/FUTURE/SPOT)；`subject_ref VARCHAR(128)`；`instrument_id BIGINT`；`direction VARCHAR(16)`(LONG/SHORT/NEUTRAL/UNCERTAIN)；`research_period VARCHAR(16)`(SHORT/MEDIUM/LONG)；`impact_variables JSON`；`transmission_chain JSON`；`direct_beneficiaries JSON`；`potential_losers JSON`；`reverse_transmission JSON`；`unknown_items JSON`；`follow_up_data JSON`；`status VARCHAR(32)`(DRAFT/ACTIVE/CONFLICTED/INVALIDATED/EXPIRED/REVISED)；`version_no INT NOT NULL`；`previous_thesis_id BIGINT`；`current_flag TINYINT`；`valid_from/valid_to DATETIME(6)`；**末尾追加五日志字段** | `UK(thesis_key, version_no)`；`IX(task_id)`；`IX(instrument_id, direction)`；`IX(status, create_time)` | Dify 生成的传导链草稿落在 `ai_research_draft`，Spring Boot 校验后写本表；Dify 只读已核验假设用于研究卡解释，不得改 `direction/status` |
| `quantpf_research_prediction` | `id BIGINT PK`；`thesis_id BIGINT NOT NULL`(FK thesis)；`draft_id BIGINT`；`direction VARCHAR(16) NOT NULL`；`forecast_horizon VARCHAR(16)`(SHORT/MEDIUM/LONG)；`conditions JSON`；`invalidating_conditions JSON`；`expected_path JSON`；`probability_status VARCHAR(32)`(NOT_COMPUTED/COMPUTED/INSUFFICIENT_SAMPLE)；`sample_result_ref BIGINT`(逻辑 FK 模块四)；`sample_count INT`；`confidence_interval JSON`；`cost_adjusted_expectation DECIMAL(24,8)`；`max_adverse_excursion DECIMAL(24,8)`；`data_snapshot_id BIGINT`；`status VARCHAR(24)`；`version_no INT`；`current_flag TINYINT`；**末尾追加五日志字段** | `UK(thesis_id, version_no)`；`IX(thesis_id)`；`IX(probability_status, create_time)` | Dify 只能给方向/条件草稿；样本、概率、期望由 Spring Boot/验证域计算；Dify 不可写统计字段 |
| `quantpf_research_counter_evidence` | `id BIGINT PK`；`target_type VARCHAR(32) NOT NULL`(CLAIM/THESIS/PREDICTION/CARD)；`target_id BIGINT NOT NULL`；`counter_type VARCHAR(32) NOT NULL`(COUNTER_EVIDENCE/UNKNOWN_ITEM/FALSIFICATION_CONDITION/ALTERNATIVE_EXPLANATION/ALTERNATIVE_INSTRUMENT)；`statement VARCHAR(2000) NOT NULL`；`evidence_refs JSON`；`importance VARCHAR(16)`(HIGH/MEDIUM/LOW)；`check_status VARCHAR(32)`(PENDING/CHECKED/CANNOT_CHECK/REFUTED_CONFIRMED)；`origin VARCHAR(24)`(AI_DRAFT/USER/SYSTEM)；`draft_id BIGINT`；**末尾追加五日志字段** | `IX(target_type, target_id)`；`IX(counter_type, check_status)` | Dify 生成反方问题候选入草稿；核查状态由 Spring Boot/人工推进，Dify 不可改 `check_status` |

### 6. 表级详细设计：资产研究

`disclosure_method` = `DISCLOSED / CALCULATED / MODELLED / INFERRED`；`tool_expression` = `FUTURE_LONG / FUTURE_SHORT / HEDGE / SPREAD / AVOID / NONE`。

| 表 | 字段（类型；约束） | 索引/唯一键 | Dify 交互 |
|---|---|---|---|
| `quantpf_fundamental_factor_snapshot` | `id BIGINT PK`；`instrument_id BIGINT NOT NULL`(FK instrument)；`report_period VARCHAR(16)`；`market_local_date DATE`；`as_of_time DATETIME(6)`；`business_authenticity JSON`；`earnings_quality JSON`；`valuation JSON`；`event_sensitivity JSON`；`disclosure_method VARCHAR(32)`；`factor_version VARCHAR(64) NOT NULL`；`point_in_time_id BIGINT NOT NULL`(FK point_in_time)；`source_snapshot_id BIGINT`；`status VARCHAR(24)`；**末尾追加五日志字段** | `UK(instrument_id, report_period, factor_version, point_in_time_id)`；`IX(instrument_id, market_local_date)`；`IX(factor_version)` | 纯确定性计算，Dify 只读解释；不得由 Dify 补造缺失因子或修改数值 |
| `quantpf_event_exposure` | `id BIGINT PK`；`event_id BIGINT NOT NULL`(FK research_event)；`event_variable_name VARCHAR(128) NOT NULL`；`exposure_type VARCHAR(32) NOT NULL`(INDUSTRY/COMMODITY/INSTRUMENT)；`exposure_code VARCHAR(64) NOT NULL`；`instrument_id BIGINT`；`exposure_level VARCHAR(32)`(DIRECT/INDIRECT/UNCERTAIN)；`direction VARCHAR(32)`(POSITIVE/NEGATIVE/BOTH/UNCERTAIN)；`business_exposure DECIMAL(24,8)`；`sensitivity_score DECIMAL(24,8)`；`sensitivity_method VARCHAR(32)`；`evidence_refs JSON`；`status VARCHAR(24)`；`point_in_time_id BIGINT`；**末尾追加五日志字段** | `UK(event_id, exposure_type, exposure_code)`；`IX(exposure_code, exposure_type)`；`IX(event_id)` | Dify 可提行业/个股关联候选（入草稿）；暴露/敏感度由 Spring Boot 确定性映射计算，Dify 不可改 |
| `quantpf_commodity_research_snapshot` | `id BIGINT PK`；`future_product_id BIGINT NOT NULL`(FK future_product)；`commodity_code VARCHAR(64)`；`task_id BIGINT`；`market_local_date DATE`；`as_of_time DATETIME(6)`；`industry_judgment VARCHAR(24)`(BULLISH/BEARISH/NEUTRAL/CONFLICT)；`price_judgment VARCHAR(24)`；`tool_expression VARCHAR(32)`；`contract_instrument_id BIGINT`；`spot_exposure_type VARCHAR(32)`(INVENTORY/PURCHASE_CONTRACT/SALES_CONTRACT/NONE)；`supply_demand_summary JSON`；`basis_term_summary JSON`；`contract_risk JSON`；`evidence_refs JSON`；`status VARCHAR(24)`；`point_in_time_id BIGINT`；**末尾追加五日志字段** | `UK(future_product_id, market_local_date, point_in_time_id)`；`IX(future_product_id, market_local_date)`；`IX(tool_expression, status)` | 供需/基差/期限结构由 Spring Boot 计算，Dify 只读解释传导链；无敞口时 `spot_exposure_type=NONE`，Dify 不得派生现货空单 |

### 7. 表级详细设计：候选筛选 / 评分 / 研究卡

`candidate_stage` = `RESEARCH_LEAD / RESEARCH_CANDIDATE / VERIFIED_RESEARCH_CANDIDATE / RESEARCH_CARD_READY / RESEARCH_READY`；`status`（评估）= `PASS / REJECT / DEFER / OPPORTUNITY_PASSED / NO_MATCH / INVALIDATED / EXPIRED`；研究卡 `message_validity` 承接总纲 5.4.1 七态。

| 表 | 字段（类型；约束） | 索引/唯一键 | Dify 交互 |
|---|---|---|---|
| `quantpf_screening_run` | `id BIGINT PK`；`screening_key VARCHAR(128) NOT NULL`；`entry_type VARCHAR(32) NOT NULL`(EVENT/STOCK/COMMODITY/PRICE_VOLUME)；`asset_scope JSON`；`screening_condition JSON`；`rule_version VARCHAR(64)`；`score_version VARCHAR(64)`；`data_snapshot_id BIGINT`；`as_of_time DATETIME(6)`；`status VARCHAR(24)`(RUNNING/SUCCEEDED/PARTIAL/FAILED)；`total_count/pass_count/reject_count INT`；`started_at/finished_at DATETIME(6)`；`trigger_source VARCHAR(24)`(USER/JOB/SYSTEM)；**末尾追加五日志字段** | `UK(screening_key)`；`IX(entry_type, create_time)`；`IX(status, create_time)` | 无交互；纯 Spring Boot 规则主控 |
| `quantpf_candidate_assessment` | `id BIGINT PK`；`screening_run_id BIGINT NOT NULL`(FK screening_run)；`candidate_key VARCHAR(128)`；`task_id BIGINT`；`asset_type VARCHAR(16) NOT NULL`；`subject_ref VARCHAR(128)`；`instrument_id BIGINT`；`direction VARCHAR(16)`；`research_period VARCHAR(16)`；`candidate_stage VARCHAR(32)`；`hard_gate_results JSON`；`research_score DECIMAL(6,2)`；`score_version VARCHAR(64)`；`dimension_scores JSON`；`opportunity_status VARCHAR(32)`；`status VARCHAR(32)`；`reject_reason_codes JSON`；`evidence_refs JSON`；`data_snapshot_id BIGINT`；`quant_feature_snapshot_id BIGINT`；`card_id BIGINT`；**末尾追加五日志字段** | `UK(screening_run_id, subject_ref, direction, research_period)`；`IX(instrument_id)`；`IX(status, create_time)`；`IX(card_id)` | 无交互；硬门禁/评分由 Spring Boot 确定性计算，Dify 不得评分或改状态 |
| `quantpf_research_card` | `id BIGINT PK`；`card_key VARCHAR(128) NOT NULL`；`card_version_no INT NOT NULL`；`candidate_assessment_id BIGINT NOT NULL`；`asset_type VARCHAR(16)`；`subject_ref VARCHAR(128)`；`instrument_id BIGINT`；`research_entry VARCHAR(32)`；`task_id BIGINT`；`direction VARCHAR(16)`；`research_period VARCHAR(16)`；`research_conclusion VARCHAR(2000)`；`research_status VARCHAR(24)`；`message_validity VARCHAR(32)`；`price_reaction_status VARCHAR(32)`；`opportunity_status VARCHAR(32)`；`handoff_status VARCHAR(32)`；`fact_refs/event_refs/source_refs JSON`；`fundamental_or_supply_demand_analysis JSON`；`valuation_or_basis_analysis JSON`；`support_evidence_refs/counter_evidence_refs JSON`；`unknown_items/falsification_conditions JSON`；`market_state VARCHAR(32)`；`price_position VARCHAR(32)`；`volume_state VARCHAR(32)`；`strategy_evaluation_hints JSON`；`sample_result_ref BIGINT`；`sample_count INT`；`probability DECIMAL(12,8)`；`confidence_interval JSON`；`cost_adjusted_expectation DECIMAL(24,8)`；`max_adverse_excursion DECIMAL(24,8)`；`out_of_sample_status VARCHAR(24)`；`satisfied_conditions/unsatisfied_conditions/next_check_conditions JSON`；`valid_until DATETIME(6)`；`data_snapshot_id/quant_feature_snapshot_id BIGINT`；`previous_card_version_id BIGINT`；`current_flag TINYINT`；**末尾追加五日志字段** | `UK(card_key, card_version_no)`；当前版本过滤唯一键（`current_key` 生成列：`current_flag=1` 取 `card_key` 否则 NULL，`UK(card_key, current_key)`）；`IX(candidate_assessment_id)`；`IX(asset_type, opportunity_status)`；`IX(instrument_id)` | Spring Boot 形成结构化结果，Dify 仅通过 `RESEARCH_CARD_EXPLANATION` Workflow 生成普通语言说明草稿，不写本表任何状态 |

### 7.1 评分口径、多周期预期与问答边界

- **评分口径**：`quantpf_candidate_assessment.research_score` 与 `dimension_scores` 只承载模块二的研究分贡献（合计 70 分制），四个维度严格沿用总纲 18.3 的命名与权重——`事件可信度与新颖性 15`、`行业传导链 20`、`标的质量/供需质量 20`、`估值/预期差 15`。总 100 分中的 `策略匹配 15`（模块三）、`执行和流动性 5`（模块三/四）、`风险收益比 10`（模块四）不在本表计算，也不由模块二给出。总纲 70/80 分门槛只能由总分在完整门禁与适用阶段具备后由决策编排聚合评估，禁止把 `research_score`（≤70）误当总分或策略放行条件；总分缺项时标记 `SCORE_INCOMPLETE`，缺维度不得默认满分。
- **多周期预期**：单个候选/研究卡短期（约 2 周）、中期（约 3 个月–1 年）、长期（约半年–3 年）的条件式预期通过多条 `quantpf_research_prediction` 记录表达（各自 `forecast_horizon` 与成立/失效条件），`research_card` 通过 `candidate_assessment_id` 关联其全部预测；不得以短期技术信号外推长期结论。
- **研究卡统计字段引用模块四**：`sample_result_ref / sample_count / probability / confidence_interval / cost_adjusted_expectation / max_adverse_excursion / out_of_sample_status` 均为对模块四验证域结果的聚合快照或稳定引用，本表不计算概率；无统计结果时 `probability` 置空并以 `probability_status`/`out_of_sample_status` 标记，不得填伪精确值。
- **研究问答**：自然语言问答不建专门表；查询结果引用研究卡/主张/因子表，Dify 调用记录与原始响应落模块一 `quantpf_dify_invocation`/`quantpf_ai_research_draft`，问答动作审计走 `quantpf_audit_record`。
- **机会状态枚举**：`opportunity_status` = `ACTIONABLE_FOR_RESEARCH / WAIT_PULLBACK / WAIT_BREAKOUT / OPPORTUNITY_PASSED / OBSERVE_ONLY / BLOCKED / DATA_INSUFFICIENT / INVALIDATED / NO_OPPORTUNITY`；`ACTIONABLE_FOR_RESEARCH` 仅表示研究侧允许继续评估，不等于策略触发或获准交易。

### 8. 表级详细设计：审核与交接

`review_action` = `CONFIRM / REJECT / DEFER / REQUEST_MORE / OVERRIDE / EXPIRE`；`handoff_status` = `HANDOFF_PENDING / HANDOFF_VALIDATING / HANDED_OFF / HANDOFF_REJECTED / HANDOFF_EXPIRED / HANDOFF_INVALIDATED`。

| 表 | 字段（类型；约束） | 索引/唯一键 | Dify 交互 |
|---|---|---|---|
| `quantpf_research_review` | `id BIGINT PK`；`review_key VARCHAR(128) NOT NULL`；`object_type VARCHAR(24) NOT NULL`(CLAIM/CARD/CANDIDATE/HANDOFF/THESIS)；`object_id BIGINT NOT NULL`；`object_version_no INT`；`review_action VARCHAR(32) NOT NULL`；`reviewer_user_id BIGINT`；`review_reason VARCHAR(500)`；`review_scope JSON`；`before_state VARCHAR(32)`；`after_state VARCHAR(32)`；`override_expiry DATETIME(6)`；**末尾追加五日志字段** | `UK(review_key)`；`IX(object_type, object_id)`；`IX(reviewer_user_id, create_time)` | 无交互；仅用户/系统审核写入，覆盖范围受 hard gate 约束 |
| `quantpf_candidate_handoff` | `id BIGINT PK`；`handoff_key VARCHAR(128) NOT NULL`；`candidate_assessment_id BIGINT NOT NULL`(FK assessment)；`research_card_id BIGINT`；`card_version_no INT`；`target_module VARCHAR(32) NOT NULL`(STRATEGY)；`handoff_status VARCHAR(32) NOT NULL`；`handoff_payload JSON`；`reject_reason VARCHAR(500)`；`correlation_id VARCHAR(64)`；`causation_id VARCHAR(64)`；`handed_off_at DATETIME(6)`；**末尾追加五日志字段** | `UK(handoff_key)`；`IX(candidate_assessment_id)`；`IX(target_module, handoff_status)` | 无交互；Spring Boot 确定性交接，Dify 不得创建/修改交接 |

### 9. Dify 数据交互矩阵（模块二）

| 数据类别 | Dify 可读 | 传递方式 | Dify 可写 |
|---|---:|---|---:|
| 研究主张/证据链 | 是（已核验摘要） | 问答/研究卡解释上下文，带 claim_id/证据引用 | 否 |
| 研究假设/预测/反方 | 是（已核验版本） | 研究卡解释上下文 | 否 |
| 基本面因子/事件暴露/商品结论 | 是（最小化） | 结构化 JSON + factor_version/snapshot | 否 |
| 候选评估/分项评分/硬门禁结果 | 是（摘要） | 研究卡说明，带 score_version | 否 |
| 研究卡状态/版本 | 是（只读结果） | `RESEARCH_CARD_EXPLANATION` 输出说明草稿 | 否 |
| 研究审核/交接主表 | 否 | 不传 | 否 |
| 候选关键 ID、account/Paper 数据 | 否 | 不传 | 否 |

所有 Dify 调用沿用模块一定义的链路：Spring Boot 冻结快照 → 组装最小授权上下文 → Workflow API（服务端 Bearer Key、streaming）→ 保存 `dify_invocation`/`ai_research_draft` → Schema/引用/时间/授权/身份/冲突校验 → 通过后升级为 claim/thesis/card，失败保留草稿。Dify 的 `directionCandidate`、`normalizedSubjectHint`、`confidence` 只作草稿字段，不进入 `verified_direction`、`research_score`、`candidate_stage` 或交接状态。

### 10. 索引、约束与版本策略

- 复合索引优先：`(instrument_id, market_local_date)`、`(verification_status, create_time)`、`(status, create_time)`、`(object_type, object_id)`、`(asset_type, opportunity_status)`。
- 版本表 `(thesis_key, version_no)` / `(card_key, card_version_no)` 唯一；"仅一个当前版本"用 `current_key` 生成列 + 过滤唯一索引实现（`current_flag=1` 取业务键，否则 NULL，对 `(业务键, current_key)` 建唯一键），禁止对 `(object_id, current_flag)` 直接建唯一键，禁止用 `ORDER BY id` 推断最新。
- 状态列建 CHECK 或由应用枚举校验；MySQL 若 CHECK 执行受限，CI 必须用约束测试补齐。
- `JSON` 不建全文索引；需检索/过滤的字段（方向、周期、状态、原因码、机会状态、披露口径）实体化。原始正文/引用已由模块一材料快照承载，本 change 不重复存大对象。
- 模块二不引入 MySQL 分区，先按复合索引与一期日线规模运行；性能超阈值再走独立 ADR。

### 11. 迁移分批与依赖顺序

建议新增迁移（实际编号以施工时 Flyway 版本扫描为准，不可覆盖模块一 `V1_10_0`~`V1_16_0`）：

1. `V1_17_0__research_fact_and_chain`：`research_claim`、`research_evidence_link`、`research_thesis`、`research_prediction`、`research_counter_evidence`。
2. `V1_18_0__asset_research_factors`：`fundamental_factor_snapshot`、`event_exposure`、`commodity_research_snapshot`。
3. `V1_19_0__candidate_screening_and_card`：`screening_run`、`candidate_assessment`、`research_card`。
4. `V1_20_0__review_and_handoff`：`research_review`、`candidate_handoff`。
5. `V1_21_0__research_indexes_and_constraints`（可选合并）：跨表复合索引补强、CHECK 校验、当前版本过滤唯一键校验。

种子与迁移分离：候选原因码、研究/证据/审核/交接状态与机会状态使用 `sys_dict_*` 展示字典，数据库 CHECK + 应用枚举为约束；`sys_menu` 权限码（`quant:research:*`、`quant:candidate:*`、`quant:research:review` 等）由正式菜单契约与幂等种子脚本维护，不留 Token/凭据。模块一研究入口表已含任务/草稿/事件状态字典，本 change 不重复种入。

## Risks / Trade-offs

- **[Risk] 与模块一研究入口表边界不清导致重复建表** → 本设计以模块一 `data-foundation-db-design` 的 9 张表为唯一权威来源，模块二只增 13 张并禁止同名重建；施工前用 `openspec` 与 `SHOW TABLES` 盘点防重。
- **[Risk] 模块三/四主表尚未建导致物理 FK 无法落地** → 跨模块一律逻辑外键（BIGINT 稳定引用 + 应用层校验），模块二内部才建物理 FK，等下游模块定形后再在独立迁移补同库 FK。
- **[Risk] JSON 承载分项评分/传导链造成不可查询** → 各分项评分维度、方向、周期、原因码、状态全部实体化或 JSON 内固定 key + 摘要列，评分/筛选查询不依赖 JSON 全文。
- **[Risk] 研究结论被最新数据静默覆盖，回放失去依据** → 版本号 + `current_flag` + 点时快照 + `previous_*_id` 全链路保留；修订只新增版本。
- **[Risk] Dify 越过门禁把草稿当事实** → 草稿、主张、假设、研究卡分层；Dify 无 write 通道；升级只由 Spring Boot 校验流程执行。
- **[Risk] 一期数据少却过早做分区/复杂索引** → 先复合索引 + 规模测试，需要时独立 ADR，不改业务自然键与点时语义。

## Migration Plan

1. CI 用全新 MySQL schema 执行 `V1_0_0`→模块一新迁移→`V1_17_0`~`V1_21_0`，验证顺序、CHECK、UK、索引、当前版本过滤键与重复执行。
2. 在副本环境核对模块一 9 张表的 FK 依赖与稳定 ID 语义；新表只加列/建表，不触碰模块一历史迁移。
3. 种子脚本幂等：字典/菜单可重复执行，不产生重复记录、不留凭据。
4. Mock 链路验收：研究任务→草稿校验→claim/evidence_link→thesis/prediction→资产因子→screening_run→candidate_assessment→research_card→research_review→candidate_handoff 全链可回放、幂等。
5. 回滚：新表迁移失败则 Flyway 停在失败版本前；业务切换失败关闭新写路径保留旧查询；不删除已成功写入版本，删除需单独审批并备份。

## Open Questions

以下事项不改变本设计的表边界、评分口径或 Dify 隔离原则，可在实施前作为配置登记：

- 研究分项评分（事件可信度 15 / 传导链 20 / 标的质量 20 / 估值 15）各维度的精确打分公式与证据字段清单（随模块二代码施工 + 详设 2C 固化）。
- `dimension_scores` 内部是否进一步按维度拆实际列（当前以 JSON 承载，若后续需按维度聚合再作实体化迁移）。
- 标的静态行业分类的权威归属（当前模块一 `instrument_version` 未含行业列；事件暴露以 `exposure_code` 引用行业码，静态行业→标的映射若需落库，是模块一标的域扩展还是模块二暴露域派生，建议在模块一/二联调前确认）。
- 模块四验证结果表的最终命名及预测/研究卡 `sample_result_ref` 的正式 FK 落地时机（当前逻辑引用）。
- RESEARCH_CARD_EXPLANATION 等 Workflow 的输入输出 Schema 精确字段（随 Dify 子详设）。