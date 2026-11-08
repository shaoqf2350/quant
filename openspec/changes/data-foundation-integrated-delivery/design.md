# Design

## Context

本设计整合并核查三个来源 change：

- `data-foundation-db-design`：MySQL/DuckDB 表、点时/授权、Dify 持久化、雪花 ID 与日志字段；
- `data-foundation-code-guidance`：Spring Boot 分层、类职责、Dify 调用和配置；
- `data-foundation-ui-guidance`：plus-ui 页面结构、角色旅程和 Dify 触发入口。

核查基线是总纲 V4.5、技术分册、详设 0 V1.4、详设 1 V1.5 以及实际源码库。仓库当前为两个源码目录：后端 `D:\workspace\quant\RuoYi-Vue-Plus`，前端 `D:\workspace\quant\plus-ui`。后端现有 Paper 核心位于 `quantpf-domain/execution/finance/risk`，`quantpf-marketdata/research/common/job` 多数是骨架；前端已采用 plus-ui 的 Vue3/Element Plus 动态菜单体系，但 QuantPF 模块一页面仍待实现。现有 MyBatis-Plus `ASSIGN_ID` 与 `DefaultIdentifierGenerator`、`InjectionMetaObjectHandler` 已存在；菜单事实源为 `RuoYi-Vue-Plus/contract/menu/quantpf-menu-facts.yaml`。

### 审查结果与统一修正

1. **边界一致**：数据库方案的 MySQL 业务主库、DuckDB 仅 K 线/Tick、Spring Boot 事实源、Dify 草稿角色、一期日线及真实交易禁区，与总纲 ADR-9~12、详设 0/1 一致。
2. **关键路径一致**：管理员数据源治理、审核用户冲突处理、普通用户低输入研究、Dify 草稿校验、Paper 前置资格，与详设 0 的三条流和详设 1 的用途资格/点时模型一致。
3. **需从来源文档调整为一体化任务顺序**：已有 `data-foundation-db-design/tasks.md` 和 `data-foundation-code-guidance/tasks.md` 将实体/迁移先后表达为不同组；本提案统一为用户要求的阶段顺序：数据库→后端实体→后端接口→Dify 集成→前端页面→前端接口→联调验证。各阶段内允许按依赖拆成细任务，不改变这七个阶段的顺序。
4. **路径表述统一**：数据库迁移位置按实际后端仓 `RuoYi-Vue-Plus/db/migration/` 和现有 Flyway 约定，不采用不存在的 `[后端]/sql/` 作为迁移权威位置；如某脚本只是种子或工具脚本，则放 `RuoYi-Vue-Plus/script/sql/`。实体类按实际 `quantpf-modules/quantpf-domain/src/main/java/com/quantpf/domain/`，Controller 按 `ruoyi-admin/src/main/java/com/quantpf/web/controller/`；前端按 `plus-ui/src/views/quant/` 与 `plus-ui/src/api/quant/`。
5. **数据库规范统一**：所有 QuantPF MySQL 业务表主键为应用生成有符号 `BIGINT`，不使用自增；MyBatis-Plus `IdentifierGenerator` 与业务手动发号 Hutool Snowflake 共用 worker/datacenter；五个日志字段固定列尾。若依 `sys_*` 表、Flyway 元数据、schema marker/version 元数据属于底座/迁移元数据，不按 QuantPF 业务表重复改造。
6. **Dify 调用统一**：自托管 Dify v1.16.1，Workflow API `POST /v1/workflows/run`，`Authorization: Bearer` 服务端注入，默认 WebClient 消费 `text/event-stream`；可选 HMAC 回调只入草稿校验队列，不能直写研究事件。
7. **XXL-JOB 唯一调度**：采集与补偿任务由 Spring Boot 应用服务提供，XXL-JOB Handler 只调用应用服务；不恢复 SnailJob 双栈、不在 Handler 复制业务规则。
8. **原数据库设计表数与行细节原则**：`data-foundation-db-design/design.md` 是数据库字段/索引详表来源；本整合文档摘要关键表域和引用，不再复制一份可能漂移的全量字段清单。若后续发现数据库详表与本设计不一致，以数据库设计 change 为表级权威，同时必须回写本整合 change 的映射和任务。

## Goals / Non-Goals

**Goals:**

