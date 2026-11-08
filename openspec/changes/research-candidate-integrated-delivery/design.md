# Design

## Context

本设计整合并核查三个来源 change：

- `research-candidate-db-design`：模块二 13 张 MySQL 表、雪花 ID 与五日志字段、研究 70 分制边界、Dify 交互矩阵；
- `research-candidate-code-guidance`：Spring Boot 分层、claim/thesis/screening/card/handoff 服务职责、研究型 Dify 调用与配置；
- `research-candidate-ui-guidance`：plus-ui 候选池/研究卡/事实核验/资产研究/审核/交接页面、角色旅程与 Dify 触发入口。

核查基线是总纲 V4.5、技术分册、详设 0、详设 2 与实际源码库。仓库为两个源码目录：后端 `D:\workspace\quant\RuoYi-Vue-Plus`，前端 `D:\workspace\quant\plus-ui`。后端现有 Paper 核心在 `quantpf-domain/execution/finance/risk`，`quantpf-research`/`quantpf-strategy` 仍为骨架；前端 plus-ui 的 `src/views` 尚无 `quant/` 目录。菜单事实源为 `RuoYi-Vue-Plus/contract/menu/quantpf-menu-facts.yaml`。

### 审查结果与统一修正

1. **边界一致**：模块二复用模块一 9 张研究入口表 + 新增 13 张专属表、Spring Boot 事实源、Dify 草稿角色、研究 70 分制、一期日线及真实交易禁区，与总纲 8/11/12/18/25 章、详设 0 三条流、详设 2 §13–18 一致。
2. **关键路径一致**：普通用户低输入研究→研究卡、审核用户核验/覆盖、候选交接模块三、研究型 Dify 触发，与详设 0 三流、详设 2 研究链/候选/研究卡/交接一致。
3. **70 分制统一**：`research_score`/`dimension_scores` 只承载模块二四维（事件可信度与新颖性 15、行业传导链 20、标的质量/供需质量 20、估值/预期差 15，合计 70 分）；策略匹配 15、执行流动性 5、风险收益 10 属模块三/四；70/80 总门槛须总分合并后评估，模块二不得单方放行。
4. **路径表述统一（修正用户模板与实际仓库偏差）**：
   - 数据库迁移在 `RuoYi-Vue-Plus/db/migration/`（Flyway 权威），非 `[后端]/sql/`；幂等种子放 `script/sql/`；
   - 实体在 `quantpf-modules/quantpf-domain/src/main/java/com/quantpf/domain/{research,asset}/`，非 `[后端]/domain/`；
   - Controller 在 `ruoyi-admin/src/main/java/com/quantpf/web/controller/`，非 `[后端]/controller/`；
   - 服务在 `quantpf-modules/quantpf-research/src/main/java/com/quantpf/research/`（含 `dify/`），非 `[后端]/service/`；
   - 前端 API 为 `plus-ui/src/api/quant/*.ts`（TypeScript，非 `.js`），页面为 `plus-ui/src/views/quant/research/*.vue`。
5. **数据库规范统一**：所有模块二 MySQL 业务表主键 `BIGINT` 应用生成、禁 `AUTO_INCREMENT`，MyBatis-Plus `IdentifierGenerator` 与 Hutool Snowflake 共享 worker/datacenter；列尾固定五日志字段；版本对象禁用 `(object_id,current_flag)` 直接唯一键。
6. **Dify 调用统一**：复用模块一 `DifyResearchClient/DifyStreamConsumer/DifyCallbackVerifier`，新增 8 个研究型 Workflow；确定性因子/评分/筛选由 Spring Boot 计算，Dify 只解释。
7. **原表详表以 `research-candidate-db-design/design.md` 为字段/索引权威**：本整合文档只做表域摘要与引用，不回写全量字段清单；不一致时以 db-design 为表级权威并回写本 change 映射。

## Goals / Non-Goals

**Goals:**

