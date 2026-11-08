# Design

## Context

本设计是模块二【研究与候选发现】的统一审查与施工基线，承接：

- `D:\workspace\quant\doc\量化交易系统完整设计方案说明书_V5.md`：Spring Boot 是业务事实源，Dify 是编排/语言层，TradingAgents 是独立的多智能体研究引擎；
- `D:\workspace\quant\doc\详设文档\详设_0_整体流程文档.md`：浏览器只访问 Spring Boot，主链路为 `Vue → Spring Boot → Dify →（按需）TradingAgents Adapter → TradingAgents → Dify → Spring Boot`；
- `D:\workspace\quant\doc\详设文档\详设_2_研究与候选发现详细设计方案.md`：四类候选入口、事实/解释/预测分层、研究 70 分制、研究卡、审核和模块三交接；
- 四份在途提案 `research-candidate-db-design`、`research-candidate-code-guidance`、`research-candidate-ui-guidance`、`research-candidate-integrated-delivery`：分别提供表级、代码、界面和综合交付草案。

当前实际源码仓为后端 `D:\workspace\quant\RuoYi-Vue-Plus`、前端 `D:\workspace\quant\plus-ui`。后端采用 Flyway；数据库迁移权威路径是 `RuoYi-Vue-Plus/db/migration/`，而不是抽象的 `[后端]/sql/`。前端 API 约定使用 TypeScript `.ts`。本设计不覆盖源码实现，只冻结实现前必须一致的架构、数据库、接口、页面和验收边界。

## Goals / Non-Goals

**Goals:**

- 将四份既有提案整合为一个可实施的模块二基线，覆盖数据库、Spring Boot、Dify/TradingAgents、若依 Vue 页面和联调验收。
- 固化 TradingAgents 的正确位置：由 Dify 按任务策略路由，仅服务复杂影响链、多空辩论、反方研究等深度任务，不作为每个任务的必经步骤。
- 让所有 AI 输出可追溯、可回放、可核验、可审核；保留多角色原始观点和分歧。
- 让确定性事实核验、点时、标的身份、基本面/供需/量价计算、硬门禁、研究评分、候选状态、研究卡版本和交接由 Spring Boot 主控。
- 以真实仓库路径、Flyway、RuoYi 权限、幂等和版本校验约束实施任务。

**Non-Goals:**

- 不在本 change 中创建 Java、Vue、SQL、Dify Workflow 或 TradingAgents Python 源码。
- 不创建模块三策略信号、交易计划、Paper 订单、账户/持仓变更或真实交易能力。
- 不让 Dify/TradingAgents 直连 QuantPF MySQL、Redis、DuckDB、MinIO 权威数据、账户、风险或订单接口。
- 不把 TradingAgents 研究分直接当成总分，不改变详设 2 的四维 70 分研究评分口径。
- 不把 MCP 作为一期 Spring Boot-Dify-TradingAgents 主协议；若未来引入，只能位于受控 Adapter 之上并另行评审。

## Decisions

### 1. 统一职责和调用链

```text
浏览器/Vue
  │ HTTPS REST/SSE（只访问 Spring Boot）
  ▼
Spring Boot 研究 API
  ├─创建任务、权限/授权/点时/身份/质量校验、冻结 InputSnapshot
  ├─调用已登记且已发布的 Dify Workflow（HTTPS REST，streaming SSE）
  └─消费 Dify 运行事件、保存调用关联和 AI_DRAFT
       │
       ▼
Dify Workflow
  ├─简单抽取/摘要/格式化/研究卡解释：直接由 Dify 完成
  └─复杂影响链/多空辩论/反方深研：调用内网 TradingAgents Adapter
       │ HTTPS REST/JSON，受控工具 trading_agents_research
       ▼
TradingAgents Adapter → TradingAgents 多智能体研究引擎
       │ 结构化角色观点、引用、分歧、运行元数据
       └──────────────→ Dify 汇总 → Spring Boot Schema/引用/时间/授权/身份/冲突校验
```

Spring Boot 也可为健康检查、运行状态、取消、人工批准的离线重试调用 Adapter，但必须使用独立的 `execution_route` 并记录审计；生产研究主路径仍由 Dify 编排。TradingAgents 不返回可执行交易指令。所有最终候选和研究卡字段由 Spring Boot 生成或校验后写入。

### 2. TradingAgents 路由规则

