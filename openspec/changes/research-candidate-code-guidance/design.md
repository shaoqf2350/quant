# Design

## Context

本 change 对应已完成的 `research-candidate-db-design` 数据库设计（13 张模块二专属表），并承接 `data-foundation-code-guidance` 已规划的模块一研究入口服务骨架，不替代二者。当前仓库是 Java 21 的 RuoYi-Vue-Plus 单体工程：

- `quantpf-domain` 已有 Paper 领域实体 + 模块一规划的数据/标的/日历/行情/研究入口（material/task/draft/event/evidence）领域对象与 Repository 端口；
- `quantpf-research` 模块一已规划 `application/`（研究任务、材料、事实/事件编排）、`dify/`（DifyClient、流式消费、回调校验）、`validation/`（Schema/引用/时间/授权/身份/冲突）、`adapter/mysql/`（研究域适配）、`event/`（研究事件发布）；
- `quantpf-strategy` 是空骨架（模块三拥有，需先暴露交接端口）；`quantpf-job` 只有 `PaperApplicationServicePort`；
- MyBatis-Plus 已配 `idType: ASSIGN_ID` + `DefaultIdentifierGenerator`，模块一已规划 `quantpf.id.*` 雪花配置与五个末尾日志字段；
- 模块一 `design.md` §6 已定义 Dify `/v1/workflows/run` 调用、streaming 消费、可选 HMAC 回调与降级。

因此模块二施工必须**在模块一骨架之上增量扩展**：不重建 Dify 客户端或 ID/日志字段基建，不新建独立服务，不把模块三策略/模块四风险与 Paper 事务写进研究域，也不让 Dify 或调度 Handler 成为事实/评分/研究卡/交接的权威源。

## Goals / Non-Goals

**Goals:**

- 给出模块二可直接施工的目录、依赖方向、关键接口职责、确定性计算与 Dify 解释的边界、配置契约。
- 覆盖事实核验、研究链、资产研究、候选筛选/评分、研究卡、审核、交接七类服务，使其可测试、可回放、可幂等、可审计。
- 明确研究评分 70 分制（模块二贡献）与总 100 分门槛（需模块三/四合并）的边界，防止实现时把研究分当总分。
- 复用模块一 Dify 适配器，新增研究型 Workflow 的请求/输出契约与确定性-草稿分离。
- 保持模块二与 Paper、RiskCheck、订单、账户、持仓和真实交易通道的隔离。

**Non-Goals:**

- 不实现 Java 代码、DDL、Dify Workflow、前端页面或 XXL-JOB Handler 内容；只冻结指导与行为契约。
- 不实现模块三策略评估/观察池/预警、模块四组合风险/回测/概率/复盘；模块二只依赖其端口、只交接候选。
- 不新增真实交易通道、QMT/CTP、LIVE 或自动实盘。
- 不冻结评分公式精确实现、策略参数、概率门槛、Paper 手续费/滑点（属模块三/四与后续子详设）。

## Decisions

### 1. 仓库目录结构与依赖方向

在模块一骨架之上按域扩展 `quantpf-domain` 与 `quantpf-research`，不新增顶层模块：

