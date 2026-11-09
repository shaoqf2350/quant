# Design

## Context

动机见 `proposal.md - Why`，行为契约见 `specs/*/spec.md`。本节只记录影响技术选型的**当前状态与约束**。

### 代码现状（实测，非文档转述）

| 项 | 现状 |
|:---|:---|
| `quantpf-domain` | 54 个 Java 文件：`Instrument`/`TradingAccount`/`Position`/`PaperOrder`/`PaperExecution` 实体、10 个枚举、5 个值对象、10 组 Mapper+Record+Repository |
| `quantpf-finance` | 22 个：账务台账、跟盘、审计持久化服务、`PaperPersistenceTransactionService`（显式 MyBatis 事务） |
| `quantpf-execution` | 6 个：`OrderStateMachine`、`PaperBrokerAdapter`/`InMemoryPaperBrokerAdapter`、`PaperOrderService` |
| `quantpf-risk` | 6 个：`RiskGate`/`PaperRiskGate`（staleness 窗口 60s）、`RiskContext`/`RiskDecision` |
| `quantpf-marketdata` / `quantpf-research` / `quantpf-strategy` / `quantpf-common` | **0 个 Java 文件**，仅 `pom.xml` 占位 |
| `quantpf-backtest` / `quantpf-job` | 各 1 个接口占位（`BacktestExecutionBoundary` / `PaperApplicationServicePort`） |
| `ruoyi-admin` | `com/quantpf/web` 下 13 个类（7 controller + dto + `QuantPfPaperRuntimeConfiguration`） |
| Flyway | `V1_0_0` ~ `V1_9_0`，19 张 `quantpf_*` 表，全部带 CHECK 与唯一键 |
| 契约 | `contract/openapi/quantpf-v29.yaml`、4 个事件 Schema、`error-codes`、`menu-facts`（9 个顶层 code） |
| 调度 | 根 `pom.xml` 仍是 SnailJob 2.0.2；XXL-JOB **未接入** |
| 前端 | `plus-ui` 分支 `6.X-Vue`，`src/views` 下**无 `quant` 目录** |
| 数据库菜单 | Phase1C handoff 记录：`sys_menu` 109 条通用记录，QuantPF 菜单 **0 行** |
| 工作区 | 根 POM、`ruoyi-admin/pom.xml`、`DromaraApplication.java`、`application-dev.yml` 有未提交修改；`contract/`、`db/`、`docs/`、`quantpf-modules/`、`openspec/`、`BASELINE.md` 均为未跟踪新增 |

### 约束

