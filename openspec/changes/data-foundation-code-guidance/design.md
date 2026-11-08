# Design

## Context

本 change 对应已完成的 `data-foundation-db-design` 数据库设计，不替代它。当前仓库是 Java 21 的 RuoYi-Vue-Plus 单体工程：

- `quantpf-domain` 已有 Paper 领域实体、Repository 端口、MyBatis Mapper/Record、幂等/账务/审计相关实现；
- `quantpf-common`、`quantpf-marketdata`、`quantpf-research`、`quantpf-strategy` 目前主要是 Maven 骨架；
- `quantpf-job` 目前只有 `PaperApplicationServicePort` 占位；
- `ruoyi-admin` 通过 `QuantPfPaperRuntimeConfiguration` 显式装配 Paper 服务，并明确没有 Redis、Broker、调度或 LIVE Bean；
- MyBatis-Plus 已配置 `idType: ASSIGN_ID`，`MybatisPlusConfig` 已提供 `DefaultIdentifierGenerator(NetUtil.getLocalhost())` 和 `InjectionMetaObjectHandler`；
- 现有业务基线使用 MySQL/Flyway，数据库设计另行规定新业务表使用应用雪花 ID、五个末尾日志字段、DuckDB 只存 K 线/Tick、自托管 Dify v1.16.1 和 XXL-JOB 唯一调度。

因此代码施工必须增量接入现有模块，不重写 Paper 核心事务，不把模块一做成新的独立微服务，也不把 Dify 或调度 Handler 变成业务事实源。

## Goals / Non-Goals

**Goals:**

- 给出可直接用于代码施工的目录、依赖、关键接口、服务边界和配置契约。
- 使数据治理、标的/日历、行情、研究任务和 Dify 草稿链路可测试、可回放、可幂等。
- 统一 MyBatis-Plus `IdentifierGenerator` 与 Hutool Snowflake 的 worker/datacenter 配置；统一五个日志字段自动填充，同时保留系统任务和人工操作者审计语义。
- 用 WebClient 服务端适配 Dify Workflow API，默认消费流式响应，可选启用签名回调；Dify 结果只能经过校验进入研究域草稿/事件流程。
- 让普通用户得到可读的“数据可用性/下一步”状态，专业配置只对管理员和审核角色开放。
- 保持模块一与 Paper、RiskCheck、订单、账户、持仓和真实交易通道的隔离。

**Non-Goals:**

- 本 change 不实现 Java 代码、DDL、Flyway、DuckDB 表、前端页面、XXL-JOB Handler 或 Dify Workflow 内容。
- 不实现真实行情交易通道、QMT/CTP、LIVE、真实账户同步或自动实盘。
- 不让 Dify 直接访问 QuantPF 数据库、Redis、Paper、风险或订单服务。
- 不在本 change 冻结策略参数、概率参数、Paper 手续费/滑点或二期自动择时算法。

## Decisions

### 1. 仓库目录结构与依赖方向

以现有 Maven 多模块为基础，不新增顶层服务：