```text
RuoYi-Vue-Plus/
├── quantpf-modules/
│   ├── quantpf-common/                  # 已有：ID/审计/时间/错误码/事件，扩展研究域错误码
│   ├── quantpf-domain/
│   │   └── com/quantpf/domain/
│   │       ├── research/                # 模块二新增：Claim、EvidenceLink、Thesis、Prediction、
│   │       │                            #   CounterEvidence、ScreeningRun、CandidateAssessment、
│   │       │                            #   ResearchCard、ResearchReview、CandidateHandoff
│   │       ├── asset/                   # 模块二新增：FundamentalFactorSnapshot、EventExposure、
│   │       │                            #   CommodityResearchSnapshot
│   │       ├── repository/              # research/asset 的领域 Repository 端口
│   │       └── persistence/             # 新增 Record/Mapper/XML，按 research/asset 分包
│   ├── quantpf-research/
│   │   └── com/quantpf/research/
│   │       ├── application/             # 已有（任务/材料/事件编排）之外，新增研究链/候选/卡片/审核/交接编排入口
│   │       ├── claim/                   # ClaimVerificationService、EvidenceLinkService
│   │       ├── thesis/                  # ResearchChainService、失效传播（NEEDS_RECALCULATION）
│   │       ├── asset/                   # FundamentalFactorCalculator、EventExposureMapper、CommodityResearchService
│   │       ├── screening/               # ScreeningRunner、CandidateAssessor、ResearchScorer、硬门禁
│   │       ├── card/                    # ResearchCardService（聚合/版本/当前版本/多周期预期）
│   │       ├── handoff/                 # CandidateHandoffService（模块三交接、回执）
│   │       ├── qa/                      # ResearchQaService（Spring Boot 查询 + Dify 组织答案）
│   │       ├── dify/                    # 已有（模块一）：复用，仅新增研究型 workflow 输入/输出构建
│   │       ├── validation/              # 已有：扩展 claim 级引用/事实升级校验
│   │       ├── adapter/mysql/           # research/asset Repository Adapter
│   │       └── event/                   # 研究链/候选/研究卡/交接事件发布
│   ├── quantpf-strategy/                # 模块三：模块二只依赖其交接端口（CandidateHandoffSink），不写实现
│   └── quantpf-job/
│       └── com/quantpf/job/handler/     # ResearchScreeningJobHandler、RevalidationJobHandler、
│                                        #   HandoffRetryJobHandler（仅调用应用服务）
└── ruoyi-admin/
    ├── src/main/java/com/quantpf/web/
    │   ├── controller/                  # research/claims/theses/candidates/cards/review/handoff/qa/dify
    │   ├── dto/                         # 请求/响应 DTO，不暴露内部 Record
    │   └── config/                      # 评分/筛选/问答配置绑定
    └── src/main/resources/application*.yml
```

依赖方向：

```text
ruoyi-admin -> quantpf-job / quantpf-research / quantpf-domain
quantpf-job -> application ports（不复制业务规则）
quantpf-research -> quantpf-marketdata ports（只读行情/标的/因子/点时）+ quantpf-strategy ports（仅交接端口）
quantpf-research -> quantpf-domain/common
quantpf-domain -> common（不依赖 Web、Dify、XXL-JOB、Paper execution、strategy 实现）
quantpf-strategy 不被 quantpf-research 反向直连实现类，只面向端口
```

### 2. 关键类和接口职责