- 形成模块二单一整合落地设计，含数据库详细设计、用户操作界面说明、代码仓库落地指导三章。
- 把 13 表映射到后端实体/服务、API/事件、前端页面与验收场景。
- 明确 Dify 在何处触发、输入/输出、状态回显、失败恢复与确定性边界。
- 用指定顺序生成 tasks，任务描述明确前后端真实源码根下的相对路径与验证动作。
- 保持总纲/详设规定的低输入、点时回放、候选≠信号、Paper 隔离、一期/二期边界。

**Non-Goals:**

- 不修改/重写三个已存在 change；以本 change 为综合审查与实施入口，来源 change 保留历史。
- 不创建 Java、Vue、SQL、Dify Workflow 或生产配置实现。
- 不把 `[后端]/sql/`、`.js` 等虚构成既有约定；实际按 `db/migration/`、`.ts` 落地。
- 不实现真实供应商调用、LIVE/QMT/CTP、分钟/Tick UI、自动实盘或 Dify 直连浏览器。

## Decisions

## 核心章节一：数据库详细设计

### 1. 存储与职责边界

```text
MySQL：模块二 13 张研究/候选表 + 复用模块一 9 张研究入口表 + 引用标的/行情/因子/点时表
DuckDB/RRedis/对象存储：本模块不新增表；因子/结论/评分/研究卡/审核/交接均为 MySQL 业务分析数据
Dify 自有存储：仅 Dify Workflow 运行所需内部数据，不作为 QuantPF 事实库
```

### 2. 既有底座与模块二表清单

**若依既有表，只复用不复制**：`sys_user`（审核人/操作者）、`sys_role`、`sys_menu`（权限）、`sys_dict_type/sys_dict_data`（原因码/状态/证据/审核/交接字典）、`sys_config`（非敏感开关）、`sys_message/sys_notice`（站内通知）。Dify 不得访问。

**复用模块一 9 张研究入口表（不重建）**：

| 模块一表 | 模块二引用字段 |
|---|---|
| `research_material` / `research_material_relation` | `material_id` |
| `research_task` / `research_task_run` | `task_id` / `task_run_id` |
| `dify_workflow_registry` / `dify_invocation` | `invocation_id` 关联草稿 |
| `ai_research_draft` | `draft_id` |
| `research_event` / `event_evidence` | `event_id` / `event_evidence_id` |

**新增 13 张模块二表（字段/约束/索引逐表见 `research-candidate-db-design/design.md`）**：

| 子域 | 表 | Dify 边界 |
|---|---|---|
| 事实核验 | `research_claim`、`research_evidence_link` | Dify 输出事实候选先入草稿，Spring Boot 校验后升级；不可直写 |
| 研究链 | `research_thesis`、`research_prediction`、`research_counter_evidence` | 传导链/预测/反方草稿入草稿；统计字段后端算 |
| 资产研究 | `fundamental_factor_snapshot`、`event_exposure`、`commodity_research_snapshot` | 确定性计算；Dify 只解释，不可补造数值 |
| 候选筛选 | `screening_run`、`candidate_assessment`、`research_card` | 硬门禁/评分/筛选由 Spring Boot；Dify 只产研究卡说明草稿 |
| 审核交接 | `research_review`、`candidate_handoff` | 无 Dify 交互 |

### 3. 全库字段与约束规范

- MySQL 业务表主键 `id BIGINT NOT NULL PRIMARY KEY`（无 `AUTO_INCREMENT`）；关联 ID `BIGINT` 同号型；跨模块引用（模块三/四）为逻辑外键，不建物理 FK。
- 列末五日志字段固定：`create_by BIGINT NULL`、`create_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`、`update_by BIGINT NULL`、`update_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)`、`remark VARCHAR(500) NULL`。
- 版本对象以 `(thesis_key,version_no)`/`(card_key,card_version_no)` 唯一；单一当前版本用 `current_key` 生成列 + 过滤唯一索引，不用 `(object_id,current_flag)` 直接唯一键；禁止 `ORDER BY id` 推断最新。
- 研究评分 `research_score DECIMAL(6,2)` + `dimension_scores JSON` 只承载 70 分制；`hard_gate_results JSON` 独立，评分不得补偿硬门禁。
- 写命令幂等、审计、`expectedVersion`（审核/交接/版本变更）；Dify 草稿、核验事实/假设、研究卡、交接分层。

