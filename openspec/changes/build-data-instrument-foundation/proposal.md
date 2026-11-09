# Proposal

## Why

QuantPF 当前只落地了 Paper 模拟盘后端闭环（`quantpf-domain` / `execution` / `finance` / `risk` + 13 个 REST API + Flyway `V1_0_0`~`V1_9_0`），而 `quantpf-marketdata`、`quantpf-research`、`quantpf-common` 仍是**零 Java 源文件的空骨架**。这意味着系统目前没有任何一条「数据从哪来、是否被授权、什么时候可见、质量如何、标的到底是谁、今天是不是交易日、日线到没到、这份数据能不能用于新开仓」的可验证链路。

总纲 §24.1 把「所有事实可追溯到来源、所有数据有可见时间、修订不覆盖历史、授权与延迟被记录、历史完整性不被伪装」列为第一顺位业务成功标准；详设 0 §15.5 与详设 1 §22.2 也把**数据采集与治理**排在全部施工顺序的第一位（`Phase0 门禁 → 数据采集与治理 → 研究任务与 Dify 适配 → 事实核验 → 候选 → 策略 → Paper`）。模块二（研究与候选发现）、模块三（策略与交易决策）、模块四（风险、交易与验证）全部依赖本模块输出的**标的身份、交易日历、数据质量状态与用途资格**；没有这一层，下游任何「候选」「概率」「回测」结论都无法满足总纲 §12 的可验证要求，也无法通过详设 0 §17.2 的阶段门禁。

因此本变更建设**详设模块一：数据与标的基础**，把总纲第 5 章的数据体系、详设 1 的八个子域落成可运行、可审计、可回放的代码底座。

## What Changes

### 新增能力