1. **保护已落地 Paper 能力**：13 个 API、订单状态机、复式台账、幂等键、`SELECT ... FOR UPDATE` 显式事务、数据库 CHECK/唯一键、审计，MUST 全部保持行为不变。本变更只做**加字段、加索引、加表**，不改既有约束语义、不重写既有事务。
2. **保留未提交改动**：不得 `git checkout`/`reset`/`clean`，不得覆盖 `pom.xml`、`DromaraApplication.java`、`application-dev.yml` 的现有修改。
3. **契约先行**：新能力先改 `contract/`，再写代码（分册 §34.3、详设 0 §15.2）。
4. **只走 Flyway**：禁止执行 `doc/schema.sql`（V21 遗留，29 表且含 `DROP TABLE IF EXISTS`）与任何历史 SQL 直改。
5. **不引入双栈调度**：SnailJob → XXL-JOB 迁移属 Phase 0 门禁，本变更只提供 Handler 与审计对接。
6. **Stage 1 禁止真实交易**：不得产生 `LIVE`/`QMT`/`CTP`/`REAL_BROKER_ORDER`/`REAL_BROKER_CANCEL` 任何路径。
7. **组件扫描**：`DromaraApplication` 默认扫描根为 `org.dromara`，QuantPF 代码位于 `com.quantpf`，依赖 `QuantPfPaperRuntimeConfiguration` 显式装配。本变更新增的 Bean MUST 沿用同一装配方式，不得修改既有启动类扫描根。
8. **JDK 21**：根 POM 要求 Java 21；`BASELINE.md` 记录当前 shell 为 Java 8/17，施工与验收 MUST 在 JDK21 进程环境执行。
9. **业务文档实际路径**：`D:\workspace\quant\doc\`（`openspec/config.yaml` 中的 `D:\我的文档\私人\量化` 已失效）。

## Goals / Non-Goals

**Goals:**

- 用 8 个能力规格覆盖详设 1 §2.2 的八个子域，形成从「供应商登记」到「用途资格判定」的完整可验证链路。
- 建立**点时版本 + 原始快照 + 六时间字段 + 修订链**四件套，使总纲 §24.1 的数据成功标准可被测试断言。
- 把 Tushare 日历适配落到「源接口 + 字段 Schema 版本」级别，原始值与标准化值双存，解析失败进 `INVALID`。
- 把 Dify 协作固化为**单向受控契约**：Spring Boot 出题 → Dify 出草稿 → Spring Boot 校验升级 → Spring Boot 发布事件。
- 前端提供引导/审核/专业三种体验模式，普通用户核心输入 ≤ 3 项，所有阻断状态有普通语言解释。

**Non-Goals（设计层边界，不重复 proposal 的范围声明）:**

- 不设计候选筛选算法、策略形态识别、仓位公式、回测撮合与概率校准（模块二/三/四）。
- 不设计 Dify Workflow 的内部节点编排、提示词正文与向量库索引结构（属详设 2A 与 Dify 侧工程）。
- 不设计分钟线/Tick/盘口的存储与下推时序库方案（ADR-02 已裁决归二期）。
- 不设计港股/美股/外汇的日历与品种规则细节，只预留编码、时区与分类字段。
- 不在本变更内修改总纲、分册、详设文档正文（只产出「待回写清单」）。
- 不修改 `openspec/config.yaml`（workflow 配置变更属独立变更）。

## Decisions

### ADR-01：表命名统一收敛到 `quantpf_*`，遗留脚本一律不执行

**决定**：本模块全部新表以 `quantpf_` 前缀命名，通过 Flyway `V1_10_0` 起的迁移创建。`doc/schema.sql`（V21 时代 `qs_/qt_/qm_/qn_/qf_` 29 表）与分册 §26.3 的 `qm_stock_basic`/`qs_kline_bar`/`qt_trade_order` 命名**全部作废**，仅作为历史参考。

**理由**：代码已用 `quantpf_*` 落地 19 张表并带完整约束；三代命名并存会让 AI 施工与人工审查都无法判断哪套是事实源。`doc/schema.sql` 含 `DROP TABLE IF EXISTS`，一旦误执行会摧毁已落地 Paper 数据。

**备选**：①保留 `qs_/qt_` 并新建映射视图 —— 否决，双写与视图会让点时版本与外键约束无法保证；②重命名既有 `quantpf_*` 表 —— 否决，破坏已落地代码与迁移历史。

**待回写**：总纲 §16.2/§16.3/§16.5 中的 `qs_kline_bar`、`qn_news_raw`、`qf_financial_statement`、`qs_strategy_param_config`、`qt_trade_signal` 需按分册 §26.2 的拆分规则移出总纲；`doc/schema.sql` 建议归档并加注「已作废，禁止执行」。

### ADR-02：一期只用日线，分钟线/Tick/盘口与盘中调度归二期

**决定**：本变更只实现日线行情的采集、校验、导入、复权、市场状态、容量与量价特征。分钟线、Tick、盘口实时、盘中日内界面与盘中频率调度**不实现**。`quantpf_market_bar.timeframe` 保留周期字段以支持二期扩展，但一期只写入 `D1`。

**冲突来源**：总纲 §3.5 允许「分钟线、盘口、逐笔摘要用于市场确认、入场等待、流动性检查和模拟执行」，§5.7 列「资金流向、盘口、逐笔摘要」为一期数据类别，§7.8 定义一期盘口用途，§16.8 含 `tradingLoopJob`（每 5 分钟）与 `positionSyncJob`（每 5 秒）；而详设 5 表头与 D5-06 明确「一期业务只使用日线；不实现分钟线、Tick 和盘中日内交易界面/调度」，详设 4 L1174 明确「一期 Paper 使用日线」。

**裁决依据**：详设 5 §前言写明「用户已经确认的方案以本文为准」；且取更严格边界符合总纲 §3.6「不把系统变成高频交易系统」与 §2.3「不把模拟盘成交等同于真实市场成交」。层级规则（总纲 §27 三）要求业务原则变化先改总纲——本变更**按详设执行并记录总纲待回写**，不自行修改总纲。

**连带影响**：
- 总纲 §16.8 的 `tradingLoopJob`（5 分钟）、`positionSyncJob`（5 秒）在一期降级为日线时点任务；`crawlClsNewsJob`（30 秒）、`crawlSinaNewsJob`（1 分钟）、`analyzeNewsJob`（5 分钟）属**新闻采集**而非行情，按详设 1 §7.5「采集频率是数据源配置，不硬编码统一频率」改为可配置，默认盘后与固定间隔组合。
- `PaperRiskGate` 现有 staleness 窗口 60 秒针对实时行情；一期日线场景下，行情新鲜度判定 MUST 改为「数据基准日期 vs 交易日历应到日期」，而不是墙钟秒级窗口。**本变更不修改 `PaperRiskGate` 既有行为**，改为在 `MarketDataReadinessCalculator` 输出日线级新鲜度，由后续模块四变更决定如何接入闸门（见 Open Questions Q1）。

**备选**：一期同时实现日线 + 分钟线 —— 否决，会同时放大 `marketdata` 存储、时序库下推、调度频率、前端图表与风控新鲜度五处复杂度，且与用户已确认的详设 5 冲突。

### ADR-03：`quantpf_trading_calendar` 保留为「日历日」物理表，新增独立的日历定义表

**决定**：已落地的 `quantpf_trading_calendar`（`market_code`+`trading_date`+`is_open`+`session_open/close`）语义上是**日历日表**，本变更保留其表名与既有唯一键，通过 ALTER 扩展字段，并新增 `quantpf_trading_calendar_def` 作为日历定义表。领域对象 `TradingCalendarDay` 映射到 `quantpf_trading_calendar`，`TradingCalendar` 映射到 `quantpf_trading_calendar_def`。

**理由**：详设 1 §12.1 结论「交易日历不是单表」。但重命名已落地表会破坏 `V1_1_0` 迁移历史、既有 Mapper/Record 与任何已写入的数据；用「保留物理表名 + 新增定义表 + 领域对象分别映射」既满足多对象模型，又零风险。

**备选**：新建 `quantpf_trading_calendar_day` 并迁移旧数据 —— 否决，需要数据搬迁与双写期，收益仅为命名美观。

### ADR-04：`quantpf_research_news` 保留为新闻原文表，新增 `quantpf_research_material` 作统一材料索引

**决定**：已落地的 `quantpf_research_news`（按 `external_id`+`source`、`content_hash` 去重）保留为**新闻类原始材料**表并 ALTER 扩展六时间字段、授权、质量、用途资格与快照引用。新增 `quantpf_research_material` 作为跨类型统一索引表，通过 `source_table` + `source_id` 多态引用新闻、公告、财报、研报与产业材料，`ResearchTask` 只引用 `material_id`。

**理由**：研究任务与 Dify 协作需要统一的材料入口，但公告/财报/研报的字段结构与新闻差异大，塞进一张宽表会产生大量空字段与语义混淆；多态索引表既不破坏已落地新闻链路，又给下游一个稳定的引用点。

**代价**：多态外键无法用数据库 FOREIGN KEY 强约束，MUST 在应用层保证引用完整性，并对 `(source_table, source_id)` 建唯一键防止重复索引。

### ADR-05：Dify 集成采用「Spring Boot 主动调用 + 异步回调」双通道，回调入口与 Paper API 物理隔离

**决定**：
- **主通道（同步/异步任务式）**：`DifyWorkflowClient` 由 Spring Boot 主动发起，携带 `requestId` + `idempotencyKey` + 受控上下文；Dify 返回结构化草稿 JSON。
- **回调通道**：`/api/quant/research/dify/callback` 独立 controller，只接收研究草稿与运行结果，MUST 校验来源签名或共享密钥、`requestId` 与任务状态；与 `/api/quant/order/**`、`/api/quant/risk/**`、`/api/quant/paper/**` 在包路径、权限标识与 Sa-Token 拦截规则上完全隔离。
- **不采用** iframe 嵌入 Dify 前端作为主交互（详见附录 B.4 的取舍说明）。

**理由**：详设 1 §18.3 与详设 0 §15.4 明确禁止 Dify 直接调用下单接口、直接写事实主表。物理隔离 + 签名校验 + 只读白名单接口是最小可行防线。

### ADR-06：用途资格采用「实时计算 + 结果留痕」而非物化状态字段

**决定**：`UsageEligibilityDecider` 每次按详设 1 §16.1 的七项输入实时计算五类资格（展示/研究/筛选/回测/Paper 新开仓），返回逐项结果与受限原因列表；同时把**关键判定结果**写入 `quantpf_data_usage_eligibility` 留痕（谁在什么时间、基于哪个数据版本、得到什么资格）。

**理由**：物化状态字段会与七个输入产生一致性维护成本，且详设 1 §9.4 矩阵要求逐项返回；但完全不留痕就无法满足总纲 §24.1「所有事实可追溯」与审计回放要求。实时计算保证正确性，留痕保证可审计。

**备选**：全量物化资格字段到每张数据表 —— 否决，七输入任一变化都需批量刷新，且容易与真实判定漂移。

### ADR-07：人工待办统一到一张 `quantpf_manual_review_task` 表

**决定**：日历冲突、标的身份映射、数据质量问题、AI 草稿复核、授权确认、事件核验六类待办共用一张表，以 `task_type` 区分，`payload_json` 承载类型特有字段，统一记录 `decision`（`CONFIRMED`/`REJECTED`/`DEFERRED`/`OVERRIDDEN`/`EXPIRED`）、理由、有效期、操作者与当时可见数据版本。

**理由**：详设 5 §8.1 的「我的待办」与 §10 驾驶舱需要跨类型聚合；总纲 §14.6 要求人工决策五态统一。分表会让待办中心做六路 UNION。

**代价**：`payload_json` 弱类型，MUST 在应用层按 `task_type` 做 Schema 校验，并对每类建立独立的服务与测试。

### ADR-08：新增统一幂等表，但不改动既有 Paper 幂等实现

**决定**：新增 `quantpf_idempotency_record` 服务于本模块的采集入库、研究任务、Dify 回调、人工审核、日历发布等写操作。已落地 Paper 链路的幂等（`quantpf_paper_order.idempotency_key`、`quantpf_paper_execution.idempotency_key`、`quantpf_cash_ledger.idempotency_key`、跟盘 `start_request_id`/`stop_request_id`）**保持原样不动**。

**理由**：ADR「保护已落地能力」优先于实现统一。两套幂等并存期通过 `scope` 字段区分，后续模块四变更再评估是否收敛。

### ADR-09：一期不建时序库，日线留在 MySQL；行情表预留下推标记

**决定**：分册 §26.3 ADR-7 建议行情走时序库（DuckDB/TSDB）。一期只有日线，A 股约 5000 标的 × 每年约 242 交易日 ≈ 120 万行/年，加上期货合约，MySQL 单表配合 `(instrument_id, timeframe, bar_time)` 唯一键与覆盖索引完全可承载。本变更**不引入时序库依赖**，但在 `quantpf_market_bar` 预留 `storage_tier` 字段（`MYSQL`/`TSDB`）以便二期无迁移地下推。

**理由**：避免一期同时引入新存储中间件的运维、备份与事务边界复杂度；日线数据量不构成瓶颈。

### ADR-10：前端在 `plus-ui` 新增 `quant` 视图域，菜单通过 Flyway 写入 `sys_menu` 并与 `contract/menu` 双向闭合

**决定**：`plus-ui/src/views/quant/data/**` 与 `plus-ui/src/api/quant/**` 新建；菜单与权限通过 `V1_18_0__data_foundation_menu_and_dict.sql` 以 `INSERT ... ON DUPLICATE KEY UPDATE` 幂等写入 `sys_menu`，同时更新 `contract/menu/quantpf-menu-facts.yaml` 增加本模块菜单条目，使 YAML 与数据库**互为校验**（CI 增加一致性检查任务）。

**理由**：Phase1C handoff 记录 YAML 有 9 条而数据库 QuantPF 菜单 0 行，属未闭合状态。本变更必须闭合，否则新页面无法在若依菜单中显示，权限也无法生效。

**约束**：`forbiddenCapabilities`（`LIVE`/`QMT`/`CTP`/`REAL_BROKER_ORDER`/`REAL_BROKER_CANCEL`）MUST 保留，本变更不新增任何与之相关的菜单或权限。
---

## 附录 A：数据库详细设计

> 命名与迁移规则见 ADR-01/03/04/08/09。所有新表统一：`id BIGINT UNSIGNED AUTO_INCREMENT` 主键、`created_at`/`updated_at DATETIME(6)`、`ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`；枚举列一律 `VARCHAR` + `CHECK` 约束；金额价格 `DECIMAL(24,8)`，数量 `DECIMAL(30,8)`。
>
> **字段责任标记**（详设 0 §1.6）：`[S]` = SYSTEM_DERIVED 系统生成、`[U]` = USER_CONFIRMATION 用户确认、`[R]` = REVIEW_REQUIRED 需审核、`[A]` = ADMIN_CONFIG 管理员配置。前端 MUST NOT 把 `[S]` 字段渲染成普通用户的必填输入。

### A.1 表清单总览

#### A.1.1 复用若依底座已有表（**不新建、不改结构**）

| 表 | 本模块用途 | 交互方式 |
|:---|:---|:---|
| `sys_menu` | 挂载「数据与标的」分组菜单与按钮权限 | Flyway `INSERT ... ON DUPLICATE KEY UPDATE` 幂等写入；与 `contract/menu/quantpf-menu-facts.yaml` 双向闭合（ADR-10） |
| `sys_role` / `sys_role_menu` | 三种体验模式的角色绑定（引导型普通用户 / 业务审核用户 / 专业管理用户） | 只读引用 + 角色菜单授权，权限标识沿用总纲 §15.2 的 `quant:*` 命名 |
| `sys_user` / `sys_dept` | 操作者与数据范围 | 只读引用；人工待办的 `assignee` 存 `sys_user.user_id` |
| `sys_dict_type` / `sys_dict_data` | 质量状态、供应商状态、标的状态、容量状态、市场状态、决策结果等枚举字典，供前端下拉与标签渲染 | Flyway 幂等写入字典；后端 MUST 以 Java 枚举为准，字典仅用于展示，不参与业务判定 |
| `sys_oper_log` | 通用操作日志 | 由底座 `@Log` 注解自动写入；本模块**额外**写 `quantpf_audit_record` 承载业务级前后状态与版本（两者并存，不互相替代） |
| `sys_job` / `sys_job_log` | SnailJob 底座调度表 | **不使用**。本模块调度走 XXL-JOB（迁移门禁后）+ `quantpf_job_execution_audit`（已落地），禁止双栈 |

#### A.1.2 已落地 `quantpf_*` 表（本变更只 ALTER 扩展，不改既有约束语义）

| 表 | 迁移 | 本变更的扩展 | 与 Dify 的交互 |
|:---|:---|:---|:---|
| `quantpf_instrument` | `V1_1_0` | 见 A.3.1：加 `market_code`/`company_id`/`security_name`/`listing_status`/`price_tick`/`identity_status`/`current_version_id`；放宽 `product_type` CHECK 至 `STOCK/FUTURE/SPOT`；放宽 `status` CHECK 至详设 1 §11.6 全集 | **只读**，通过 `/api/quant/instrument/**` 白名单字段作为对象解析上下文；Dify MUST NOT 写 |
| `quantpf_trading_calendar` | `V1_1_0` | 见 A.3.2：加 `calendar_code`/`market_timezone`/`pretrade_date`/`next_trade_date`/`source_name`/`endpoint_name`/`raw_is_open`/`calendar_version`/`quality_status` | **禁止访问**。日历是确定性事实，Dify 不参与 |
| `quantpf_market_bar` | `V1_1_0` | 见 A.3.3：加 `adjust_flag`/`quality_status`/`snapshot_id`/`pit_version`/`storage_tier`/`session_template_id` | **禁止访问主表**。Dify 只能读由 Spring Boot 计算好的市场状态与量价状态标签 |
| `quantpf_research_news` | `V1_2_0` | 见 A.3.4：加六时间字段、`source_level`/`authorization_status`/`usage_scope`/`quality_status`/`snapshot_id`/`dedup_hit_reason` | **只读原文与元数据**（作为 Dify 输入材料）；Dify 输出 MUST NOT 写回本表 |
| `quantpf_financial_statement` | `V1_2_0` | 加 `published_at`/`available_at`/`effective_at`/`revised_at`/`pit_version`/`source_level`/`quality_status` | **只读**，作为财报研究材料输入 |
| `quantpf_audit_record` | `V1_7_0` | 无结构变更；新增 `action` 取值（数据源、采集、日历、标的、待办、Dify 相关） | 只写不读；Dify **禁止访问** |
| `quantpf_job_execution_audit` | `V1_7_0` | 无结构变更；本模块采集/日历/就绪/研究任务 Handler 全部写入 | Dify **禁止访问** |
| `quantpf_data_source`（分册 §29.1 建议表） | 本变更新建 | 见 A.2.1 | 存 `authorization_status`/`usage_scope`；Dify **禁止访问**（含授权与凭据画像） |

> `quantpf_trading_account`/`cash_ledger`/`position`/`paper_order`/`paper_execution`/`risk_rule`/`risk_snapshot`/`trading_halt`/`paper_follow_session`/`strategy*`/`backtest*`/`strategy_signal` 属 Paper 与模块三/四范围，**本变更零改动**，仅在联调阶段做回归保护。

#### A.1.3 本变更新增表（22 张）

| # | 表 | 能力 | 与 Dify 的交互方式 |
|:--|:---|:---|:---|
| 1 | `quantpf_data_source` | data-source-governance | **禁止**（含授权、额度、凭据画像） |
| 2 | `quantpf_data_product_endpoint` | data-source-governance | **禁止** |
| 3 | `quantpf_endpoint_field_catalog` | data-source-governance | **禁止** |
| 4 | `quantpf_source_group` | data-source-governance | **禁止** |
| 5 | `quantpf_source_group_member` | data-source-governance | **禁止** |
| 6 | `quantpf_source_cross_check` | data-source-governance | 只读**结论**（冲突/一致标签）可作解释上下文 |
| 7 | `quantpf_collect_task` | data-ingestion-snapshot | **禁止** |
| 8 | `quantpf_data_raw_snapshot` | data-ingestion-snapshot | **只读授权文本**（新闻/公告/研报原文），按 `material_id` 白名单提供给 Dify；行情与结构化数值**禁止** |
| 9 | `quantpf_data_point_in_time` | data-ingestion-snapshot | **禁止** |
| 10 | `quantpf_data_quality_issue` | data-quality-eligibility | 只读**问题结论**供 Dify 生成普通语言解释；Dify MUST NOT 修改状态 |
| 11 | `quantpf_data_usage_eligibility` | data-quality-eligibility | **禁止**（资格判定是确定性结果） |
| 12 | `quantpf_instrument_version` | instrument-identity | **禁止** |
| 13 | `quantpf_instrument_identity_mapping` | instrument-identity | 只读**候选映射**可由 Dify 提出建议，落库仍需人工确认 |
| 14 | `quantpf_corporate_action` | instrument-lifecycle-events | 只读事件事实；Dify 可解释，MUST NOT 改状态 |
| 15 | `quantpf_contract_event` | instrument-lifecycle-events | 只读事件事实；Dify 可解释，MUST NOT 改状态 |
| 16 | `quantpf_trading_calendar_def` | trading-calendar | **禁止** |
| 17 | `quantpf_trading_session_template` | trading-calendar | **禁止** |
| 18 | `quantpf_instrument_calendar_binding` | trading-calendar | **禁止** |
| 19 | `quantpf_calendar_source_snapshot` | trading-calendar | **禁止** |
| 20 | `quantpf_calendar_conflict` | trading-calendar | 只读冲突事实供 Dify 生成解释草稿；裁决由人工完成 |
| 21 | `quantpf_market_data_readiness` | trading-calendar / market-data-foundation | 只读**就绪结论**，用于回答「今天数据到了吗」类问答 |
| 22 | `quantpf_capacity_assessment` | market-data-foundation | 只读容量状态标签；Dify MUST NOT 参与计算 |
| 23 | `quantpf_quant_feature_snapshot` | market-data-foundation | 只读**状态标签**；特征值与算法版本**禁止**改写 |
| 24 | `quantpf_research_material` | research-material-intake | **只读**（统一材料索引，Dify 输入的入口） |
| 25 | `quantpf_research_task` | research-material-intake | Dify **运行关联**：存 `run_id`/`workflow_version`/`prompt_version`/状态；Dify MUST NOT 直接改状态 |
| 26 | `quantpf_ai_research_draft` | research-material-intake | **接收 Dify 输出**的唯一落点（草稿区，非事实主表） |
| 27 | `quantpf_research_event` | research-material-intake | Spring Boot 校验后写入；Dify **禁止直写** |
| 28 | `quantpf_event_evidence` | research-material-intake | Spring Boot 写入；Dify 只读引用定位 |
| 29 | `quantpf_manual_review_task` | 全能力共用（ADR-07） | Dify 可**创建建议**（`task_type=AI_DRAFT_REVIEW`），裁决由人工完成 |
| 30 | `quantpf_idempotency_record` | 全能力共用（ADR-08） | **禁止** |
| 31 | `quantpf_dify_invocation_log` | research-material-intake | 记录请求/响应/校验结果；Dify **禁止读写** |

> 表数多于 22 是因为三张跨能力共用表（29/30/31）在总览里单列。Flyway 迁移按能力分组，见 A.4。

### A.2 新增表详细设计（一）：数据源与来源组

#### A.2.1 `quantpf_data_source` — 供应商 / 数据源

| 字段 | 类型 | 约束 | 责任 | 说明 |
|:---|:---|:---|:--|:---|
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | [S] | |
| `source_code` | VARCHAR(64) | NOT NULL, UNIQUE | [A] | 内部唯一编码，如 `TUSHARE_PRO` |
| `provider_name` | VARCHAR(128) | NOT NULL | [A] | 供应商名称 |
| `product_name` | VARCHAR(128) | NOT NULL | [A] | 数据产品名称 |
| `account_type` | VARCHAR(32) | NOT NULL, CHECK IN (`PERSONAL`,`TEAM`,`ENTERPRISE`,`TRIAL`) | [A] | 账号类型 |
| `contract_ref` | VARCHAR(128) | NULL | [A] | 合同/订阅标识（**不存凭据**） |
| `service_period_start` | DATE | NULL | [A] | 服务起始 |
| `service_period_end` | DATE | NULL | [A] | 到期日，驱动 `EXPIRING`/`EXPIRED` |
| `fee_amount` | DECIMAL(18,2) | NULL | [A] | 费用 |
| `currency` | VARCHAR(8) | NOT NULL DEFAULT 'CNY' | [A] | |
| `status` | VARCHAR(32) | NOT NULL DEFAULT 'DRAFT', CHECK IN (`DRAFT`,`PENDING_CONFIRMATION`,`ACTIVE`,`DEGRADED`,`EXPIRING`,`EXPIRED`,`SUSPENDED`,`RETIRED`) | [S]/[A] | 详设 1 §5.3 状态机 |
| `usage_scope` | VARCHAR(32) | NOT NULL DEFAULT 'PERSONAL_RESEARCH', CHECK IN (`PERSONAL_RESEARCH`,`TEAM_INTERNAL`,`COMMERCIAL_SERVICE`,`NO_REDISTRIBUTION`) | [A]/[R] | 总纲 §5.9 |
| `authorization_status` | VARCHAR(32) | NOT NULL DEFAULT 'UNCONFIRMED', CHECK IN (`UNCONFIRMED`,`CONFIRMED`,`RESTRICTED`,`BLOCKED`) | [R] | |
| `authorization_evidence_ref` | VARCHAR(512) | NULL | [R] | 授权凭证引用（条款截图/合同编号），**不存敏感原文** |
| `permission_profile_ref` | VARCHAR(256) | NULL | [A] | 账号权限画像引用（积分等级等），**MUST NOT 存 Token 明文** |
| `rate_limit_per_min` | INT | NULL | [A] | 调用额度 |
| `concurrency_limit` | INT | NULL | [A] | 并发限制 |
| `retention_scope` | VARCHAR(32) | NULL, CHECK IN (`NO_STORE`,`METADATA_ONLY`,`FULL_TEXT`,`FULL_WITH_HISTORY`) | [A] | 保存范围 |
| `redistribution_allowed` | TINYINT(1) | NOT NULL DEFAULT 0 | [A] | 是否允许再分发 |
| `sla_available` | TINYINT(1) | NOT NULL DEFAULT 0 | [A] | 非官方源置 0，驱动可用性监控 |
| `health_status` | VARCHAR(32) | NOT NULL DEFAULT 'UNKNOWN', CHECK IN (`UNKNOWN`,`HEALTHY`,`DEGRADED`,`UNAVAILABLE`) | [S] | 由健康检查任务写 |
| `last_health_check_at` | DATETIME(6) | NULL | [S] | |
| `fail_count_recent` | INT | NOT NULL DEFAULT 0 | [S] | 近期连续失败数，驱动降级 |
| `degrade_reason` | VARCHAR(512) | NULL | [S] | |
| `fallback_source_id` | BIGINT UNSIGNED | NULL, FK→`quantpf_data_source(id)` | [A] | 备源 |
| `remark` | VARCHAR(512) | NULL | [A] | |
| `created_at`/`updated_at` | DATETIME(6) | NOT NULL | [S] | |

**索引**：`uk_data_source_code(source_code)` UNIQUE；`idx_data_source_status(status)`；`idx_data_source_expiry(service_period_end)`；`idx_data_source_health(health_status)`。

**与 Dify**：**完全禁止**。本表含授权范围、额度与权限画像，属详设 1 §18.3「Dify 禁止负责：决定授权和用途资格」。Dify 需要解释授权条款时，只接收**脱敏后的条款文本片段**作为一次性输入，输出存 `quantpf_ai_research_draft`，不回写本表。

#### A.2.2 `quantpf_data_product_endpoint` — 接口目录

| 字段 | 类型 | 约束 | 责任 |
|:---|:---|:---|:--|
| `id` | BIGINT UNSIGNED | PK | [S] |
| `data_source_id` | BIGINT UNSIGNED | NOT NULL, FK→`quantpf_data_source(id)` | [A] |
| `endpoint_name` | VARCHAR(128) | NOT NULL | [A] |
| `endpoint_purpose` | VARCHAR(32) | NOT NULL, CHECK IN (`CALENDAR`,`INSTRUMENT`,`MARKET_BAR`,`NEWS`,`FINANCIAL`,`RESEARCH_REPORT`,`SUPPLY_DEMAND`,`BASIS`,`FUND_FLOW`,`RULE`) | [A] |
| `permission_confirmed` | TINYINT(1) | NOT NULL DEFAULT 0 | [R] |
| `permission_probe_at` | DATETIME(6) | NULL | [S] |
| `permission_probe_result` | VARCHAR(512) | NULL | [S] |
| `required_points` | INT | NULL | [A] |
| `history_range_from` | DATE | NULL | [A] |
| `history_range_to` | DATE | NULL | [A] |
| `update_frequency` | VARCHAR(32) | NULL | [A] |
| `typical_delay_seconds` | INT | NULL | [A] |
| `max_rows_per_call` | INT | NULL | [A] |
| `response_schema_version` | VARCHAR(64) | NULL | [S]/[A] |
| `enabled` | TINYINT(1) | NOT NULL DEFAULT 0 | [A] |
| `eligible_usage` | VARCHAR(128) | NULL | [R] |
| `created_at`/`updated_at` | DATETIME(6) | NOT NULL | [S] |

**索引**：`uk_endpoint(data_source_id, endpoint_name)` UNIQUE；`idx_endpoint_purpose(endpoint_purpose)`；`idx_endpoint_enabled(enabled)`。
**启用门禁**：`enabled=1` 要求 `permission_confirmed=1` 且至少一条 `quantpf_endpoint_field_catalog` 记录（对应 spec「接口未登记字段目录时不允许启用」）。MUST 由应用层服务校验，数据库以 CHECK 无法表达。
**与 Dify**：**禁止**。

#### A.2.3 `quantpf_endpoint_field_catalog` — 字段目录

`id` PK；`endpoint_id` NOT NULL FK；`field_name` VARCHAR(128) NOT NULL；`field_label` VARCHAR(128)；`data_type` VARCHAR(32) NOT NULL CHECK IN (`STRING`,`INT`,`FLOAT`,`DECIMAL`,`DATE`,`DATETIME`,`BOOL`)；`source_declared_type` VARCHAR(64)（**保留来源声明类型原文**，用于 A.3.2 的 `is_open` 字符串/整数不混用）；`nullable` TINYINT(1) NOT NULL DEFAULT 1；`unit` VARCHAR(32)；`timezone_basis` VARCHAR(64)（如 `GMT`/`Asia/Shanghai`/`America/New_York`）；`semantic_note` VARCHAR(512)；`authorized` TINYINT(1) NOT NULL DEFAULT 0 [R]；`retention_allowed` TINYINT(1) NOT NULL DEFAULT 0 [A]；`created_at`/`updated_at`。
**索引**：`uk_field(endpoint_id, field_name)` UNIQUE；`idx_field_authorized(authorized)`。
**与 Dify**：**禁止**。

#### A.2.4 `quantpf_source_group` — 来源组

`id` PK；`group_code` VARCHAR(64) NOT NULL UNIQUE；`group_type` VARCHAR(32) NOT NULL CHECK IN (`NEWS_POLICY`,`COMPANY_DISCLOSURE`,`RESEARCH_REPORT`,`MARKET_DATA`)；`group_name` VARCHAR(128) NOT NULL；`target_main_source_count` INT NOT NULL DEFAULT 2（总纲 §5.1.2 的 2~3 主来源目标）；`cross_check_required` TINYINT(1) NOT NULL DEFAULT 1；`status` VARCHAR(32) NOT NULL DEFAULT `DRAFT` CHECK IN (`DRAFT`,`ACTIVE`,`SINGLE_SOURCE`,`CONFLICTED`,`DEGRADED`,`RETIRED`)；`historical_completeness` VARCHAR(16) NOT NULL DEFAULT `UNKNOWN` CHECK IN (`COMPLETE`,`PARTIAL`,`UNAVAILABLE`,`UNKNOWN`)（总纲 §5.11 三态）；`degrade_rule` VARCHAR(512)；`created_at`/`updated_at`。
**索引**：`uk_source_group_code(group_code)`；`idx_source_group_type(group_type)`；`idx_source_group_status(status)`。
**与 Dify**：**禁止**（来源组配置属 `[A]` 专业管理员职责）。

#### A.2.5 `quantpf_source_group_member` — 来源组成员

`id` PK；`source_group_id` NOT NULL FK；`data_source_id` NOT NULL FK；`role` VARCHAR(16) NOT NULL CHECK IN (`MAIN`,`BACKUP`,`CROSS_CHECK_ONLY`)；`priority` INT NOT NULL DEFAULT 100；`coverage_scope` VARCHAR(256)；`authorized` TINYINT(1) NOT NULL DEFAULT 0；`enabled` TINYINT(1) NOT NULL DEFAULT 1；`created_at`/`updated_at`。
**索引**：`uk_group_member(source_group_id, data_source_id, role)` UNIQUE；`idx_group_member_group(source_group_id, enabled)`。
**与 Dify**：**禁止**。

#### A.2.6 `quantpf_source_cross_check` — 交叉核验结果

`id` BIGINT UNSIGNED PK；`group_id` NOT NULL FK；`subject_type` VARCHAR(32) NOT NULL CHECK IN (`NEWS`,`FINANCIAL`,`MARKET_BAR`,`CALENDAR`,`SUPPLY_DEMAND`)；`subject_key` VARCHAR(256) NOT NULL（如 `instrument_id:report_period`）；`primary_source_id` NOT NULL FK；`primary_value` VARCHAR(512)；`secondary_source_id` NOT NULL FK；`secondary_value` VARCHAR(512)；`result` VARCHAR(16) NOT NULL CHECK IN (`CONSISTENT`,`CONFLICTED`,`TIME_MISMATCH`,`MISSING`)；`difference_detail` VARCHAR(1024)；`checked_at` DATETIME(6) NOT NULL；`rule_version` VARCHAR(64) NOT NULL；`quality_status_impact` VARCHAR(32)；`review_task_id` BIGINT UNSIGNED NULL；`created_at`。
**索引**：`idx_cross_check_subject(subject_type, subject_key)`；`idx_cross_check_result(result)`；`idx_cross_check_time(checked_at)`。
**与 Dify**：**只读结论**。Dify 可读取 `result` 与 `difference_detail` 用于生成「为什么两个来源不一致、影响什么」的普通语言解释；MUST NOT 修改 `result` 或 `quality_status_impact`。
### A.3 新增表详细设计（二）：采集、快照、点时版本与质量

#### A.3.0 既有表 ALTER（保护已落地能力，只加不改）

```sql
-- V1_10_0: quantpf_instrument 扩展（ADR-03 同源迁移）
ALTER TABLE quantpf_instrument
  ADD COLUMN market_code        VARCHAR(32)  NULL AFTER exchange_code,
  ADD COLUMN company_id         VARCHAR(64)  NULL AFTER market_code,
  ADD COLUMN security_name      VARCHAR(128) NULL AFTER company_id,
  ADD COLUMN listing_status     VARCHAR(32)  NULL AFTER security_name,
  ADD COLUMN price_tick         DECIMAL(20,8) NULL AFTER tick_size,
  ADD COLUMN identity_status    VARCHAR(32)  NOT NULL DEFAULT 'DISCOVERED' AFTER status,
  ADD COLUMN current_version_id BIGINT UNSIGNED NULL AFTER identity_status,
  ADD COLUMN pit_note           VARCHAR(512) NULL;
ALTER TABLE quantpf_instrument DROP CONSTRAINT ck_quantpf_instrument_product_type;
ALTER TABLE quantpf_instrument
  ADD CONSTRAINT ck_quantpf_instrument_product_type CHECK (product_type IN ('STOCK','FUTURE','SPOT'));
ALTER TABLE quantpf_instrument DROP CONSTRAINT ck_quantpf_instrument_status;
ALTER TABLE quantpf_instrument
  ADD CONSTRAINT ck_quantpf_instrument_status
  CHECK (status IN ('DISCOVERED','MAPPING_REQUIRED','VERIFIED','ACTIVE','SUSPENDED','EXPIRED','RETIRED','DELISTED'));
-- 说明：保留 'DELISTED' 以兼容已写入数据；新代码写 'RETIRED'/'EXPIRED'。
-- 既有唯一键 uk_quantpf_instrument_code_exchange 保持不变。
```

> `identity_status` 与 `status` 分离：`status` 是既有的交易可用性（Paper 链路在读），`identity_status` 是详设 1 §11.6 的身份状态机。**MUST NOT 复用 `status` 承载身份语义**，否则会改变已落地 Paper 资格判定行为。

#### A.3.1 `quantpf_instrument_version` — 标的交易属性版本

| 字段 | 类型 | 约束 | 责任 |
|:---|:---|:---|:--|
| `id` | BIGINT UNSIGNED | PK | [S] |
| `instrument_id` | BIGINT UNSIGNED | NOT NULL, FK→`quantpf_instrument(id)` | [S] |
| `version_no` | INT | NOT NULL | [S] |
| `lot_size` | DECIMAL(20,8) | NULL | [A]/[S] |
| `tick_size` | DECIMAL(20,8) | NULL | [A]/[S] |
| `contract_multiplier` | DECIMAL(20,8) | NULL | [A]/[S] |
| `margin_rate` | DECIMAL(10,6) | NULL | [A]/[S] |
| `limit_rule` | VARCHAR(256) | NULL | [A]/[S] |
| `delivery_month` | VARCHAR(16) | NULL | [A] |
| `expiry_date` | DATE | NULL | [A] |
| `last_trade_date` | DATE | NULL | [A] |
| `session_template_id` | BIGINT UNSIGNED | NULL, FK→`quantpf_trading_session_template(id)` | [A] |
| `listing_status` | VARCHAR(32) | NULL | [S] |
| `valid_from` | DATETIME(6) | NOT NULL | [S] |
| `valid_until` | DATETIME(6) | NULL | [S] |
| `change_reason` | VARCHAR(512) | NOT NULL | [R] |
| `source_ref` | VARCHAR(512) | NOT NULL | [S] |
| `contract_event_id` | BIGINT UNSIGNED | NULL | [S] |
| `created_at`/`updated_at` | DATETIME(6) | NOT NULL | [S] |

**索引**：`uk_instrument_version(instrument_id, version_no)` UNIQUE；`idx_instrument_version_valid(instrument_id, valid_from, valid_until)`；`idx_instrument_version_expiry(expiry_date)`。
**约束（应用层）**：新版本 `valid_from` MUST 等于旧版本 `valid_until`；同一 `instrument_id` 的有效期区间 MUST NOT 重叠；`quantpf_instrument.current_version_id` MUST 指向 `valid_until IS NULL` 的那一版。
**与 Dify**：**禁止**。

#### A.3.2 既有 `quantpf_trading_calendar` ALTER + `quantpf_trading_calendar_def`

```sql
-- V1_11_0
ALTER TABLE quantpf_trading_calendar
  ADD COLUMN calendar_code      VARCHAR(32)  NULL AFTER market_code,
  ADD COLUMN market_timezone    VARCHAR(64)  NULL AFTER calendar_code,
  ADD COLUMN pretrade_date      DATE         NULL AFTER is_open,
  ADD COLUMN next_trade_date    DATE         NULL AFTER pretrade_date,
  ADD COLUMN source_name        VARCHAR(64)  NULL AFTER next_trade_date,
  ADD COLUMN endpoint_name      VARCHAR(128) NULL AFTER source_name,
  ADD COLUMN raw_is_open        VARCHAR(16)  NULL AFTER endpoint_name,
  ADD COLUMN raw_is_open_type   VARCHAR(16)  NULL AFTER raw_is_open,
  ADD COLUMN calendar_version   VARCHAR(64)  NULL AFTER raw_is_open_type,
  ADD COLUMN quality_status     VARCHAR(32)  NOT NULL DEFAULT 'AVAILABLE' AFTER calendar_version,
  ADD COLUMN snapshot_id        BIGINT UNSIGNED NULL AFTER quality_status,
  ADD COLUMN updated_at         DATETIME(6)  NULL;
-- 既有 uk_quantpf_calendar_market_date(market_code, trading_date) 保持不变
ALTER TABLE quantpf_trading_calendar
  ADD CONSTRAINT ck_quantpf_calendar_quality
  CHECK (quality_status IN ('AVAILABLE','CONFLICTED','INVALID','STALE','PENDING_REVIEW'));
```

`raw_is_open` + `raw_is_open_type` 是 A.1 spec「开市标记类型不符」与「Tushare 两接口 `is_open` 字符串/整数不混用」的落点：`trade_cal` 写 `raw_is_open_type='STRING'`，`fut_trade_cal` 写 `'INT'`，`hk/us_tradecal` 写 `'INT'`。解析失败时 `is_open` 保持默认值但 `quality_status='INVALID'`，MUST NOT 被下游当作开市。

`quantpf_trading_calendar_def`：`id` PK；`calendar_code` VARCHAR(32) NOT NULL UNIQUE（如 `CN_STOCK_SSE`/`CN_STOCK_SZSE`/`CN_STOCK_BSE`/`CN_FUTURE_CFFEX`/`CN_FUTURE_SHFE`/`CN_FUTURE_CZCE`/`CN_FUTURE_DCE`/`CN_FUTURE_INE`/`CN_FUTURE_GFEX`/`HK_STOCK_HKEX`/`US_STOCK_GENERIC`）；`calendar_name` VARCHAR(128) NOT NULL；`market_type` VARCHAR(16) NOT NULL CHECK IN (`STOCK`,`FUTURE`,`FX`,`INDEX`)；`market_region` VARCHAR(32) NOT NULL；`market_timezone` VARCHAR(64) NOT NULL；`currency` VARCHAR(8)；`coverage_status` VARCHAR(16) NOT NULL DEFAULT `UNCONFIRMED` CHECK IN (`CONFIRMED`,`PARTIAL`,`UNCONFIRMED`,`GAP`)；`coverage_gap_note` VARCHAR(512)；`authorized_source_id` BIGINT UNSIGNED NULL FK；`phase` VARCHAR(16) NOT NULL DEFAULT `PHASE1` CHECK IN (`PHASE1`,`PHASE2_RESERVED`)；`version` VARCHAR(64) NOT NULL；`valid_from` DATE；`created_at`/`updated_at`。
**索引**：`uk_calendar_def_code(calendar_code)`；`idx_calendar_def_phase(phase)`；`idx_calendar_def_coverage(coverage_status)`。
**GFEX 缺口**：`CN_FUTURE_GFEX` 以 `coverage_status='GAP'` + `phase='PHASE1'` 预置一行，`authorized_source_id=NULL`，对应 spec「广期所日历缺口」；港股/美股/外汇行以 `phase='PHASE2_RESERVED'` 预置，一期 MUST NOT 采集。
**与 Dify**：**禁止**。

#### A.3.3 既有 `quantpf_market_bar` ALTER

```sql
-- V1_11_0
ALTER TABLE quantpf_market_bar
  ADD COLUMN adjust_flag        VARCHAR(16)  NOT NULL DEFAULT 'NONE' AFTER turnover,
  ADD COLUMN quality_status     VARCHAR(32)  NOT NULL DEFAULT 'AVAILABLE' AFTER adjust_flag,
  ADD COLUMN snapshot_id        BIGINT UNSIGNED NULL AFTER quality_status,
  ADD COLUMN pit_version        VARCHAR(64)  NULL AFTER snapshot_id,
  ADD COLUMN storage_tier       VARCHAR(16)  NOT NULL DEFAULT 'MYSQL' AFTER pit_version,
  ADD COLUMN session_template_id BIGINT UNSIGNED NULL AFTER storage_tier,
  ADD COLUMN validation_rule_version VARCHAR(64) NULL AFTER session_template_id;
ALTER TABLE quantpf_market_bar
  ADD CONSTRAINT ck_quantpf_market_bar_adjust CHECK (adjust_flag IN ('NONE','FORWARD','BACKWARD')),
  ADD CONSTRAINT ck_quantpf_market_bar_quality CHECK (quality_status IN ('AVAILABLE','DELAYED','PARTIAL','CONFLICTED','STALE','INVALID','PENDING_REVIEW')),
  ADD CONSTRAINT ck_quantpf_market_bar_tier CHECK (storage_tier IN ('MYSQL','TSDB'));
-- 既有 uk_quantpf_market_bar(instrument_id, timeframe, bar_time) 与 FK 保持不变
```

> ADR-02：一期 `timeframe` 只写 `D1`；ADR-09：`storage_tier` 一期恒为 `MYSQL`。`adjust_flag` 默认 `NONE`（非复权），前复权序列以 `FORWARD` 独立存储，**MUST NOT 覆盖 `NONE` 行**（对应 spec「复权价与非复权价不得混为同一序列」）。

#### A.3.4 既有 `quantpf_research_news` ALTER

```sql
-- V1_12_0
ALTER TABLE quantpf_research_news
  ADD COLUMN published_at    DATETIME(6) NULL,
  ADD COLUMN available_at    DATETIME(6) NULL,
  ADD COLUMN effective_at    DATETIME(6) NULL,
  ADD COLUMN observed_at     DATETIME(6) NULL,
  ADD COLUMN revised_at      DATETIME(6) NULL,
  ADD COLUMN pit_version     VARCHAR(64) NULL,
  ADD COLUMN source_level    VARCHAR(8)  NOT NULL DEFAULT 'D' ,
  ADD COLUMN authorization_status VARCHAR(32) NOT NULL DEFAULT 'UNCONFIRMED',
  ADD COLUMN usage_scope     VARCHAR(32) NOT NULL DEFAULT 'PERSONAL_RESEARCH',
  ADD COLUMN quality_status  VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW',
  ADD COLUMN time_visibility_status VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
  ADD COLUMN snapshot_id     BIGINT UNSIGNED NULL,
  ADD COLUMN dedup_hit_reason VARCHAR(64) NULL,
  ADD COLUMN transport_delay_seconds INT NULL,
  ADD COLUMN material_id     BIGINT UNSIGNED NULL;
ALTER TABLE quantpf_research_news
  ADD CONSTRAINT ck_quantpf_news_source_level CHECK (source_level IN ('A','B','C','D')),
  ADD CONSTRAINT ck_quantpf_news_quality CHECK (quality_status IN ('AVAILABLE','DELAYED','PARTIAL','CONFLICTED','UNAUTHORIZED','STALE','INVALID','PENDING_REVIEW')),
  ADD CONSTRAINT ck_quantpf_news_time_vis CHECK (time_visibility_status IN ('VALID','DELAYED_ARRIVAL','MISSING_PUBLISHED_AT','UNKNOWN'));
-- 既有 external_id+source、content_hash 去重约束保持不变
```

`transport_delay_seconds` = `available_at - published_at`，由采集服务计算写入，驱动 spec「新闻晚于市场反应到达」。`material_id` 反向指向 `quantpf_research_material`（ADR-04）。

#### A.3.5 `quantpf_collect_task` — 采集任务

`id` PK；`task_code` VARCHAR(64) NOT NULL UNIQUE；`task_type` VARCHAR(32) NOT NULL CHECK IN (`SOURCE_HEALTH`,`INSTRUMENT_SYNC`,`CALENDAR_SYNC`,`FX_SESSION_SYNC`,`NEWS_CRAWL`,`DISCLOSURE_CRAWL`,`FINANCIAL_CRAWL`,`MARKET_BAR_IMPORT`,`SNAPSHOT_ARCHIVE`,`QUALITY_CHECK`,`READINESS_CALC`,`FEATURE_CALC`,`CAPACITY_CALC`,`RESEARCH_TASK_CREATE`,`DIFY_VALIDATE_COMPENSATE`,`EVENT_PUBLISH_COMPENSATE`)；`data_source_id` FK NULL；`endpoint_id` FK NULL；`source_group_id` FK NULL；`scope_params_json` JSON NOT NULL（采集范围与请求参数）；`status` VARCHAR(24) NOT NULL DEFAULT `CREATED` CHECK IN (`CREATED`,`RUNNING`,`SUCCEEDED`,`PARTIAL_SUCCESS`,`FAILED`,`RETRYING`,`CANCELED`)；`requested_count` INT NOT NULL DEFAULT 0；`succeeded_count` INT NOT NULL DEFAULT 0；`failed_count` INT NOT NULL DEFAULT 0；`dedup_hit_count` INT NOT NULL DEFAULT 0；`retry_count` INT NOT NULL DEFAULT 0；`max_retry` INT NOT NULL DEFAULT 3；`failure_reason` VARCHAR(1024) NULL；`idempotency_key` VARCHAR(128) NOT NULL UNIQUE；`correlation_id` VARCHAR(64) NULL；`job_execution_audit_id` BIGINT UNSIGNED NULL；`started_at`/`finished_at` DATETIME(6) NULL；`created_at`/`updated_at`。
**索引**：`uk_collect_task_code(task_code)`；`uk_collect_task_idem(idempotency_key)`；`idx_collect_task_status(status, created_at)`；`idx_collect_task_source(data_source_id, status)`；`idx_collect_task_type_time(task_type, created_at)`。
**与 Dify**：**禁止**（`RESEARCH_TASK_CREATE` 类型只记录任务编排状态，不含 Dify 凭据）。

#### A.3.6 `quantpf_data_raw_snapshot` — 原始快照

`id` PK；`collect_task_id` FK NOT NULL；`data_source_id` FK NOT NULL；`endpoint_name` VARCHAR(128) NOT NULL；`request_params_json` JSON NOT NULL；`response_schema_version` VARCHAR(64) NULL；`raw_payload_hash` CHAR(64) NOT NULL（SHA-256）；`raw_payload_ref` VARCHAR(512) NOT NULL（对象存储/文件路径引用，**大文本不入库**）；`raw_payload_inline` MEDIUMTEXT NULL（仅小体积响应内联，受 `quantpf.snapshot.inline-max-bytes` 控制）；`payload_kind` VARCHAR(16) NOT NULL CHECK IN (`JSON`,`XML`,`HTML`,`TEXT`,`FILE`)；`content_type` VARCHAR(64)；`external_id` VARCHAR(191) NULL；`retrieved_at` DATETIME(6) NOT NULL；`source_observed_at` DATETIME(6) NULL；`published_at` DATETIME(6) NULL；`account_permission_profile` VARCHAR(256) NULL（**MUST NOT 存 Token 明文**）；`parse_status` VARCHAR(16) NOT NULL DEFAULT `PENDING` CHECK IN (`PENDING`,`PARSED`,`INVALID`)；`parse_error` VARCHAR(1024) NULL；`superseded_by_id` BIGINT UNSIGNED NULL FK→self（修订链）；`created_at`。
**索引**：`uk_snapshot_hash(raw_payload_hash, endpoint_name)` UNIQUE（去重主路径之一）；`idx_snapshot_task(collect_task_id)`；`idx_snapshot_external(endpoint_name, external_id)`；`idx_snapshot_retrieved(retrieved_at)`；`idx_snapshot_parse(parse_status)`。
**与 Dify**：**只读授权文本**。Dify 通过 `material_id` → 快照白名单接口读取 `raw_payload_inline` 或 `raw_payload_ref` 指向的新闻/公告/研报正文；MUST NOT 读取行情类、日历类、授权类快照（按 `endpoint_name` 白名单过滤，见附录 C.4）。

#### A.3.7 `quantpf_data_point_in_time` — 点时版本

`id` PK；`subject_type` VARCHAR(32) NOT NULL CHECK IN (`MARKET_BAR`,`FINANCIAL`,`SUPPLY_DEMAND`,`BASIS`,`CALENDAR`,`INSTRUMENT_VERSION`,`NEWS`,`RULE`)；`subject_id` BIGINT UNSIGNED NOT NULL；`snapshot_id` BIGINT UNSIGNED NULL FK；`pit_version` VARCHAR(64) NOT NULL；`visible_at` DATETIME(6) NOT NULL（决策时点可见时间）；`effective_at` DATETIME(6) NULL；`revised_at` DATETIME(6) NULL；`revision_no` INT NOT NULL DEFAULT 0；`superseded` TINYINT(1) NOT NULL DEFAULT 0；`payload_ref` VARCHAR(512) NULL；`created_at`。
**索引**：`uk_pit(subject_type, subject_id, pit_version)` UNIQUE；`idx_pit_visible(subject_type, visible_at)`；`idx_pit_superseded(superseded)`。
**语义**：任何修订 MUST 新增行 + 把旧行 `superseded=1`，MUST NOT UPDATE 覆盖 `payload_ref`（spec「统计数据被官方修订」「回测读取历史点时版本」）。
**与 Dify**：**禁止**。

#### A.3.8 `quantpf_data_quality_issue` — 数据质量问题

`id` PK；`issue_code` VARCHAR(64) NOT NULL UNIQUE；`subject_type` VARCHAR(32) NOT NULL；`subject_id` BIGINT UNSIGNED NULL；`subject_ref` VARCHAR(256) NULL；`data_source_id` FK NULL；`issue_type` VARCHAR(32) NOT NULL CHECK IN (`MISSING`,`CONFLICT`,`DELAY`,`INVALID_VALUE`,`SCHEMA_CHANGE`,`PARSE_FAILURE`,`TIME_VISIBILITY`,`AUTHORIZATION`,`HISTORY_INCOMPLETE`,`IDENTITY_UNRESOLVED`,`SOURCE_UNAVAILABLE`)；`severity` VARCHAR(16) NOT NULL CHECK IN (`INFO`,`MINOR`,`MAJOR`,`CRITICAL`)；`status` VARCHAR(24) NOT NULL DEFAULT `OPEN` CHECK IN (`OPEN`,`ANALYZING`,`WAITING_SOURCE`,`WAITING_HUMAN`,`RESOLVED`,`VERIFIED`,`CLOSED`)；`business_impact` VARCHAR(512) NOT NULL；`affected_usage` VARCHAR(128) NULL（受影响的用途资格，逗号分隔）；`assignee_id` BIGINT UNSIGNED NULL；`resolution_action` VARCHAR(512) NULL；`resolution_basis` VARCHAR(512) NULL；`fix_version` VARCHAR(64) NULL；`review_task_id` BIGINT UNSIGNED NULL；`opened_at`/`resolved_at`/`verified_at`/`closed_at` DATETIME(6) NULL；`created_at`/`updated_at`。
**索引**：`uk_quality_issue_code(issue_code)`；`idx_quality_issue_status(status, severity)`；`idx_quality_issue_subject(subject_type, subject_id)`；`idx_quality_issue_assignee(assignee_id, status)`；`idx_quality_issue_type(issue_type)`。
**约束（应用层）**：状态转移 MUST 遵循 `OPEN→ANALYZING→WAITING_*→RESOLVED→VERIFIED→CLOSED`，MUST NOT 从 `RESOLVED` 直接跳 `CLOSED`（spec「质量问题修复后需验证」）。
**与 Dify**：**只读结论**。Dify 可读 `issue_type`/`business_impact`/`affected_usage` 生成普通语言解释；MUST NOT 改 `status` 或 `resolution_*`。
#### A.3.9 `quantpf_data_usage_eligibility` — 用途资格留痕（ADR-06）

`id` PK；`subject_type` VARCHAR(32) NOT NULL；`subject_id` BIGINT UNSIGNED NOT NULL；`as_of_date` DATE NOT NULL；`quality_status` VARCHAR(32) NOT NULL；`authorization_status` VARCHAR(32) NOT NULL；`time_visibility_status` VARCHAR(32) NOT NULL；`instrument_status` VARCHAR(32) NOT NULL；`market_session_status` VARCHAR(32) NOT NULL；`capacity_status` VARCHAR(32) NOT NULL；`business_usage_status` VARCHAR(32) NOT NULL；`allow_display` TINYINT(1) NOT NULL；`allow_research` TINYINT(1) NOT NULL；`allow_screening` TINYINT(1) NOT NULL；`allow_backtest` TINYINT(1) NOT NULL；`allow_paper_open` TINYINT(1) NOT NULL；`blocked_reasons_json` JSON NOT NULL（**逐项**受限原因，对应 spec「资格查询返回全部受限原因」）；`plain_language_reason` VARCHAR(1024) NOT NULL（普通语言解释，前端第一层直接展示）；`rule_version` VARCHAR(64) NOT NULL；`requested_by` VARCHAR(64) NULL；`created_at`。
**索引**：`uk_eligibility(subject_type, subject_id, as_of_date, rule_version)` UNIQUE；`idx_eligibility_paper(allow_paper_open, as_of_date)`；`idx_eligibility_subject(subject_type, subject_id, as_of_date)`。
**语义**：五个 `allow_*` 是**实时计算结果的留痕**，不是权威状态；权威判定永远走 `UsageEligibilityDecider`。留痕用于审计回放「当时为什么放行/阻断」。
**与 Dify**：**禁止**。

#### A.3.10 `quantpf_instrument_identity_mapping` — 身份映射候选

`id` PK；`instrument_id` FK NOT NULL；`external_code` VARCHAR(64) NOT NULL；`external_exchange` VARCHAR(32) NULL；`data_source_id` FK NOT NULL；`mapping_status` VARCHAR(24) NOT NULL DEFAULT `PENDING` CHECK IN (`PENDING`,`SUGGESTED`,`CONFIRMED`,`REJECTED`,`AMBIGUOUS`)；`suggested_by` VARCHAR(16) NOT NULL CHECK IN (`SYSTEM`,`DIFY`,`HUMAN`)；`suggestion_payload_json` JSON NULL；`confidence_note` VARCHAR(256) NULL；`review_task_id` BIGINT UNSIGNED NULL；`confirmed_by` BIGINT UNSIGNED NULL；`confirmed_at` DATETIME(6) NULL；`confirm_basis` VARCHAR(512) NULL；`created_at`/`updated_at`。
**索引**：`idx_identity_mapping_instrument(instrument_id, mapping_status)`；`idx_identity_mapping_external(external_code, external_exchange)`；`idx_identity_mapping_status(mapping_status)`。
**语义**：Dify 可以 `suggested_by='DIFY'` 提出映射**建议**，但 `mapping_status` 只能由人工审核推进到 `CONFIRMED`（spec「标的核验通过」「智能体只解释不改状态」）。
**与 Dify**：**可写建议、不可写裁决**。Dify 通过受控接口提交 `SUGGESTED` 记录；MUST NOT 直接置 `CONFIRMED`。

#### A.3.11 `quantpf_corporate_action` — 公司行为

`id` PK；`instrument_id` FK NOT NULL；`action_type` VARCHAR(32) NOT NULL CHECK IN (`EX_DIVIDEND`,`CASH_DIVIDEND`,`BONUS_SHARES`,`SUSPENSION`,`RESUMPTION`,`ST_WARNING`,`DELIST_RISK`,`RESTRUCTURING`,`EARNINGS_DATE`,`RIGHTS_ISSUE`,`OTHER`)；`announce_at` DATETIME(6) NULL；`effective_date` DATE NOT NULL；`end_date` DATE NULL；`impact_params_json` JSON NULL（除权比例、分红金额等）；`report_period` VARCHAR(16) NULL；`source_ref` VARCHAR(512) NOT NULL；`source_level` VARCHAR(8) NOT NULL DEFAULT 'A'；`snapshot_id` BIGINT UNSIGNED NULL；`status` VARCHAR(24) NOT NULL DEFAULT `PENDING_VERIFY` CHECK IN (`PENDING_VERIFY`,`VERIFIED`,`PENDING_EFFECTIVE`,`EFFECTIVE`,`INVALIDATED`,`CANCELED`)；`quality_status` VARCHAR(32) NOT NULL DEFAULT `PENDING_REVIEW`；`event_published` TINYINT(1) NOT NULL DEFAULT 0；`idempotency_key` VARCHAR(128) NOT NULL UNIQUE；`created_at`/`updated_at`。
**索引**：`uk_corporate_action_idem(idempotency_key)`；`idx_corporate_action_instrument(instrument_id, effective_date)`；`idx_corporate_action_type_date(action_type, effective_date)`；`idx_corporate_action_status(status)`。
**与 Dify**：**只读事件事实**，可生成解释；MUST NOT 改 `status`。

#### A.3.12 `quantpf_contract_event` — 期货合约与制度事件

`id` PK；`instrument_id` FK NULL（品种级事件可为空）；`product_code` VARCHAR(32) NULL；`exchange_code` VARCHAR(32) NOT NULL；`event_type` VARCHAR(32) NOT NULL CHECK IN (`LISTING`,`ENTER_DELIVERY_MONTH`,`LAST_TRADING_DAY`,`EXPIRY`,`ROLL_WINDOW`,`MARGIN_CHANGE`,`LIMIT_CHANGE`,`SESSION_CHANGE`,`POSITION_LIMIT_CHANGE`,`RULE_CHANGE`,`DELISTING`)；`effective_date` DATE NOT NULL；`end_date` DATE NULL；`impact_params_json` JSON NULL；`rule_version` VARCHAR(64) NULL；`source_ref` VARCHAR(512) NOT NULL；`source_level` VARCHAR(8) NOT NULL DEFAULT 'A'；`snapshot_id` BIGINT UNSIGNED NULL；`status` VARCHAR(24) NOT NULL DEFAULT `PENDING_VERIFY` CHECK IN (`PENDING_VERIFY`,`VERIFIED`,`PENDING_EFFECTIVE`,`EFFECTIVE`,`INVALIDATED`,`CANCELED`)；`advance_notice_days` INT NOT NULL DEFAULT 10；`todo_generated` TINYINT(1) NOT NULL DEFAULT 0；`idempotency_key` VARCHAR(128) NOT NULL UNIQUE；`created_at`/`updated_at`。
**索引**：`uk_contract_event_idem(idempotency_key)`；`idx_contract_event_instrument(instrument_id, effective_date)`；`idx_contract_event_product(exchange_code, product_code, effective_date)`；`idx_contract_event_type(event_type, effective_date)`。
**与 Dify**：**只读事件事实**；MUST NOT 改 `status` 或 `rule_version`。

### A.4 新增表详细设计（三）：日历、时段、就绪与冲突

#### A.4.1 `quantpf_trading_session_template`

`id` PK；`template_code` VARCHAR(64) NOT NULL UNIQUE；`template_name` VARCHAR(128) NOT NULL；`market_type` VARCHAR(16) NOT NULL CHECK IN (`STOCK`,`FUTURE`,`FX`,`INDEX`)；`session_type` VARCHAR(24) NOT NULL CHECK IN (`REGULAR`,`LUNCH_BREAK`,`CALL_AUCTION`,`NIGHT`,`CROSS_DAY`,`SPECIAL`,`HALF_DAY`)；`open_time` TIME NULL；`close_time` TIME NULL；`crosses_midnight` TINYINT(1) NOT NULL DEFAULT 0；`trading_day_attribution` VARCHAR(16) NOT NULL DEFAULT `SAME_DAY` CHECK IN (`SAME_DAY`,`NEXT_TRADING_DAY`)（夜盘归属）；`timezone` VARCHAR(64) NOT NULL；`version` VARCHAR(64) NOT NULL；`valid_from` DATE NOT NULL；`valid_until` DATE NULL；`phase` VARCHAR(16) NOT NULL DEFAULT `PHASE1` CHECK IN (`PHASE1`,`PHASE2_RESERVED`)；`created_at`/`updated_at`。
**索引**：`uk_session_template_code(template_code)`；`idx_session_template_market(market_type, session_type)`；`idx_session_template_valid(valid_from, valid_until)`。
**与 Dify**：**禁止**。

#### A.4.2 `quantpf_instrument_calendar_binding`

`id` PK；`instrument_id` FK NULL；`product_code` VARCHAR(32) NULL；`exchange_code` VARCHAR(32) NULL；`calendar_code` VARCHAR(32) NOT NULL；`session_template_id` FK NOT NULL；`binding_level` VARCHAR(16) NOT NULL CHECK IN (`INSTRUMENT`,`PRODUCT`,`EXCHANGE`)；`tradable` TINYINT(1) NOT NULL DEFAULT 1；`binding_status` VARCHAR(24) NOT NULL DEFAULT `PENDING` CHECK IN (`PENDING`,`CONFIRMED`,`UNRESOLVED`)；`source_ref` VARCHAR(512) NULL；`valid_from` DATE NOT NULL；`valid_until` DATE NULL；`created_at`/`updated_at`。
**索引**：`uk_binding(instrument_id, calendar_code, session_template_id, valid_from)` UNIQUE；`idx_binding_calendar(calendar_code, binding_level)`；`idx_binding_product(exchange_code, product_code)`；`idx_binding_status(binding_status)`。
**语义**：`binding_status='UNRESOLVED'` 时可交易资格 MUST 返回「无法判定」（spec「未绑定日历的标的」）。
**与 Dify**：**禁止**。

#### A.4.3 `quantpf_calendar_source_snapshot`

`id` PK；`calendar_code` VARCHAR(32) NOT NULL；`data_source_id` FK NOT NULL；`endpoint_name` VARCHAR(128) NOT NULL；`request_params_json` JSON NOT NULL；`response_schema_version` VARCHAR(64) NULL；`raw_payload_hash` CHAR(64) NOT NULL；`raw_payload_ref` VARCHAR(512) NULL；`retrieved_at` DATETIME(6) NOT NULL；`source_observed_at` DATETIME(6) NULL；`account_permission_profile` VARCHAR(256) NULL；`covered_date_from` DATE NOT NULL；`covered_date_to` DATE NOT NULL；`row_count` INT NOT NULL DEFAULT 0；`parse_status` VARCHAR(16) NOT NULL DEFAULT `PENDING` CHECK IN (`PENDING`,`PARSED`,`INVALID`)；`collect_task_id` FK NULL；`created_at`。
**索引**：`uk_cal_snapshot_hash(raw_payload_hash, endpoint_name)` UNIQUE；`idx_cal_snapshot_calendar(calendar_code, covered_date_from, covered_date_to)`；`idx_cal_snapshot_source(data_source_id, retrieved_at)`。
**与 Dify**：**禁止**。

#### A.4.4 `quantpf_calendar_conflict`

`id` PK；`conflict_code` VARCHAR(64) NOT NULL UNIQUE；`calendar_code` VARCHAR(32) NOT NULL；`calendar_date` DATE NOT NULL；`primary_source_id` FK NOT NULL；`primary_is_open` TINYINT(1) NULL；`primary_raw_value` VARCHAR(16) NULL；`secondary_source_id` FK NOT NULL；`secondary_is_open` TINYINT(1) NULL；`secondary_raw_value` VARCHAR(16) NULL；`conflict_type` VARCHAR(24) NOT NULL CHECK IN (`OPEN_MISMATCH`,`DATE_MISSING`,`TIMEZONE_MISMATCH`,`PRETRADE_MISMATCH`,`COVERAGE_GAP`)；`impact_scope_json` JSON NOT NULL（受影响标的、研究任务、候选筛选、Paper 资格范围）；`status` VARCHAR(24) NOT NULL DEFAULT `OPEN` CHECK IN (`OPEN`,`ANALYZING`,`WAITING_HUMAN`,`RESOLVED`,`VERIFIED`,`CLOSED`)；`resolution_source_id` BIGINT UNSIGNED NULL；`review_task_id` BIGINT UNSIGNED NULL；`detected_at` DATETIME(6) NOT NULL；`resolved_at` DATETIME(6) NULL；`rule_version` VARCHAR(64) NOT NULL；`created_at`/`updated_at`。
**索引**：`uk_calendar_conflict_code(conflict_code)`；`uk_calendar_conflict_date(calendar_code, calendar_date, conflict_type)` UNIQUE；`idx_calendar_conflict_status(status)`；`idx_calendar_conflict_detected(detected_at)`。
**语义**：冲突未 `RESOLVED` 前，对应 `quantpf_trading_calendar.quality_status` MUST 为 `CONFLICTED`，且系统 MUST NOT 静默择一（spec「两个来源对同一日期结论不同」）。
**与 Dify**：**只读冲突事实**，可生成解释草稿；裁决 MUST 由人工完成。

#### A.4.5 `quantpf_market_data_readiness`

`id` PK；`calendar_code` VARCHAR(32) NOT NULL；`trade_date` DATE NOT NULL；`instrument_id` FK NULL（NULL 表示市场级汇总）；`expected` TINYINT(1) NOT NULL（应到）；`arrived` TINYINT(1) NOT NULL（已到）；`validated` TINYINT(1) NOT NULL（已校验）；`strategy_ready` TINYINT(1) NOT NULL；`paper_ready` TINYINT(1) NOT NULL；`data_basis_date` DATE NULL；`freshness_status` VARCHAR(24) NOT NULL DEFAULT `UNKNOWN` CHECK IN (`FRESH`,`STALE`,`NOT_DUE`,`NOT_ARRIVED`,`PARTIAL`,`UNKNOWN`)；`arrived_ratio` DECIMAL(8,4) NULL（部分覆盖比例）；`blocking_reasons_json` JSON NULL；`plain_language_note` VARCHAR(1024) NULL；`rule_version` VARCHAR(64) NOT NULL；`calculated_at` DATETIME(6) NOT NULL；`created_at`/`updated_at`。
**索引**：`uk_readiness(calendar_code, trade_date, instrument_id)` UNIQUE（`instrument_id` 为 NULL 时以 `0` 占位以保证唯一键生效）；`idx_readiness_paper(paper_ready, trade_date)`；`idx_readiness_freshness(freshness_status, trade_date)`；`idx_readiness_instrument(instrument_id, trade_date)`。
**语义**：交易日与就绪状态**分离**判定；非交易日 `expected=0` 且 MUST NOT 生成缺失告警（spec「非交易日不产生就绪缺口」）。ADR-02：一期 `freshness_status` 基于**数据基准日期 vs 日历应到日期**，不基于墙钟秒级窗口。
**与 Dify**：**只读就绪结论**，用于回答「今天数据到了吗 / 为什么不能建仓」类问答。

#### A.4.6 `quantpf_capacity_assessment`

`id` PK；`instrument_id` FK NOT NULL；`assess_date` DATE NOT NULL；`product_type` VARCHAR(16) NOT NULL；`avg_turnover_20d` DECIMAL(30,8) NULL；`avg_volume_20d` DECIMAL(30,8) NULL；`avg_spread` DECIMAL(24,8) NULL；`turnover_rate` DECIMAL(12,6) NULL；`float_market_cap` DECIMAL(30,8) NULL；`limit_hit_frequency` DECIMAL(8,4) NULL；`suspension_risk_flag` TINYINT(1) NOT NULL DEFAULT 0；`open_interest` DECIMAL(30,8) NULL；`tick_value` DECIMAL(24,8) NULL；`margin_rate` DECIMAL(10,6) NULL；`night_liquidity_note` VARCHAR(256) NULL；`days_to_delivery` INT NULL；`roll_cost_estimate` DECIMAL(24,8) NULL；`plan_amount_ratio_limit` DECIMAL(8,6) NULL（计划金额占成交额比例上限）；`capacity_status` VARCHAR(16) NOT NULL CHECK IN (`SUFFICIENT`,`LIMITED`,`HIGH_IMPACT`,`UNTRADEABLE`,`UNKNOWN`)；`max_plan_amount` DECIMAL(30,8) NULL；`estimated_slippage` DECIMAL(24,8) NULL；`stop_loss_executable` TINYINT(1) NULL；`blocking_reasons_json` JSON NULL；`plain_language_note` VARCHAR(1024) NULL；`rule_version` VARCHAR(64) NOT NULL；`data_basis_date` DATE NOT NULL；`created_at`/`updated_at`。
**索引**：`uk_capacity(instrument_id, assess_date, rule_version)` UNIQUE；`idx_capacity_status(capacity_status, assess_date)`；`idx_capacity_delivery(days_to_delivery)`。
**语义**：`UNTRADEABLE`/`UNKNOWN` MUST 阻断新开仓计划；`HIGH_IMPACT` 只允许观察或低规模 Paper（spec「不可交易或未知」「高冲击成本」）。
**与 Dify**：**只读状态标签**；MUST NOT 参与计算或修改。

#### A.4.7 `quantpf_quant_feature_snapshot`（含二期预留）

`id` PK；`instrument_id` FK NOT NULL；`trade_date` DATE NOT NULL；`timeframe` VARCHAR(16) NOT NULL DEFAULT `D1`；`feature_name` VARCHAR(64) NOT NULL；`feature_value` DECIMAL(30,10) NULL；`feature_category` VARCHAR(32) NOT NULL CHECK IN (`PRICE_VOLUME`,`RELATIVE_STRENGTH`,`VOLATILITY`,`LIQUIDITY`,`POSITION_STRUCTURE`,`BASIS`,`MARKET_STATE`)；`observation_window` VARCHAR(32) NULL；`signal_time` DATETIME(6) NULL；`market_state` VARCHAR(32) NULL；`volume_price_state` VARCHAR(32) NULL（总纲 §7.4 的 `ACCUMULATION_CANDIDATE`/`BREAKOUT_CONFIRM`/... 全集）；`algorithm_version` VARCHAR(64) NOT NULL；`data_version` VARCHAR(64) NOT NULL；`pit_version` VARCHAR(64) NULL；**二期预留**：`success_label` VARCHAR(16) NULL、`failure_label` VARCHAR(16) NULL、`label_horizon_days` INT NULL、`cost_estimate` DECIMAL(24,8) NULL、`slippage_estimate` DECIMAL(24,8) NULL、`replay_ref` VARCHAR(512) NULL、`phase` VARCHAR(16) NOT NULL DEFAULT `PHASE1` CHECK IN (`PHASE1`,`PHASE2_RESERVED`)；`created_at`。
**索引**：`uk_feature(instrument_id, trade_date, timeframe, feature_name, algorithm_version)` UNIQUE；`idx_feature_state(volume_price_state, trade_date)`；`idx_feature_market_state(market_state, trade_date)`；`idx_feature_phase(phase)`。
**语义**：一期只写 `phase='PHASE1'` 且 `success_label`/`failure_label`/`replay_ref` 恒为 NULL；MUST NOT 基于预留字段生成时机结论或订单（spec「二期字段仅预留」「量价状态不直接变成订单」）。
**与 Dify**：**只读状态标签**（`volume_price_state`/`market_state`）用于解释；特征值与算法版本 MUST NOT 被改写。
### A.5 新增表详细设计（四）：研究材料、任务、Dify 草稿与研究事件

#### A.5.1 `quantpf_research_material` — 统一材料索引（ADR-04）

`id` PK；`material_code` VARCHAR(64) NOT NULL UNIQUE；`material_type` VARCHAR(24) NOT NULL CHECK IN (`NEWS`,`ANNOUNCEMENT`,`FINANCIAL_REPORT`,`RESEARCH_REPORT`,`INDUSTRY_DATA`,`POLICY`,`RULE_DOC`)；`source_table` VARCHAR(64) NOT NULL（如 `quantpf_research_news`）；`source_id` BIGINT UNSIGNED NOT NULL；`title` VARCHAR(512) NULL；`data_source_id` FK NOT NULL；`source_group_id` FK NULL；`source_level` VARCHAR(8) NOT NULL CHECK IN (`A`,`B`,`C`,`D`)；`snapshot_id` BIGINT UNSIGNED NULL FK；`content_hash` CHAR(64) NOT NULL；`external_id` VARCHAR(191) NULL；`published_at`/`available_at`/`effective_at`/`observed_at`/`revised_at` DATETIME(6) NULL；`transport_delay_seconds` INT NULL；`time_visibility_status` VARCHAR(32) NOT NULL DEFAULT `UNKNOWN`；`authorization_status` VARCHAR(32) NOT NULL DEFAULT `UNCONFIRMED`；`usage_scope` VARCHAR(32) NOT NULL DEFAULT `PERSONAL_RESEARCH`；`quality_status` VARCHAR(32) NOT NULL DEFAULT `PENDING_REVIEW`；`dedup_status` VARCHAR(24) NOT NULL DEFAULT `NEW` CHECK IN (`NEW`,`DUP_EXTERNAL_ID`,`DUP_CONTENT_HASH`,`REPRINT_MERGED`)；`dedup_ref_material_id` BIGINT UNSIGNED NULL FK→self（转载归并指向原始材料）；`industry_mapping_json` JSON NULL；`instrument_mapping_status` VARCHAR(24) NOT NULL DEFAULT `UNRESOLVED` CHECK IN (`UNRESOLVED`,`PARTIAL`,`RESOLVED`,`AMBIGUOUS`)；`eligible_for_task` TINYINT(1) NOT NULL DEFAULT 0；`created_at`/`updated_at`。
**索引**：`uk_material_code(material_code)`；`uk_material_source(source_table, source_id)` UNIQUE（防重复索引）；`uk_material_hash(content_hash, material_type)` UNIQUE（内容去重主路径）；`idx_material_external(external_id, data_source_id)`；`idx_material_task_eligible(eligible_for_task, quality_status)`；`idx_material_published(published_at)`；`idx_material_dedup(dedup_status)`。
**与 Dify**：**只读**。Dify 通过 `material_id` 拉取标题、正文引用、时间与来源等级；MUST NOT 写本表任何字段。

#### A.5.2 `quantpf_research_task` — 研究任务

`id` PK；`task_code` VARCHAR(64) NOT NULL UNIQUE；`task_type` VARCHAR(32) NOT NULL CHECK IN (`NEWS_FACT_EXTRACT`,`EVENT_CLASSIFY`,`DISCLOSURE_ANALYZE`,`FINANCIAL_EXPLAIN`,`SUPPLY_DEMAND_EXPLAIN`,`QA_OPPORTUNITY`,`QA_INSTRUMENT`,`DOC_EXPLAIN`,`QUALITY_EXPLAIN`,`COUNTER_ARGUMENT`)；`material_id` FK NOT NULL；`snapshot_id` BIGINT UNSIGNED NULL；`subject_refs_json` JSON NULL（标的/行业/品种引用）；`status` VARCHAR(24) NOT NULL DEFAULT `CREATED` CHECK IN (`CREATED`,`CONTEXT_FROZEN`,`DISPATCHED`,`RUNNING`,`DRAFT_RECEIVED`,`VALIDATING`,`VALIDATED`,`VALIDATION_FAILED`,`UPGRADED`,`PENDING_REVIEW`,`RETRYING`,`FAILED`,`CANCELED`,`TIMEOUT`)；`idempotency_key` VARCHAR(128) NOT NULL UNIQUE；`correlation_id` VARCHAR(64) NULL；`causation_id` VARCHAR(64) NULL；`workflow_id` VARCHAR(128) NULL；`workflow_version` VARCHAR(64) NULL；`prompt_version` VARCHAR(64) NULL；`dify_run_id` VARCHAR(128) NULL；`dispatched_at`/`completed_at` DATETIME(6) NULL；`timeout_seconds` INT NOT NULL DEFAULT 120；`retry_count` INT NOT NULL DEFAULT 0；`max_retry` INT NOT NULL DEFAULT 2；`failure_reason` VARCHAR(1024) NULL；`trigger_source` VARCHAR(24) NOT NULL DEFAULT `SYSTEM` CHECK IN (`SYSTEM`,`USER`,`SCHEDULE`,`EVENT`)；`triggered_by` BIGINT UNSIGNED NULL；`draft_id` BIGINT UNSIGNED NULL；`review_task_id` BIGINT UNSIGNED NULL；`created_at`/`updated_at`。
**索引**：`uk_research_task_code(task_code)`；`uk_research_task_idem(idempotency_key)`；`idx_research_task_material(material_id, status)`；`idx_research_task_status(status, created_at)`；`idx_research_task_run(dify_run_id)`。
**语义**：`CONTEXT_FROZEN` 表示已冻结材料快照与授权上下文（spec「用户从材料页发起研究草稿」）；重复触发命中 `idempotency_key` 或 `(material_id, task_type, status IN 未完成)` 时返回既有任务（spec「重复发起同一材料的研究任务」）；`RETRYING` MUST 复用同一 `task_code` 与 `idempotency_key`（spec「任务可从断点继续」）。
**与 Dify**：**运行关联表**。存 `dify_run_id` 与版本三元组；Dify MUST NOT 直接改 `status`，状态只由 Spring Boot 依调用与校验结果推进。

#### A.5.3 `quantpf_ai_research_draft` — Dify 草稿区（**接收 Dify 输出的唯一落点**）

`id` PK；`research_task_id` FK NOT NULL；`dify_run_id` VARCHAR(128) NULL；`model_name` VARCHAR(128) NULL；`workflow_version` VARCHAR(64) NULL；`prompt_version` VARCHAR(64) NULL；`raw_response_json` JSON NOT NULL（**原始响应全文**）；`raw_response_hash` CHAR(64) NOT NULL；`summary` TEXT NULL；`fact_candidates_json` JSON NULL；`event_type_candidates_json` JSON NULL；`impact_level_candidates_json` JSON NULL；`impact_scope_candidates_json` JSON NULL；`direction_candidates_json` JSON NULL；`subject_candidates_json` JSON NULL；`citations_json` JSON NULL（原文引用定位）；`counter_questions_json` JSON NULL；`warnings_json` JSON NULL；`received_at` DATETIME(6) NOT NULL；`elapsed_ms` INT NULL；`schema_version_expected` VARCHAR(64) NOT NULL；`validation_status` VARCHAR(24) NOT NULL DEFAULT `PENDING` CHECK IN (`PENDING`,`PASSED`,`SCHEMA_FAILED`,`CITATION_UNVERIFIED`,`TIME_INVALID`,`AUTHORIZATION_BLOCKED`,`SUBJECT_UNRESOLVED`,`CONFLICTED`,`REJECTED`)；`validation_detail_json` JSON NULL；`validated_at` DATETIME(6) NULL；`is_fact_table` TINYINT(1) NOT NULL DEFAULT 0（**恒为 0，标记本表非事实主表**）；`review_task_id` BIGINT UNSIGNED NULL；`created_at`。
**索引**：`idx_draft_task(research_task_id)`；`uk_draft_run(dify_run_id)` UNIQUE（同一次运行只落一条草稿，回调重复投递幂等）；`idx_draft_validation(validation_status, received_at)`；`idx_draft_hash(raw_response_hash)`。
**语义**：本表是详设 1 §18.3「Dify 禁止直接写研究事件主表」的物理体现——Dify 的一切输出只落这里；`is_fact_table=0` 是显式自证，代码审查与 CI MUST 断言无任何路径把它当作事实源读取。
**与 Dify**：**唯一写入通道**，且只能经 `/api/quant/research/dify/callback`（签名校验）或由 `DifyWorkflowClient` 代写；MUST NOT 授予 Dify 数据库直连。

#### A.5.4 `quantpf_research_event` — 核验后的研究事件（Spring Boot 主表）

`id` PK；`event_code` VARCHAR(64) NOT NULL UNIQUE；`event_type` VARCHAR(32) NOT NULL；`title` VARCHAR(512) NOT NULL；`material_id` FK NOT NULL；`draft_id` BIGINT UNSIGNED NULL FK；`research_task_id` FK NULL；`impact_level` VARCHAR(16) NULL CHECK IN (`GENERAL`,`IMPORTANT`,`MAJOR_RISK`)；`impact_scope` VARCHAR(16) NULL CHECK IN (`MARKET_WIDE`,`INDUSTRY`,`COMPANY`,`COMMODITY`)；`direction_candidate` VARCHAR(16) NULL CHECK IN (`BULLISH`,`BEARISH`,`NEUTRAL`,`BIDIRECTIONAL`,`UNCERTAIN`)；`status` VARCHAR(24) NOT NULL DEFAULT `NEW` CHECK IN (`NEW`,`COLLECTED`,`DEDUPLICATED`,`AI_ANALYZED`,`VERIFIED`,`VARIABLE_MAPPED`,`PUBLISHED`,`DISMISSED`,`EXPIRED`,`REVISED`,`AI_ANALYSIS_PENDING`,`SOURCE_UNRESOLVED`,`CONFLICTED`,`SUBJECT_UNRESOLVED`,`AUTHORIZATION_BLOCKED`)；`verified_by` BIGINT UNSIGNED NULL；`verified_at` DATETIME(6) NULL；`verify_basis` VARCHAR(1024) NULL；`variable_mapping_json` JSON NULL（影响变量与传导链）；`affected_industry_json` JSON NULL；`affected_instrument_json` JSON NULL；`pit_version` VARCHAR(64) NULL；`snapshot_ref_json` JSON NULL（当时可见数据快照）；`event_version` INT NOT NULL DEFAULT 1；`superseded_by_id` BIGINT UNSIGNED NULL FK→self；`published_at` DATETIME(6) NULL；`publish_idempotency_key` VARCHAR(128) NULL；`correlation_id` VARCHAR(64) NULL；`major_event_flag` TINYINT(1) NOT NULL DEFAULT 0；`risk_review_required` TINYINT(1) NOT NULL DEFAULT 0；`stop_new_risk_suggested` TINYINT(1) NOT NULL DEFAULT 0；`human_confirm_required` TINYINT(1) NOT NULL DEFAULT 0；`review_task_id` BIGINT UNSIGNED NULL；`created_at`/`updated_at`。
**索引**：`uk_research_event_code(event_code)`；`idx_research_event_status(status, published_at)`；`idx_research_event_material(material_id)`；`idx_research_event_major(major_event_flag, risk_review_required)`；`idx_research_event_type_time(event_type, published_at)`；`uk_research_event_publish(publish_idempotency_key)` UNIQUE（发布幂等）。
**语义**：状态机严格按 spec「研究事件升级」；`VERIFIED` 前 MUST NOT `PUBLISHED`；`major_event_flag=1` 时 MUST 置 `risk_review_required=1` + `human_confirm_required=1`，且 MUST NOT 产生任何平仓/开仓动作字段（spec「重大利空材料的处置」）。
**与 Dify**：**禁止直写**；只读（事件结论、方向候选、影响范围）作为问答与解释上下文。

#### A.5.5 `quantpf_event_evidence` — 证据链

`id` PK；`research_event_id` FK NOT NULL；`material_id` FK NOT NULL；`snapshot_id` BIGINT UNSIGNED NULL；`evidence_type` VARCHAR(16) NOT NULL CHECK IN (`FACT`,`OPINION`,`PREDICTION`,`QUOTE`)；`quote_text` TEXT NOT NULL；`quote_locator_json` JSON NULL（原文定位：段落/字符区间/页码）；`source_ref` VARCHAR(512) NOT NULL；`source_level` VARCHAR(8) NOT NULL；`published_at` DATETIME(6) NULL；`available_at` DATETIME(6) NULL；`content_hash` CHAR(64) NOT NULL；`locator_verified` TINYINT(1) NOT NULL DEFAULT 0；`verified_by` BIGINT UNSIGNED NULL；`verified_at` DATETIME(6) NULL；`draft_evidence_ref` VARCHAR(128) NULL（对应草稿中的哪条引用）；`created_at`。
**索引**：`idx_evidence_event(research_event_id)`；`idx_evidence_material(material_id)`；`idx_evidence_locator_verified(locator_verified)`；`uk_evidence_hash(research_event_id, content_hash)` UNIQUE。
**语义**：`locator_verified=0` 的引用 MUST 阻断该条事实候选升级（spec「引用无法定位到原文」）；转载材料 MUST 通过 `material_id` → `dedup_ref_material_id` 追溯到原始出处，独立证据计数只加一次（spec「多份相同转载不计为多个独立证据」）。
**与 Dify**：**只读引用定位**；MUST NOT 修改 `locator_verified`。

#### A.5.6 `quantpf_manual_review_task` — 统一人工待办（ADR-07）

`id` PK；`task_code` VARCHAR(64) NOT NULL UNIQUE；`task_type` VARCHAR(32) NOT NULL CHECK IN (`CALENDAR_CONFLICT`,`INSTRUMENT_MAPPING`,`DATA_QUALITY`,`AI_DRAFT_REVIEW`,`AUTHORIZATION_CONFIRM`,`EVENT_VERIFY`,`CORPORATE_ACTION_VERIFY`,`CONTRACT_EVENT_VERIFY`,`SOURCE_DEGRADE`,`CALENDAR_GAP`)；`subject_type` VARCHAR(32) NOT NULL；`subject_id` BIGINT UNSIGNED NOT NULL；`subject_ref` VARCHAR(256) NULL；`priority` VARCHAR(16) NOT NULL DEFAULT `NORMAL` CHECK IN (`LOW`,`NORMAL`,`HIGH`,`URGENT`)；`status` VARCHAR(24) NOT NULL DEFAULT `OPEN` CHECK IN (`OPEN`,`ASSIGNED`,`IN_PROGRESS`,`DECIDED`,`EXPIRED`,`CANCELED`)；`assignee_id` BIGINT UNSIGNED NULL；`required_role` VARCHAR(32) NOT NULL CHECK IN (`REVIEWER`,`ADMIN`)；`payload_json` JSON NOT NULL（类型特有字段，应用层按 `task_type` 校验 Schema）；`plain_language_prompt` VARCHAR(1024) NOT NULL（普通语言说明：为什么需要处理、系统已完成什么、可做什么动作）；`decision` VARCHAR(16) NULL CHECK IN (`CONFIRMED`,`REJECTED`,`DEFERRED`,`OVERRIDDEN`,`EXPIRED`)；`decision_reason` VARCHAR(1024) NULL；`override_risk_note` VARCHAR(1024) NULL（`OVERRIDDEN` 时必填）；`valid_until` DATETIME(6) NULL；`decided_by` BIGINT UNSIGNED NULL；`decided_at` DATETIME(6) NULL；`visible_data_version_json` JSON NULL（**决策时可见的数据版本**，总纲 §14.6 要求）；`idempotency_key` VARCHAR(128) NOT NULL UNIQUE；`created_at`/`updated_at`。
**索引**：`uk_review_task_code(task_code)`；`uk_review_task_idem(idempotency_key)`；`idx_review_task_assignee(assignee_id, status, priority)`；`idx_review_task_type_status(task_type, status)`；`idx_review_task_subject(subject_type, subject_id)`；`idx_review_task_expiry(valid_until, status)`。
**语义**：`decision='OVERRIDDEN'` MUST 同时写 `decision_reason` 与 `override_risk_note`；`valid_until` 过期后 `status='EXPIRED'`，MUST 重新检查不得沿用（spec「审核用户覆盖系统结论」）；人工覆盖 MUST NOT 绕过数据授权、标的身份与确定性风控（应用层强制）。
**与 Dify**：Dify 可**创建** `task_type='AI_DRAFT_REVIEW'` 的建议待办（经校验失败路径由 Spring Boot 代建），MUST NOT 写 `decision`。

#### A.5.7 `quantpf_idempotency_record` — 统一幂等（ADR-08）

`id` PK；`scope` VARCHAR(32) NOT NULL CHECK IN (`DATA_COLLECT`,`SNAPSHOT_WRITE`,`RESEARCH_TASK`,`DIFY_CALLBACK`,`MANUAL_REVIEW`,`CALENDAR_PUBLISH`,`INSTRUMENT_SYNC`,`EVENT_PUBLISH`,`QUALITY_UPDATE`)；`idempotency_key` VARCHAR(128) NOT NULL；`request_hash` CHAR(64) NOT NULL（请求参数摘要，用于同键不同参检测）；`response_ref` VARCHAR(512) NULL；`result_status` VARCHAR(24) NOT NULL；`first_executed_at` DATETIME(6) NOT NULL；`hit_count` INT NOT NULL DEFAULT 0；`last_hit_at` DATETIME(6) NULL；`created_at`。
**索引**：`uk_idempotency(scope, idempotency_key)` UNIQUE；`idx_idempotency_time(first_executed_at)`。
**语义**：命中同键同 `request_hash` → 返回原结果并 `hit_count+1`；同键不同 `request_hash` → 返回 `QPF-IDEMP-001`（spec「重复提交同一采集请求」「同一幂等键不同参数」）。**既有 Paper 幂等键不在本表，保持原实现**。
**与 Dify**：**禁止**。

#### A.5.8 `quantpf_dify_invocation_log` — Dify 调用审计

`id` PK；`research_task_id` FK NULL；`dify_run_id` VARCHAR(128) NULL；`direction` VARCHAR(16) NOT NULL CHECK IN (`OUTBOUND`,`INBOUND_CALLBACK`)；`endpoint` VARCHAR(256) NOT NULL；`workflow_id` VARCHAR(128) NULL；`workflow_version`/`prompt_version`/`model_name` VARCHAR；`request_id` VARCHAR(64) NOT NULL；`idempotency_key` VARCHAR(128) NOT NULL；`request_payload_ref` VARCHAR(512) NULL（**大请求体不入库，只存引用与哈希**）；`request_payload_hash` CHAR(64) NULL；`response_status_code` INT NULL；`response_payload_ref` VARCHAR(512) NULL；`response_payload_hash` CHAR(64) NULL；`signature_verified` TINYINT(1) NULL；`elapsed_ms` INT NULL；`outcome` VARCHAR(24) NOT NULL CHECK IN (`SUCCESS`,`TIMEOUT`,`SCHEMA_FAILED`,`HTTP_ERROR`,`SIGNATURE_INVALID`,`REJECTED`)；`retry_of_id` BIGINT UNSIGNED NULL FK→self；`error_detail` VARCHAR(1024) NULL；`invoked_at` DATETIME(6) NOT NULL；`created_at`。
**索引**：`idx_dify_log_task(research_task_id, invoked_at)`；`idx_dify_log_run(dify_run_id)`；`idx_dify_log_outcome(outcome, invoked_at)`；`idx_dify_log_idem(idempotency_key)`。
**安全**：MUST NOT 记录 `DIFY_API_KEY`、`TUSHARE_TOKEN` 或任何凭据；请求/响应体只存引用与哈希（详设 5 §16、分册 §31.3）。
**与 Dify**：**禁止读写**（本表是对 Dify 的审计，不是 Dify 的接口）。

### A.6 Flyway 迁移文件规划（接续 `V1_9_0`）

| 迁移文件 | 内容 | 能力 |
|:---|:---|:---|
| `V1_10_0__data_source_governance.sql` | `quantpf_data_source`、`quantpf_data_product_endpoint`、`quantpf_endpoint_field_catalog`、`quantpf_source_group`、`quantpf_source_group_member`、`quantpf_source_cross_check` | data-source-governance |
| `V1_11_0__ingestion_snapshot_pit.sql` | `quantpf_collect_task`、`quantpf_data_raw_snapshot`、`quantpf_data_point_in_time`、`quantpf_market_bar` ALTER（ADR-09 `storage_tier`） | data-ingestion-snapshot / market-data-foundation |
| `V1_12_0__quality_and_eligibility.sql` | `quantpf_data_quality_issue`、`quantpf_data_usage_eligibility`、`quantpf_research_news` ALTER（六时间字段等） | data-quality-eligibility |
| `V1_13_0__instrument_identity_version.sql` | `quantpf_instrument` ALTER、`quantpf_instrument_version`、`quantpf_instrument_identity_mapping` | instrument-identity |
| `V1_14_0__lifecycle_events.sql` | `quantpf_corporate_action`、`quantpf_contract_event` | instrument-lifecycle-events |
| `V1_15_0__calendar_session_binding.sql` | `quantpf_trading_calendar_def`、`quantpf_trading_calendar` ALTER、`quantpf_trading_session_template`、`quantpf_instrument_calendar_binding`、日历定义与 GFEX 缺口/二期预留种子数据 | trading-calendar |
| `V1_16_0__calendar_snapshot_conflict_readiness.sql` | `quantpf_calendar_source_snapshot`、`quantpf_calendar_conflict`、`quantpf_market_data_readiness` | trading-calendar |
| `V1_17_0__capacity_and_feature_snapshot.sql` | `quantpf_capacity_assessment`、`quantpf_quant_feature_snapshot`（含二期预留列） | market-data-foundation |
| `V1_18_0__research_material_task_draft.sql` | `quantpf_research_material`、`quantpf_research_task`、`quantpf_ai_research_draft`、`quantpf_research_event`、`quantpf_event_evidence`、`quantpf_financial_statement` ALTER | research-material-intake |
| `V1_19_0__manual_review_idempotency_dify_log.sql` | `quantpf_manual_review_task`、`quantpf_idempotency_record`、`quantpf_dify_invocation_log` | 全能力共用 |
| `V1_20_0__data_foundation_indexes_and_constraints.sql` | 跨表补充索引、外键与 CHECK 收口（对齐 `V1_8_0` 风格） | 全部 |
| `V1_21_0__data_foundation_menu_and_dict.sql` | `sys_menu` 幂等写入本模块菜单与 `quant:*` 按钮权限、`sys_dict_*` 幂等写入枚举字典 | 全部（ADR-10） |

**迁移纪律**：
1. 全部走 Flyway，MUST NOT 手工执行 SQL 或修改已发布迁移文件。
2. 对已落地表只做 `ADD COLUMN` / `ADD INDEX` / 放宽 CHECK；MUST NOT `DROP COLUMN`、MUST NOT 收紧既有约束、MUST NOT `DROP TABLE`。
3. 放宽 `ck_quantpf_instrument_product_type` / `ck_quantpf_instrument_status` 时 MUST 先 `DROP CONSTRAINT` 再 `ADD CONSTRAINT`，并在同一迁移内完成，避免中间态无约束。
4. 种子数据（日历定义、时段模板、GFEX 缺口行、二期预留行）MUST 幂等（`INSERT ... ON DUPLICATE KEY UPDATE` 或 `INSERT IGNORE`）。
5. `V1_21_0` 写 `sys_menu` MUST 幂等，且 MUST NOT 触碰既有 109 条通用菜单记录。
6. 每个迁移 MUST 有对应回滚说明（Flyway Community 无 undo，回滚 = 前向修复迁移 + 数据备份点）。
---

## 附录 B：用户操作界面说明

> 前端栈实测：`plus-ui` = Vue 3.5 + Element Plus 2.14 + TypeScript 6 + Vite 8 + vxe-table 4.21 + UnoCSS + Pinia 4，页面惯例 `src/views/<域>/<子域>/index.vue`，接口惯例 `src/api/<域>/<子域>/{index.ts, types.ts}`。当前 `src/views` 下**无 `quant` 目录**，本变更新建。
>
> 三种体验模式来自详设 5 §7 与总纲 §2.5，通过**角色 + 页面内 `mode` 开关**双重实现：角色决定菜单与按钮权限（后端 Sa-Token 独立校验，前端只做展示收敛），`mode` 决定同一页面内专业字段的默认折叠状态。

### B.1 信息架构：「数据与标的」分组

对齐详设 5 §8.1 主导航中的「数据与标的」与「研究中心」两个分组，本变更落地以下菜单（写入 `sys_menu` + `contract/menu/quantpf-menu-facts.yaml`）：

```text
数据与标的  /quant/data
├── 数据就绪看板    /quant/data/readiness        quant:data-readiness:view   ← 普通用户主入口
├── 数据源与供应商  /quant/data/source           quant:data-source:list      ← 专业
├── 来源组与核验    /quant/data/source-group     quant:data-source-group:list← 专业
├── 采集任务与快照  /quant/data/collect-task     quant:collect-task:list     ← 专业
├── 数据质量与问题  /quant/data/quality          quant:data-quality:list     ← 审核 + 专业
├── 标的中心        /quant/data/instrument       quant:instrument:list       ← 全员
├── 交易日历与时段  /quant/data/calendar         quant:calendar:list         ← 审核 + 专业
├── 日历冲突待办    /quant/data/calendar-conflict quant:calendar-conflict:list← 审核
├── 公司行为与合约事件 /quant/data/lifecycle-event quant:lifecycle-event:list ← 审核 + 专业
├── 日线行情        /quant/data/market-bar       quant:kline:list            ← 全员
├── 容量与流动性    /quant/data/capacity         quant:capacity:list         ← 审核 + 专业
└── 研究材料与任务  /quant/data/research-material quant:research-material:list← 普通用户主入口
```

**我的待办**（详设 5 §8.1）复用底座工作台，本模块向 `quantpf_manual_review_task` 投递六类待办，前端在待办中心按 `task_type` 分组，点击直达对应处置页。

### B.2 逐页面设计

#### B.2.1 数据就绪看板 `/quant/data/readiness` ★普通用户主入口

**这是普通用户每天打开的第一个页面**，回答四个问题：今天数据到了吗、能用来做判断吗、为什么不能、下一步做什么。

| 区域 | 内容 | 可见角色 |
|:---|:---|:---|
| 顶部状态条 | 数据基准日期、日线新鲜度（`FRESH`/`STALE`/`NOT_ARRIVED`）、来源健康计数、授权受限计数、Dify/调度降级标记、**「模拟资金 · 非真实交易」固定提示** | 全员 |
| 主卡片区 | 每个市场一张卡：`今日是否交易日` / `日线应到·已到比例` / `策略可用` / `Paper 可用`，全部用**普通语言 + 图标**，不显示字段名 | 全员 |
| 阻断说明区 | 当 `paper_ready=false` 时展开：为什么不能（普通语言）、系统已完成什么、下一步能做什么、预计恢复时间 | 全员 |
| 我的标的就绪表 | 用户关注标的的逐条就绪状态，vxe-table，默认只显示标的名称、状态标签、普通语言原因 | 全员 |
| 专业明细（默认折叠） | 质量维度明细、`blocking_reasons_json`、规则版本、日历版本、来源接口、就绪计算时间 | 审核 + 专业 |

**核心操作**：`刷新`（重新计算就绪）、`查看某标的详情`（跳标的中心）、`加入关注`（≤2 个动作，总纲 §4.4）、`发起研究`（跳研究材料页并预填标的，**普通用户核心输入 ≤ 3 项**）。

**接口**：`GET /api/quant/calendar/readiness/dashboard`、`GET /api/quant/calendar/readiness/page`、`GET /api/quant/data/quality/summary`。

#### B.2.2 研究材料与任务 `/quant/data/research-material` ★普通用户主入口 + Dify 触发点

**列表页**：材料类型筛选（新闻/公告/财报/研报/政策）、时间范围、来源等级标签（A/B/C/D 以颜色区分并附「A=事实确认来源」等悬浮解释）、质量状态标签、是否可发起研究。

**详情抽屉**（三层渐进展开，总纲 §5.2）：

```text
第一层（默认展开）：普通语言摘要占位 · 来源等级 · 发布时间 · 可见时间 · 当前状态 · 下一步建议
第二层（点击「查看依据」）：原文正文 · 影响行业/标的候选 · 时间延迟说明 · 质量维度 · 反方问题
第三层（点击「专业明细」，仅审核+专业可见）：
   快照 ID 与哈希 · 请求参数 · Schema 版本 · 授权状态 · usage_scope ·
   pit_version · 六个时间字段原值 · 规则版本 · 审计链
```

**★ Dify 交互入口（主触发点）**：详情页右上角按钮 **「生成研究草稿」**。

```text
用户点击「生成研究草稿」（无需填写任何参数）
  → POST /api/quant/research/task
      body: { materialId, taskType: 系统按材料类型自动推断, triggerSource: "USER" }
      header: Idempotency-Key（前端生成 UUID，重试复用）
  → 后端冻结材料快照 + 授权上下文 → 创建 ResearchTask(CONTEXT_FROZEN)
  → 前端立即收到 taskCode + 预计耗时，按钮转为「整理中…」并禁用
  → 前端轮询 GET /api/quant/research/task/{taskCode}（默认 3s，指数退避至 15s 上限）
  → 状态流转展示：已冻结上下文 → 已派发智能体 → 智能体处理中 → 草稿已返回 → 校验中 → 校验通过/待人工复核
  → 完成后详情抽屉第一层填入普通语言结论，并新增「研究草稿」区块
```

**草稿结果区块**（`quantpf_ai_research_draft` 渲染）：摘要 / 事实候选（每条带**原文引用定位**，点击高亮原文对应段落）/ 事件类型候选 / 影响层级候选 / 影响范围候选 / 方向候选 / 关联标的候选 / 反方问题 / 智能体警告。**区块顶部固定标注**：「以下为智能体研究草稿，尚未核验，不构成事实或交易建议」+ 显示 `dify_run_id`、`model_name`、`workflow_version`、`prompt_version`、`received_at`。

**校验失败时**：区块显示 `validation_status` 的普通语言映射（见 B.3），并出现「提交人工复核」按钮 → 创建 `AI_DRAFT_REVIEW` 待办。

**其余操作**：`重新生成`（专业角色，需二次确认，说明会产生新的智能体调用）、`查看任务历史`（同一材料的所有任务与草稿版本）、`查看审计链`（第三层）。

**接口**：`GET /api/quant/research/material/page`、`GET /api/quant/research/material/{id}`、`POST /api/quant/research/task`、`GET /api/quant/research/task/{taskCode}`、`GET /api/quant/research/task/{taskCode}/draft`、`POST /api/quant/research/task/{taskCode}/submit-review`。

#### B.2.3 数据源与供应商 `/quant/data/source`（专业）

主从三级下钻，直接对应详设 1 §5.2「不能只保存已配置某数据源这种不可验收的粗粒度状态」：

```text
供应商列表（状态标签 + 到期倒计时 + 健康灯 + usage_scope 标签）
  └── 数据产品 / 接口列表（endpoint_name · 用途 · 权限已确认? · 积分要求 · 历史范围 · 频率 · 延迟 · enabled）
        └── 字段目录（field_name · 来源声明类型 · 单位 · 时区口径 · 是否授权 · 是否允许保存）
```

**核心操作**：`新增供应商` → 向导式四步（基本信息 → 产品与套餐 → 接口与字段目录 → 授权与用途确认）；`测试连接`（结果只把状态推到 `PENDING_CONFIRMATION`，界面明确提示「连接成功不等于业务可用，仍需完成授权、字段与用途确认」）；`启用`（后端校验 `permission_confirmed=1` 且字段目录非空，否则按钮禁用并显示原因）；`暂停/恢复`；`登记到期与费用`。

**Dify 交互入口（次触发点）**：供应商详情页 **「解释授权条款」** 按钮 → 上传/粘贴脱敏条款文本 → `POST /api/quant/research/task` with `taskType=DOC_EXPLAIN` → Dify 返回普通语言的「允许什么、禁止什么、到期日、是否可团队共享」解释 → 存草稿，**不回写 `quantpf_data_source`**；专业用户阅读后手工更新 `authorization_status` 与 `usage_scope`（`[R]` 责任字段）。

**安全**：页面 MUST NOT 出现 Token/API Key 输入框；凭据只走环境变量（详设 5 §16、分册 §35.2）。`permission_profile_ref` 只存引用标识。

#### B.2.4 来源组与核验 `/quant/data/source-group`（专业）

来源组列表（`group_type` 分组、主源数量是否达标 2~3、`historical_completeness` 三态标签）→ 成员配置（主源/备源/仅交叉核验、优先级、覆盖范围、授权状态）→ 交叉核验结果表（`result` 标签、双方取值、差异详情、影响的质量状态、关联待办）。

**关键交互**：当某组 `status='SINGLE_SOURCE'` 时，页面顶部橙色横幅「该来源组目前只有一个可用来源，交叉核验不足；系统会保留研究线索，但不会据此形成建仓建议」。当 `result='CONFLICTED'` 时，行内「发起人工复核」按钮 → 创建 `DATA_QUALITY` 待办。

**Dify 交互入口**：冲突行 「解释冲突原因」 → `taskType=QUALITY_EXPLAIN` → 只读 `quantpf_source_cross_check` 的 `result` + `difference_detail` → 生成普通语言解释草稿。

#### B.2.5 采集任务与快照 `/quant/data/collect-task`（专业）

任务列表（类型、来源、状态、请求/成功/失败/去重命中条数、重试次数、耗时、关联 `job_execution_audit`）→ 任务详情（`scope_params_json`、失败原因、逐条失败明细）→ 关联原始快照列表（哈希、载荷引用、抓取时间、解析状态、修订链 `superseded_by_id`）。

**核心操作**：`手动触发采集`（选择任务类型 + 范围，系统预填来源与接口，二次确认）、`重试失败任务`（复用同一 `idempotency_key`）、`取消进行中任务`、`查看原始载荷`（第三层，只读）、`导入行情`（`FULL` 模式 MUST 二次确认并显示将被重建的范围与数据量，ADR/spec「全量重建需要二次确认」）。

#### B.2.6 数据质量与问题 `/quant/data/quality`（审核 + 专业）

质量问题看板：按 `severity` × `status` 的矩阵计数 + 问题列表。详情页展示受影响数据、业务影响、`affected_usage`（哪些用途被限制）、处理人、生命周期时间线（`OPEN→ANALYZING→WAITING_*→RESOLVED→VERIFIED→CLOSED`）。

**核心操作**：`认领`、`转人工/转来源方`、`提交修复动作`（→ `RESOLVED`，**不直接 CLOSED**）、`验证`（→ `VERIFIED`）、`关闭`。状态转移按钮按当前状态动态启用，非法转移在前端禁用并在后端拒绝。

**用途资格矩阵视图**（本模块最有业务价值的审核视图）：选中一个标的或数据集，以表格展示详设 1 §9.4 的五行八列矩阵实际判定结果，每格显示允许/禁止/限制 + 悬浮显示是哪一项输入导致（对应 spec「多项输入同时受限」MUST 逐项返回）。

**Dify 交互入口**：问题详情 「用普通语言解释这个问题」 → `taskType=QUALITY_EXPLAIN`。

#### B.2.7 标的中心 `/quant/data/instrument`（全员）

标的列表（代码、名称、产品类型、交易所、身份状态标签、交易属性版本、上市状态）→ 标的详情：

```text
第一层：这是什么标的 · 现在能不能交易 · 为什么 · 下一步
第二层：交易属性（每手数量、最小变动、乘数、保证金率、涨跌停、交割月、到期日）
        · 版本历史时间线（validFrom/validUntil/changeReason/sourceRef）
        · 日历与时段绑定 · 容量状态 · 关联公司行为与合约事件
第三层：身份映射候选与裁决记录 · 来源接口 · 快照引用 · 审计链
```

**核心操作（审核角色）**：`处理身份映射待办` —— 页面并排显示候选映射（系统建议 / Dify 建议 / 人工建议，标注 `suggested_by`）、双方来源与依据，审核者选择 `CONFIRMED`/`REJECTED`/`DEFERRED`/`OVERRIDDEN`，MUST 填理由，`OVERRIDDEN` MUST 额外填风险影响与有效期。**界面明确显示禁止自动合并的七类情形**（不同交易所同代码、同名不同品级、连续 vs 具体合约、不同地区现货、不同交割月、不同单位币种、复权 vs 非复权），命中时按钮强制走人工路径。

#### B.2.8 交易日历与时段 `/quant/data/calendar`（审核 + 专业）

日历矩阵视图（月视图，每格显示开市/休市/半日市 + 质量状态色标 + 冲突角标）；切换 `calendar_code`（一期只列出 `CN_STOCK_*` 与 `CN_FUTURE_*`，港股/美股/外汇行以「二期预留」灰显且不可点击）；时段模板管理（常规/午休/集合竞价/夜盘/跨日/特殊，含夜盘交易日归属）；标的绑定管理；覆盖缺口面板（**GFEX 缺口以红色显式呈现「需另找授权来源或人工维护」**）。

**核心操作**：`同步日历`（按接口分别触发，显示每个接口的权限探测结果）、`发布日历版本`（幂等键 + 二次确认 + 显示影响范围）、`查看来源快照`（原始 `is_open` 值 + 类型 + 标准化值并排显示，`INVALID` 行高亮）。

#### B.2.9 日历冲突待办 `/quant/data/calendar-conflict`（审核）

冲突列表（日期、日历、冲突类型、双方来源与取值、影响范围摘要、状态）→ 冲突处置页：

```text
左侧：来源 A 的原始值、抓取时间、快照哈希、接口名、历史准确率
右侧：来源 B 的同项信息
中间：影响范围（受影响标的数、研究任务数、候选筛选、Paper 资格判定）
底部：裁决区 —— 采信 A / 采信 B / 人工指定 / 延期，MUST 填理由，OVERRIDDEN MUST 填风险影响 + 有效期
```

裁决后系统自动刷新对应日历日 `quality_status` 并重算就绪状态（spec「审核用户确认来源结论」）。过期未处理的冲突自动转 `EXPIRED` 并重新排队（spec「审核用户覆盖系统结论」）。

#### B.2.10 公司行为与合约事件 `/quant/data/lifecycle-event`（审核 + 专业）

事件日历（按标的/品种/日期范围）+ 事件列表（类型、生效日、状态、来源等级、是否已发布事件）。**提前提示区**：未来 N 天内的交割月进入、最后交易日、移仓窗口、财报披露日、规则变化，按紧急度排序。

**核心操作**：`核验事件`（→ `VERIFIED`）、`标记撤销`（更高等级来源否定时，保留原记录 + 撤销原因，触发下游重算通知）、`生成移仓待办`（显示旧合约、建议新合约、最后交易日、移仓价差上限参考，并说明移仓需拆成平旧+开新多笔）。

#### B.2.11 日线行情 `/quant/data/market-bar`（全员）

一期只有日线（ADR-02）：K 线图（ECharts，日线 OHLCV + 成交量）、复权口径切换器（`NONE`/`FORWARD`/`BACKWARD`，**切换时明确提示两者不可混用**）、数据质量色标、导入模式面板（`FULL` 需二次确认 / `INCREMENTAL` / `REPAIR` 需指定区间与原因）、缺口检测结果。

**二期边界提示**：页面 MUST 显示「一期仅提供日线数据；分钟线、Tick 与盘口为二期能力」，MUST NOT 提供任何分钟级选择器（spec「一期不提供分钟线能力」）。

#### B.2.12 容量与流动性 `/quant/data/capacity`（审核 + 专业）

容量评估列表（标的、评估日、`capacity_status` 标签、可承载规模、预计滑点、止损可执行性、受限原因）+ 详情页（股票因素/期货因素分组显示，规则版本与数据基准日期）。`UNTRADEABLE`/`UNKNOWN` 行显示「不得生成新开仓计划」的红色标记与具体原因（是数据不足还是规则未覆盖，并给出对应待办入口）。

### B.3 阻断状态的普通语言文案表

前端 MUST 内置以下映射（i18n key 统一 `quant.data.block.*`），**MUST NOT 把技术错误码作为唯一提示**（详设 0 §1.5、§17.3）：

| 状态 | 第一层文案（普通用户看到） | 可执行动作 |
|:---|:---|:---|
| `DELAYED` | 这份数据比正常时间晚到了，系统已经标记延迟。可以先看，但暂时不能用来做建仓判断。 | 查看延迟时长 · 加入观察 · 等更新 |
| `PARTIAL` | 数据不完整，只有一部分。系统不会用它来证明成功率。 | 查看缺失范围 · 加入观察 |
| `CONFLICTED` | 两个来源的数据不一致，系统已经提交人工复核。复核完成前不能用来做建仓判断。 | 查看双方差异 · 加入观察 · 催办 |
| `UNAUTHORIZED` | 这份数据的授权范围还没确认，暂时只能人工查看，不能用于筛选和模拟交易。 | 联系管理员确认授权 |
| `STALE` | 这份数据已经过期，只能用于查看历史和复盘，不能做新的判断。 | 查看历史 · 触发重新采集（专业） |
| `INVALID` | 这份数据格式有问题，系统没有猜测或填充，已标记异常。 | 查看异常详情 · 转人工处理 |
| `PENDING_REVIEW` | 这份数据正在等人工确认，确认后会继续。 | 查看待确认项 · 催办 |
| `MAPPING_REQUIRED` | 系统还不能确定这是哪一个标的（可能不同交易所代码相同），需要人工确认后继续。 | 提交映射待办（审核） |
| `AUTHORIZATION_BLOCKED` | 授权检查没通过，系统已停止后续处理，不会产生任何交易建议。 | 联系管理员 |
| `SUBJECT_UNRESOLVED` | 智能体提到的标的没法唯一对应到系统里的对象，需要人工确认。 | 提交映射待办（审核） |
| `CONFLICTED`(草稿) | 智能体的说法和系统已核验的事实冲突，系统没有用它覆盖已有结论。 | 人工复核 |
| 日历 `CONFLICTED` | 这一天的开市安排两个来源说法不同，系统没有随便选一个，已提交人工裁决。 | 去日历冲突待办（审核） |
| 日历 `INVALID` | 这一天的是否开市信息缺失或格式错误，系统既没当成开市也没当成休市。 | 转人工处理 |
| 日历 `GAP` | 这个交易所的日历暂时没有授权来源，系统不会假装已经覆盖。 | 登记来源或人工维护（专业） |
| 就绪 `NOT_ARRIVED` | 今天的日线还没到，暂时不能做新的建仓判断；数据到达后系统会自动更新。 | 稍后刷新 · 查看昨日结论 |
| 就绪 `PARTIAL` | 只有一部分标的的日线到了（已到 X / 应到 Y），未到的部分不能做判断。 | 查看未就绪清单 |
| 容量 `HIGH_IMPACT` | 这个标的成交不够活跃，按当前规模买入会明显推动价格，止损也可能无法按计划成交。 | 只观察 · 降低规模（专业） |
| 容量 `UNTRADEABLE` / `UNKNOWN` | 系统判断这个标的当前不适合交易 / 还判断不了（数据不足或规则未覆盖）。 | 查看具体原因 · 转待办 |
| Dify `TIMEOUT` | 智能分析暂时不可用。行情、数据质量、既有模拟盘和风控不受影响；依赖智能分析的部分已标记待补充。 | 稍后重试 · 查看确定性结果 |
| Dify `SCHEMA_FAILED` | 智能体返回的内容格式不符合要求，系统已保存原始结果供人工复核，没有用默认值填充。 | 提交人工复核（审核） |
| Dify `CITATION_UNVERIFIED` | 智能体给的引用在原文里找不到，这条结论没有被采纳。 | 查看原文 · 人工复核 |

### B.4 Dify 接入方式的用户可见形态（取舍说明）

| 方案 | 用户在哪触发 | 采纳 | 理由 |
|:---|:---|:--|:---|
| **A. 后端 API 代理 + 前端原生渲染**（本变更采用） | 若依页面内的按钮（「生成研究草稿」「解释授权条款」「解释冲突原因」「用普通语言解释这个问题」），结果以若依原生组件渲染在详情抽屉内 | ✅ 主方案 | 三层渐进披露可控；`runId`/版本/校验状态与业务数据同页展示；权限与审计走底座；Dify 不可用时前端能优雅降级；符合详设 5 §15「Dify 独立部署 + Spring Boot 适配」 |
| B. iframe 嵌入 Dify WebApp | 独立菜单项打开 Dify 会话页 | ❌ 一期不采用 | 无法强制三层披露；无法保证「草稿非事实」的界面标注；权限与审计脱离底座；降级体验差；违反详设 5「界面操作 MUST 映射到明确的业务状态和服务契约」 |
| C. 前端直连 Dify API | 浏览器直接调用 | ❌ 禁止 | 会暴露 `DIFY_API_KEY`；绕过 Spring Boot 校验与授权门禁；违反详设 1 §18.3 |

**方案 A 的用户体验细节**：
- 所有 Dify 结果区块 MUST 带统一头部徽标：`智能体草稿 · 未核验` + `runId` + `模型/工作流/提示词版本` + `接收时间`（可折叠到第三层，但徽标本身不可隐藏）。
- 轮询期间显示进度文案（不是转圈无信息）：「已冻结数据快照 → 已派发智能体 → 智能体整理中 → 草稿已返回 → 正在校验引用与时间 → 完成/待人工复核」。
- 降级时显示 B.3 的 `TIMEOUT` 文案 + 「查看确定性结果」链接，MUST NOT 显示绿色「系统正常」（详设 5 §8.2）。
- 普通用户 MUST NOT 看到 `workflow_id`、`prompt_version` 等技术字段作为主要信息（折叠到第三层）。

### B.5 全局状态条（详设 5 §8.2）在本模块的必显项

在所有涉及数据或交易的页面固定显示：`当前用户/scope` · `数据基准日期` · `日线新鲜度` · `来源健康（正常/降级/不可用计数）` · `授权受限计数` · `风险/熔断状态`（只读引用已落地 `/api/quant/risk/status`） · `Dify 或调度降级标记` · **`模拟资金 · 非真实交易` 固定提示**。MUST NOT 用单一绿色「系统正常」代替这些子状态。

### B.6 权限编码与菜单闭合

新增权限标识（沿用总纲 §15.2 命名风格，写入 `sys_menu.perms` 与 `contract/menu/quantpf-menu-facts.yaml`）：

```text
quant:data-readiness:view
quant:data-source:list / view / add / edit / probe / enable / suspend
quant:data-source-group:list / edit / cross-check
quant:collect-task:list / trigger / retry / cancel / snapshot:view
quant:data-quality:list / view / claim / resolve / verify / close
quant:eligibility:view
quant:instrument:list / view / edit / sync / mapping:review
quant:calendar:list / import / sessions / publish / binding
quant:calendar-conflict:list / resolve
quant:lifecycle-event:list / verify / cancel / rollover-todo
quant:kline:list / import
quant:capacity:list / view
quant:research-material:list / view
quant:research-task:create / view / retry / submit-review
quant:research-event:list / view / evidence
```

**闭合方式**（ADR-10）：`V1_21_0` 迁移以 `INSERT ... ON DUPLICATE KEY UPDATE` 幂等写入 `sys_menu`（目录 + 菜单 + 按钮三级，`parent_id` 用子查询按 `menu_name` 定位，避免硬编码 ID）；同一次变更内更新 `contract/menu/quantpf-menu-facts.yaml` 增加对应条目；CI 增加一致性检查任务，断言 YAML 中每个 `enabled: true` 的菜单在 `sys_menu` 中存在且 `perms` 一致。`forbiddenCapabilities`（`LIVE`/`QMT`/`CTP`/`REAL_BROKER_ORDER`/`REAL_BROKER_CANCEL`）MUST 原样保留，本变更 MUST NOT 新增相关菜单或权限。

### B.7 普通用户关键路径的操作步数核算（总纲 §4.4 验收基线）

| 路径 | 步数 | 达标 |
|:---|:--|:--|
| 查看今日数据是否就绪 | 登录 → 数据就绪看板（1 步，默认首页） | ✅ |
| 从材料发起研究草稿 | 研究材料页 → 点选一条材料 → 点「生成研究草稿」= **核心输入 0 项 / 2 个动作** | ✅ ≤3 项输入 |
| 把标的加入关注 | 就绪看板或标的详情 → 「加入关注」 → 确认 = **2 个动作** | ✅ ≤2 |
| 查看某标的是否能建仓（问答） | 标的详情 → 「问一下」 → 输入自然语言 = **1 项输入** | ✅ ≤3 |
| 处理一个待办（审核用户） | 待办中心 → 打开 → 选择裁决 + 填理由 → 提交 = **必填仅理由一项** | ✅ |

普通用户全程 MUST NOT 被要求填写：数据源配置、接口字段、指标周期、策略表达式、仓位公式、风险预算、撮合参数、日历版本、复权口径公式。
---

## 附录 C：代码仓库落地指导

> 实测惯例：领域层包根 `com.quantpf.domain.{entity,enums,exception,persistence{mapper,record},repository,value}`；Mapper XML 在 `quantpf-domain/src/main/resources/mapper/quantpf/*Mapper.xml`；Web 层在 `ruoyi-admin/src/main/java/com/quantpf/web/{config,controller,dto}`；Bean 由 `QuantPfPaperRuntimeConfiguration` 显式装配（`DromaraApplication` 扫描根为 `org.dromara`，MUST NOT 修改）。`application-*.yml` 当前**无任何** `quantpf.*` 配置键。

### C.1 模块目录结构建议

#### C.1.1 `quantpf-modules/quantpf-common`（从 0 落地，只放无状态基础设施）

```text
src/main/java/com/quantpf/common/
├── time/
│   ├── MarketLocalDate.java          # 市场本地日期值对象（含时区，禁止用服务器自然日）
│   ├── MarketTimezones.java          # 时区常量：Asia/Shanghai、America/New_York、GMT
│   ├── TimeVisibility.java           # 六时间字段聚合值对象 + 传输延迟计算
│   └── TimeVisibilityStatus.java     # VALID / DELAYED_ARRIVAL / MISSING_PUBLISHED_AT / UNKNOWN
├── source/
│   ├── SourceLevel.java              # A / B / C / D（总纲 §5.1）
│   ├── UsageScope.java               # PERSONAL_RESEARCH / TEAM_INTERNAL / COMMERCIAL_SERVICE / NO_REDISTRIBUTION
│   └── AuthorizationStatus.java      # UNCONFIRMED / CONFIRMED / RESTRICTED / BLOCKED
├── quality/
│   ├── DataQualityStatus.java        # 详设 1 §9.2 八态
│   ├── HistoricalCompleteness.java   # COMPLETE / PARTIAL / UNAVAILABLE / UNKNOWN
│   └── UsageKind.java                # DISPLAY / RESEARCH / SCREENING / BACKTEST / PAPER_OPEN
├── version/
│   ├── PitVersion.java               # 点时版本值对象
│   └── VersionedRule.java            # 规则版本 + 有效期
├── event/
│   ├── DomainEventEnvelope.java      # eventId/eventType/aggregateId/occurredAt/availableAt/
│   │                                 # schemaVersion/source/idempotencyKey/correlationId/causationId/payload
│   └── DomainEventPublisher.java     # 接口（实现在 marketdata/research）
├── idempotency/
│   ├── IdempotencyScope.java         # 对应 A.5.7 的 scope 枚举
│   ├── IdempotencyKey.java           # 非空、≤128 值对象（对齐既有 OrderIdempotencyKey 风格）
│   └── IdempotencyGuard.java         # 接口：executeOrReturnPrevious(...)
├── audit/
│   └── AuditFields.java              # requestId/actor/action/aggregate/before/after/reason/source/version
├── payload/
│   ├── PayloadHasher.java            # SHA-256 统一实现
│   └── PayloadRef.java               # 大载荷引用（inline vs external 阈值判定）
└── exception/
    ├── QuantPfDataException.java     # 携带 ErrorCode
    └── DataFoundationErrorCode.java  # QPF-DATA-* / QPF-CAL-* / QPF-SUBJ-* / QPF-AI-* / QPF-AUTH-*
```

#### C.1.2 `quantpf-modules/quantpf-domain`（扩展，**不改既有类语义**）

```text
src/main/java/com/quantpf/domain/
├── entity/                            # 新增（既有 5 个实体不动）
│   ├── DataSource.java                DataSourceEndpoint.java
│   ├── EndpointFieldCatalog.java      SourceGroup.java   SourceGroupMember.java
│   ├── SourceCrossCheck.java          CollectTask.java   DataRawSnapshot.java
│   ├── DataPointInTime.java           DataQualityIssue.java  DataUsageEligibility.java
│   ├── InstrumentVersion.java         InstrumentIdentityMapping.java
│   ├── CorporateAction.java           ContractEvent.java
│   ├── TradingCalendarDef.java        TradingCalendarDay.java   # 映射既有 quantpf_trading_calendar
│   ├── TradingSessionTemplate.java    InstrumentCalendarBinding.java
│   ├── CalendarSourceSnapshot.java    CalendarConflict.java     MarketDataReadiness.java
│   ├── CapacityAssessment.java        QuantFeatureSnapshot.java
│   ├── ResearchMaterial.java          ResearchTask.java         AiResearchDraft.java
│   ├── ResearchEvent.java             EventEvidence.java
│   ├── ManualReviewTask.java          IdempotencyRecord.java    DifyInvocationLog.java
│   └── MarketBar.java                 # 新增实体映射既有表（含扩展列）
├── enums/                             # 新增：DataSourceStatus / EndpointPurpose / SourceGroupType /
│                                      # SourceGroupRole / CrossCheckResult / CollectTaskType /
│                                      # CollectTaskStatus / ParseStatus / QualityIssueType /
│                                      # QualityIssueStatus / Severity / IdentityStatus /
│                                      # MappingStatus / MappingSuggestedBy / LifecycleEventType /
│                                      # ContractEventType / LifecycleEventStatus / SessionType /
│                                      # TradingDayAttribution / BindingLevel / BindingStatus /
│                                      # CalendarConflictType / FreshnessStatus / CapacityStatus /
│                                      # MarketState / VolumePriceState / FeatureCategory /
│                                      # FeaturePhase / MaterialType / DedupStatus /
│                                      # MappingResolveStatus / ResearchTaskType / ResearchTaskStatus /
│                                      # TriggerSource / DraftValidationStatus / ResearchEventStatus /
│                                      # EvidenceType / ReviewTaskType / ReviewTaskStatus /
│                                      # HumanDecision / DifyDirection / DifyOutcome /
│                                      # ProductType(扩展 SPOT) / InstrumentStatus(扩展全集)
├── value/                             # 新增：CalendarCode / MarketLocalDateRange / RawIsOpenValue /
│                                      # ContentHash / ExternalId / SnapshotRef / DecisionReason /
│                                      # OverrideRiskNote（对齐既有 Money/Quantity 值对象风格）
├── repository/                        # 新增 22 个仓储端口接口（纯接口，无实现）
├── persistence/mapper/                # 新增 22 个 Mapper 接口
└── persistence/record/                # 新增 22 个 Record（DB 行映射）
src/main/resources/mapper/quantpf/     # 新增 22 个 *Mapper.xml（沿用既有命名与目录）
```

> **纪律**：既有 `Instrument` 只**加字段**（`marketCode`/`companyId`/`securityName`/`listingStatus`/`priceTick`/`identityStatus`/`currentVersionId`），MUST NOT 改既有 getter 语义；`ProductType` 加 `SPOT`、`InstrumentStatus` 扩展全集时 MUST 保留既有枚举常量名（`ACTIVE`/`SUSPENDED`/`DELISTED`），否则会破坏已落地 Paper 链路与 `ck_quantpf_instrument_status` 兼容。
#### C.1.3 `quantpf-modules/quantpf-marketdata`（从 0 落地，本变更主战场）

```text
src/main/java/com/quantpf/marketdata/
├── source/                            # 供应商与来源组治理
│   ├── DataSourceRegistry.java        # 登记/查询/状态推进；启用门禁校验
│   ├── DataSourceHealthChecker.java   # 失败率/延迟/额度 → health_status + fail_count_recent
│   ├── SourceGroupService.java        # 来源组配置、主备切换、SINGLE_SOURCE 判定
│   └── SourceCrossCheckService.java   # 交叉核验 → CONSISTENT/CONFLICTED/TIME_MISMATCH/MISSING
├── tushare/                           # ★ 每接口一个适配器，禁止合并调用逻辑（详设 1 §12.2）
│   ├── TushareClient.java             # HTTP 客户端：token 注入、限流、重试、原始响应留存
│   ├── TushareProperties.java         # @ConfigurationProperties("quantpf.tushare")
│   ├── TushareEndpointAdapter.java    # 接口：endpointName() / schemaVersion() / parse(raw)
│   ├── TushareTradeCalAdapter.java    # trade_cal：is_open 为 STRING "0"/"1"
│   ├── TushareFutTradeCalAdapter.java # fut_trade_cal：is_open 为 INT 0/1；覆盖不含 GFEX
│   ├── TushareHkTradeCalAdapter.java  # 二期预留：不返回 exchange，由适配器配置补市场身份
│   ├── TushareUsTradeCalAdapter.java  # 二期预留：US_STOCK_GENERIC + America/New_York
│   ├── TushareFxObasicAdapter.java    # 二期预留：classify 保留、traget_spread 原拼写保留
│   ├── TushareFxDailyAdapter.java     # 二期预留：trade_date 为 GMT，只作数据日期
│   ├── TushareInstrumentAdapter.java  # 股票/期货合约基础信息
│   ├── TushareDailyBarAdapter.java    # 日线行情
│   ├── TusharePermissionProber.java   # 逐接口权限探测 → permission_confirmed / probe_result
│   └── RawIsOpenParser.java           # ★ 严格类型解析：空/类型不符 → INVALID，MUST NOT 默认开市
├── ingest/
│   ├── CollectTaskService.java        # 任务生命周期 + 幂等 + 部分成功计数
│   ├── RawSnapshotStore.java          # 原始快照落库（inline vs external 阈值）
│   ├── ContentHashDeduplicator.java   # content_hash + external_id 双路去重
│   ├── StandardizationService.java    # 字段/单位/时区/日期口径标准化，原值与标准值双存
│   └── PointInTimeVersionService.java # 点时版本与修订链（新增行 + superseded 标记）
├── quality/
│   ├── DataQualityEvaluator.java      # 十维度 → DataQualityStatus
│   ├── TimeVisibilityEvaluator.java   # 六时间字段 + 传输延迟阈值
│   ├── DataQualityIssueService.java   # 问题生命周期（禁止 RESOLVED→CLOSED 跳变）
│   └── UsageEligibilityDecider.java   # ★ 七输入 → 五资格 + 逐项 blocked_reasons + 普通语言原因
├── instrument/
│   ├── InstrumentIdentityResolver.java   # 身份解析；命中七类禁止合并情形 → MAPPING_REQUIRED
│   ├── InstrumentVersionService.java     # 版本区间闭合、current_version_id 维护
│   ├── InstrumentTradabilityService.java # 可交易资格（状态+日历+时段+合约生命周期+容量+资格）
│   ├── CorporateActionService.java       # 公司行为登记/核验/生效/发布
│   └── ContractEventService.java         # 合约与制度事件；提前提示待办生成
├── calendar/
│   ├── TradingCalendarService.java       # 日历定义 + 日历日 + 前后交易日 + 版本
│   ├── CalendarNormalizationService.java # 各适配器 → 统一内部模型（市场本地时区）
│   ├── CalendarConflictDetector.java     # 重叠区间比对 → CalendarConflict；MUST NOT 静默择一
│   ├── CalendarCoverageGapService.java   # GFEX 等缺口显式登记 + 待办
│   ├── SessionTemplateService.java       # 时段模板 + 夜盘交易日归属
│   ├── InstrumentCalendarBindingService.java
│   └── MarketDataReadinessCalculator.java# ★ 应到/已到/已校验/策略可用/Paper可用（ADR-02 日线口径）
├── market/
│   ├── MarketBarValidator.java        # OHLC 合法性 + 日历归属 + 身份有效性
│   ├── MarketBarImportService.java    # FULL / INCREMENTAL / REPAIR
│   ├── AdjustFlagService.java         # 复权口径；除权除息后按版本重算并保留旧版本
│   ├── MarketStateCalculator.java     # 确定性规则 → MarketState（数据不足 → DATA_UNAVAILABLE）
│   └── QuantFeatureCalculator.java    # 一期基础量价特征；二期标签字段恒 null
├── capacity/
│   └── CapacityAssessor.java          # 股票/期货因素 → CapacityStatus + 可承载规模 + 滑点
├── persistence/                       # 仓储适配器实现（Repository Adapter，对齐 finance 模块风格）
├── tx/
│   └── DataFoundationTransactionService.java  # 显式 MyBatis 事务（对齐 PaperPersistenceTransactionService）
└── event/
    └── MarketDataDomainEventPublisher.java    # Spring 事件 + Redis Pub/Sub
```

#### C.1.4 `quantpf-modules/quantpf-research`（从 0 落地，Dify 边界所在）

```text
src/main/java/com/quantpf/research/
├── material/
│   ├── ResearchMaterialService.java   # 统一材料索引（ADR-04 多态引用 + 唯一键防重）
│   ├── MaterialGovernanceService.java # 采集→原文→去重→时间→授权→质量→初步映射→eligible_for_task
│   └── IndustryInstrumentMapper.java  # 初步行业/标的映射；不确定 → UNRESOLVED，禁止模糊强匹配
├── task/
│   ├── ResearchTaskService.java       # 任务创建/幂等/上下文冻结/状态推进/断点续跑
│   └── ResearchTaskContextFreezer.java# 冻结材料快照 + 授权上下文（对应 CONTEXT_FROZEN 状态）
├── dify/
│   ├── DifyWorkflowClient.java        # ★ 出站调用：注入 Idempotency-Key/requestId、超时、退避重试
│   ├── DifyProperties.java            # @ConfigurationProperties("quantpf.dify")
│   ├── DifyRequestBuilder.java        # 构造详设 1 §20.2 请求体（字段白名单）
│   ├── DifyResponseParser.java        # 解析响应 → 原始 JSON 全量留存
│   ├── DifyCallbackVerifier.java      # ★ 入站校验：签名/共享密钥 + requestId + 任务状态
│   ├── DifyReadOnlyQueryFacade.java   # ★ 供 Dify 读取的最小权限只读接口（字段白名单）
│   ├── DifyFieldWhitelist.java        # 白名单定义（禁止授权/凭据/行情主表/特征值）
│   ├── DifyInvocationLogger.java      # 调用审计（不记凭据，大载荷只存引用+哈希）
│   └── DifyDegradeManager.java        # 降级矩阵（详设 0 §16.4）：确定性链路继续，事实升级阻断
├── draft/
│   ├── AiDraftPersistService.java     # 草稿落库（唯一写入通道）；is_fact_table 恒 0
│   └── AiDraftValidator.java          # ★ Schema/引用可定位/时间/来源授权/对象身份/冲突 六校验
├── event/
│   ├── FactVerificationService.java   # 草稿 → 核验事实（VERIFIED）
│   ├── EvidenceLocatorVerifier.java   # 引用定位校验 → locator_verified
│   ├── ResearchEventService.java      # 研究事件状态机（严格按 spec）
│   ├── MajorEventDispositionService.java # 重大事件 → 研究事件/风险复核/停新增风险建议/人工确认
│   └── ResearchEventPublisher.java    # 发布 + 幂等 + 补偿重发
├── review/
│   ├── ManualReviewTaskService.java   # 统一待办（ADR-07）：创建/认领/裁决/过期
│   └── HumanDecisionRecorder.java     # CONFIRMED/REJECTED/DEFERRED/OVERRIDDEN/EXPIRED + 可见数据版本
├── persistence/
└── tx/
    └── ResearchTransactionService.java
```

#### C.1.5 `quantpf-modules/quantpf-job`（从 1 个接口占位扩展）

```text
src/main/java/com/quantpf/job/
├── PaperApplicationServicePort.java   # 既有，保持不动
├── handler/                           # XXL-JOB Handler（迁移门禁通过后才注册，禁止双栈）
│   ├── SourceHealthCheckJob.java          # 来源健康检查
│   ├── InstrumentSyncJob.java             # 股票/期货基础信息同步
│   ├── CalendarSyncJob.java               # trade_cal / fut_trade_cal（hk/us 二期预留不启用）
│   ├── CalendarOverlapCompareJob.java     # 重叠区间比对与版本发布
│   ├── CalendarConflictDetectJob.java     # 冲突检测 + 人工覆盖待办
│   ├── MarketDataReadinessJob.java        # 日线就绪状态计算
│   ├── InstrumentBindingSyncJob.java      # 标的/合约与日历绑定同步
│   ├── LifecycleEventSyncJob.java         # 公司行为与合约事件同步
│   ├── MarketBarIncrementalJob.java       # 日线增量同步（收盘后触发，非盘中频率）
│   ├── RawSnapshotArchiveJob.java         # 原始快照归档
│   ├── DataQualityCheckJob.java           # 质量与过期检查
│   ├── ResearchTaskCreateJob.java         # 研究任务创建
│   ├── DifyValidationCompensateJob.java   # Dify 结果校验补偿
│   ├── ResearchEventPublishCompensateJob.java # 研究事件发布补偿
│   ├── QuantFeatureCalcJob.java           # 量价基础特征计算
│   └── CapacityAndMarketStateJob.java     # 容量与市场状态更新
├── audit/
│   └── JobExecutionAuditRecorder.java # 写入既有 quantpf_job_execution_audit，scheduler_type 置 XXL_JOB
└── gate/
    └── SchedulerMigrationGate.java    # ★ 断言：XXL-JOB 未接入时 Handler 不注册；禁止双栈承载
```

#### C.1.6 `ruoyi-admin/src/main/java/com/quantpf/web`（扩展）

```text
├── config/
│   ├── QuantPfPaperRuntimeConfiguration.java   # 既有，保持不动
│   └── QuantPfDataFoundationConfiguration.java # ★ 新增：本模块全部 Bean 显式装配
├── controller/
│   ├── QuantDataSourceController.java          # /api/quant/data/source/**
│   ├── QuantSourceGroupController.java         # /api/quant/data/source-group/**（含 cross-check）
│   ├── QuantCollectTaskController.java         # /api/quant/data/collect-task/**、snapshot/**
│   ├── QuantDataQualityController.java         # /api/quant/data/quality/**、eligibility/**
│   ├── QuantInstrumentController.java          # /api/quant/instrument/**、tradability/**
│   ├── QuantLifecycleEventController.java      # /api/quant/event/**
│   ├── QuantCalendarController.java            # /api/quant/calendar/**（day/session/readiness/conflict/binding）
│   ├── QuantMarketDataController.java          # /api/quant/market/**
│   ├── QuantCapacityController.java            # /api/quant/capacity/**
│   ├── QuantResearchMaterialController.java    # /api/quant/research/material/**
│   ├── QuantResearchTaskController.java        # /api/quant/research/task/**
│   ├── QuantResearchEventController.java       # /api/quant/research/event/**（含 evidence）
│   ├── QuantManualReviewController.java        # /api/quant/review/**
│   └── QuantDifyCallbackController.java        # ★ /api/quant/research/dify/callback —— 独立、隔离
├── dto/                                         # 各 Controller 的请求 DTO（含校验注解）
└── vo/                                          # 响应 VO（三层披露：普通语言层 + 专业层分离字段）
```

> **隔离要求（ADR-05）**：`QuantDifyCallbackController` MUST 独立于 `QuantPaperOrderController`/`QuantPaperRiskController`/`QuantPaperFollowController`，MUST NOT 注入任何 Paper 应用服务，MUST 在 Sa-Token 拦截配置中单独声明（签名/共享密钥校验，不走用户登录态），MUST NOT 出现在 `contract/menu` 中。

#### C.1.7 `plus-ui`（新增 `quant` 视图域）

```text
src/api/quant/data/
├── readiness/{index.ts, types.ts}       source/{index.ts, types.ts}
├── sourceGroup/{index.ts, types.ts}     collectTask/{index.ts, types.ts}
├── quality/{index.ts, types.ts}         instrument/{index.ts, types.ts}
├── calendar/{index.ts, types.ts}        calendarConflict/{index.ts, types.ts}
├── lifecycleEvent/{index.ts, types.ts}  marketBar/{index.ts, types.ts}
├── capacity/{index.ts, types.ts}        researchMaterial/{index.ts, types.ts}
├── researchTask/{index.ts, types.ts}    review/{index.ts, types.ts}
src/views/quant/data/
├── readiness/index.vue                  ← 普通用户主入口
├── research-material/index.vue + components/{MaterialDetailDrawer.vue, DraftPanel.vue, TaskProgress.vue}
├── source/index.vue + components/{ProviderWizard.vue, EndpointTable.vue, FieldCatalogTable.vue}
├── source-group/index.vue + components/{CrossCheckTable.vue}
├── collect-task/index.vue + components/{TaskDetail.vue, SnapshotTable.vue, ImportDialog.vue}
├── quality/index.vue + components/{IssueBoard.vue, EligibilityMatrix.vue, IssueTimeline.vue}
├── instrument/index.vue + components/{InstrumentDetail.vue, VersionTimeline.vue, MappingReviewDialog.vue}
├── calendar/index.vue + components/{CalendarMatrix.vue, SessionTemplateTable.vue, CoverageGapPanel.vue}
├── calendar-conflict/index.vue + components/{ConflictResolvePanel.vue}
├── lifecycle-event/index.vue + components/{EventCalendar.vue, AdvanceNoticePanel.vue, RolloverTodoDialog.vue}
├── market-bar/index.vue + components/{DailyKlineChart.vue, AdjustFlagSwitch.vue, GapPanel.vue}
├── capacity/index.vue
src/components/quant/
├── GlobalDataStatusBar.vue              ← 详设 5 §8.2 全局状态条（B.5）
├── PlainLanguageBlocker.vue             ← B.3 阻断文案统一组件（三层披露第一层）
├── ProgressiveDisclosure.vue            ← 三层渐进披露容器（第二/三层默认折叠）
├── DraftBadge.vue                       ← 「智能体草稿 · 未核验」+ runId + 版本徽标（不可隐藏）
├── EligibilityMatrixTable.vue           ← 详设 1 §9.4 用途资格矩阵
└── HumanDecisionDialog.vue              ← 五态裁决对话框（理由必填，OVERRIDDEN 需风险影响+有效期）
src/enums/quant/data.ts                  ← 状态枚举与 i18n key 映射
src/lang/*/quant/data.ts                 ← B.3 全部普通语言文案（zh-CN 优先）
```
### C.2 关键类职责说明（含「不得做什么」）

#### C.2.1 数据源与采集

| 类 | 职责 | 输入 → 输出 | MUST NOT |
|:---|:---|:---|:---|
| `DataSourceRegistry` | 供应商/产品/接口/字段登记与状态推进；启用门禁校验 | 登记请求 → `DataSource` + 状态 | 不得在 `permission_confirmed=0` 或字段目录为空时置 `enabled=1`；不得存凭据明文 |
| `DataSourceHealthChecker` | 失败率/延迟/额度监控 → `health_status` + `fail_count_recent` + 降级 | 采集结果统计 → 健康状态 + 告警 | 不得因单次失败即判定不可用；不得静默降级而不生成待办 |
| `SourceGroupService` | 来源组配置、主备切换、`SINGLE_SOURCE` 判定 | 来源组配置 → 组状态 | 仅剩单一来源时不得放行方向性新开仓 |
| `SourceCrossCheckService` | 交叉核验 → `CONSISTENT`/`CONFLICTED`/`TIME_MISMATCH`/`MISSING` | 多源同主题数据 → 核验结果 | 不得把转载计为多个独立证据；不得静默择一 |
| `TusharePermissionProber` | 逐接口探测权限、字段类型、历史范围、重叠日期结果 | 接口清单 → 探测结果 + 能力配置 | 不得凭接口名或目录索引假定有权限；不得把探测失败当可用 |
| `CollectTaskService` | 任务生命周期 + 幂等 + 部分成功计数 | 采集请求 → `CollectTask` | 不得把 `PARTIAL_SUCCESS` 标记为 `SUCCEEDED`；重试不得换新幂等键 |
| `RawSnapshotStore` | 原始载荷落库（inline/external 阈值分流）+ 哈希 + 请求参数 + Schema 版本 | 原始响应 → `DataRawSnapshot` | 不得被解析结果覆盖；不得存凭据明文；解析失败不得丢弃原始载荷 |
| `ContentHashDeduplicator` | `content_hash` + `external_id` 双路去重与转载归并 | 新材料 → 命中/新建 + 归并指向 | 不得为重复材料创建第二条快照或第二个研究任务 |
| `StandardizationService` | 字段/单位/时区/日期口径标准化，原值与标准值双存 | 原始记录 → 标准化记录 | 不得丢弃原始值；不得用服务器时区替代市场本地日期 |
| `PointInTimeVersionService` | 点时版本新增与修订链 | 修订数据 → 新版本 + 旧版本 `superseded` | **不得 UPDATE 覆盖历史 `payload_ref`**；不得把不完整历史包装为完整样本 |

#### C.2.2 质量与资格

| 类 | 职责 | 输入 → 输出 | MUST NOT |
|:---|:---|:---|:---|
| `DataQualityEvaluator` | 十维度质量评估 | 数据 + 来源 + 时间 + 授权 → `DataQualityStatus` | 不得用默认值填充非法字段后判为可用 |
| `TimeVisibilityEvaluator` | 六时间字段完整性与传输延迟判定 | 时间字段 → `TimeVisibilityStatus` + 延迟秒数 | `published_at` 缺失时不得用采集时间伪造 |
| `DataQualityIssueService` | 问题生命周期管理 | 问题 → 状态流转 + 待办 | 不得从 `RESOLVED` 直接跳 `CLOSED`（必须经 `VERIFIED`） |
| `UsageEligibilityDecider` | 七输入 → 五资格 + 逐项原因 + 普通语言说明 | 资格查询 → `EligibilityResult` | 不得只返回第一个受限原因；不得用单一「可用/不可用」代替五项；`CONFLICTED`/`UNAUTHORIZED`/`INVALID` 下不得放行筛选与 Paper 新开仓 |

#### C.2.3 标的与事件

| 类 | 职责 | 输入 → 输出 | MUST NOT |
|:---|:---|:---|:---|
| `InstrumentIdentityResolver` | 身份解析与七类禁止合并判定 | 外部代码 + 交易所 → 身份 或 `MAPPING_REQUIRED` | **不得自动合并**七类情形；不得随机选主记录；不得模糊强匹配 |
| `InstrumentVersionService` | 交易属性版本区间闭合与 `current_version_id` 维护 | 属性变更 → 新版本 | 旧版本 `valid_until` 不得留空导致区间重叠；不得静默覆盖 |
| `InstrumentTradabilityService` | 综合可交易资格 | 资格查询 → 结论 + 全部受限原因 | 不得把日历开市单独当作可开仓充分条件 |
| `CorporateActionService` | 公司行为登记/核验/生效/发布 | 公告数据 → `CorporateAction` + 领域事件 | 线索级来源不得直接调整资格或复权口径 |
| `ContractEventService` | 合约与制度事件；提前提示待办生成 | 交易所规则数据 → `ContractEvent` + 待办 | 不得在事件发生后才提示；规则未核验时按最严格处理 |

#### C.2.4 日历

| 类 | 职责 | 输入 → 输出 | MUST NOT |
|:---|:---|:---|:---|
| `TushareTradeCalAdapter` | `trade_cal` 适配：定位为 A 股通用日历来源，`is_open` 按 **STRING** 解析 | 原始响应 → `CalendarSourceRecord` | 不得作为期货权威日历；不得与 `fut_trade_cal` 共用解析逻辑 |
| `TushareFutTradeCalAdapter` | `fut_trade_cal` 适配：`is_open` 按 **INT** 解析；覆盖 SHFE/DCE/CFFEX/CZCE/INE | 原始响应 → `CalendarSourceRecord` + 覆盖交易所集合 | 不得声称覆盖 GFEX；不得把 INT 与 STRING 混用 |
| `TushareHkTradeCalAdapter` / `TushareUsTradeCalAdapter` / `TushareFxObasicAdapter` / `TushareFxDailyAdapter` | 二期预留适配器，`enabled=false` 时不注册不采集 | — | 一期不得启用；hk/us 不得从响应推导交易所；`fx_daily.trade_date` 不得反推休市日历 |
| `RawIsOpenParser` | 严格类型解析开市标记，空值/类型不符 → `INVALID` | 原始值 + 声明类型 → 标准化布尔 或 `INVALID` | **不得把空值默认转为开市，也不得默认转为休市** |
| `CalendarNormalizationService` | 各适配器记录 → 统一内部模型，绑定 `calendar_code`、`market_timezone`、版本 | 源记录 → `TradingCalendarDay` | 不得用服务器时区自然日替代市场本地日期 |
| `CalendarConflictDetector` | 同 `calendar_code` 同日期多源比对，生成冲突记录与影响范围 | 多源日历日 → `CalendarConflict` | **不得静默择一**；冲突未解决时不得把 `quality_status` 置为 `AVAILABLE` |
| `CalendarCoverageGapService` | 登记覆盖缺口（GFEX 等），预置内部编码，生成人工维护待办 | 已授权接口覆盖集合 → 缺口清单 | 不得伪造为已被某来源覆盖 |
| `SessionTemplateService` | 时段模板与夜盘交易日归属 | 时段配置 → 模板 + 归属规则 | 未绑定标的不得用其他市场日历默认替代 |
| `MarketDataReadinessCalculator` | 按日历应到 + 实际到达 + 校验结果计算五级就绪与 `freshness_status` | 日历日 + 行情到达情况 → `MarketDataReadiness` | 不得仅凭墙钟时间假设数据完整；非交易日不得生成缺口告警；一期不得按秒级窗口判定；部分到达不得标记整体就绪 |

#### C.2.5 行情、量价与容量

| 类 | 职责 | 输入 → 输出 | MUST NOT |
|:---|:---|:---|:---|
| `MarketBarValidator` | OHLC 关系、非负量额、时段归属、日历归属、身份有效性 | 原始行情 → 通过 / `INVALID` + 问题记录 | **不得自动修正价格后入库**；不得用零或前值填充 |
| `MarketBarImportService` | `FULL`/`INCREMENTAL`/`REPAIR` 三模式导入 | 导入请求 → 结果统计 + 版本 | `FULL` 不得跳过二次确认；`REPAIR` 不得改动区间外数据 |
| `AdjustFlagService` | 复权口径管理与除权除息后按版本重算 | 标的 + 公司行为 → 复权序列新版本 | 不得把复权与非复权混为同一序列；不得覆盖 `NONE` 行 |
| `MarketStateCalculator` | 确定性规则计算市场状态 | 日线区间 + 规则版本 → `MarketState` | 数据不足不得推测，MUST 返回 `DATA_UNAVAILABLE`；不得接受 Dify 改写状态 |
| `QuantFeatureCalculator` | 一期基础量价特征与状态标签，二期标签字段恒 null | 日线 + 窗口 → `QuantFeatureSnapshot` | 不得实现自动择时；不得由特征状态直接生成订单；不得填写二期预留标签 |
| `CapacityAssessor` | 股票/期货容量因素评估 | 行情与合约属性 → `CapacityStatus` + 规模上限 | `UNTRADEABLE`/`UNKNOWN` 不得放行新开仓；不得因资金或保证金充足忽略退出风险 |
#### C.2.6 研究与 Dify 边界

| 类 | 职责 | 输入 → 输出 | MUST NOT |
|:---|:---|:---|:---|
| `ResearchMaterialService` | 统一材料索引（多态引用 + 唯一键防重） | 各类原始材料 → `ResearchMaterial` | 不得为同一 `(source_table, source_id)` 建第二条索引 |
| `MaterialGovernanceService` | 采集→原文→去重→时间→授权→质量→初步映射→`eligible_for_task` | 新材料 → 治理结果 | 未完成治理的材料不得进入研究事件主表或交易信号链路 |
| `IndustryInstrumentMapper` | 初步行业/标的映射 | 材料文本 → 映射候选 | 不确定时不得模糊强匹配，MUST 置 `UNRESOLVED` |
| `ResearchTaskService` | 任务创建、幂等、断点续跑、状态推进 | 触发 → `ResearchTask` | 不得为未治理材料创建自动任务；重试不得换新幂等键；不得要求用户重填已确认内容 |
| `ResearchTaskContextFreezer` | 冻结材料快照与授权上下文 | 任务 → `CONTEXT_FROZEN` + 快照引用 | 不得在冻结后允许上下文漂移 |
| `DifyWorkflowClient` | 出站调用：注入 `Idempotency-Key`/`requestId`、超时、指数退避、原始响应留存 | 任务上下文 → 草稿 JSON | **不得传递交易凭据、账户密钥或任何下单能力**；不得在 Schema 失败时盲目重试；不得传真实终端用户身份 |
| `DifyRequestBuilder` | 构造请求体（字段白名单） | 任务 + 材料 → 请求 JSON | 不得包含 Token、账户、资金、下单地址 |
| `DifyCallbackVerifier` | 入站校验：签名/共享密钥、`requestId`、任务状态、`dify_run_id` 唯一 | 回调 → 通过/拒绝 | 校验失败不得落库为草稿；不得放行无签名请求 |
| `DifyReadOnlyQueryFacade` | 供 Dify 读取的最小权限结构化结果 | 白名单查询 → 受控字段 | **不得开放数据库直连**；不得返回授权、凭据、行情主表数值、特征值、资格判定过程、订单与资金数据 |
| `DifyFieldWhitelist` | 白名单定义与强制 | 字段请求 → 允许/拒绝 | 白名单外字段 MUST 拒绝并记审计 |
| `AiDraftPersistService` | 草稿落库（Dify 输出唯一写入通道） | 原始响应 → `AiResearchDraft` | `is_fact_table` MUST 恒为 0；不得写入事实主表、研究事件主表、行情主表或信号表 |
| `AiDraftValidator` | 六项校验（Schema/引用可定位/时间/来源授权/对象身份/冲突） | 草稿 → `PASSED` 或具体失败态 | 校验失败不得静默丢弃；不得自动升级；不得用默认值或模板文本补全 |
| `EvidenceLocatorVerifier` | 引用在原文中的可定位性校验 | 引用 + 原文 → `locator_verified` | 定位失败的事实候选不得升级 |
| `FactVerificationService` | 草稿 → 核验事实 | 已校验草稿 → 核验事实 + 证据 | 不得跳过引用定位校验 |
| `ResearchEventService` | 研究事件状态机与版本 | 核验事实 → `ResearchEvent` | `VERIFIED` 前不得 `PUBLISHED`；不得被 Dify 直写；不得用草稿覆盖既有核验事实 |
| `MajorEventDispositionService` | 重大事件处置 | 重大研究事件 → 研究事件 + 风险复核任务 + 停新增风险建议 + 人工确认任务 | **不得产生平仓命令、开仓命令、`SELL_OPEN`、真实订单或 Paper 订单副作用** |
| `ResearchEventPublisher` | 事件发布、幂等、补偿重发 | `PUBLISHED` 事件 → 领域事件 | 重复投递不得产生重复副作用；发布失败不得丢失事件 |
| `ManualReviewTaskService` | 统一待办创建/认领/裁决/过期 | 待办 → 决策记录 | `OVERRIDDEN` 无理由与风险影响不得提交；过期不得沿用；人工覆盖不得绕过授权、身份与风控门禁 |
| `HumanDecisionRecorder` | 五态决策 + 当时可见数据版本留痕 | 裁决 → 决策记录 | 不得缺失 `visible_data_version_json` |
| `DifyDegradeManager` | 降级矩阵执行 | 故障类型 → 可继续/必须阻断清单 | 不得因 Dify 不可用而伪造结论；不得用绿色「系统正常」掩盖降级 |
| `DifyInvocationLogger` | 调用审计 | 每次出站/入站 → 日志 | 不得记录 `DIFY_API_KEY`、`TUSHARE_TOKEN` 或任何凭据；大载荷只存引用与哈希 |
| `SchedulerMigrationGate` | 调度双栈防护 | 环境配置 → Handler 是否注册 | XXL-JOB 未接入时不得注册 Handler；不得让 SnailJob 与 XXL-JOB 同时承载 QuantPF 任务 |

### C.3 与 Dify API 的调用约定

#### C.3.1 出站请求（Spring Boot → Dify）

```http
POST {quantpf.dify.base-url}/v1/workflows/run
Authorization: Bearer ${DIFY_API_KEY}          # 只从环境变量注入，MUST NOT 入库/入日志
Content-Type: application/json
X-QuantPf-Request-Id: {requestId}              # UUID，全链路追踪
X-QuantPf-Idempotency-Key: {idempotencyKey}    # = research_task.idempotency_key
X-QuantPf-Workflow-Version: {workflowVersion}
```

```jsonc
{
  "inputs": {
    "requestId": "…", "idempotencyKey": "…", "researchTaskId": "…",
    "taskType": "NEWS_FACT_EXTRACT",
    "materialIds": [123], "sourceRefs": ["…"], "subjectRefs": ["…"],
    "snapshotId": 456,
    "publishedAt": "2026-10-24T09:30:00+08:00",
    "availableAt": "2026-10-24T09:31:12+08:00",
    "authorizationStatus": "CONFIRMED",
    "workflowVersion": "wf-news-v1.3.0", "promptVersion": "p-news-v1.3.0",
    "materialContent": "…（授权文本，按白名单）",
    "readonlyContext": { }                      // 经 DifyReadOnlyQueryFacade 提供的白名单字段
  },
  "response_mode": "blocking",                  // 或 streaming，由 quantpf.dify.response-mode 配置
  "user": "quantpf-backend"                     // MUST NOT 传真实终端用户身份
}
```

**请求字段 MUST NOT 包含**：`DIFY_API_KEY`、`TUSHARE_TOKEN`、任何数据库凭据、Paper 账户 ID 与资金信息、下单接口地址、授权凭据原文。

#### C.3.2 出站响应（Dify → Spring Boot，对齐详设 1 §20.2）

```jsonc
{
  "requestId": "…", "researchTaskId": "…", "runId": "…",
  "status": "SUCCEEDED",
  "model": "…", "workflowVersion": "…", "promptVersion": "…",
  "summary": "…",
  "factCandidates": [ { "claim": "…", "citationRef": "…", "quoteLocator": { } } ],
  "impactScopeCandidate": "INDUSTRY",
  "impactLevelCandidate": "IMPORTANT",
  "directionCandidate": "BULLISH",
  "subjectCandidates": [ { "externalCode": "…", "exchange": "…", "confidenceNote": "…" } ],
  "citations": [ { "ref": "…", "quote": "…", "locator": { } } ],
  "counterQuestions": [ "…" ],
  "warnings": [ "…" ],
  "completedAt": "…"
}
```

`workflowVersion`/`promptVersion` 与请求不一致 → 校验失败（`SCHEMA_FAILED`）。全部字段落 `quantpf_ai_research_draft.raw_response_json`（原始全文）+ 拆列存储（便于查询）。

#### C.3.3 入站回调（Dify → Spring Boot，异步模式）

```http
POST /api/quant/research/dify/callback
X-QuantPf-Signature: {HMAC-SHA256(body, sharedSecret)}   # 由 DifyCallbackVerifier 校验
X-QuantPf-Request-Id: {requestId}
X-QuantPf-Idempotency-Key: {idempotencyKey}
```

校验顺序：`签名 → requestId 存在性 → 任务状态合法（DISPATCHED/RUNNING）→ dify_run_id 唯一（uk_draft_run）→ Schema`。任一步失败：记录 `quantpf_dify_invocation_log`（`outcome` 对应枚举），任务置对应失败态，**不落草稿、不升级、不重试**。仅网络超时按 `max_retry` 重试并复用同一幂等键；Schema、授权、身份类错误 MUST NOT 盲目重试。

#### C.3.4 只读白名单（`DifyFieldWhitelist`）

| Dify 可读 | Dify MUST NOT 读 |
|:---|:---|
| `quantpf_research_material`：标题、类型、来源等级、时间字段、质量状态标签 | `quantpf_data_source` 全表（授权、额度、凭据画像） |
| `quantpf_data_raw_snapshot`：新闻/公告/研报正文（按 `endpoint_name` 白名单） | 行情类、日历类、授权类快照载荷 |
| `quantpf_research_event`：已发布事件的结论、方向候选、影响范围 | `quantpf_endpoint_field_catalog`（接口能力清单） |
| `quantpf_market_data_readiness`：就绪结论与普通语言说明 | `quantpf_data_usage_eligibility`（资格判定过程） |
| `quantpf_source_cross_check`：`result` + `difference_detail` | `quantpf_market_bar` 原始行情数值 |
| `quantpf_quant_feature_snapshot`：`volume_price_state` / `market_state` **标签** | 量价特征值、算法版本、二期预留标签字段 |
| `quantpf_data_quality_issue`：`issue_type`/`business_impact`/`affected_usage` | `quantpf_instrument_version`（保证金、涨跌停等交易属性） |
| `quantpf_capacity_assessment`：`capacity_status` + 普通语言说明 | `quantpf_idempotency_record`、`quantpf_dify_invocation_log` |
| — | 一切 Paper 账户、资金、订单、成交、持仓、风控、熔断数据 |

#### C.3.5 超时、重试与降级（对齐详设 0 §16.4）

| 配置 | 默认 | 说明 |
|:---|:---|:---|
| `quantpf.dify.connect-timeout-ms` | 5000 | 连接超时 |
| `quantpf.dify.read-timeout-ms` | 120000 | 读超时（= `research_task.timeout_seconds`） |
| `quantpf.dify.max-retry` | 2 | 仅网络类错误重试 |
| `quantpf.dify.retry-backoff-ms` | 2000 | 指数退避基数（2s → 4s） |
| `quantpf.dify.retry-on-schema-error` | false | **MUST 为 false** |

降级矩阵（`DifyDegradeManager`）：

| 故障 | 可继续 | 必须阻断 |
|:---|:---|:---|
| Dify 超时/不可用 | 采集、标准化、质量评估、日历、就绪计算、既有 Paper、风控、审计 | 依赖新 AI 解释的事实升级、候选升级、计划生成 |
| Dify Schema 错误 | 保存原始响应 + 人工复核待办 | 候选升级与计划生成 |
| 签名校验失败 | 记录安全事件 | 该次回调全部处理 |
| 备源可用 | 使用备源并标记 | — |
| 全部行情源失效/日线未就绪 | 展示、复盘、风险监控 | 新开仓判断 |
| 授权不明 | 内部问题处理 | 该用途的下游使用 |
### C.4 配置文件需要新增的项

`ruoyi-admin/src/main/resources/application.yml`（通用默认）+ `application-dev.yml` / `application-prod.yml`（环境覆盖）。**当前 yml 中无任何 `quantpf.*` 键，本变更为首次引入。**

```yaml
quantpf:
  foundation:
    enabled: true                        # 本模块总开关，false 时 Bean 不装配
    phase: PHASE1                        # PHASE1；PHASE2_RESERVED 能力一律不启用
    daily-bar-only: true                 # ADR-02：一期只用日线，MUST 为 true
    allow-live-trading: false            # MUST 恒为 false；CI 断言
    allow-real-broker: false             # MUST 恒为 false；CI 断言
  data-source:
    health-check-cron: "0 0/30 * * * ?"
    fail-count-degrade-threshold: 3      # 连续失败达此值 → DEGRADED
    expiry-warning-days: 30              # 到期提醒窗口 → EXPIRING
    default-usage-scope: PERSONAL_RESEARCH
    strictest-on-unknown-authorization: true   # 授权不明按最严格处理
  tushare:
    enabled: true
    base-url: https://api.tushare.pro
    token: ${TUSHARE_TOKEN:}             # ★ 只从环境变量注入，MUST NOT 硬编码
    rate-limit-per-minute: 200
    concurrency-limit: 2
    connect-timeout-ms: 5000
    read-timeout-ms: 30000
    max-retry: 3
    permission-probe-on-startup: false   # 由手动或调度触发，避免启动阻塞
    endpoints:
      trade-cal:      { enabled: true,  phase: PHASE1, schema-version: "v1" }
      fut-trade-cal:  { enabled: true,  phase: PHASE1, schema-version: "v1" }
      hk-tradecal:    { enabled: false, phase: PHASE2_RESERVED, schema-version: "v1" }
      us-tradecal:    { enabled: false, phase: PHASE2_RESERVED, schema-version: "v1" }
      fx-obasic:      { enabled: false, phase: PHASE2_RESERVED, schema-version: "v1" }
      fx-daily:       { enabled: false, phase: PHASE2_RESERVED, schema-version: "v1" }
      stock-basic:    { enabled: true,  phase: PHASE1, schema-version: "v1" }
      fut-basic:      { enabled: true,  phase: PHASE1, schema-version: "v1" }
      daily-bar:      { enabled: true,  phase: PHASE1, schema-version: "v1" }
  calendar:
    coverage-gap-allow-manual: true      # GFEX 等缺口允许人工维护
    conflict-auto-resolve: false         # ★ MUST 为 false：禁止静默择一
    version-publish-require-confirm: true
    readiness-check-cron: "0 30 16 * * MON-FRI"
  marketdata:
    daily-bar-only: true
    import-default-mode: INCREMENTAL
    full-import-require-confirm: true
    adjust-flag-default: NONE
    validation-rule-version: "v1.0.0"
    readiness-freshness-max-delay-hours: 26   # 日线级新鲜度（非秒级）
  snapshot:
    inline-max-bytes: 65536              # 超过则只存 raw_payload_ref
    external-store-type: FILE            # FILE / OSS
    external-store-base-path: ${QUANTPF_SNAPSHOT_PATH:./data/quantpf/snapshots}
    archive-after-days: 90
    store-credentials-plaintext: false   # ★ MUST 为 false；CI 断言
  capacity:
    rule-version: "v1.0.0"
    plan-amount-turnover-ratio-limit: 0.05
    assess-cron: "0 40 16 * * MON-FRI"
  feature:
    algorithm-version: "v1.0.0"
    phase2-label-enabled: false          # ★ MUST 为 false：二期预留字段不启用
    calc-cron: "0 50 16 * * MON-FRI"
  dify:
    enabled: true
    base-url: ${DIFY_BASE_URL:}
    api-key: ${DIFY_API_KEY:}            # ★ 只从环境变量注入
    callback-shared-secret: ${DIFY_CALLBACK_SECRET:}   # ★ 只从环境变量注入
    callback-signature-required: true    # ★ MUST 为 true
    response-mode: blocking
    connect-timeout-ms: 5000
    read-timeout-ms: 120000
    max-retry: 2
    retry-backoff-ms: 2000
    retry-on-schema-error: false         # ★ MUST 为 false
    field-whitelist-strict: true         # ★ MUST 为 true
    grant-trade-capability: false        # ★ MUST 为 false；CI 断言
    read-only-query-enabled: true
    workflows:
      news-fact-extract: { id: "", version: "wf-news-v1.0.0",  prompt-version: "p-news-v1.0.0" }
      event-classify:    { id: "", version: "wf-event-v1.0.0", prompt-version: "p-event-v1.0.0" }
      doc-explain:       { id: "", version: "wf-doc-v1.0.0",   prompt-version: "p-doc-v1.0.0" }
      quality-explain:   { id: "", version: "wf-qual-v1.0.0",  prompt-version: "p-qual-v1.0.0" }
      qa-opportunity:    { id: "", version: "wf-qa-v1.0.0",    prompt-version: "p-qa-v1.0.0" }
  job:
    scheduler: NONE                      # NONE / XXL_JOB；★ MUST NOT 与 SnailJob 同时承载 QuantPF 任务
    xxl-job-enabled: false               # 迁移门禁通过后置 true
    executor-appname: quant-executor
    executor-port: 9999
    access-token: ${XXL_JOB_ACCESS_TOKEN:}