### 4. Dify 数据流（模块二）

```text
Spring Boot 查询授权/质量/身份/点时
→ 冻结最小输入上下文（task/material/event/snapshot/引用）
→ POST /v1/workflows/run (streaming)
→ 服务端 WebClient 消费 text/event-stream，保存 invocation/原始响应
→ Schema/引用/时间/授权/身份/冲突校验
→ 只写 ai_research_draft（模块一表）
→ 通过后由 Spring Boot 升级 claim/thesis/research_card 等主表
→ 发布研究链/候选/研究卡/交接事件
```

确定性因子、事件暴露、商品结论、硬门禁、评分、筛选由 Spring Boot 计算；Dify 的 `directionCandidate`/自算评分/因子只落草稿，不写主表。研究型 Workflow 均需已发布版本 + 最小上下文。

## 核心章节二：用户操作界面说明

### 1. 菜单和页面结构

复用 `contract/menu/quantpf-menu-facts.yaml` 的 Research 菜单，在其下扩展模块二子页面（承接模块一 `task/material/draft/event/assistant`）：

```text
Research/研究中心
  快捷研究、研究任务、AI草稿、研究事件、全局研究助手（模块一已有）
  候选池 / 研究决策卡 / 事实核验 / 资产研究 / 候选交接（模块二新增）
```

全局状态条显示用户 scope/模式、市场本地日期与时区、最新日线新鲜度、当前候选/研究卡版本、Paper 前置/不连接真实交易提示、Dify 降级标记。引导/审核/专业三模式只控信息披露，后端权限另行校验。

### 2. 用户关键流程

**普通用户研究到研究卡**：快捷研究/一句话问题 → 核心输入 ≤3 项 → 系统自动准备对象/来源/时点/日线 → 确认提交 → 查看事实/解释/预测分层与生成的研究卡 → 查看候选或加入关注。退出后 GuidedTask 可恢复。

**审核用户核验与覆盖**：待办/研究卡 → 查看事实主张/证据链/版本 → 确认/否决/延期/请求补充/受限覆盖并填理由 → 服务端 expectedVersion 复核 → 交接或退回。普通用户只看结论与下一步。

**候选交接**：审核通过候选 → 交接模块三 → 查看回执（接受/拒绝/要求补充）→ 拒绝则回到待补充。页面明确"已交策略评估，后续由模块三/四执行"，不显示下单。

**Paper/策略前置**：研究卡 → 查看数据日期、身份、质量、机会状态 → 不合格显示"暂不能生成模拟建议"及原因；合格仅交接策略评估，模块二不建订单。

### 3. Dify 触发入口与展现

不 iframe、不让浏览器直连，入口均经 Spring Boot：

| 前端位置 | 用户动作 | Workflow/结果 |
|---|---|---|
| 研究任务向导 | "开始 AI 研究" | 事件影响/股票/商品/量价研究草稿 |
| 新闻/材料详情 | "分析影响" | `NEWS_IMPACT_ANALYSIS` 草稿 |
| 资产研究/标的详情 | "用当前证据解释" | 股票/商品/量价/现货解释草稿 |
| 研究卡页 | "用当前证据解释" | `RESEARCH_CARD_EXPLANATION` 说明，只读已审卡 |
| 全局研究助手 | 自然语言问答 | `RESEARCH_QA`（Spring Boot 先查询） |
| 复盘入口 | "生成复盘摘要" | `REVIEW_SUMMARY` 文字草稿 |

Dify 输出标签固定"AI 草稿/待核验/已核验事实/未知"；超时、限流、非法输出有降级状态与"稍后重试/查看结构化数据"。

## 核心章节三：代码仓库落地指导

### 1. 代码根与目录结构

**后端根** `D:\workspace\quant\RuoYi-Vue-Plus`：