| 任务类型 | 默认路径 | 是否调用 TradingAgents | 说明 |
|---|---|---:|---|
| 材料摘要、格式转换、简单事实候选抽取 | Spring Boot → Dify | 否 | 结果只进入 `ai_research_draft` |
| 事件影响分析 | Spring Boot → Dify | 按复杂度 | 冲突来源、跨行业传导、重要事件可路由 TA |
| 股票基本面/预期差研究 | Spring Boot → Dify | 按 Profile | 确定性因子由 Spring Boot 算；TA 做多视角解释 |
| 商品供需、现货、基差/期限结构 | Spring Boot → Dify | 按 Profile | TA 可做多空辩论，不能改供需或行情数值 |
| 量价状态解释 | Spring Boot → Dify | 通常否 | 指标和状态由 Spring Boot 计算；复杂反方研究可选 TA |
| 研究卡普通语言说明 | Spring Boot → Dify | 否 | 只解释已形成的结构化研究卡 |
| 反方研究/多空辩论/重要卡深研 | Spring Boot → Dify → TA | 是 | `max_rounds` 一期不超过 2，输出保留角色分歧 |
| 研究问答 | Spring Boot 查询结构化上下文 → Dify | 按问题复杂度 | Dify 不得越过门禁或凭空生成候选 |

路由由服务端登记的 `WorkflowProfile` 决定，用户不能通过自由文本强行打开任意工具、任意 URL 或无限辩论轮次。

### 3. 通信协议、安全和可靠性

- Vue 与 Spring Boot：HTTPS REST；任务进度通过 Spring Boot 轮询或 SSE/WebSocket。浏览器不持有 Dify Key、TradingAgents 地址或内部 Token。
- Spring Boot 与 Dify：Dify Workflow Service API，`Authorization: Bearer {DIFY_APP_API_KEY}`，默认 `POST /v1/workflows/run` + `response_mode=streaming`，消费 `text/event-stream`；调用前保存 `invocationId`、task、snapshot、request hash，返回 `runId` 后关联。
- Dify 与 TradingAgents：Dify HTTP Request/登记 OpenAPI 工具调用 `TradingAgents Adapter`，使用内网 HTTPS REST/JSON、服务身份、OpenAPI Schema、`Idempotency-Key`、超时、限流和熔断。Adapter 屏蔽 TradingAgents-CN 版本和 Python 内部结构。
- 结果格式：至少包含 `runId`、`status`、`agentViews[]`、`citations[]`、`unknowns[]`、`warnings[]`、`schemaVersion`、`engineVersion`；角色观点必须包含 `agentRole`、`content`、`evidenceRefs`、`confidence`（只作草稿置信度）和 `completedAt`。
- 重试：Dify 和 Adapter 不得双重无限重试；调用方只能按 Profile 做有界重试，保留 `retryOf`，同一业务意图复用幂等键。外部调用不得包在长数据库事务中。
- 数据边界：只传最小授权上下文（任务 ID、材料/事件/标的引用、点时快照、结构化指标、质量/授权状态）；不传密码、API Key、账户、资金、持仓、订单、风控内部状态或无关个人信息。
- MCP：一期不用作主链路。未来只能作为 Adapter 的工具外壳，必须有白名单、Schema、认证、审计、超时/限流和 ADR，不能绕过 Adapter。

### 4. 数据库详细设计

#### 4.1 复用表与新增表边界

**若依底座复用，不重建：** `sys_user`、`sys_role`、`sys_user_role`、`sys_menu`、`sys_dict_type`、`sys_dict_data`、`sys_config`、`sys_notice`、`sys_message`、`sys_oper_log`。模块二只引用用户/角色/权限稳定 ID；Dify/TradingAgents 不可访问这些表。

**复用模块一研究入口表，不重建：**

| 表 | 模块二引用 | 作用 |
|---|---|---|
| `quantpf_research_material`、`quantpf_research_material_relation` | `material_id`、关系 ID | 材料、版本、转载/修订关系 |
| `quantpf_research_task`、`quantpf_research_task_run` | `task_id`、`task_run_id` | 研究任务、运行和输入快照 |
| `quantpf_dify_workflow_registry`、`quantpf_dify_invocation` | `workflow_registry_id`、`invocation_id` | Workflow 登记、Dify 运行和 SSE 事件 |
| `quantpf_trading_agents_run`、`quantpf_trading_agents_agent_view` | `ta_run_id`、角色观点 | 模块一复核新增的 TA 运行和多角色结果；模块二只关联/展示 |
| `quantpf_ai_research_draft`、`quantpf_ai_validation_result` | `draft_id`、验证结果 | AI_DRAFT 和校验结果，不是事实主表 |
| `quantpf_research_event`、`quantpf_event_evidence` | `event_id`、`event_evidence_id` | 核验后的事件和证据 |
| 模块一标的/行情/点时/因子/供需表 | 稳定 ID | `instrument`、`point_in_time`、行情、量价、供需等权威数据 |