```

**敏感项纪律**（分册 §35.2、详设 5 §16）：`TUSHARE_TOKEN`、`DIFY_API_KEY`、`DIFY_CALLBACK_SECRET`、`XXL_JOB_ACCESS_TOKEN`、`DB_PASSWORD`、`REDIS_PASSWORD` MUST 只走环境变量或密钥管理；MUST NOT 出现在 yml 明文、日志、审计记录、`quantpf_dify_invocation_log`、异常堆栈或前端响应中。`application-dev.yml` 已有未提交修改，本变更 MUST 以**追加**方式修改，不得回滚既有内容。

### C.5 XXL-JOB Handler 清单与 Cron（对齐详设 1 §20.4，按 ADR-02 调整为日线口径）

| JobHandler | Cron | 重试 | 说明 | 与总纲 §16.8 的差异 |
|:---|:---|:--|:---|:---|
| `sourceHealthCheckJob` | `0 0/30 * * * ?` | 2 | 来源健康检查 | 新增 |
| `instrumentSyncJob` | `0 0 17 * * MON-FRI` | 3 | 股票/期货基础信息同步 | 新增 |
| `calendarSyncJob` | `0 0 3 1 * ?` | 2 | `trade_cal` / `fut_trade_cal` 月度同步 | 对齐总纲 `calendarSyncJob` |
| `calendarOverlapCompareJob` | `0 30 3 1 * ?` | 2 | 重叠区间比对与版本发布 | 新增 |
| `calendarConflictDetectJob` | `0 0 4 * * ?` | 2 | 冲突检测 + 人工覆盖待办 | 新增 |
| `marketBarIncrementalJob` | `0 0 16 * * MON-FRI` | 3 | 日线增量同步（收盘后） | **替代**总纲盘中频率行情同步（ADR-02） |
| `marketDataReadinessJob` | `0 30 16 * * MON-FRI` | 2 | 日线就绪状态计算 | 新增 |
| `instrumentBindingSyncJob` | `0 0 5 1 * ?` | 2 | 标的与日历绑定同步 | 新增 |
| `lifecycleEventSyncJob` | `0 0 6 * * ?` | 2 | 公司行为与合约事件同步 | 新增（含总纲 `autoCheckRolloverJob` 职责） |
| `newsCrawlJob` | 可配置（默认 `0 0/30 9-18 * * MON-FRI`） | 2 | 授权新闻源采集 | **合并**总纲 `crawlClsNewsJob`(30s)/`crawlSinaNewsJob`(1min)；频率改为数据源配置（详设 1 §7.5） |
| `disclosureCrawlJob` | `0 0 18 * * MON-FRI` | 3 | 公告/财报采集 | 对齐总纲 `crawlFinanceJob` |
| `rawSnapshotArchiveJob` | `0 0 2 * * ?` | 2 | 原始快照归档 | 新增 |
| `dataQualityCheckJob` | `0 0 7 * * ?` | 2 | 质量与过期检查 | 新增 |
| `researchTaskCreateJob` | `0 0/10 9-18 * * MON-FRI` | 2 | 研究任务创建 | 新增 |
| `difyValidationCompensateJob` | `0 0/15 * * * ?` | 3 | Dify 结果校验补偿 | 新增 |
| `researchEventPublishCompensateJob` | `0 0/15 * * * ?` | 3 | 研究事件发布补偿 | 新增 |
| `quantFeatureCalcJob` | `0 50 16 * * MON-FRI` | 2 | 一期量价基础特征 | 新增 |
| `capacityAndMarketStateJob` | `0 40 16 * * MON-FRI` | 2 | 容量与市场状态更新 | 新增 |
| ~~`tradingLoopJob`~~ | ~~`0 0/5 * * * ?`~~ | — | **一期不实现**（ADR-02 盘中频率） | 归二期 |
| ~~`positionSyncJob`~~ | ~~`0/5 * * * * ?`~~ | — | **一期不实现**（且一期不得同步真实账户） | 归二期 |
| ~~`analyzeNewsJob`~~ | ~~`0 0/5 * * * ?`~~ | — | 由 `researchTaskCreateJob` + `difyValidationCompensateJob` 承接 | 语义重划 |

**过渡策略**：`quantpf.job.scheduler=NONE` 且 `xxl-job-enabled=false` 时，`SchedulerMigrationGate` 阻止全部 Handler 注册，任务只能由 Controller 手动触发（便于 Phase 1A 前的联调）。SnailJob 保持底座现状不删不改；MUST NOT 让 SnailJob 与 XXL-JOB 同时承载 QuantPF 任务（分册 §34.3「不得双栈调度」）。

### C.6 契约先行清单（`contract/`）

| 文件 | 新增内容 |
|:---|:---|
| `contract/openapi/quantpf-data-foundation-v1.yaml` | 本模块全部 REST 端点（**新文件**，不改 `quantpf-v29.yaml` 既有 Paper 端点；CI 合并校验无路径冲突） |
| `contract/events/quantpf-collect-task-event.v1.schema.json` | 采集任务状态变更 |
| `contract/events/quantpf-data-quality-event.v1.schema.json` | 质量状态与问题生命周期 |
| `contract/events/quantpf-calendar-conflict-event.v1.schema.json` | 日历冲突检测与裁决 |
| `contract/events/quantpf-market-readiness-event.v1.schema.json` | 就绪状态变更 |
| `contract/events/quantpf-instrument-event.v1.schema.json` | 标的身份与版本变更 |
| `contract/events/quantpf-lifecycle-event.v1.schema.json` | 公司行为与合约事件生效 |
| `contract/events/quantpf-research-task-event.v1.schema.json` | 研究任务状态变更 |
| `contract/events/quantpf-research-event-published.v1.schema.json` | 研究事件发布（对齐总纲 §16.9 `ResearchEventPublishedEvent`） |
| `contract/error-codes/quantpf-error-codes.yaml` | 追加 `QPF-DATA-*`、`QPF-CAL-*`、`QPF-SUBJ-*`、`QPF-AI-*`、`QPF-AUTH-*`、`QPF-SRC-*`；**不改既有 `QPF-REQ/IDEMP/ORDER/CASH/POSITION/RISK/MARKET/FOLLOW` 语义** |
| `contract/menu/quantpf-menu-facts.yaml` | 追加「数据与标的」分组 12 个菜单 + 权限；保留 9 个既有 code 与 `forbiddenCapabilities` 原样 |

### C.7 测试目录与命名

```text
quantpf-modules/quantpf-common/src/test/java/com/quantpf/common/
  MarketLocalDateTest · TimeVisibilityTest · PayloadHasherTest · IdempotencyGuardTest