```text
quantpf-modules/quantpf-domain/src/main/java/com/quantpf/domain/
    research/   # Claim/EvidenceLink/Thesis/Prediction/CounterEvidence/ScreeningRun/
                #   CandidateAssessment/ResearchCard/ResearchReview/CandidateHandoff
    asset/      # FundamentalFactorSnapshot/EventExposure/CommodityResearchSnapshot
    repository/ # 研究/资产域 Repository 端口
    persistence/# 13 张表 Record/Mapper/XML
quantpf-modules/quantpf-research/src/main/java/com/quantpf/research/
    application/ claim/ thesis/ asset/ screening/ card/ handoff/ qa/ dify/ validation/ adapter/mysql/ event/
quantpf-modules/quantpf-strategy/src/main/java/com/quantpf/strategy/   # CandidateHandoffSink 端口，不写实现
quantpf-modules/quantpf-job/src/main/java/com/quantpf/job/handler/     # 筛选/复核/交接重试 Handler
ruoyi-admin/src/main/java/com/quantpf/web/{controller,dto,config}/
ruoyi-admin/src/main/resources/application*.yml
contract/{openapi,events,error-codes,menu}/
db/migration/                 # Flyway 权威迁移
script/sql/                   # 幂等字典/菜单种子
```

**前端根** `D:\workspace\quant\plus-ui`：

```text
src/api/quant/{researchCandidate,researchCard,researchClaim,researchFactor,researchReview,handoff,researchQa,types}.ts
src/views/quant/research/{candidate,card,claim,thesis,factor,handoff}/...
src/views/quant/components/{ResearchCardStatus,CandidateScorePanel,ClaimEvidenceDrawer,MultiHorizonView}.vue
```

真实路由/API 按 RuoYi 动态菜单、`contract/menu`、OpenAPI 契约落地。

### 2. 关键类/接口职责与依赖

| 类/接口 | 建议模块 | 职责/边界 |
|---|---|---|
| `ClaimVerificationService` / `EvidenceLinkService` | research | 草稿→主张升级、证据关联、状态机、版本前驱 |
| `ResearchChainService` | research | 假设/预测/反方构建与失效传播 |
| `FundamentalFactorCalculator` / `EventExposureMapper` / `CommodityResearchService` | research | 确定性因子/暴露/商品结论，`DISCLOSED/CALCULATED/MODELLED/INFERRED` |
| `ScreeningRunner` / `CandidateAssessor` / `ResearchScorer` | research | 硬门禁→70 分制评分→候选阶段/原因码 |
| `ResearchCardService` | research | 研究卡聚合/版本/当前版本/多周期预期 |
| `ResearchReviewService` | research | 审核动作、版本校验、受限覆盖 |
| `CandidateHandoffService` | research | 交接模块三、幂等回执、correlation/causation |
| `ResearchQaService` | research | Spring Boot 查询 + Dify 组织回答 |
| `DifyResearchClient`（复用） | research/dify | Workflow API、Bearer、SSE、invocation 留痕 |
| `ResearchScreeningJobHandler` | job | 仅调用应用服务、调度参数幂等与审计 |
| `ResearchCandidateController` 等 | ruoyi-admin | Sa-Token/scope、DTO、应用服务；不直取 Mapper |

依赖方向：`Controller/Job → Application Service → Domain Repository Port → Adapter/Mapper`；`quantpf-domain` 不依赖 Web/Dify/Job/Paper execution；`quantpf-research` 只依赖 `quantpf-marketdata/strategy` 端口，不依赖 `quantpf-execution/finance/risk` 实现。

### 3. Dify API 调用约定（研究型）

复用模块一 `POST {base-url}/v1/workflows/run`、`Authorization: Bearer ${DIFY_APP_API_KEY}`、`response_mode=streaming`。研究型 Workflow 8 个（对应详设 2 §9.1 / 详设 5 §15.2）：

```text
NEWS_IMPACT_ANALYSIS / STOCK_FUNDAMENTAL_RESEARCH / COMMODITY_SUPPLY_DEMAND
SPOT_BASIS_TERM_STRUCTURE / PRICE_VOLUME_RESEARCH / RESEARCH_CARD_EXPLANATION
RESEARCH_QA / REVIEW_SUMMARY
```