**本模块新增 13 张 MySQL 业务表：**

| 子域 | 表 |
|---|---|
| 事实核验 | `quantpf_research_claim`、`quantpf_research_evidence_link` |
| 研究链 | `quantpf_research_thesis`、`quantpf_research_prediction`、`quantpf_research_counter_evidence` |
| 资产研究 | `quantpf_fundamental_factor_snapshot`、`quantpf_event_exposure`、`quantpf_commodity_research_snapshot` |
| 候选筛选 | `quantpf_screening_run`、`quantpf_candidate_assessment`、`quantpf_research_card` |
| 审核交接 | `quantpf_research_review`、`quantpf_candidate_handoff` |

不新增 DuckDB 表；研究结论、评分、研究卡、审核和交接是 MySQL 业务数据。原文/超长 AI 响应可存 MinIO，MySQL 只保存对象引用、Hash、大小和保留期。

#### 4.2 统一字段、主键、版本和索引规则

- 每张新增表：`id BIGINT NOT NULL PRIMARY KEY`，应用生成、禁止 `AUTO_INCREMENT`；关联 ID 同为 `BIGINT`。
- 表尾固定：`create_by BIGINT NULL`、`create_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`、`update_by BIGINT NULL`、`update_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)`、`remark VARCHAR(500) NULL`。
- 幂等键 `VARCHAR(128)`（Adapter 外部幂等可按接口要求扩至 192），唯一索引；版本对象保留 `version_no`、`previous_*_id`、`current_flag`，当前版本用生成列/过滤唯一策略，不以 `ORDER BY id` 推断最新。
- 时间：UTC `DATETIME(6)`，同时保留市场本地日期；价格、金额、比例、评分用 `DECIMAL`；集合/传导链/引用用 JSON，但必须有可检索状态和摘要列。
- 模块三/四引用为逻辑外键；模块二内部父子关系可建物理 FK。所有写命令支持 `requestId`、幂等键和需要并发控制的 `expectedVersion`。

#### 4.3 13 张表逐表字段、约束、索引和 Dify 边界