- **数据源与供应商治理**：供应商 / 数据产品 / 接口 / 字段 / 历史范围 / 更新频率 / 延迟 / 调用额度 / 保存范围 / 使用范围 / 费用 / 到期日的逐级登记，供应商状态机（`DRAFT → PENDING_CONFIRMATION → ACTIVE → DEGRADED → EXPIRING → EXPIRED → SUSPENDED → RETIRED`），以及「连接成功 ≠ 业务可用」的启用门禁。
- **来源组与交叉核验**：每类关键研究材料配置 2~3 个可核验主来源 + 1 个备源，记录主备优先级、授权状态、覆盖范围、延迟、历史回溯能力与失效替代规则；来源冲突、来源时间不一致、授权不明、仅剩单一来源时**降低研究状态并禁止方向性新开仓**。
- **采集任务与原始快照**：采集任务状态机（`CREATED / RUNNING / SUCCEEDED / PARTIAL_SUCCESS / FAILED / RETRYING / CANCELED`）、原始响应优先保存、`raw_payload_hash` + `external_id` 双路去重、请求参数与响应 Schema 版本留痕。
- **标准化、去重与点时版本**：字段 / 单位 / 时区 / 标的身份标准化；`published_at`、`available_at`、`effective_at`、`observed_at`、`revised_at`、`snapshot_version` 六个时间维度；任何修订保留旧版本，禁止静默覆盖历史决策依据。
- **数据质量、授权与用途资格**：质量状态机（`AVAILABLE / DELAYED / PARTIAL / CONFLICTED / UNAUTHORIZED / STALE / INVALID / PENDING_REVIEW`）、`usage_scope`（`PERSONAL_RESEARCH / TEAM_INTERNAL / COMMERCIAL_SERVICE / NO_REDISTRIBUTION`）、质量问题生命周期（`OPEN → ANALYZING → WAITING_SOURCE / WAITING_HUMAN → RESOLVED → VERIFIED → CLOSED`），以及详设 1 §9.4 的**用途资格矩阵**（展示 / 研究 / 筛选 / 回测 / Paper 新开仓分别判定）。
- **标的身份与版本**：股票、期货具体合约、连续/主力合约、现货对象的身份区分与禁止自动合并规则；标的状态机（`DISCOVERED → MAPPING_REQUIRED → VERIFIED → ACTIVE → SUSPENDED → EXPIRED → RETIRED`）；交易属性变化生成 `InstrumentVersion`（`validFrom` / `validUntil` / `changeReason` / `sourceRef`）。
- **公司行为与合约事件**：除权除息、分红、停牌、ST/退市、重组、财报日；期货交割月、最后交易日、移仓窗口、保证金与涨跌停规则变化、交易制度事件；事件状态与生效处理要求。
- **交易日历、时段与就绪状态**：`TradingCalendar` / `TradingCalendarDay` / `TradingSessionTemplate` / `InstrumentCalendarBinding` / `CalendarSourceSnapshot` / `CalendarConflict` / `MarketDataReadiness` 一组对象（**日历不是单表**）；Tushare `trade_cal`、`fut_trade_cal` 逐接口适配（原始值 + 标准化值双存、类型不混用、GFEX 缺口不伪造）；日历冲突检测与人工覆盖待办；日线数据「应到 / 已到 / 已校验 / 策略可用 / Paper 可用」就绪状态计算。
- **日线行情、市场状态与容量**：日线 OHLCV 合法性检查、`FULL / INCREMENTAL / REPAIR` 三种导入模式、市场状态（`TREND_UP / TREND_DOWN / RANGE / HIGH_VOLATILITY / LOW_LIQUIDITY / SYSTEMIC_RISK / DATA_UNAVAILABLE / SPECIAL_RULE`）、容量状态（`SUFFICIENT / LIMITED / HIGH_IMPACT / UNTRADEABLE / UNKNOWN`）与冲击成本基础评估。
- **研究材料与研究任务入口**：新闻 / 公告 / 财报 / 研报材料的采集、去重、时间、授权、质量与初步标的行业映射；创建 `ResearchTask`；调用 Dify Workflow 生成研究草稿；保存 AI 原始响应；执行 Schema / 引用 / 时间 / 授权 / 身份 / 冲突校验；升级为核验事实与 `ResearchEvent` 并发布下游事件。
- **量价特征快照的二期预留**：`QuantFeatureSnapshot` 表结构、特征版本、观察窗口、信号时间、市场状态、成功/失败标签与点时回放字段。**仅预留存储与接口边界，一期不实现任何自动择时或时机优化。**

### 修改与扩展

- **扩展已落地表**（只加字段/索引，不破坏既有约束与 Paper 链路）：`quantpf_instrument` 增加市场、上市状态、价格最小变动、公司标识等身份字段并放宽 `product_type` / `status` CHECK 至详设 1 §11.6 全集；`quantpf_trading_calendar` 增加市场本地时区、上一交易日、来源接口、原始 `is_open` 值、日历版本与质量状态；`quantpf_market_bar` 增加复权口径、数据质量状态、来源快照引用与点时版本。
- **扩展契约**：`contract/openapi` 新增 `/api/quant/data/*`、`/api/quant/instrument/*`、`/api/quant/calendar/*`、`/api/quant/market/*`、`/api/quant/capacity/*`、`/api/quant/research/task/*`、`/api/quant/research/event/*` 分组；`contract/events` 新增数据采集、质量、日历、标的、研究任务、研究事件类事件 Schema；`contract/error-codes` 新增 `QPF-DATA-*`、`QPF-CAL-*`、`QPF-SUBJ-*`、`QPF-AI-*`、`QPF-AUTH-*` 系列；`contract/menu` 与 `sys_menu` 闭合本模块菜单与 `quant:*` 权限标识（当前数据库 QuantPF 菜单为 0 行）。
- **新增前端页面**：`plus-ui` 在「数据与标的」分组下新增数据源与供应商、来源组与核验、采集任务与快照、数据质量与问题、标的中心、交易日历与时段、日历冲突待办、公司行为与合约事件、日线行情、数据就绪看板、容量与流动性、研究材料与任务等页面，并实现引导 / 审核 / 专业三种体验模式与全局状态条。
- **新增配置项**：`quantpf.data-source.*`、`quantpf.tushare.*`、`quantpf.dify.*`、`quantpf.calendar.*`、`quantpf.marketdata.*`、`quantpf.snapshot.*`；`TUSHARE_TOKEN`、`DIFY_API_KEY` 只走环境变量 / 密钥管理，禁止硬编码与明文入库。