```text
RuoYi-Vue-Plus/
├── quantpf-modules/
│   ├── quantpf-common/
│   │   └── src/main/java/com/quantpf/common/
│   │       ├── config/                 # QuantPfProperties、ID/时钟/序列化配置
│   │       ├── audit/                  # AuditContext、ActorContext、日志字段值
│   │       ├── id/                     # SnowflakeFacade、WorkerNodeProperties
│   │       ├── time/                   # UTC/market timezone 值对象
│   │       ├── error/                  # 数据域异常与错误码映射
│   │       └── event/                  # 事件元数据、Outbox 端口、幂等键
│   ├── quantpf-domain/
│   │   └── src/main/java/com/quantpf/domain/
│   │       ├── data/                   # DataSource、Vendor、SourceGroup、Snapshot
│   │       ├── instrument/             # Instrument、Version、FutureProduct、Relation
│   │       ├── calendar/               # Calendar、Day、Session、Readiness、Conflict
│   │       ├── market/                 # Quote、MarketState、Capacity、Feature
│   │       ├── research/               # Material、Task、Draft、ResearchEvent、Evidence
│   │       ├── repository/              # 领域 Repository 端口
│   │       └── persistence/            # MyBatis Record/Mapper/XML，按域分包
│   ├── quantpf-marketdata/
│   │   └── src/main/java/com/quantpf/marketdata/
│   │       ├── application/            # 采集、治理、身份、日历、行情应用服务
│   │       ├── adapter/source/          # Tushare/新闻/规则 Mock 与真实适配器接口
│   │       ├── adapter/duckdb/          # DuckDbSchemaInitializer、Kline/Tick Adapter
│   │       ├── adapter/mysql/           # 数据域 Repository Adapter
│   │       ├── quality/                 # 校验、点时、用途资格
│   │       └── event/                   # 数据健康/就绪/资格事件发布
│   ├── quantpf-research/
│   │   └── src/main/java/com/quantpf/research/
│   │       ├── application/            # ResearchTask、材料、事实/事件编排
│   │       ├── dify/                    # DifyClient、请求构建、流式消费、回调校验
│   │       ├── validation/              # Schema/引用/时间/授权/身份/冲突校验
│   │       ├── adapter/mysql/           # 研究域 Repository Adapter
│   │       └── event/                   # ResearchEvent 发布与补偿
│   ├── quantpf-job/
│   │   └── src/main/java/com/quantpf/job/
│   │       ├── handler/                 # 仅 XXL-JOB Handler，调用应用服务
│   │       ├── command/                 # 任务参数解析/幂等
│   │       └── audit/                   # 任务执行审计适配
│   └── quantpf-strategy/                # 本 change 只提供输入端口，不写策略实现
└── ruoyi-admin/
    ├── src/main/java/com/quantpf/web/
    │   ├── controller/                  # 数据/日历/研究/Dify查询与命令入口
    │   ├── dto/                         # 请求/响应 DTO，不暴露内部 Record
    │   └── config/                      # QuantPF 总装配与配置绑定
    └── src/main/resources/
        ├── application.yml
        ├── application-dev.yml
        └── application-prod.yml
```

依赖规则：

```text
ruoyi-admin -> quantpf-job / quantpf-research / quantpf-marketdata / quantpf-domain
quantpf-job -> application ports（不复制业务规则）
quantpf-research -> quantpf-marketdata ports + quantpf-domain/common
quantpf-marketdata -> quantpf-domain/common
quantpf-domain -> common/基础库（不得依赖 Web、Dify、XXL-JOB、Paper execution）
Dify adapter -> research application ports（不得反向写 Domain Mapper）
```

`quantpf-marketdata` 不依赖 `quantpf-execution`、`quantpf-finance` 或 `quantpf-risk`；需要影响 Paper 时只发布数据资格/健康事件，由模块四消费。

### 2. 关键类和接口职责