| 表 | 字段（类型；主要约束） | 索引/唯一键 | Dify/TradingAgents 交互 |
|---|---|---|---|
| `quantpf_research_claim` | `id BIGINT PK`；`claim_hash CHAR(64) NN`；`draft_id/task_id BIGINT`；`material_id/event_id BIGINT`；`claim_type VARCHAR(32) NN`；`statement VARCHAR(2000) NN`；`subject_ref/source_ref VARCHAR(128)`；`quote_text TEXT`；`quote_location/structured_field_ref VARCHAR(256)`；`published_at/available_at/valid_from/valid_to DATETIME(6)`；`snapshot_id BIGINT`；`verification_status VARCHAR(32) NN`；`quality_status/authorization_status/conflict_status VARCHAR(24)`；`verification_method VARCHAR(32)`；`verifier_user_id BIGINT`；`review_reason VARCHAR(500)`；`version_no INT NN`；`previous_claim_id BIGINT`；`current_flag TINYINT`；五日志字段 | `UK(claim_hash)`；`IX(task_id)`；`IX(subject_ref)`；`IX(verification_status,create_time)` | AI/TA 事实候选先入 `quantpf_ai_research_draft`；通过 Schema、引用、点时、授权、身份和冲突校验后由 Spring Boot 升级；AI 不直写或改 VERIFIED 主张 |
| `quantpf_research_evidence_link` | `id BIGINT PK`；`claim_id BIGINT NN`；`evidence_type VARCHAR(32) NN`；`material_id/event_evidence_id/source_id/snapshot_id BIGINT`；`quote_text TEXT`；`quote_location/structured_field_ref VARCHAR(256)`；`independence_group VARCHAR(64)`；`verification_status VARCHAR(24)`；五日志字段 | `UK(claim_id,material_id,quote_location,evidence_type)`；`IX(claim_id)`；`IX(source_id,independence_group)`；`IX(event_evidence_id)` | AI 引用只作 `AI_EXTRACTED/PENDING`；Spring Boot 绑定可回源证据，防止同一转载被算作多份独立证据 |
| `quantpf_research_thesis` | `id BIGINT PK`；`thesis_key VARCHAR(128) NN`；`task_id/event_id/draft_id/instrument_id BIGINT`；`asset_type VARCHAR(16) NN`；`subject_ref VARCHAR(128)`；`direction VARCHAR(16)`；`research_period VARCHAR(16)`；`impact_variables/transmission_chain/direct_beneficiaries/potential_losers/reverse_transmission/unknown_items/follow_up_data JSON`；`status VARCHAR(32)`；`version_no INT NN`；`previous_thesis_id BIGINT`；`current_flag TINYINT`；`valid_from/valid_to DATETIME(6)`；五日志字段 | `UK(thesis_key,version_no)`；`IX(task_id)`；`IX(instrument_id,direction)`；`IX(status,create_time)` | Dify/TA 可提供传导链草稿；Spring Boot 校验后写入；AI 不改方向、状态、版本 |
| `quantpf_research_prediction` | `id BIGINT PK`；`thesis_id/draft_id BIGINT`；`direction VARCHAR(16) NN`；`forecast_horizon VARCHAR(16)`；`conditions/invalidating_conditions/expected_path JSON`；`probability_status VARCHAR(32)`；`sample_result_ref BIGINT`；`sample_count INT`；`confidence_interval JSON`；`cost_adjusted_expectation/max_adverse_excursion DECIMAL(24,8)`；`data_snapshot_id BIGINT`；`status VARCHAR(24)`；`version_no INT`；`current_flag TINYINT`；五日志字段 | `UK(thesis_id,version_no)`；`IX(thesis_id)`；`IX(probability_status,create_time)` | AI/TA 只给条件和方向草稿；样本、概率、期望由 Spring Boot/模块四计算，不能接受模型自报概率 |
| `quantpf_research_counter_evidence` | `id BIGINT PK`；`target_type VARCHAR(32) NN`；`target_id BIGINT NN`；`counter_type VARCHAR(32) NN`；`statement VARCHAR(2000) NN`；`evidence_refs JSON`；`importance VARCHAR(16)`；`check_status VARCHAR(32)`；`origin VARCHAR(24)`；`draft_id BIGINT`；五日志字段 | `IX(target_type,target_id)`；`IX(counter_type,check_status)` | TA 多空/反方输出可作为反方项草稿；核查状态由 Spring Boot/人工推进，AI 不改 `check_status` |
| `quantpf_fundamental_factor_snapshot` | `id BIGINT PK`；`instrument_id BIGINT NN`；`report_period VARCHAR(16)`；`market_local_date DATE`；`as_of_time DATETIME(6)`；`business_authenticity/earnings_quality/valuation/event_sensitivity JSON`；`disclosure_method VARCHAR(32)`；`factor_version VARCHAR(64) NN`；`point_in_time_id BIGINT NN`；`source_snapshot_id BIGINT`；`status VARCHAR(24)`；五日志字段 | `UK(instrument_id,report_period,factor_version,point_in_time_id)`；`IX(instrument_id,market_local_date)`；`IX(factor_version)` | 纯确定性计算结果；Dify/TA 只读解释，不可补造或修改因子数值 |
| `quantpf_event_exposure` | `id BIGINT PK`；`event_id BIGINT NN`；`event_variable_name VARCHAR(128) NN`；`exposure_type VARCHAR(32) NN`；`exposure_code VARCHAR(64) NN`；`instrument_id BIGINT`；`exposure_level/direction VARCHAR(32)`；`business_exposure/sensitivity_score DECIMAL(24,8)`；`sensitivity_method VARCHAR(32)`；`evidence_refs JSON`；`status VARCHAR(24)`；`point_in_time_id BIGINT`；五日志字段 | `UK(event_id,exposure_type,exposure_code)`；`IX(exposure_code,exposure_type)`；`IX(event_id)` | Dify/TA 可提关联候选；暴露/敏感度由 Spring Boot 确定性映射，AI 不改数值 |
| `quantpf_commodity_research_snapshot` | `id BIGINT PK`；`future_product_id BIGINT NN`；`commodity_code VARCHAR(64)`；`task_id BIGINT`；`market_local_date DATE`；`as_of_time DATETIME(6)`；`industry_judgment/price_judgment VARCHAR(24)`；`tool_expression VARCHAR(32)`；`contract_instrument_id BIGINT`；`spot_exposure_type VARCHAR(32)`；`supply_demand_summary/basis_term_summary/contract_risk/evidence_refs JSON`；`status VARCHAR(24)`；`point_in_time_id BIGINT`；五日志字段 | `UK(future_product_id,market_local_date,point_in_time_id)`；`IX(future_product_id,market_local_date)`；`IX(tool_expression,status)` | 供需、基差、期限结构由 Spring Boot 计算；Dify/TA 只读解释，不能派生现货空单或交易指令 |
| `quantpf_screening_run` | `id BIGINT PK`；`screening_key VARCHAR(128) NN`；`entry_type VARCHAR(32) NN`；`asset_scope/screening_condition JSON`；`rule_version/score_version VARCHAR(64)`；`data_snapshot_id BIGINT`；`as_of_time DATETIME(6)`；`status VARCHAR(24)`；`total_count/pass_count/reject_count INT`；`started_at/finished_at DATETIME(6)`；`trigger_source VARCHAR(24)`；五日志字段 | `UK(screening_key)`；`IX(entry_type,create_time)`；`IX(status,create_time)` | 无 AI 写入；Spring Boot 按确定性规则运行，Dify/TA 不能触发未经授权的筛选参数 |
| `quantpf_candidate_assessment` | `id BIGINT PK`；`screening_run_id BIGINT NN`；`candidate_key VARCHAR(128)`；`task_id BIGINT`；`asset_type VARCHAR(16) NN`；`subject_ref VARCHAR(128)`；`instrument_id BIGINT`；`direction/research_period VARCHAR(16)`；`candidate_stage VARCHAR(32)`；`hard_gate_results JSON`；`research_score DECIMAL(6,2)`；`score_version VARCHAR(64)`；`dimension_scores JSON`；`opportunity_status/status VARCHAR(32)`；`reject_reason_codes/evidence_refs JSON`；`data_snapshot_id/quant_feature_snapshot_id BIGINT`；`card_id BIGINT`；五日志字段 | `UK(screening_run_id,subject_ref,direction,research_period)`；`IX(instrument_id)`；`IX(status,create_time)`；`IX(card_id)` | 硬门禁/四维研究分由 Spring Boot 计算；AI 自报评分、门禁、机会状态只作草稿展示或忽略 |
| `quantpf_research_card` | `id BIGINT PK`；`card_key VARCHAR(128) NN`；`card_version_no INT NN`；`candidate_assessment_id BIGINT NN`；`asset_type/subject_ref/instrument_id/research_entry/task_id`；`direction VARCHAR(16)`；`research_period VARCHAR(16)`；`research_conclusion VARCHAR(2000)`；`research_status/message_validity/price_reaction_status/opportunity_status/handoff_status VARCHAR(32)`；`fact_refs/event_refs/source_refs JSON`；`fundamental_or_supply_demand_analysis/valuation_or_basis_analysis JSON`；`support_evidence_refs/counter_evidence_refs/unknown_items/falsification_conditions JSON`；`market_state/price_position/volume_state VARCHAR(32)`；`strategy_evaluation_hints JSON`；`sample_result_ref BIGINT`；`sample_count INT`；`probability DECIMAL(12,8)`；`confidence_interval JSON`；`cost_adjusted_expectation/max_adverse_excursion DECIMAL(24,8)`；`out_of_sample_status VARCHAR(24)`；`satisfied_conditions/unsatisfied_conditions/next_check_conditions JSON`；`valid_until DATETIME(6)`；`data_snapshot_id/quant_feature_snapshot_id BIGINT`；`previous_card_version_id BIGINT`；`current_flag TINYINT`；五日志字段 | `UK(card_key,card_version_no)`；当前版本过滤唯一键；`IX(candidate_assessment_id)`；`IX(asset_type,opportunity_status)`；`IX(instrument_id)` | Spring Boot 形成结构化卡；Dify `RESEARCH_CARD_EXPLANATION` 只生成普通语言说明草稿，不写状态/评分/版本 |
| `quantpf_research_review` | `id BIGINT PK`；`review_key VARCHAR(128) NN`；`object_type VARCHAR(24) NN`；`object_id BIGINT NN`；`object_version_no INT`；`review_action VARCHAR(32) NN`；`reviewer_user_id BIGINT`；`review_reason VARCHAR(500)`；`review_scope JSON`；`before_state/after_state VARCHAR(32)`；`override_expiry DATETIME(6)`；五日志字段 | `UK(review_key)`；`IX(object_type,object_id)`；`IX(reviewer_user_id,create_time)` | 无 AI 交互；只由用户/系统审核写入，覆盖不得越过身份、授权、质量和风险硬门禁 |
| `quantpf_candidate_handoff` | `id BIGINT PK`；`handoff_key VARCHAR(128) NN`；`candidate_assessment_id BIGINT NN`；`research_card_id BIGINT`；`card_version_no INT`；`target_module VARCHAR(32) NN`；`handoff_status VARCHAR(32) NN`；`handoff_payload JSON`；`reject_reason VARCHAR(500)`；`correlation_id/causation_id VARCHAR(64)`；`handed_off_at DATETIME(6)`；五日志字段 | `UK(handoff_key)`；`IX(candidate_assessment_id)`；`IX(target_module,handoff_status)` | 无 AI 交互；Spring Boot 通过模块三端口交接，Dify/TA 不能创建或修改交接 |