- 形成模块一单一整合落地设计，必须含数据库详细设计、用户操作界面说明、代码仓库落地指导三章。
- 把数据库表域映射到后端 Repository/Service、API/事件、前端页面与验收场景。
- 明确 Dify 在何处触发、输入/输出、状态回显、失败恢复和权限边界。
- 用指定 1–7 顺序生成 tasks，任务描述明确前后端绝对源码根下的目标相对路径和验证动作。
- 保持总纲/详设规定的用户友好、数据授权、点时可回放、Paper 隔离、一期/二期边界。

**Non-Goals:**

- 不在本 change 修改/重写三个已存在 change；以本 change 作为综合审查和实施入口，来源 change 保留历史。
- 不在此创建 Java、Vue、SQL、Dify Workflow、Docker 或生产配置实现。
- 不把 `[后端]/sql/` 虚构成既有 Flyway 根；实际建表迁移按 `db/migration/`，初始化种子可按脚本职责放 `script/sql/`。
- 不实现真实行情供应商调用、LIVE/QMT/CTP、分钟/Tick UI、自动实盘或 Dify 直连浏览器。

## Decisions

## 核心章节一：数据库详细设计

### 1. 存储与职责边界

```text
MySQL：供应商/来源/采集/快照/质量/点时/用途资格/标的/日历/盘口摘要/因子/研究任务/Dify草稿/研究事件/证据
DuckDB：K线（30m/日/周/月）与Tick；一期业务主运行日线
Redis：缓存/锁/短期状态，非事实主库
对象存储(sys_oss)：超阈值原始正文/附件；MySQL 保留引用和哈希
Dify 自有存储：仅 Dify Workflow 运行所需内部数据，不作为 QuantPF 事实库
```

### 2. 既有底座与 QuantPF 表分类

**若依既有表，复用不复制**：`sys_user/sys_role/sys_menu/sys_user_role`（身份/授权）、`sys_dict_type/sys_dict_data`（展示字典）、`sys_config`（仅非敏感配置）、`sys_oper_log`（通用操作日志）、`sys_notice/sys_message`（站内通知）、`sys_oss/sys_oss_config`（大对象引用）。Dify 不访问这些表。

**现有 QuantPF 表**：

| 表 | 本 change 的处理 |
|---|---|
| `quantpf_instrument` | 增加市场/时区/类型/来源/当前版本字段，增加版本表，按统一 BIGINT/日志规范兼容迁移 |
| `quantpf_trading_calendar` | 旧模型回填至分层日历模型后冻结只读 |
| `quantpf_market_bar` | 回填可用历史数据到 DuckDB 后冻结只读 |
| `quantpf_research_news` | 增加可见/生效/修订/来源/质量/授权/快照引用；接入统一材料对象 |
| `quantpf_financial_statement` | 增加发布时间/可见时间/修订/质量/授权/快照字段 |
| 交易/风控/账户/订单/审计表 | 不改变其业务职责；涉及主键/列规范演进须通过显式兼容迁移并先验证全部 FK |

所有现有 QuantPF MySQL 业务表兼容迁移范围和五列规范见 `data-foundation-db-design/design.md` §3/§14；本 change tasks 将按用户指定阶段纳入核查，不假设只改模块一三张表。

**新增 MySQL 表域**（字段类型、约束和索引按数据库详细设计逐表执行）：

| 子域 | 表清单摘要 | Dify 边界 |
|---|---|---|
| 数据源治理 | vendor、product、interface、field、data_source、source_group、source_group_member | Dify不读凭据/合同；只收授权后的来源引用/状态摘要 |
| 采集与质量 | collection_task/attempt、raw_snapshot、observation、quality_issue、point_in_time、use_eligibility | Dify不能采集或决定用途资格；任务和快照由 Spring Boot 管理 |
| 标的/日历 | instrument_version、future_product、instrument_relation、calendar_def/day/session/binding、readiness、calendar_source_snapshot/conflict、corporate_action、contract_event、fx_session_rule | Dify只解释身份/日历结论，不改身份/开闭市/交易资格 |
| 行情衍生 | market_quote_snapshot、market_state、capacity_assessment、quant_feature_snapshot、supply_demand_observation、basis_term_structure、spot_quote | 只按授权将最小结构化摘要给 Dify；Dify不能写指标/状态/因子 |
| 研究入口 | research_material/relation、research_task/task_run、dify_workflow_registry/invocation、ai_research_draft、research_event、event_evidence | Dify只生成草稿；Spring Boot校验并写草稿/核验事件 |