quantpf-modules/quantpf-marketdata/src/test/java/com/quantpf/marketdata/
  tushare/RawIsOpenParserTest                     ★ 空值/类型不符 → INVALID，不默认开市
  tushare/TushareTradeCalAdapterTest              ★ is_open STRING 解析
  tushare/TushareFutTradeCalAdapterTest           ★ is_open INT 解析 + 覆盖不含 GFEX
  tushare/TushareHkUsFxAdapterReservedTest        ★ 二期适配器 enabled=false 时不采集
  tushare/TusharePermissionProberTest
  calendar/CalendarNormalizationServiceTest       ★ 时区隔离、市场本地日期
  calendar/CalendarConflictDetectorTest           ★ 不静默择一
  calendar/CalendarCoverageGapServiceTest         ★ GFEX 缺口不伪造覆盖
  calendar/MarketDataReadinessCalculatorTest      ★ 交易日 vs 就绪分离、非交易日不告警
  market/MarketBarValidatorTest                   ★ OHLC 非法、负量、非交易日
  market/MarketBarImportServiceTest               ★ FULL/INCREMENTAL/REPAIR + 去重
  market/AdjustFlagServiceTest                    ★ 复权与非复权不混序列
  market/MarketStateCalculatorTest                ★ 数据不足 → DATA_UNAVAILABLE
  quality/DataQualityEvaluatorTest
  quality/UsageEligibilityDeciderTest             ★ 详设 1 §9.4 矩阵逐格断言
  ingest/ContentHashDeduplicatorTest              ★ 双路去重 + 转载归并
  ingest/PointInTimeVersionServiceTest            ★ 修订不覆盖历史
  instrument/InstrumentIdentityResolverTest       ★ 七类禁止合并
  instrument/InstrumentVersionServiceTest         ★ 版本区间闭合
  instrument/InstrumentTradabilityServiceTest
  capacity/CapacityAssessorTest