**研究评分明确为模块二 70 分制：** 事件可信度与新颖性 15、行业传导链 20、标的质量/供需质量 20、估值/预期差 15。策略匹配 15、执行流动性 5、风险收益比 10 属模块三/四；模块二的 `research_score` 不能冒充总 100 分，缺维度不补满分。研究卡概率字段只引用模块四验证结果，不由本模块计算伪精确概率。

#### 4.4 Dify/TradingAgents 数据交互矩阵

| 数据 | Dify/TA 可读 | 写入位置 | 禁止 |
|---|---|---|---|
| 已核验主张、证据摘要 | 最小化只读 | Spring Boot `claim/evidence` | AI 直写事实主表 |
| AI 提取、角色观点、原始响应 | 本次 Workflow 上下文 | `quantpf_ai_research_draft`、`quantpf_trading_agents_*` | 直接升级 VERIFIED |
| 基本面/供需/量价因子 | 结构化只读 | Spring Boot 计算表 | AI 补造/覆盖数值 |
| 候选、硬门禁、研究评分 | 只读摘要 | Spring Boot 计算 | AI 自报分数/状态 |
| 研究卡结构化字段 | 只读 | Spring Boot 版本表 | Dify 改状态/版本 |
| 审核/交接/账户/风险/订单 | 不可读 | Spring Boot | 任何 AI 访问或写入 |