### 需要裁决的文档冲突（本变更内做出决定并记录 ADR）

- **一期行情边界**：总纲 §3.5 / §5.7 / §7.8 允许分钟线、盘口、逐笔用于市场确认与模拟执行，§16.8 含 5 分钟 `tradingLoopJob` 与 5 秒 `positionSyncJob`；但详设 5 表头与 D5-06、详设 4 L1174 明确「一期业务只使用日线，不实现分钟线、Tick 和盘中日内界面/调度」。本变更**按详设 5（用户已确认方案）与更严格边界裁决为一期只用日线**，盘中频率调度与分钟/Tick/盘口归入二期，并在 `design.md` 记录 ADR 与总纲回写建议。
- **表命名三代并存**：`doc/schema.sql`（V21 时代 `qs_/qt_/qm_/qn_/qf_` 29 表，含 `DROP TABLE`）、分册 §26.3（`qm_stock_basic` / `qs_kline_bar`）、代码实际 `quantpf_*`。本变更**统一收敛到 `quantpf_*` 且只走 Flyway**，禁止执行任何历史 SQL 脚本。
- **总纲违反自身拆分规则**：总纲 §26.2 规定总纲不再出现表结构、接口路径、类名，但 §16–17 仍含 `StrategyEngineService.runAll`、`qs_strategy_param_config`、`qt_trade_signal` 等。本变更以分册 + 详设 + 代码为准，仅记录待回写项，**不在本变更内修改总纲**。
- **业务文档路径失效**：`openspec/config.yaml` 指向 `D:\我的文档\私人\量化`，该目录不存在；实际文档位于 `D:\workspace\quant\doc`。本变更按实际路径引用，并记录 `config.yaml` 待修正项（修改 workflow 配置属独立变更，不在本变更内执行）。

### 非目标（Non-Goals）

- 港股 / 美股 / 外汇与海外经纪商品的日历、品种与时段规则（二期；本期仅预留 `calendar_code`、`market_timezone`、`classify`、`fx_session_rule` 扩展位）。
- 分钟线、Tick、盘口实时数据、盘中日内交易界面与盘中频率调度（二期）。
- 量价关系优化、交易时机研究、自动择时模型、完整订单流与图表工作台（二期）。
- 真实证券 / 期货行情交易通道、QMT / CTP 真实 `submit` / `cancel`、`LIVE` 路由、真实资金交易（Stage 1 永久禁止）。
- Dify 直写事实主表、研究事件主表、行情主表、量价特征主表，或直接开仓 / 平仓 / 下单。
- 候选筛选与研究卡（模块二）、策略适配与交易计划（模块三）、组合风险与 Paper 执行、回测与概率验证（模块四）。
- 用不完整历史证明策略概率；未授权数据的团队共享、对外展示或再分发。
- SnailJob → XXL-JOB 的调度底座迁移本身（属 Phase 0 门禁；本变更只提供 Handler 与审计对接，不引入双栈调度）。

## Capabilities

### New Capabilities