**DuckDB 表**：`qpf_schema_version`、`qpf_kline_bar`、`qpf_tick_data`。不能存盘口摘要、研究/质量/风险/账户事实，不用 MySQL Flyway 管理 DuckDB schema。

### 3. 全库字段与约束规范

- MySQL QuantPF 业务表：`id BIGINT NOT NULL PRIMARY KEY`，应用生成，禁止 `AUTO_INCREMENT`；关联 ID 均 `BIGINT` 同号型。
- MP 实体 ID：现有 `ASSIGN_ID`/`IdentifierGenerator`；业务需要预发号时使用 Hutool Snowflake；两者共享 `workerId/datacenterId`，启动时校验实例唯一性。
- 每个 MySQL 业务表列定义最后五列严格为：

```sql
create_by   BIGINT NULL,
create_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
update_by   BIGINT NULL,
update_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                            ON UPDATE CURRENT_TIMESTAMP(6),
remark      VARCHAR(500) NULL
```

- 历史表不回写历史 Flyway；新迁移先盘点 FK/UK 与停写/备份方案，再追加并回填五列、迁移父子 BIGINT、重建约束、验证后移除自增。
- 版本与点时用版本号、生效区间、`available_at`、快照 ID；唯一当前版本使用过滤唯一策略，不对 `(object_id,current_flag)` 直接做唯一约束。
- 所有写命令幂等、审计、expectedVersion（适用时）；Dify 草稿、核验事实、研究事件分层。

### 4. Dify 数据流

```text
Spring Boot 查询来源/授权/质量/身份/点时
→ 冻结最小输入上下文
→ POST /v1/workflows/run (streaming)
→ 服务端 WebClient 消费 text/event-stream
→ 保存 invocation 与原始响应 hash
→ 校验 Schema/引用/时间/授权/身份/冲突
→ 只写 AiResearchDraft
→ 审核/规则确认后由 Spring Boot 写 ResearchEvent/Evidence
→ 发布 ResearchEventPublishedEvent
```

API Key 服务端 Bearer 注入；callback_url + HMAC 仅在部署实测可用时启用，回调只入草稿校验队列。Dify 无数据库、Redis、Paper、订单或风险写权限。

## 核心章节二：用户操作界面说明

### 1. 菜单和页面结构

复用 `contract/menu/quantpf-menu-facts.yaml` 的 Dashboard、Research、Market、Audit 权威菜单，不新增第二套菜单事实源。plus-ui 建议页面：

```text
Dashboard/工作台
  数据健康、今日数据日期、待办、Dify/调度状态、Paper前置摘要
Market/数据与标的
  数据源与来源组、数据质量、快照与版本、标的中心、交易日历/时段/冲突、日线行情
Research/研究中心
  快捷研究、研究任务、材料详情、AI草稿、证据链、研究事件、全局研究助手
Audit/审计
  数据采集/资格变化/审核/Dify调用状态时间线（按权限）
```

全局状态条显示用户 scope/模式、市场本地日期与时区、最新日线日期、数据/Dify/任务状态、模拟资金隔离提示。引导/审核/专业三种模式只控制信息披露，后端权限另行校验。

### 2. 用户关键流程

**管理员数据治理**：工作台数据问题 → 来源/来源组 → 查看授权/字段/健康 → 连接测试/采集或补偿 → 质量问题 → 重新计算资格 → 时间线留痕。Token 永不回显。

**审核用户处理冲突**：待办/冲突 → 对比来源、时间、版本、影响范围 → 确认/拒绝/延期/要求补证据并填写理由 → 服务端按 expectedVersion 复核 → 资格重算/通知。普通用户只看原因和下一步。

**普通用户研究到候选**：快捷研究/一句话问题 → 最多 3 项核心输入 → 系统自动准备来源、标的、时点、日线 → 用户确认提交 → 查看任务状态、结论/原因/风险/下一步 → 加入观察或等待。退出后 GuidedTask 可恢复。

**数据到 Paper 前置**：标的/研究卡 → 查看日历、数据新鲜度、身份、用途资格、容量、研究状态 → 不合格显示“暂不能生成模拟建议”与原因；合格仅进入模块三策略适配，不由模块一直接建订单。

### 3. Dify 触发入口与展现

Dify 不 iframe、不让浏览器直连。入口：