| 组件 | 建议职责 | 明确禁止 |
|---|---|---|
| `ClaimVerificationService` | 从 AI 草稿升级事实候选为核验/待核验主张，绑定证据，执行主张状态机，版本与前驱 | 让 Dify 直写主张，跳过引用校验，覆盖历史结论 |
| `EvidenceLinkService` | 建立主张↔来源/材料/事件证据关联，标注证据类型与独立性分组，重复/转载归组 | 把转载当独立来源，把 USER_ANNOTATION 伪装外部证据 |
| `ResearchChainService` | 研究假设/条件式预测/反方证据的构建、版本化与失效传播 | 把模型方向草稿当统计概率，短期信号外推长期 |
| `FundamentalFactorCalculator` | 确定性计算股票业务真实性/盈利质量/估值/预期差因子，标记 `DISCLOSED/CALCULATED/MODELLED/INFERRED`，按点时/版本落库 | 用模型输出补造缺失因子或数值 |
| `EventExposureMapper` | 事件→行业/商品/标的暴露映射、方向、敏感度与证据；确定性规则加人工候选 | 接受 Dify 自报映射作为权威暴露 |
| `CommodityResearchService` | 商品产业判断/价格判断/工具表达 + 现货敞口登记；无敞口不派生现货空单 | 把连续/主力合约当执行工具 |
| `ScreeningRunner` | 固定范围/规则版本/数据快照的筛选跑批，幂等、可回放，产出筛选运行记录 | 在 Handler 内写筛选逻辑、覆写历史运行 |
| `CandidateAssessor` | 逐项硬门禁 → 分项研究评分（70 分制）→ 候选阶段/机会状态/原因码 | 用评分补偿硬门禁，把 70 分当总分 |
| `ResearchScorer` | 承载四维研究评分（事件可信度与新颖性 15/行业传导链 20/标的质量·供需质量 20/估值·预期差 15）、版本与证据 | 计算策略/执行/风险维度，或宣布收益概率 |
| `ResearchCardService` | 研究卡聚合/版本/单一当前版本/多周期预期引用/统计字段引用模块四 | 把研究卡写成策略触发或订单 |
| `ResearchReviewService` | 审核动作 CONFIRM/REJECT/DEFER/REQUEST_MORE/OVERRIDE，期望版本校验、覆盖范围受限 | 越过授权/身份/质量/风险硬门禁 |
| `CandidateHandoffService` | 候选择交接模块三（幂等键/回执/`correlationId`/`causationId`） | 创建策略信号、交易计划或 Paper 订单 |
| `ResearchQaService` | 意图识别后由 Spring Boot 查询结构化结果，交给 Dify 组织带证据的回答 | 让 Dify 直连数据库或绕过权限查询 |
| `ResearchScreeningJobHandler` | XXL-JOB 参数/幂等/审计，调用 `ScreeningRunner` | 在 Handler 内实现筛选或评分 |

已有 `quantpf-domain` Paper 事务、`PaperRiskGate`、审计 Repository 与模块一 `DifyResearchClient`/`AiDraftValidationService` 保持原职责；模块二通过端口/事件与其交接，不改其内部实现。

### 3. ID、日志字段与审计（继承模块一）

复用模块一 `quantpf.id.*`/`IdentifierGenerator`/`SnowflakeFacade`/`InjectionMetaObjectHandler` 约定，模块二新增实体一律 `Long/BIGINT` 主键、无自增、五列末尾日志字段（`create_by/create_time/update_by/update_time/remark`）自动填充；系统任务用 `ActorContext` 系统 actor 或 NULL。业务侧需主动发号的 `handoff_key`/`card_key`/`screening_key` 使用 Hutool `Snowflake`（或应用稳定 UUID+前缀），与 MP 生成器共用节点配置。DTO 不把数据库 ID 当权限凭证，服务端始终做 owner/scope 校验。错误码在 `quantpf-common.error` 扩展研究/候选域编码，进入 `contract/error-codes`。

### 4. MySQL Repository 与事务边界

- MyBatis Mapper/XML 只在 `persistence`；应用服务调用领域 Repository 端口；Controller/Job/Dify 不直取 Mapper。
- 主张升级/修订、研究链构建、筛选运行、候选评估、研究卡写、审核与交接的单个状态迁移用显式事务；候选筛选是批处理，不持大事务——先写 `screening_run`（运行态）+ 按批写 `candidate_assessment`，最后原子置运行态，失败逐批回滚/重试。
- 版本对象（claim/thesis/prediction/card）写入前校验 `current_flag` 与乐观锁/期望版本；修订只新增版本，不 UPDATE 覆盖历史行。
- 交接使用幂等键 + 唯一约束；重复交接返回原回执。Dify 调用记录、AI 草稿、研究链升级分别保存，草稿校验失败不得回滚原始采集事实。
- 硬门禁/评分结果只由确定性服务写；Dify 相关表只写 `dify_invocation`/`ai_research_draft`（模块一），不得写研究链/评估/研究卡主表。

### 5. 确定性研究计算 vs Dify 解释（硬边界）