| 组件 | 建议职责 | 明确禁止 |
|---|---|---|
| `DataSourceApplicationService` | 数据源、产品、接口字段、来源组配置和健康状态命令；权限/幂等/审计入口 | 直接调用 Dify、直接写 Mapper |
| `CollectionApplicationService` | 创建采集任务、调用 SourcePort、保存原始快照/观测、发布完成/失败事件 | 在 Handler 中复制采集业务逻辑 |
| `SourceAdapter` | `sourceCode` 对应的外部/Mock 采集端口，返回原始响应和来源元数据 | 决定研究结论、写研究事件/订单 |
| `RawSnapshotService` | content hash、敏感字段清洗、对象存储引用、重复观测和点时版本 | 丢弃原始响应、静默覆盖旧版本 |
| `DataQualityService` | OHLC、字段、授权、时间、冲突、身份和用途资格计算 | 接受 AI 置信度作为资格 |
| `InstrumentApplicationService` | 标的同步、身份映射、具体/连续合约关系和版本属性 | 自动把连续合约当执行标的 |
| `TradingCalendarService` | 日历定义/日历日/时段/绑定/就绪/冲突处理和可交易资格查询 | 用服务器时区或单一 is_open 推导交易资格 |
| `MarketDataApplicationService` | MySQL 盘口摘要、DuckDB K 线/Tick 读写、市场状态/容量/特征快照编排 | 直接调用 Paper/Broker |
| `DuckDbSchemaInitializer` | 启动时锁定 DuckDB 文件，校验 schema version/checksum，幂等升级 | 使用 MySQL Flyway 管理 DuckDB 表 |
| `DuckDbKlineRepository` / `DuckDbTickRepository` | 按自然键批量写入和查询时序数据，校验周期/身份/点时引用 | 写 MySQL 业务表或账户状态 |
| `ResearchTaskApplicationService` | 研究任务、输入快照、幂等、任务状态和运行审计 | 让 Dify 自己创建任务 |
| `DifyWorkflowRegistryService` | 校验 Workflow 环境、发布状态、版本和调用限额 | 保存 API Key 明文 |
| `DifyResearchClient` | WebClient 调用 `/v1/workflows/run`，Bearer 认证，流式解析，超时/重试 | 将 Dify 响应直接写事实表 |
| `DifyStreamConsumer` | 解析 `text/event-stream` 事件，组装最终响应，保存中间状态/哈希 | 根据部分文本发布研究事件 |
| `DifyCallbackVerifier` | 可选 callback 的 HMAC、时间窗口、Nonce、防重放、run/task 关联 | 直接推进业务状态 |
| `AiDraftValidationService` | Schema、来源/引用、时间、授权、身份、冲突七步校验 | 将模型 confidence 当概率/事实 |
| `ResearchEventApplicationService` | 由 Spring Boot/人工审核升级、版本化和发布研究事件 | 接受 Dify 直写 |
| `DataFoundationController` | DTO 校验、Sa-Token/资源 scope、调用应用服务、返回用户可读状态 | 直接使用 Mapper、直接触发 Paper 订单 |
| `DataFoundationJobHandler` | XXL-JOB 参数/幂等/执行审计，调用应用服务 | 在 Handler 内实现领域规则 |

已有 `quantpf-domain` Paper 的 `PaperPersistenceTransactionService`、`PersistentPaperOrderApplicationService`、`PaperRiskGate`、审计 Repository 和订单 Mapper 保持原职责，模块一只能通过契约/事件交接，不改写其内部事务。

### 3. MyBatis-Plus ID、日志字段与审计上下文

现有 `mybatis-plus.global-config.dbConfig.idType: ASSIGN_ID` 和 `DefaultIdentifierGenerator` 是默认路径。施工时：

1. 在 `quantpf-common.id.WorkerNodeProperties` 绑定 `quantpf.id.worker-id`、`quantpf.id.datacenter-id`；启动时校验范围、环境隔离和实例唯一性。
2. `QuantPfIdentifierGenerator` 作为 MyBatis-Plus `IdentifierGenerator` Bean，使用共享 worker/datacenter 配置；不允许模块自行 new 另一套 MP generator。
3. `SnowflakeFacade` 封装 Hutool Snowflake，只在业务侧需要预先生成业务 ID/幂等关联号时使用；它与 MP 生成器共用节点配置，并提供碰撞/时钟回拨保护。
4. `InjectionMetaObjectHandler` 扩展或通过 QuantPF handler 为所有新实体填充：`create_by/create_time/update_by/update_time/remark`。若请求带人工操作者，优先使用 `ActorContext`；系统任务使用系统 actor 或 NULL。
5. 主键和关联 ID 使用 `Long/BIGINT`，禁止 `AUTO_INCREMENT`、`UNSIGNED` 或 String 主键；DTO 不把数据库 ID 当作权限凭证，服务端始终执行资源 scope 校验。
6. 对既有表的 ID/日志兼容迁移必须在代码支持双读/停写窗口后执行；不能在 Domain 层假设旧列已立即删除。