### 5. 用户操作界面说明

#### 5.1 若依前端页面结构

复用 `contract/menu/quantpf-menu-facts.yaml` 的 Research 菜单事实源，页面建议如下：

```text
Research/研究中心
├─ 快捷研究、研究任务、材料、AI草稿、研究事件、研究助手（模块一入口）
├─ 候选池                         /quant/research/candidate/index
├─ 候选详情                       /quant/research/candidate/detail
├─ 研究决策卡列表/详情             /quant/research/card/index、detail
├─ 事实主张与证据核验              /quant/research/claim/index
├─ 研究假设/预测/反方              /quant/research/thesis/detail
├─ 资产研究                        /quant/research/factor/index
└─ 候选交接与回执                  /quant/research/handoff/index
```

角色分层：普通用户查看结论、证据摘要和下一步；审核用户核验事实、研究卡、受限覆盖和交接；专业用户查看评分、快照、Workflow/Prompt/Schema、TA 角色观点和运行诊断。专业字段默认折叠，后端仍必须校验 Sa-Token 权限和 owner/scope。

#### 5.2 关键用户流程

1. **普通用户低输入研究**：选择标的/材料或自然语言问题、研究周期、研究入口三项以内 → Spring Boot 自动补齐授权材料、点时、市场日期和上下文 → 提交 → 查看事实/解释/预测分层、候选和研究卡。
2. **复杂研究**：在任务/研究卡点击“补充多视角/反方研究” → 前端只提交对象 ID、版本和意图 → Spring Boot 做权限、版本和 Profile 校验 → Dify 按需调用 TA → 页面展示阶段、角色观点、轮次、引用、分歧和 AI_DRAFT 标签。
3. **事实/研究卡审核**：审核用户查看证据回源、材料版本、点时和冲突 → 确认/否决/延期/请求补充/受限覆盖 → 必填理由并带 `expectedVersion` → 成功后更新业务状态或生成下一版本；旧版本冲突时拒绝写入。
4. **候选交接**：研究卡满足研究域门禁 → 用户点击“提交策略评估” → Spring Boot 生成幂等交接并调用模块三端口 → 页面显示接受/拒绝/待补充回执；拒绝时可补证据，不显示下单、开仓或平仓按钮。
5. **无候选/降级**：无合格候选显示门禁失败原因和下一步；Dify/TA 超时显示“暂不可用/稍后重试”，保留确定性筛选和既有研究卡。

#### 5.3 Dify/TradingAgents 交互入口

| 页面入口 | 动作 | 后端行为 | 结果展示 |
|---|---|---|---|
| 研究任务向导 | 开始 AI 研究 | 调用对应 Workflow，按 Profile 决定是否 TA | 进度、草稿、引用、校验 |
| 材料/事件详情 | 分析影响 | `NEWS_IMPACT_ANALYSIS`，复杂任务可 TA | 传导链、角色观点、分歧 |
| 股票/商品/量价详情 | 用当前证据解释/补充多视角 | 先读取确定性结果，再调用 Dify/TA | 因子解释、反方问题，不改数值 |
| 研究卡详情 | 解释当前研究卡 | `RESEARCH_CARD_EXPLANATION`，默认不 TA | 普通语言说明，引用回源 |
| 研究助手 | 提问 | Spring Boot 先查授权结构化结果，再调 `RESEARCH_QA` | 事实/解释/未知分层 |
| 审核待办 | 请求反方研究 | 权限和版本通过后触发深度 Profile | AI_DRAFT，等待审核 |