quantpf-modules/quantpf-research/src/test/java/com/quantpf/research/
  task/ResearchTaskServiceTest                    ★ 幂等、断点续跑
  dify/DifyWorkflowClientTest                     ★ 请求不含凭据、超时重试复用幂等键
  dify/DifyCallbackVerifierTest                   ★ 签名失败拒绝、run_id 唯一
  dify/DifyFieldWhitelistTest                     ★ 白名单外字段 MUST 被拒
  dify/DifyDegradeManagerTest                     ★ 降级矩阵逐行断言
  draft/AiDraftValidatorTest                      ★ 六校验各一失败用例
  event/ResearchEventServiceTest                  ★ 状态机 + VERIFIED 前不得 PUBLISHED
  event/MajorEventDispositionServiceTest          ★ MUST NOT 产生订单副作用
  event/ResearchEventPublisherTest                ★ 幂等 + 补偿
  review/ManualReviewTaskServiceTest              ★ 五态 + OVERRIDDEN 必填 + 过期
quantpf-modules/quantpf-domain/src/test/java/com/quantpf/domain/persistence/
  新增 22 个 *MapperIntegrationTest（沿用既有 TradingAccountMapperIntegrationTest 风格）
ruoyi-admin/src/test/java/com/quantpf/web/controller/
  QuantDataFoundationApiWebIntegrationTest
  QuantDifyCallbackIsolationTest                  ★ 回调无法触达 Paper 服务
  QuantPaperRegressionAfterDataFoundationTest     ★ 13 个既有 API 全回归
  QuantNoLiveTradingPathContractTest              ★ 断言无 LIVE/QMT/CTP 路径