### 4. MySQL Repository 与事务边界

- MyBatis Mapper/XML 只在 persistence 包中；应用服务调用领域 Repository 端口；Controller/Job/Dify 不直接拿 Mapper。
- 数据源配置、采集任务、快照、质量、点时、资格、身份、日历和研究任务的单个状态迁移使用显式事务；原始大对象存 `sys_oss` 时保存引用与 hash。
- 采集外部调用不持有 MySQL 长事务：先创建任务/调用记录，再外部请求，最后以幂等键提交快照/质量结果。
- 质量状态与用途资格更新必须在同一业务一致性边界内，或通过 Outbox 可靠发布 `DataQualityStatusChangedEvent`/`MarketDataReadyEvent`。
- Dify 调用记录、AI 草稿和研究事件升级分别保存；草稿校验失败不得回滚原始采集事实。
- 所有写命令带 `requestId`、`Idempotency-Key`、`expectedVersion`（适用时）和 actor；重复请求返回原结果，参数不一致返回幂等冲突。

### 5. DuckDB 适配与生命周期

`DuckDbDataSourceConfiguration` 创建独立 JDBC DataSource，路径只能来自 `quantpf.duckdb.path`，生产路径必须是挂载卷。`DuckDbSchemaInitializer`：

```text
获取文件级/应用级锁
→ 读取 qpf_schema_version
→ 校验版本和 checksum
→ 执行幂等 schema migration
→ 发布 DuckDbReady
```

`DuckDbKlineRepository` 只接受 `30m/1d/1w/1M`；一期业务任务默认 `1d`。写入前校验标的、交易日、OHLC、质量和点时引用；唯一自然键冲突走 upsert/重复观测，不覆盖不同来源版本。DuckDB 不承担：

```text
供应商授权、质量问题、研究事件、Dify 草稿、Paper账户、风险、订单、持仓、审计主记录
```

### 6. Dify API 调用约定

#### 6.1 配置与请求

目标端点：

```text
POST {base-url}/v1/workflows/run
Authorization: Bearer ${DIFY_APP_API_KEY}
Content-Type: application/json
```

请求由 `DifyRequestFactory` 生成，最小字段：

```json
{
  "inputs": {
    "research_task_id": "...",
    "material_refs": ["..."],
    "source_refs": ["..."],
    "subject_refs": ["..."],
    "snapshot_id": "...",
    "as_of_time": "...",
    "authorization_status": "PERSONAL_RESEARCH_ALLOWED",
    "quality_status": "AVAILABLE",
    "research_question": "...",
    "research_period": "SHORT"
  },
  "user": "quantpf-system",
  "response_mode": "streaming"
}
```

请求不得传交易密码、Dify Key、数据库连接、Paper 账户明细或未授权正文。`user` 使用不可反推敏感身份的业务调用主体；用户审计在 QuantPF 自己的 `actorId` 中保存。

#### 6.2 调用状态

建议状态：

```text
CREATED → SUBMITTED → STREAMING → RECEIVED → VALIDATING → SUCCEEDED
                                  ├→ TIMEOUT
                                  ├→ RETRYABLE_FAILED
                                  ├→ RATE_LIMITED
                                  ├→ AUTH_FAILED
                                  └→ INVALID_RESPONSE
```

同一研究任务的调用使用稳定幂等键；网络错误按有限次数、指数退避和抖动重试。Schema/授权/身份失败不得盲目重试。连接中断后保留原始片段/响应 hash，补偿任务不得重复发布事件。

#### 6.3 输出校验

`DifyResearchClient` 只负责传输，`AiDraftValidationService` 负责业务校验：

```text
JSON Schema/字段枚举
→ run/task/workflow 关联
→ source/material/snapshot 存在
→ 引用片段可定位且支持主张
→ available_at <= as_of_time
→ authorization/useEligibility
→ subject/instrument identity
→ 来源冲突
```

结果只写 `quantpf_ai_research_draft`。全部通过后，由 Spring Boot 的研究服务或人工审核升级 `quantpf_research_event`；Dify不能调用研究事件、策略、风险或订单 API。