| 能力 | Spring Boot（权威） | Dify（草稿/解释） |
|---|---|---|
| 基本面因子 / 收益质量 / 估值 | 确定性计算 + 标注口径 | 只解释 |
| 事件暴露 / 敏感度 | 确定性映射/计算，人工候选可采纳 | 只提映射候选，不可写 |
| 商品供需/基差/期限结构结论 | 确定性计算 | 只解释传导链 |
| 硬门禁 / 分项评分 / 筛选 | 确定性，权威 | 禁止 |
| 研究卡状态/方向/版本 | 权威 | 只生成普通语言说明草稿 |
| 问答 | 结构化查询 + 门禁 | 组织带证据的回答 |
| 复盘摘要 | 权威事实 | 只起草文字 |

Dify 的 `directionCandidate`/`normalizedSubjectHint`/`confidence`/自算因子只落在 `ai_research_draft`，不得进入 `verified_direction`、`research_score`、因子快照、机会状态或交接。

### 6. Dify API 调用约定（模块二增量）

复用模块一 `DifyResearchClient`/`DifyStreamConsumer`/`DifyCallbackVerifier`，不新建客户端。模块二新增研究型 Workflow（对应详设 2 §9.1 / 详设 5 §15.2），均走已发布版本 + 最小上下文：

| workflowKey | 用途 | 输出落点 | 确定性边界 |
|---|---|---|---|
| `NEWS_IMPACT_ANALYSIS` | 事件影响变量/传导链/受益受损/反方草稿 | `ai_research_draft`，校验后升级 `research_thesis` | 方向/敏感度由 Spring Boot 定 |
| `STOCK_FUNDAMENTAL_RESEARCH` | 股票研究解释 | 草稿，因子来自后端 | 因子值后端算 |
| `COMMODITY_SUPPLY_DEMAND` | 商品供需解释 | 草稿 | 结论后端算 |
| `SPOT_BASIS_TERM_STRUCTURE` | 现货/基差/期限结构解释 | 草稿 | 计算后端 |
| `PRICE_VOLUME_RESEARCH` | 量价状态解释 | 草稿 | 量价指标后端 |
| `RESEARCH_CARD_EXPLANATION` | 研究卡普通语言说明 | 说明草稿，只读已审卡 | 不写卡状态 |
| `RESEARCH_QA` | 受控研究问答 | 回答 + 引用，Spring Boot 先查询 | 不直连库 |
| `REVIEW_SUMMARY` | 复盘摘要草稿 | 草稿，交易事实后端维护 | 不改事实 |

请求/输出沿用模块一 minimal 规则：请求带 `research_task_id/material_refs/source_refs/subject_refs/snapshot_id/as_of_time/authorization_status/quality_status/research_question/research_period + workflow/prompt/schema 版本`；输出校验链路（Schema → 引用存在 → 引用支持主张 → 时间可见 → 授权用途 → 身份 → 冲突）后写入研究草稿。`RESEARCH_QA` 先由 `ResearchQaService` 查询授权范围内结构化结果，Dify 组织表达并引用证据，缺少候选时明确回答"当前没有满足门禁的机会"及原因。

### 7. 配置文件与环境变量

在模块一 `quantpf.*` 基础上新增（非敏感默认值进 `application.yml`，环境差别进 dev/prod，敏感值仅环境变量）：

```yaml
quantpf:
  research:
    scoring:
      rule-version: ${QUANTPF_RESEARCH_SCORING_RULE_VERSION:v1}
      weights:
        event-credibility: ${QUANTPF_SCORE_EVENT_CREDIBILITY:15}
        transmission-chain: ${QUANTPF_SCORE_TRANSMISSION_CHAIN:20}
        subject-quality: ${QUANTPF_SCORE_SUBJECT_QUALITY:20}
        valuation-expectation: ${QUANTPF_SCORE_VALUATION_EXPECTATION:15}
      observe-threshold: ${QUANTPF_SCORE_OBSERVE_THRESHOLD:70}
      plan-threshold: ${QUANTPF_SCORE_PLAN_THRESHOLD:80}
    screening:
      batch-size: ${QUANTPF_SCREENING_BATCH_SIZE:200}
      concurrency: ${QUANTPF_SCREENING_CONCURRENCY:2}
      default-period: ${QUANTPF_SCREENING_DEFAULT_PERIOD:MEDIUM}
    card-explain:
      workflow-key: ${QUANTPF_CARD_EXPLAIN_WORKFLOW_KEY:RESEARCH_CARD_EXPLANATION}
    qa:
      max-per-minute: ${QUANTPF_RESEARCH_QA_MAX_PER_MINUTE:10}
      max-context-bytes: ${QUANTPF_RESEARCH_QA_MAX_CONTEXT_BYTES:262144}
    handoff:
      retry-max: ${QUANTPF_HANDOFF_RETRY_MAX:3}
      retry-backoff-ms: ${QUANTPF_HANDOFF_RETRY_BACKOFF_MS:1000}
```