- `data-source-governance`: 供应商、数据产品、接口与字段目录、授权用途、费用与到期、来源组主备与交叉核验、来源健康与降级。
- `data-ingestion-snapshot`: 采集任务生命周期、原始快照保存、内容哈希与外部 ID 去重、标准化、点时版本与修订链。
- `data-quality-eligibility`: 数据质量状态、时间可见性、授权范围、质量问题生命周期，以及展示 / 研究 / 筛选 / 回测 / Paper 新开仓的用途资格判定。
- `instrument-identity`: 股票、期货具体合约、连续/主力合约、现货对象的身份识别、禁止自动合并规则、标的状态机与交易属性版本。
- `instrument-lifecycle-events`: 公司行为（除权除息、分红、停牌、ST/退市、重组、财报日）与期货合约事件（交割月、最后交易日、移仓窗口、保证金与涨跌停变化、制度事件）的登记、生效与下游通知。
- `trading-calendar`: 多市场交易日历、时段模板、标的与日历绑定、Tushare 日历接口适配、日历冲突与人工覆盖、日线数据就绪状态。
- `market-data-foundation`: 日线行情采集与合法性检查、导入模式、复权口径、市场状态、量价特征快照的二期预留，以及交易容量与冲击成本基础评估。
- `research-material-intake`: 研究材料（新闻 / 公告 / 财报 / 研报）的采集治理入口、`ResearchTask` 创建、Dify Workflow 调用与草稿保存、AI 输出校验、核验事实与 `ResearchEvent` 升级与发布。

### Modified Capabilities

（无。`openspec/specs/` 当前为空，本项目尚无已发布能力规格；已落地的 Paper 模拟盘能力在本变更中只被复用与回归保护，其规格将在后续独立变更中补写。）

## Impact

### 受影响代码

| 路径 | 影响 |
|:---|:---|
| `db/migration/` | 新增 `V1_10_0` 起的迁移：数据源与来源组、快照与质量、标的身份与版本、日历与时段、公司行为与合约事件、行情扩展、容量、研究材料与任务、AI 草稿与研究事件、量价特征预留、索引与约束 |
| `quantpf-modules/quantpf-common/` | 从空骨架落地：时间与时区、来源与授权、版本与质量、事件元数据、公共枚举与值对象、公共异常 |
| `quantpf-modules/quantpf-domain/` | 新增本模块领域实体、枚举、值对象、仓储端口与 Mapper；扩展 `Instrument` |
| `quantpf-modules/quantpf-marketdata/` | 从空骨架落地：Tushare 适配器、采集任务、原始快照、标准化、质量评估、日历服务、就绪计算、行情导入、市场状态、容量评估 |
| `quantpf-modules/quantpf-research/` | 从空骨架落地：研究材料、`ResearchTask`、Dify 适配器、AI 草稿校验、`ResearchEvent` 与证据、事件发布 |
| `quantpf-modules/quantpf-job/` | 新增本模块 XXL-JOB Handler 与任务执行审计对接（不引入双栈调度） |
| `ruoyi-admin/src/main/java/com/quantpf/web/` | 新增 controller / dto / vo；Dify 回调入口与 Paper 订单 API 物理隔离 |
| `contract/openapi/`、`contract/events/`、`contract/error-codes/`、`contract/menu/` | 契约先行扩展 |
| `plus-ui/src/views/quant/`、`plus-ui/src/api/quant/` | 新增「数据与标的」分组页面与 API 封装 |
| `ruoyi-admin/src/main/resources/application-*.yml` | 新增 `quantpf.*` 配置项 |

### 受影响接口与依赖

- 新增 REST 分组：`/api/quant/data/source/*`、`/api/quant/data/quality/*`、`/api/quant/data/snapshot/*`、`/api/quant/instrument/*`、`/api/quant/instrument/tradability/*`、`/api/quant/calendar/*`、`/api/quant/calendar/day|session|readiness|conflict/*`、`/api/quant/event/*`、`/api/quant/market/*`、`/api/quant/capacity/*`、`/api/quant/research/task/*`、`/api/quant/research/event/*`。
- 新增 Spring 领域事件与 Redis 主题（对齐总纲 §16.9）：`NewsCrawledEvent`、`ResearchTaskCreatedEvent`、`AiResearchDraftCompletedEvent`、`ResearchEventVerifiedEvent`、`ResearchEventPublishedEvent`、`QuantFeatureCalculatedEvent`，以及数据质量、日历冲突、就绪状态变更事件。
- 外部依赖：Tushare Pro（按实际账号权限逐接口探测，未授权接口不得列为可用来源）、授权新闻/公告/财报来源、Dify 独立部署实例。
- 底座复用：RuoYi Sa-Token 认证授权、`sys_menu` / `sys_role` / `sys_dict_*` / `sys_oper_log`、Flyway、MyBatis-Plus、Redis。**不新建用户、角色、菜单、字典、操作日志表。**