浏览器网络面板不得出现 Dify/TradingAgents 内部地址或凭据；所有按钮只调用 Spring Boot API。

### 6. 代码仓库落地指导

#### 6.1 后端目录建议

```text
RuoYi-Vue-Plus/
├─ db/migration/                                      # Flyway 权威迁移
├─ quantpf-modules/quantpf-domain/src/main/java/com/quantpf/domain/
│  ├─ research/                                       # Claim/Evidence/Thesis/Prediction/Card 等实体和值对象
│  ├─ asset/                                          # FundamentalFactor/EventExposure/CommoditySnapshot
│  ├─ repository/                                     # Repository 端口
│  └─ persistence/                                    # Record/Mapper/XML 映射
├─ quantpf-modules/quantpf-research/src/main/java/com/quantpf/research/
│  ├─ application/                                    # 任务、候选、卡、审核、交接用例
│  ├─ claim/ thesis/ asset/ screening/ card/ handoff/ qa/
│  ├─ dify/                                           # 复用模块一 DifyClient/SSE/CallbackVerifier
│  ├─ tradingagents/                                  # Adapter Client、Run 查询、结果映射
│  ├─ validation/                                    # Schema/引用/时间/授权/身份/冲突校验
│  └─ adapter/mysql/                                  # Repository 实现
└─ ruoyi-admin/src/main/java/com/quantpf/web/controller/
   ├─ ResearchTaskController.java（复用/扩展）
   ├─ ResearchClaimController.java
   ├─ ResearchCandidateController.java
   ├─ ResearchCardController.java
   ├─ ResearchReviewController.java
   ├─ CandidateHandoffController.java
   ├─ ResearchFactorController.java
   └─ ResearchQaController.java
```

#### 6.2 关键类/接口职责

| 类/接口 | 职责 |
|---|---|
| `ClaimVerificationService`、`EvidenceLinkService` | 草稿→主张升级、证据回源、状态机、独立性校验 |
| `ResearchChainService` | 假设/预测/反方记录、事实修订失效传播 |
| `FundamentalFactorCalculator`、`EventExposureMapper`、`CommodityResearchService` | 确定性因子、事件暴露、商品供需/基差结论 |
| `ScreeningRunner`、`CandidateAssessor`、`ResearchScorer` | 硬门禁→四维 70 分研究评分→候选状态 |
| `ResearchCardService` | 结构化研究卡、版本、当前版本、多周期预期 |
| `ResearchReviewService` | expectedVersion、审核理由、受限覆盖 |
| `CandidateHandoffService` | 幂等交接模块三、回执和 correlation/causation |
| `DifyResearchClient`、`DifyStreamConsumer`、`DifyCallbackVerifier` | Dify REST/SSE、事件归一化、可选回调签名校验 |
| `TradingAgentsAdapterClient` | 仅访问受控 Adapter；健康/创建/查询/取消；不暴露 Python 内部类 |
| `ResearchQaService` | Spring Boot 查询授权上下文，Dify 组织带引用回答 |
| `ResearchScreeningJobHandler` | 只调用应用服务，不在 Job 中直接写 Mapper 或调用订单 |
| `ResearchCandidateController` 等 | REST DTO、权限、幂等、错误映射；不得直接读写 Mapper |

依赖方向：`domain ← research ← ruoyi-admin`；domain 不依赖 Web、Dify SDK、TradingAgents 实现；research 不依赖 execution/finance/risk 的写模型。

#### 6.3 API 与 Dify/TA 调用约定

对外业务 API 示例：

- `POST /quant/research/tasks`：创建研究任务，必带 `Idempotency-Key`；
- `GET /quant/research/tasks/{taskId}`、`GET /runs/{runId}`：恢复进度；
- `POST /quant/research/tasks/{taskId}/deep-research`：请求多视角研究，带 `expectedVersion`；
- `GET /quant/research/candidates`、`GET /quant/research/cards/{cardId}`：只读候选/卡；
- `POST /quant/research/cards/{cardId}/reviews`：审核/覆盖，带理由和版本；
- `POST /quant/research/candidates/{candidateId}/handoffs`：交接模块三，幂等；
- `POST /quant/research/assistant/query`：研究问答，不接受交易执行意图。