请求最小上下文：`research_task_id/material_refs/source_refs/subject_refs/snapshot_id/as_of_time/authorization_status/quality_status/research_question/research_period + workflow/prompt/schema 版本`。输出校验链：Schema→引用存在→引用支持→时间可见→授权→身份→冲突。确定性指标/评分/筛选不交 Dify；Dify 自算值只入草稿。前端只轮询 Spring Boot REST。

### 4. 配置文件新增项

在模块一 `quantpf.*` 基础上新增（敏感值仅环境变量）：

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

`weights/threshold` 为待验证参数，配置化不硬编码；变更新建 `score_version`，不改历史评分。`observe/plan-threshold` 只作总分合并后的参考，模块二不单方放行。`DIFY_APP_API_KEY` 不进 git/数据库/前端/日志。

### 5. 用户旅程 API/菜单映射

菜单以 `contract/menu/quantpf-menu-facts.yaml` 为事实源，扩展 Research 子页面与 `quant:research:*` 权限。核心 API：

```text
/api/quant/research/claims/*    /theses/* /factors/* /screening/*
/api/quant/research/candidates/* /cards/* /reviews/* /handoffs/* /qa/* /dify/*
```

写 API 带 `requestId`、`Idempotency-Key`、`expectedVersion`（状态/审核）、`reason`；返回对象 ID、状态/版本、用户可读摘要、下一步与回查链接。

## Risks / Trade-offs

- **[Risk] 三个来源 change 文档漂移** → 本 change 为综合实施入口；表以 db-design 为准、目录/类/配置以 code-guidance 为准、页面/旅程以 ui-guidance 为准；任务回写来源文档。
- **[Risk] 用户模板 `/sql/`、`.js` 与实际 Flyway/`.ts` 不符** → 以实际 `db/migration/`、`.ts` 为权威，`script/sql/` 只放幂等种子。
- **[Risk] 把研究 70 分当总分放行** → ResearchScorer 只产四维子分并标注；`observe/plan-threshold` 注明"总分合并后使用"，单测/评审断言不放行。
- **[Risk] Dify 自算因子/评分污染事实** → 只写 `ai_research_draft` 走校验，确定性表无 Dify 写通道；合同测试断言被忽略。
- **[Risk] 交接越过策略/风险直接下单** → 交接只依赖模块三 `CandidateHandoffSink` 端口；架构测试禁止 `quantpf-research` 依赖 execution/risk。
- **[Risk] 后端 JDK21 / 前端 pnpm 环境差异** → 联调先核对 Maven Wrapper 与 `package.json` scripts，记录真实命令与诊断，不把模板命令当成功事实。

## Migration Plan

1. 先审查 db-design/code-guidance/ui-guidance 的表/路径/权限/Workflow/状态词，冻结差异表。
2. 按 tasks 指定顺序完成数据库迁移、后端 Domain/实体、接口、Dify、前端页面、前端 API。
3. 迁移 DDL 在 `RuoYi-Vue-Plus/db/migration/`，历史迁移不回写；种子放 `script/sql/`。
4. 后端启动前用 JDK21 验证 Maven Wrapper 与 `DromaraApplication`；前端启动前核对 `plus-ui/package.json` 的 pnpm scripts。
5. 联调先用 Mock 来源与 Stub/自托管 Dify，验证角色路径、SSE/降级、数据资格、重复提交、交接幂等、Paper 前置与既有 Paper 回归。
6. 回滚：菜单/路由按权限与 Feature Flag 关闭；Dify 入口关闭不影响确定性候选/研究卡页；后端模块可配置禁用；数据库回滚遵循 db-design 的兼容/备份计划。

## Open Questions

以下只作实施前配置，不改变本设计架构与用户路径：

- 各用户角色最终权限码是否与部署组织已有角色名映射；
- 前端本地开发 API 代理目标端口与 dev profile；
- 四维评分分项精确打分公式（随详设 2C 固化）；
- 研究型 Workflow 的正式 Key/版本与模型供应商配置（dev/prod 分离）。