| 前端位置 | 用户动作 | Workflow/结果 |
|---|---|---|
| 研究任务向导 | “开始 AI 研究” | 创建任务，展示事实/影响草稿与状态 |
| 新闻/材料详情 | “提取事实”“分析影响” | 草稿，引用和时间待校验 |
| 标的/日线详情 | “解释当前状态” | 仅解释服务端结构化结果，不改状态 |
| 全局助手抽屉 | 自然语言问答 | Spring Boot 先查授权数据，Dify组织回答并引用 |
| 已发布研究事件 | “补充反方问题” | 建立新研究任务/版本，不覆盖旧事件 |

Dify 输出标签固定为“AI草稿/待核验/已核验事实/已发布研究事件/未知”；服务超时、限流、非法输出均有用户可读降级状态和“查看结构化数据/稍后重试”入口。

## 核心章节三：代码仓库落地指导

### 1. 代码根与目录结构

**后端根** `D:\workspace\quant\RuoYi-Vue-Plus`：

```text
quantpf-modules/quantpf-common/src/main/java/com/quantpf/common/{config,id,audit,time,event,error}/
quantpf-modules/quantpf-domain/src/main/java/com/quantpf/domain/{data,instrument,calendar,market,research,repository,persistence}/
quantpf-modules/quantpf-marketdata/src/main/java/com/quantpf/marketdata/{application,adapter/source,adapter/mysql,adapter/duckdb,quality,event}/
quantpf-modules/quantpf-research/src/main/java/com/quantpf/research/{application,dify,validation,adapter/mysql,event}/
quantpf-modules/quantpf-job/src/main/java/com/quantpf/job/{handler,command,audit}/
ruoyi-admin/src/main/java/com/quantpf/web/{controller,dto,config}/
ruoyi-admin/src/main/resources/application*.yml
contract/{openapi,events,error-codes,menu}/
db/migration/                         # Flyway 权威迁移目录
script/sql/                           # 幂等种子/初始化脚本，不替代 Flyway
```

**前端根** `D:\workspace\quant\plus-ui`：

```text
src/api/quant/{dataSource,dataQuality,snapshot,instrument,calendar,market,researchTask,dify,researchEvent}.ts
src/views/quant/dashboard/
src/views/quant/data/{source,quality,snapshot,readiness}/
src/views/quant/instrument/
src/views/quant/calendar/{index,session,conflict}/
src/views/quant/market/
src/views/quant/research/{task,material,draft,event,assistant}/
src/views/quant/components/{StatusSummaryCard,DataFreshnessBadge,EvidenceDrawer,VersionTimeline,GuidedTaskStepper,DifyRunStatus}.vue
```

上述为新增结构建议；真实路由和 API 按 RuoYi 动态菜单、`contract/menu`、OpenAPI 契约落地。

### 2. 关键类/接口职责与依赖

| 类/接口 | 建议模块 | 职责/边界 |
|---|---|---|
| `QuantPfProperties` / `WorkerNodeProperties` | common | 配置绑定、worker/datacenter 唯一性校验 |
| `QuantPfIdentifierGenerator` | common/admin wiring | MP `IdentifierGenerator`，复用统一雪花节点 |
| `SnowflakeFacade` | common | 业务侧预发号，封装 Hutool Snowflake，共享节点参数 |
| `QuantPfMetaObjectHandler` | common/admin wiring | 填充五个末尾日志字段，保留人工/系统 actor |
| `DataSourceApplicationService` | marketdata | 数据源/来源组配置与健康状态，权限/幂等/审计 |
| `CollectionApplicationService` / `SourceAdapter` | marketdata | 采集任务、Mock/供应商 adapter、快照与结果 |
| `RawSnapshotService` / `DataQualityService` | marketdata | 哈希去重、脱敏、点时、质量与用途资格 |
| `InstrumentApplicationService` / `TradingCalendarService` | marketdata | 身份、版本、日历、时段、就绪和冲突 |
| `DuckDbSchemaInitializer` / `DuckDbKlineRepository` | marketdata | DuckDB schema 版本、批量行情读取写入 |
| `ResearchTaskApplicationService` | research | 研究任务、输入冻结、run 和状态 |
| `DifyResearchClient` / `DifyStreamConsumer` | research | Workflow API、Bearer、SSE 传输与 invocation 留痕 |
| `AiDraftValidationService` | research | Schema/引用/时间/授权/身份/冲突校验 |
| `ResearchEventApplicationService` | research | 由 Spring Boot/人工审核升级和发布研究事件 |
| `DataFoundationJobHandler` | job | 仅调用应用服务、调度参数幂等与执行审计 |
| `DataFoundationController` | ruoyi-admin | Sa-Token/scope、DTO、应用服务调用、错误映射；不直接 Mapper |
| Vue pages/API | plus-ui | 展示、确认、审核、状态恢复；不计算门禁/不直连 Dify |