#### 6.4 可选签名回调

默认使用流式消费，不依赖 Dify 主动回调。若实测 Dify 版本支持回调，则使用专用路径：

```text
POST /api/quant/research/dify/callback
X-Dify-Signature: HMAC-SHA256(...)
X-Dify-Timestamp: ...
X-Dify-Nonce: ...
```

回调验证来源白名单、时间窗口、Nonce、签名、requestId/runId/幂等键；通过后只投递草稿校验队列，绝不在 Controller 中直接发布研究事件。

### 7. 配置文件与环境变量

在 `application.yml` 建立非敏感默认结构，在 `application-dev.yml`/`application-prod.yml` 覆盖环境参数；敏感值只用环境变量或密钥系统：

```yaml
quantpf:
  id:
    worker-id: ${QUANTPF_ID_WORKER_ID:0}
    datacenter-id: ${QUANTPF_ID_DATACENTER_ID:0}
    clock-backward-tolerance-ms: ${QUANTPF_ID_CLOCK_BACKWARD_TOLERANCE_MS:5}
  actor:
    system-user-id: ${QUANTPF_SYSTEM_USER_ID:}
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
      allowed-clock-skew-seconds: ${QUANTPF_DIFY_CALLBACK_CLOCK_SKEW_SECONDS:300}
      signing-secret: ${DIFY_CALLBACK_SIGNING_SECRET:}
  research:
    max-input-bytes: ${QUANTPF_RESEARCH_MAX_INPUT_BYTES:1048576}
    max-output-bytes: ${QUANTPF_RESEARCH_MAX_OUTPUT_BYTES:4194304}
    retry-queue: ${QUANTPF_RESEARCH_RETRY_QUEUE:quant:research:retry}
```

补充约定：

- `DIFY_APP_API_KEY`、`DIFY_CALLBACK_SIGNING_SECRET` 不得写入 YAML、数据库、前端 bundle 或普通日志；
- 生产 `QUANTPF_DUCKDB_PATH` 必须指向 Docker volume；
- `mock-mode=true` 仅施工/测试环境启用，联调前切换并登记真实供应商授权；
- Dify 配置按 dev/prod 分离，Workflow/Prompt/Schema 版本写入注册表和调用记录；
- 不能以 `sys_config` 保存明文密钥；可保存非敏感开关/版本和密钥 metadata。

### 8. API/事件与用户状态映射

`ruoyi-admin` 只提供服务端门面，建议分组：

```text
/api/quant/data/source/*
/api/quant/data/quality/*
/api/quant/data/snapshot/*
/api/quant/instrument/*
/api/quant/calendar/*
/api/quant/market/*
/api/quant/research/tasks/*
/api/quant/research/dify/*
/api/quant/research/event/*
```

返回统一包含：`requestId`、业务对象 ID、状态/版本、用户可读摘要、是否需要下一步和可回查链接；错误返回稳定错误码和可重试属性，不泄露 SQL、Key、Prompt 或堆栈。

关键事件：

```text
DataSourceHealthChangedEvent
DataQualityStatusChangedEvent
PointInTimeSnapshotCreatedEvent
InstrumentVerifiedEvent
TradingCalendarUpdatedEvent
MarketDataReadyEvent
MarketDataStaleEvent
ResearchTaskCreatedEvent
AiResearchDraftCompletedEvent
ResearchEventVerifiedEvent
ResearchEventPublishedEvent
```

事件通过 Outbox/等效可靠机制发布，消费者幂等；事件只通知/驱动受控后续任务，不是未经校验的交易命令。

### 9. 测试策略