Dify 请求 Envelope 至少包含：`requestId`、`taskId`、`taskRunId`、`workflowCode`、`workflowVersion`、`promptVersion`、`schemaVersion`、`inputSnapshotId/hash`、`pointInTimeId`、`authorizedRefs`、`deterministicContext`、`executionRoute`。Dify 输出 Envelope 至少包含：`runId`、`status`、`draftId`、`claims[]`、`explanations[]`、`predictions[]`、`counterEvidence[]`、`citations[]`、`unknowns[]`、`warnings[]`、`taRunId`（如有）；禁止输出被直接当成 `research_score`、`hard_gate_results` 或订单命令。

TradingAgents Adapter 接口建议：

- `POST /internal/v1/research-runs`：创建异步 run；
- `GET /internal/v1/research-runs/{taRunId}`：查询状态/结果；
- `POST /internal/v1/research-runs/{taRunId}/cancel`：取消；
- `GET /internal/v1/health`：健康检查。

请求必须带 `X-Request-Id`、服务身份、`Idempotency-Key`、`schemaVersion`、`workflowProfile`、`maxRounds`（一期 1~2）、超时预算和最小化研究上下文。响应不允许包含账户、持仓、订单或可执行交易指令。

#### 6.4 配置项

后端环境变量/`application-*.yml` 需要新增或核对：

```yaml
quantpf:
  research:
    dify:
      base-url: ${QUANTPF_DIFY_BASE_URL}
      app-api-key: ${QUANTPF_DIFY_APP_API_KEY}
      connect-timeout-ms: 3000
      read-timeout-ms: 180000
      max-retries: 2
    trading-agents:
      adapter-base-url: ${QUANTPF_TRADING_AGENTS_ADAPTER_URL}
      service-token-ref: ${QUANTPF_TRADING_AGENTS_SERVICE_TOKEN_REF}
      connect-timeout-ms: 3000
      read-timeout-ms: 180000
      max-rounds: 2
      max-concurrency: 4
      enabled: false
    workflow-profiles:
      deep-research-enabled: false
    scoring:
      event-novelty-max: 15
      transmission-max: 20
      asset-quality-max: 20
      valuation-expectation-max: 15
    screening:
      research-score-max: 70
      total-score-incomplete-status: SCORE_INCOMPLETE
    handoff:
      retry-max: 2
```

密钥只能由环境变量/密钥系统注入，不进 Git、前端 bundle、Workflow 文本、普通日志或错误消息；`sys_config` 只存非敏感开关。

### 7. Risks / Trade-offs

- **[Risk] TradingAgents 输出异构、角色部分失败或引用不稳定** → Adapter 版本化 JSON Schema，按角色保留状态，允许 `PARTIAL`，引用不通过则只能是 AI_DRAFT。
- **[Risk] Dify 与 Adapter 双重重试造成雪崩** → 明确总尝试预算、幂等键、限流、熔断和唯一重试责任。
- **[Risk] 用户把研究分当成交易信号** → 页面明确候选≠信号≠交易计划；后端不提供直达订单路径。
- **[Risk] 版本冲突导致审核覆盖旧事实** → 所有审核/卡更新传 `expectedVersion`，数据库保留前版本并拒绝陈旧写入。
- **[Risk] AI 访问过多业务数据** → 最小化 Envelope、网络白名单、独立服务身份、Dify/TA 无数据库写权限、审计拒绝请求。
- **[Trade-off] 不采用 MCP 牺牲部分工具发现便利** → 换取固定 Schema、可审计、可限流和较小的模型自主调用面；未来可在 Adapter 层兼容而不改变业务边界。

## Migration Plan

1. 先核对后端当前 Flyway head 和模块一已落迁移；模块二新增迁移按现有版本递增，规划为事实/研究链、资产研究、候选/研究卡、审核/交接四组，禁止修改已发布迁移。
2. 先创建表、索引、CHECK、字典和权限种子，再实现实体/Repository；新库和已有库各跑一次迁移验证。
3. 部署后端确定性研究服务和 Dify Stub；`trading-agents.enabled=false` 默认关闭，先验证 Dify 简单 Workflow，再以测试 Profile 启用 TA。
4. 开启复杂研究 Profile，使用小并发、测试材料和 A 股/国内期货日线数据；验证角色分歧、部分失败、超时、重试和降级。
5. 通过端到端验收后再启用审核/交接菜单。回滚时关闭 Profile、保留已产生的草稿/运行审计，不删除事实/研究卡历史；若迁移需回滚，按数据库备份和反向脚本执行，不在生产直接删除业务表。

## Open Questions

无阻塞性开放问题。Workflow code/version、TradingAgents Adapter 具体端口、Flyway 版本号和菜单 ID 在施工前从部署环境/仓库当前事实源读取，但不改变上述协议、职责、数据边界或任务顺序。