依赖方向：`Controller/Job → Application Service → Domain Repository Port → Adapter/Mapper`；Domain 不依赖 Web/Dify/Job；marketdata 不依赖 execution/finance/risk；Dify adapter 仅通过研究应用服务和 DTO 交互。

### 3. Dify API 调用约定

```http
POST {QUANTPF_DIFY_BASE_URL}/v1/workflows/run
Authorization: Bearer ${DIFY_APP_API_KEY}
Content-Type: application/json
```

请求 `response_mode=streaming`，由 WebClient 消费 `text/event-stream`。输入仅含 task/material/source/subject/snapshot/as-of/authorization/quality/researchQuestion/researchPeriod 和 Workflow/Prompt/Schema 版本。禁止传交易/数据库凭据、未授权正文、无关个人信息、完整账户/持仓明细。

流程：

```text
权限/scope/授权/质量/身份检查
→ 保存 ResearchTask 与输入快照
→ 创建 DifyInvocation
→ 调用已发布 Workflow 并消费 SSE
→ 保存原始响应引用/hash
→ Schema/引用/时间/授权/身份/冲突校验
→ 保存 AiResearchDraft
→ 必要人工审核后由 Spring Boot 保存/发布 ResearchEvent
```

有限重试使用原任务幂等键和 retry_of；Schema、授权、身份错误不盲重试。可选 callback 使用专用 endpoint、HMAC-SHA256/时间戳/Nonce/防重放/run-task 关联，回调只入草稿校验队列。前端只轮询 Spring Boot REST；二期再评估 SSE/WebSocket。

### 4. 配置文件新增项

非敏感默认结构放 `ruoyi-admin/src/main/resources/application.yml`，dev/prod 仅覆盖环境值；敏感值从环境变量/密钥系统注入：

```yaml
quantpf:
  id:
    worker-id: ${QUANTPF_ID_WORKER_ID:0}
    datacenter-id: ${QUANTPF_ID_DATACENTER_ID:0}
    clock-backward-tolerance-ms: ${QUANTPF_ID_CLOCK_BACKWARD_TOLERANCE_MS:5}
  duckdb:
    enabled: ${QUANTPF_DUCKDB_ENABLED:true}
    path: ${QUANTPF_DUCKDB_PATH:./data/quantpf.duckdb}
    schema-version: ${QUANTPF_DUCKDB_SCHEMA_VERSION:1}
    init-lock-timeout-ms: ${QUANTPF_DUCKDB_INIT_LOCK_TIMEOUT_MS:10000}
    batch-size: ${QUANTPF_DUCKDB_BATCH_SIZE:1000}
  data:
    default-timezone: ${QUANTPF_DEFAULT_TIMEZONE:Asia/Shanghai}
    snapshot-inline-max-bytes: ${QUANTPF_SNAPSHOT_INLINE_MAX_BYTES:1048576}
    stale-window-seconds: ${QUANTPF_DATA_STALE_WINDOW_SECONDS:60}
    mock-mode: ${QUANTPF_DATA_MOCK_MODE:true}
  dify:
    enabled: ${QUANTPF_DIFY_ENABLED:true}
    base-url: ${QUANTPF_DIFY_BASE_URL:http://dify-api:5001}
    api-key: ${DIFY_APP_API_KEY:}
    workflow-key: ${QUANTPF_DIFY_WORKFLOW_KEY:NEWS_FACT_EXTRACTION}
    workflow-version: ${QUANTPF_DIFY_WORKFLOW_VERSION:}
    environment: ${QUANTPF_DIFY_ENVIRONMENT:dev}
    response-mode: streaming
    connect-timeout-ms: ${QUANTPF_DIFY_CONNECT_TIMEOUT_MS:5000}
    read-timeout-ms: ${QUANTPF_DIFY_READ_TIMEOUT_MS:180000}
    max-attempts: ${QUANTPF_DIFY_MAX_ATTEMPTS:3}
    backoff-ms: ${QUANTPF_DIFY_BACKOFF_MS:1000}
    max-concurrency: ${QUANTPF_DIFY_MAX_CONCURRENCY:2}
    callback:
      enabled: ${QUANTPF_DIFY_CALLBACK_ENABLED:false}
      signing-secret: ${DIFY_CALLBACK_SIGNING_SECRET:}
      allowed-clock-skew-seconds: ${QUANTPF_DIFY_CALLBACK_CLOCK_SKEW_SECONDS:300}
  research:
    max-input-bytes: ${QUANTPF_RESEARCH_MAX_INPUT_BYTES:1048576}
    max-output-bytes: ${QUANTPF_RESEARCH_MAX_OUTPUT_BYTES:4194304}
```