### 数据授权假设

- Tushare Pro 年度订阅**只表示服务购买周期**，不代表全部接口、字段、历史范围、频率、额度、保存权与商业再分发权已获得；施工前必须按实际账号建立「接口—字段—历史范围—更新频率—延迟—额度—保存范围—使用范围—费用—到期日」清单并逐接口探测。
- 一期 `usage_scope` 默认 `PERSONAL_RESEARCH` / `TEAM_INTERNAL`；授权不明时按最严格限制处理，不进入对外展示、自动交易或商业服务链路。
- 网页可见 ≠ 可自动抓取 ≠ 可长期保存原文 ≠ 可批量计算再分发 ≠ 可商用；不绕过登录、验证码、访问控制、付费墙与反爬措施。
- 财联社 / 新浪 / 雪球等非官方接口无 SLA，必须配置可用性监控与自动降级。

### 用户影响

- **引导型普通用户**：新增「数据就绪看板」与「研究材料」两个主要入口，只看普通语言的「数据是否就绪、能不能用、为什么不能用、下一步做什么」；创建常用研究任务核心输入 ≤ 3 项；不填写任何数据源、指标、日历、授权类专业参数。
- **业务审核用户**：新增日历冲突待办、数据质量问题待办、标的身份映射待办、AI 草稿复核待办；人工决策记录 `CONFIRMED / REJECTED / DEFERRED / OVERRIDDEN / EXPIRED` 并关联当时可见数据版本。
- **专业管理用户**：新增供应商与接口字段目录、来源组、授权用途、采集频率、日历版本、标的版本、量价特征版本的管理入口。

### 验收结果（Acceptance Outcomes）

- 任一事实可回溯到来源、原始快照、哈希、采集时间与授权状态；六个时间字段齐备。
- 数据修订保留旧版本，历史决策依据不被静默覆盖；点时快照可按 `snapshot_version` 回放。
- 用途资格矩阵对 `CONFLICTED / UNAUTHORIZED / STALE / INVALID / DELAYED / PARTIAL / PENDING_REVIEW` 分别正确限制展示 / 研究 / 筛选 / 回测 / Paper 新开仓。
- Tushare 各接口原始值与标准化值双存；`is_open` 解析失败进 `INVALID` 而非默认开市；GFEX 缺口生成待办而非伪造覆盖。
- 标的身份对「不同交易所同代码」「连续合约 vs 具体合约」「不同交割月」「不同单位/币种」「复权 vs 非复权」不自动合并。
- 日线未就绪或行情过期时，展示 / 复盘 / 风险监控可继续，新开仓判断被阻断。
- Dify 全链路：草稿保存 → 校验 → 失败进 `PENDING_REVIEW` / 成功升级研究事件 → 发布事件；Dify 超时或非法 JSON 时确定性链路降级运行；Dify 无法写事实主表、行情主表或触发订单。
- 所有写操作幂等、可审计、可回放；重复 `Idempotency-Key` 返回原结果，同键不同参报幂等冲突。
- 普通用户在不填写任何专业参数的前提下，可完成「查看数据就绪 → 发起研究草稿 → 确认进入研究」全流程。
- 已落地 Paper 能力（13 个 API、订单状态机、资金台账、风控闸门、跟盘、审计）回归全绿；仓库无任何 `LIVE` / `QMT` / `CTP` / 真实 `submit` / `cancel` 路径。