补充约定：

- `weights` 初始值对应总纲 18.3 的初始权重，属**待验证参数**，配置化不硬编码；`observe-threshold/plan-threshold` 只是"是否进入观察/是否可考虑 Paper 计划"的总分参考，且必须等模块三策略匹配 15、模块四执行 5 + 风险收益 10 合并后才能用于总门槛，模块二不得单方用它们放行。
- `weights`/`threshold` 变更新建评分版本并记录在 `candidate_assessment.score_version`，不得原地改配置影响历史评分回放。
- `DIFY_APP_API_KEY`、回调签名密钥不得写入 YAML/数据库/前端/日志；研究问答限流在服务端实现。

### 8. API/事件与用户状态映射

`ruoyi-admin` 服务端门面分组（对应 `research-candidate-db-design` 的 13 表）：

```text
/api/quant/research/tasks/*          # 研究任务/运行（接模块一）
/api/quant/research/claims/*        # 事实主张与核验
/api/quant/research/theses/*        # 研究假设/预测/反方
/api/quant/research/factors/*       # 基本面因子/事件暴露/商品结论
/api/quant/research/screening/*     # 筛选运行与规则版本
/api/quant/research/candidates/*    # 候选评估（查询/复核）
/api/quant/research/cards/*         # 研究卡版本与审核
/api/quant/research/reviews/*       # 人工审核记录
/api/quant/research/handoffs/*      # 候选交接与回执
/api/quant/research/qa/*            # 受控研究问答
/api/quant/research/dify/*          # Dify 回调/run/草稿查询（接模块一）
```

返回统一 `requestId/业务对象ID/状态/版本/用户可读摘要/下一步/可回查链接`；错误返回稳定错误码与可重试属性，不泄露 SQL、Key、Prompt 或堆栈。关键事件：

```text
ResearchClaimVerifiedEvent / ResearchClaimRevisedEvent
ResearchThesisBuiltEvent / ResearchThesisInvalidatedEvent
CandidateDiscoveredEvent / CandidateRejectedEvent / CandidateDeferredEvent
ResearchCardCreatedEvent / ResearchCardReviewedEvent
ResearchCandidateReadyEvent
CandidateHandoffCompletedEvent
```

事件经 Outbox 可靠发布、消费者幂等；`ResearchCandidateReadyEvent` 只表示研究候选可被评估，不表示策略触发、计划审批或 Paper 授权；`CandidateHandoffCompletedEvent` 只表示交接完成，不创建订单副作用。

### 9. 测试策略

- **架构测试**：ArchUnit 校验 Controller/Job/Dify adapter 不直取 Mapper、`quantpf-domain` 不依赖 Web/Dify/XXL-JOB/Paper execution、`quantpf-research` 不依赖 `quantpf-execution/finance/risk` 实现。
- **单元测试**：主张状态机、引用校验、失效传播、硬门禁优先、70 分评分（不能当总分）、研究卡版本/当前版本、审核版本冲突与覆盖范围、交接幂等、原因码与机会状态。
- **集成测试**：`research_task → draft → claim/evidence_link → thesis/prediction → factor/exposure → screening_run → candidate_assessment → research_card → review → handoff` 全链可回放、幂等；重复任务/交接不产生重复。
- **Dify 合同测试**：streaming 解析、Schema/引用错误、时间未来、授权阻断、重复回调、研究卡说明/问答降级；Dify 自报评分/因子被忽略。
- **用户路径集成**：普通用户最小输入建任务→看研究卡；审核用户确认/否决/覆盖；候选交接→模块三拒绝回执；无合格候选展示原因；Dify 失败确定性链路继续。
- **Paper 回归**：现有 Paper 下单/撤单/账务/风险/审计测试必须继续通过；模块二不得新增真实 Broker Bean 或改变 `PaperRiskGate` 语义。