Key 不进 git、数据库、前端、普通日志或 `sys_config` 明文；生产 DuckDB path 指向持久化卷；`mock-mode` 生产必须关闭并且只有已确认授权数据源可启用。

### 5. 用户旅程 API/菜单映射

菜单继续以 `RuoYi-Vue-Plus/contract/menu/quantpf-menu-facts.yaml` 为事实源，扩展 Dashboard/Market/Research/Audit 子页面和权限；核心后端 API 由 OpenAPI 定义：

```text
/api/quant/data/source/*       /quality/* /snapshot/*
/api/quant/instrument/*        /calendar/* /market/*
/api/quant/research/tasks/*    /dify/* /event/* /assistant/*
```

每个写 API 带 requestId、Idempotency-Key，状态变更带 expectedVersion；返回对象 ID、状态/版本、用户可读摘要、下一步和回查链接。普通用户任务 <=3 核心输入；Paper 前置通过仅交给模块三，不生成订单。

## Risks / Trade-offs

- **[Risk] 三个来源 change 之间出现文档漂移** → 本 change 作为综合实施入口；具体表以数据库设计 change 为准、目录/类/配置以代码指导 change 为准、页面和旅程以 UI change 为准；统一任务中要求回写来源文档。
- **[Risk] 任务要求的 `/sql/` 与实际 Flyway 约定不一致** → 以实际后端 `db/migration/` 为权威；`script/sql/` 仅用于幂等种子/工具，不代替 Flyway。
- **[Risk] 现有 Paper 表存在 BIGINT UNSIGNED/AUTO_INCREMENT，批量改造风险高** → 先盘点完整 FK 图、备份、停写和类型对拍，再分批兼容迁移；不修改历史迁移。
- **[Risk] Dify UI 被误解为直接嵌入/可交易建议** → 只提供 Spring Boot 代理的触发按钮和草稿展示，固定草稿标签，无订单捷径。
- **[Risk] 用户界面承载过多专业细节** → 引导/审核/专业渐进披露，普通用户只看结论/原因/风险/下一步。
- **[Risk] 后端 JDK21 环境或前端启动命令因机器差异失败** → 联调 task 先核对实际 Maven Wrapper、pnpm scripts、端口和环境变量；失败时记录真实命令与诊断，不把模板命令当成功事实。

## Migration Plan

1. 先审查数据库设计、代码指导、UI 指导的字段/路径/权限/Workflow/状态词，冻结差异表。
2. 按 tasks 指定顺序完成数据库迁移、后端 Domain/Repository、接口、Dify、前端页面、前端 API。
3. 迁移 DDL 在 `RuoYi-Vue-Plus/db/migration/`，而非假设存在 `/sql/`；若任务逐表列出，按统一 Flyway 版本新增文件，历史迁移不改。
4. 启动后端前先用 JDK21 验证 Maven Wrapper 与 `DromaraApplication` profile；启动前端前核对 `plus-ui/package.json` 的 scripts，使用项目声明的 pnpm 命令。
5. 联调先用 Mock 来源与 Stub/自托管 Dify，验证角色路径、SSE/降级、数据资格、重复提交、Paper 前置和既有 Paper 回归。
6. 回滚：菜单/前端路由通过权限/Feature Flag 关闭；Dify 入口关闭不影响数据页；后端模块可通过配置禁用；数据库回滚遵循已批准的兼容/备份计划。

## Open Questions

以下只作为实施前配置，不改变本设计的架构和用户路径：

- 各用户角色的最终权限码是否需与部署组织已有角色名称映射；
- 前端本地开发 API 代理的目标端口及 dev profile；
- ECharts 日线图首版是否显示成交量与市场状态标记；
- Dify Workflow 在 dev/prod 的正式 Workflow Key/版本与模型供应商配置。