```

**回测对拍**：本模块不实现回测引擎，但 MUST 提供「点时数据回放对拍」测试：给定历史日期，`PointInTimeVersionService` 返回的版本 MUST 等于当时可见版本，MUST NOT 返回后续修订值（详设 0 §17.1「回测对拍」）。
---

## Risks / Trade-offs

| 风险 | 影响 | 缓解 |
|:---|:---|:---|
| **Tushare 账号实际权限与文档不符**（积分不足、接口未开通、字段变更、单次返回量限制） | 日历与标的同步无法启用，Phase 1A 阻塞 | `TusharePermissionProber` 上线前逐接口实测，结果写 `permission_confirmed` + `permission_probe_result`；未通过探测的接口 `enabled=0`；来源组配置备源；缺口走 `CalendarCoverageGapService` 人工维护而非伪造 |
| **GFEX 无授权日历来源** | 广期所品种（工业硅、碳酸锂等）无法判定可交易资格 | 预置 `CN_FUTURE_GFEX` 且 `coverage_status='GAP'`；该所合约资格返回「无法判定」并生成待办；一期不纳入 GFEX 品种的候选与 Paper 范围 |
| **放宽 `quantpf_instrument` 的 CHECK 约束**可能影响已落地 Paper 资格判定 | Paper 下单链路行为变化，回归失败 | 只**放宽**不收紧，保留 `DELISTED` 等既有取值；新增 `identity_status` 独立承载身份语义，不复用 `status`；`QuantPaperRegressionAfterDataFoundationTest` 全量回归 13 个既有 API |
| **ALTER 已落地表**在含数据环境执行失败或锁表 | 迁移中断，环境不可用 | 迁移前备份；`ADD COLUMN` 全部带默认值或允许 NULL；MySQL 8.0 INSTANT/INPLACE 算法优先；每个迁移单独文件便于定位；提供前向修复迁移而非回滚历史迁移 |
| **多态材料索引**（`source_table` + `source_id`）无数据库外键约束 | 悬挂引用，材料指向已删除记录 | `(source_table, source_id)` 唯一键 + 应用层引用完整性校验 + 定时一致性检查任务；删除原始记录 MUST 走软删并同步索引状态 |
| **Dify 输出不稳定**（Schema 漂移、引用幻觉、方向过度自信） | 草稿污染研究链，用户误信 | 六项校验强制；草稿区与事实主表物理分离（`is_fact_table=0`）；界面固定「智能体草稿 · 未核验」徽标；`retry-on-schema-error=false`；`VERIFIED` 前不得 `PUBLISHED` |
| **Dify 回调成为绕过门禁的后门** | 违反详设 1 §18.3 与 Stage 1 边界 | 回调 Controller 物理隔离、不注入任何 Paper 服务、强制 HMAC 签名校验、`QuantDifyCallbackIsolationTest` 断言不可达 Paper；`grant-trade-capability=false` 由 CI 断言 |
| **一期日线口径与总纲 §16.8 盘中调度冲突**未回写 | 后续变更按总纲实现分钟线，返工 | ADR-02 明确裁决 + 待回写清单（见 Migration Plan 第 5 步）；`daily-bar-only=true` 由配置与 CI 双重锁定 |
| **表数量多（22 张新增 + 5 张 ALTER）**，一次变更体量大 | 施工周期长、评审困难、易半途而废 | tasks.md 按「数据库 → 后端 → Dify → 前端 → 联调」五段拆分，每段可独立验收；Flyway 按能力分 12 个迁移文件，可分批合入 |
| **菜单与 `contract/menu` 未闭合**（当前 DB QuantPF 菜单 0 行） | 新页面在若依中不可见、权限不生效 | `V1_21_0` 幂等写 `sys_menu` + 同步更新 YAML + CI 一致性检查（ADR-10）；MUST NOT 触碰既有 109 条通用菜单 |
| **调度双栈风险**（SnailJob 仍在 pom，XXL-JOB 未接入） | 违反分册 §34.3 门禁 | `SchedulerMigrationGate` + `quantpf.job.scheduler=NONE` 默认不注册 Handler；SnailJob 不删不改；联调期任务由 Controller 手动触发 |
| **JDK 版本门禁**（BASELINE 记录 shell 为 Java 8/17，根 POM 要求 21） | 编译与测试无法执行 | 施工与验收 MUST 在 JDK21 进程环境；tasks.md 首项即工具链校验，未通过不进入后续任务 |
| **原始快照体积膨胀** | 存储与备份压力 | `inline-max-bytes` 阈值分流到外部存储；`rawSnapshotArchiveJob` 定期归档；`retention_scope` 按授权决定保存范围，`NO_STORE`/`METADATA_ONLY` 来源不存正文 |
| **普通用户体验验收不达标**（专业字段泄漏到必填项） | 违反总纲 §4.4 与详设 0 §17.3 门禁 | 字段责任标记 `[S]/[U]/[R]/[A]` 落到表设计与 VO；`ProgressiveDisclosure` 组件强制三层；B.7 步数核算作为验收用例；前端 lint 规则禁止把 `[S]` 字段渲染为必填 |

**主要取舍**：
- **实时计算资格 + 留痕**（ADR-06）换取正确性，代价是每次查询有计算开销 → 用 `quantpf_data_usage_eligibility` 唯一键做当日缓存式留痕，重复查询命中留痕。
- **保留既有表名 + 新增定义表**（ADR-03）换取零迁移风险，代价是 `quantpf_trading_calendar` 表名与其「日历日」语义不完全匹配 → 在领域层用 `TradingCalendarDay` 命名纠正，表名保持不变并加注释。
- **一期不建时序库**（ADR-09）换取运维简单，代价是二期下推需要迁移 → `storage_tier` 字段预留，二期可双写过渡。
- **统一待办表**（ADR-07）换取待办中心聚合简单，代价是 `payload_json` 弱类型 → 应用层按 `task_type` 做 Schema 校验 + 每类独立服务与测试。

## Migration Plan

1. **工具链与基线校验**（前置门禁）：确认 JDK21 进程环境、Maven Reactor `validate` 通过、`pnpm` 版本匹配；记录当前 `git status` 快照，确认既有未提交改动清单，全程 MUST NOT `checkout`/`reset`/`clean`。
2. **契约先行**：提交 `contract/openapi/quantpf-data-foundation-v1.yaml`、8 个事件 Schema、错误码追加、`menu-facts` 追加；CI 校验 YAML 合法、路径与既有 `quantpf-v29.yaml` 无冲突、`forbiddenCapabilities` 未被修改。
3. **数据库迁移**：按 `V1_10_0` → `V1_21_0` 顺序在** disposable/test schema** 上先行验证（Phase1C handoff 记录现有集成测试会写删数据，MUST 单独授权测试库），再在 `quantpf` 库执行；每个迁移后跑对应 Mapper 集成测试。
4. **后端分能力落地**：按 `common → domain → marketdata(source/ingest/quality) → marketdata(instrument/calendar) → marketdata(market/capacity) → research(material/task/dify/draft/event/review) → job → web` 顺序，每个能力落地后立即跑该能力的单元测试与 API 集成测试。
5. **文档回写清单**（本变更**只产出清单，不改文档正文**）：
   - 总纲 §3.5 / §5.7 / §7.8 / §16.8：按 ADR-02 标注一期日线边界，盘中频率调度移至二期；
   - 总纲 §16.2 / §16.3 / §16.5：按 ADR-01 与分册 §26.2 移出 `qs_*`/`qt_*`/`qn_*`/`qf_*` 表名与类名；
   - 分册 §26.3 数据分层：表名统一到 `quantpf_*`；
   - 分册 §31.3：补齐「敏感信息」占位内容（当前为【待补齐】）；
   - 分册 §33 / §34.2 / 落地进度总览：施工完成后三处同步更新并刷新快照日期；
   - `openspec/config.yaml`：业务文档路径由 `D:\我的文档\私人\量化` 修正为 `D:\workspace\quant\doc`（属独立变更）；
   - `doc/schema.sql`：加注「V21 遗留，已作废，禁止执行」。
6. **前端落地**：`plus-ui` 新增 `src/api/quant/data/**` 与 `src/views/quant/data/**`、`src/components/quant/**`、i18n 文案；`pnpm lint` + `pnpm build:dev` 通过；菜单在若依中可见且权限生效。
7. **联调与门禁验收**：端到端跑通「供应商登记 → 接口探测 → 来源组配置 → 采集 → 快照去重 → 标准化 → 质量评估 → 用途资格 → 标的身份 → 日历同步 → 冲突裁决 → 就绪计算 → 日线导入 → 市场状态 → 容量 → 研究材料 → 研究任务 → Dify 草稿 → 校验 → 研究事件发布」；执行详设 0 §17.2 阶段门禁检查与 §17.3 普通用户体验验收。
8. **回归保护**：`QuantPaperRegressionAfterDataFoundationTest` 全绿（13 个既有 API、订单状态机、资金台账、风控闸门、跟盘、审计）；`QuantNoLiveTradingPathContractTest` 断言无 `LIVE`/`QMT`/`CTP`/真实 `submit`/`cancel` 路径。

**回滚策略**：Flyway Community 无 undo，回滚 = **前向修复迁移**。每个迁移执行前 MUST 有数据库备份点；若某迁移失败，MUST 修复后前向推进，MUST NOT 手工改 `flyway_schema_history`。代码层回滚以 Git 分支为单位，本变更全部工作在独立分支进行，MUST NOT 影响既有未提交改动。功能开关 `quantpf.foundation.enabled=false` 可在不回滚代码的情况下停用本模块全部 Bean（既有 Paper 链路不受影响）。

## Open Questions

以下问题可延后回答，**不影响本变更的 specs、设计方法与任务拆分**：

- **Q1**：`PaperRiskGate` 现有 staleness 窗口为 60 秒（面向实时行情）。一期改为日线口径后，该窗口应改为「数据基准日期 vs 日历应到日期」还是保留秒级窗口但由 `MarketDataReadiness` 提供输入？→ 属模块四（详设 4）职责，本变更只输出 `MarketDataReadiness`，不改 `PaperRiskGate` 行为；由后续模块四变更决定接入方式。
- **Q2**：原始快照外部存储选 `FILE` 还是 `OSS`？一期单机部署 `FILE` 足够，若后续多实例部署需切 `OSS`。→ 由 `quantpf.snapshot.external-store-type` 配置切换，不影响表结构与接口。
- **Q3**：Tushare 之外是否需要立即接入 AkShare 作为行情备源？→ 取决于实际账号权限探测结果与授权确认；来源组模型已支持多源，接入新备源属增量配置而非结构变更。
- **Q4**：`quantpf_research_material` 的多态索引是否需要为公告/研报建立独立原始表（类似 `quantpf_research_news`）？→ 一期公告/研报可先复用快照 + 材料索引，若字段差异过大再在后续变更中拆表；ADR-04 的索引层设计已为此留出空间。
- **Q5**：字典表（`sys_dict_*`）中的枚举是否需要与 Java 枚举做 CI 一致性校验？→ 建议做，但属工程效能改进，可在联调阶段追加，不阻塞主链路。
- **Q6**：普通用户「问一下」自然语言问答（`QA_OPPORTUNITY`/`QA_INSTRUMENT` 任务类型）在一期是否开放？→ 总纲 §14.3 列为一期刚需，但其完整问答编排属详设 2 §20 与模块二范围；本变更只落地任务类型与 Dify 调用通道，问答业务规则由模块二变更定义。