- **架构测试**：ArchUnit/依赖检查，确保 Controller/Job/Dify adapter 不直接依赖 Mapper，Domain 不依赖 Web/Dify/XXL-JOB。
- **数据库集成**：Flyway、MyBatis Mapper、五末尾字段、BIGINT 无自增、ID 生成、幂等/乐观锁、点时/资格、父子 FK 类型。
- **DuckDB**：schema checksum、重启恢复、周期 CHECK、非法 OHLC、自然键去重、批量写入。
- **Dify 合同测试**：streaming SSE 解析、Schema 错误、引用错误、时间未来、授权阻断、超时重试、重复回调、HMAC、防重放、敏感日志扫描。
- **用户路径集成**：管理员配置源→数据就绪；审核冲突→资格重算；普通用户研究任务→草稿→事件；数据不合格→Paper 前置阻断；Dify 失败→确定性链路继续。
- **Paper 回归**：现有 Paper 下单/撤单/账务/风险/审计测试必须继续通过，数据模块不得新增真实 Broker Bean 或改变 `PaperRiskGate` 语义。

## Risks / Trade-offs

- **[Risk] 模块骨架与现有 Paper 代码风格不完全一致** → 先复用已有 Domain/Repository/事务模式，新增域只扩展端口，不重写 Paper。
- **[Risk] Dify 流式协议或版本行为变化** → 适配器隔离端点/Schema/响应事件，Workflow 注册表保存版本，使用 Stub 合同测试；默认不依赖回调。
- **[Risk] MyBatis-Plus 与 Hutool ID 生成器节点配置冲突** → 单一 `WorkerNodeProperties` 和启动校验；禁止模块自行创建生成器。
- **[Risk] DuckDB 与 MySQL 双库一致性复杂** → DuckDB 只承载 K 线/Tick，使用稳定 ID/snapshot 引用和批次状态；业务资格仍以 MySQL 为准。
- **[Risk] AI 草稿越权升级为事实** → 独立草稿校验服务、独立回调入口、数据库权限和集成测试共同阻断。
- **[Risk] Dify 长连接占用线程/资源** → WebClient/Reactor 流式消费、全局/用户并发、超时、队列和降级配置；不在 Handler 中同步长轮询。
- **[Risk] 现有旧表自增/旧日志列迁移影响运行** → 先完成兼容迁移设计和存量 FK/ID 校验，再停写切换；代码支持迁移窗口，历史 Flyway 不回写。

## Migration Plan

1. 先完成 `quantpf-common` 配置/ID/Actor/错误码基础和架构测试。
2. 补齐 `quantpf-domain` 数据、标的、日历、行情、研究对象和 Repository 端口；确认与已完成数据库设计、Flyway 和五个末尾日志字段一致。
3. 实现 MySQL Mapper/Adapter 和数据治理应用服务，先用 Mock Source 完成快照→质量→点时→资格闭环。
4. 实现 DuckDB initializer/repository，完成日线主链路和容器重启测试。
5. 实现研究任务与 Dify adapter，先用 Stub Workflow 合同测试，再接入自托管 Dify v1.16.1；真实 Key 只在联调/部署环境注入。
6. 暴露 `ruoyi-admin` 查询/命令/回调门面，接入 Sa-Token、资源 scope、审计、OpenAPI/事件/错误码/菜单契约。
7. 迁移 XXL-JOB Handler：Handler 只调用应用服务；确认不恢复 SnailJob 双栈，不创建 Paper/真实交易路径。
8. 运行全链路和 Paper 回归测试，更新技术分册落地状态；本 change 完成后再由后续实现 change 执行代码。

回滚：新模块 Bean、配置开关、任务和 Dify 集成可独立禁用；Mock/确定性数据链不依赖 Dify；数据库/迁移回滚遵循数据库设计 change 的备份、停写、兼容和 Flyway 规则，不删除已产生的历史快照/研究草稿。

## Open Questions

以下不改变本设计的分层、边界或行为契约，可在实现前作为配置登记：

- Dify 自托管部署的具体容器网络别名、资源规格和模型供应商；
- DuckDB JDBC 驱动版本及生产备份频率；
- 真实供应商授权完成后的接口字段清单和 SourceAdapter 参数；
- XXL-JOB 迁移完成后的执行器注册参数与部署拓扑。