## Risks / Trade-offs

- **[Risk] 与模块一研究入口服务职责重叠** → 只扩展任务/草稿之外的 claim/thesis/screening/card/handoff 服务，复用模块一 Dify 客户端与校验器；施工前盘点模块一 `quantpf-research` 已规划包，避免同名类冲突。
- **[Risk] 把 70 分研究分当 100 分总门槛** → `ResearchScorer` 只产四维子分并显式标注，`observe/plan-threshold` 命名与实现均注明"总分合并后使用"，单元测试断言不放行。
- **[Risk] Dify 自算因子/评分污染事实** → 只写 `ai_research_draft` 并走校验，确定性表无 Dify 写通道；合同测试断言被忽略。
- **[Risk] 候选批处理大事务/半程失败** → 先写运行态 + 分批写评估，最后原子置完成，失败逐批重试，不回写历史运行。
- **[Risk] 交接越过策略/风险直接下单** → 交接只依赖模块三端口 `CandidateHandoffSink`，不引用 `quantpf-execution/risk`；架构测试禁止依赖。
- **[Risk] 研究卡/主张被最新值覆盖失去审计** → 版本号 + `current_flag` + 乐观锁 + `previous_*_id` 全链保留，修订只新增。

## Migration Plan

1. 扩展 `quantpf-common` 研究/候选域错误码，复用 ID/Actor/时间/事件基建，补齐架构测试基线。
2. 在 `quantpf-domain` 新增 research/asset 领域对象与 Repository 端口，对齐 13 表与五日志字段。
3. 实现 `quantpf-research` 的 claim/thesis 服务与 MySQL Adapter，先用 Mock 完成草稿→主张→研究链→失效传播闭环。
4. 实现 asset 因子/暴露/商品研究确定性与 `screening`/`card` 服务，补评分分项与 70/100 边界测试。
5. 复用 Dify 客户端接入研究型 Workflow（Stub 合同测试 + 自托管 Dify 联调），实现研究卡说明与问答编排。
6. 实现 `handoff` 与模块三端口的交接契约、幂等与回执，`ruoyi-admin` 暴露 REST/回调门面并接 Sa-Token/审计/OpenAPI/事件/菜单。
7. `quantpf-job` Handler 迁移：筛选/复核/交接重试只调用应用服务，确认不恢复 SnailJob 双栈、不创建 Paper/真实交易路径。
8. 跑全链 + Paper 回归，更新技术分册落地状态；本 change 完成后由后续实现 change 执行代码。

回滚：模块二 Bean、评分/筛选/问答配置、任务和 Dify 研究型 Workflow 可独立禁用；确定性筛选不依赖 Dify；数据库回滚遵循 `research-candidate-db-design` 的 Flyway 规则，不删除已产生的历史主张/研究卡/交接。

## Open Questions

以下不改变本设计的分层、边界或行为契约，可在实现前作为配置登记：

- 四维评分各分项的打分公式与证据字段精确清单（随详设 2C / 代码施工固化）；
- 标的静态行业分类的权威归属与 `EventExposureMapper` 的行业码来源（模块一/二联调前确认）；
- 研究型 Workflow 的输入/输出 Schema 精确字段与模型版本（随 Dify 子详设）；
- 模块三 `CandidateHandoffSink` 端口的最终方法签名与回执 Schema（随模块三详设）；
- 研究问答与筛选的容量阈值（压测后定稿）。