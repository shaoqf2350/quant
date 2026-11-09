# QuantPF Follow Persistence Schema Inventory Draft

- 文档日期：2026-10-07
- 阶段：`PHASE1B-FOLLOW-PERSISTENCE-SCHEMA-APPROVAL-AND-IMPLEMENTATION-PLAN`（Schema Inventory 子任务）
- 性质：只读审计草稿
- 本审计：未修改 Java/XML/YAML/SQL/POM/Vue/TS；未执行 Git、DDL、Flyway、INSERT、UPDATE、DELETE；未连接生产系统；未输出凭据
- 数据库证据来源：①`db/migration/` 全部 9 个正式迁移文件；②上一计划阶段对本地 `127.0.0.1:3306/quantpf` 的 information_schema 只读审计结论（见 `phase1b-follow-persistence-schema-approval-and-implementation-plan.md` §2.2）；③本会话无数据库凭据，未自行连接数据库
- 上游文档：`phase1b-follow-persistence-schema-approval-and-implementation-plan.md`、`phase1b-follow-persistence-migration-proposal.md`、`phase1b-follow-persistence-risk-register.md`、`D:\我的文档\私人\量化\量化交易系统完整设计方案_V29.md`

## 1. 当前 Follow API 实际行为

代码位置：

```text
ruoyi-admin/src/main/java/com/quantpf/web/controller/QuantPaperFollowController.java
```

事实：

- `POST /api/quant/paper/follow-start`：无请求体、无请求参数，直接设置进程内 `AtomicBoolean running=true`，返回 `R.ok({mode:"PAPER", running:true})`；
- `POST /api/quant/paper/follow-stop`：无请求体、无请求参数，直接设置 `running=false`，返回 `R.ok({mode:"PAPER", running:false})`；
- 实际 HTTP 状态为 200（Spring 对 `R.ok` 默认序列化），而 OpenAPI 契约登记为 202（见第 6 节，存在不一致）；
- 无 account、strategy、mode 绑定参数，无 session 概念，无 request id / Idempotency-Key，无幂等语义；
- 无数据库读写、无审计记录、无事务边界、无 Repository/Application Service；
- `running` 状态仅存在于单个 JVM 进程：重启即丢失，多实例互不可见；
- Controller 由 `DromaraApplication`（`scanBasePackages = {"org.dromara", "com.quantpf.web"}`）组件扫描注册；
- 既有 standalone 测试 `QuantPaperApiStandaloneTest.followEndpointsArePaperOnly` 断言当前内存行为（start 返回 `mode=PAPER`，stop 后 `running=false`）。

## 2. 当前数据库表和索引

`db/migration/` 全部 9 个文件（V1_0_0 至 V1_8_0）共定义 20 张 `quantpf_*` 表：

| 表 | 来源 | 主键 | 唯一键/索引 |
|---|---|---|---|
| `quantpf_schema_marker` | V1_0_0 | id | uk(schema_version) |
| `quantpf_instrument` | V1_1_0 | id | uk(instrument_code, exchange_code)；idx(product_type, status)；idx(exchange_code) |
| `quantpf_trading_calendar` | V1_1_0 | id | uk(market_code, trading_date) |
| `quantpf_market_bar` | V1_1_0 | id | uk(instrument_id, timeframe, bar_time)；idx(bar_time) |
| `quantpf_research_news` | V1_2_0 | id | uk(external_id, source)；uk(content_hash)；idx(published_at) |
| `quantpf_financial_statement` | V1_2_0 | id | uk(instrument_id, report_period, statement_type, source) |
| `quantpf_strategy_signal` | V1_2_0 | id | idx(instrument_id, signal_time)；idx(signal_type, status) |
| `quantpf_strategy` | V1_3_0 | id | uk(strategy_code, version) |
| `quantpf_backtest_run` | V1_3_0 | id | uk(run_id)；idx(strategy_id) |
| `quantpf_backtest_result` | V1_3_0 | id | uk(backtest_run_id, metric_code) |
| `quantpf_trading_account` | V1_4_0 | id | uk(account_code) |
| `quantpf_cash_ledger` | V1_4_0 | id | uk(idempotency_key)；idx(account_id, occurred_at) |
| `quantpf_position` | V1_4_0 | id | uk(account_id, instrument_id) |
| `quantpf_paper_order` | V1_5_0 | id | uk(account_id, client_order_id)；uk(idempotency_key)；idx(account_id, status)；idx(instrument_id, created_at)；idx(request_id) |
| `quantpf_paper_execution` | V1_5_0 | id | uk(execution_id)；uk(idempotency_key)；idx(order_id, executed_at) |
| `quantpf_risk_rule` | V1_6_0 | id | uk(rule_code) |
| `quantpf_risk_snapshot` | V1_6_0 | id | idx(account_id, snapshot_at) |
| `quantpf_trading_halt` | V1_6_0 | id | idx(active, scope, target_id)；idx(halted_at) |
| `quantpf_job_execution_audit` | V1_7_0 | id | uk(execution_id) |
| `quantpf_audit_record` | V1_7_0 | id | idx(request_id)；idx(aggregate_type, aggregate_id)；idx(occurred_at)（occurred_at 索引由 V1_8_0 补充） |

关键核对：

- 20 张表中没有任何名称包含 `follow` 或 `session` 的表；
- 上一计划阶段对本地 schema 的只读审计记录为 21 张表（与本清单 20 张 `quantpf_*` 的差，最可能为 Flyway 自建的 `flyway_schema_history`，需在具备凭据的环境复核确认）；
- 上一计划阶段记录：`quantpf_audit_record` 与 `quantpf_job_execution_audit` 行数均为 0。

## 3. 可复用表

| 表 | 复用方式 | 依据 |
|---|---|---|
| `quantpf_trading_account` | Follow session 的 `account_id` 外键目标 | 有 `ck_quantpf_account_type CHECK (account_type = 'PAPER')`，天然保证 Follow 只能绑定 Paper 账户 |
| `quantpf_strategy` | Follow session 的 `strategy_id` 外键目标 | 有 `uk(strategy_code, version)` 与 `ck_quantpf_strategy_status CHECK (status IN ('DRAFT','ACTIVE','DISABLED'))`；实施时需决策是否限定 `status='ACTIVE'` |
| `quantpf_audit_record` | 与 Follow start/stop 状态变更同事务写入审计事实 | 已有表、字段（request_id/actor_id/action/aggregate_type/aggregate_id/result/detail/occurred_at）和索引足够；注意当前 `AuditRecordMapper`/Repository 只有只读分页能力，实施前必须补受控 insert 路径（与 FOL-SCHEMA-P1-002A 一致） |
| 命名与结构惯例 | 新表复用 `quantpf_paper_*` 前缀、`BIGINT UNSIGNED` 自增主键、`DATETIME(6)`、`VARCHAR(128)` 幂等键、`uk_*`/`idx_*`/`fk_*`/`ck_*` 命名 | 与 `quantpf_paper_order`、`quantpf_paper_execution` 同构，是全库一致模式 |
| 幂等键先例 | start/stop request id 可参照 `idempotency_key VARCHAR(128) UNIQUE` 模式 | `quantpf_paper_order`、`quantpf_cash_ledger`、`quantpf_paper_execution` 均已使用 |

## 4. 不可复用表

| 表 | 不可复用原因 |
|---|---|
| `quantpf_job_execution_audit` | 语义是调度执行审计（job_name/execution_id/scheduler_type），唯一键是 `execution_id`；不是运行会话生命周期，不能表达"当前唯一 RUNNING session" |
| `quantpf_trading_halt` | 语义是风险闸门（active 标志 + 恢复原因），不是 follow session；复用会造成风险与跟盘状态耦合，halt/resume 与 follow start/stop 生命周期不同 |
| `quantpf_paper_order` / `quantpf_paper_execution` | 订单/成交生命周期事实，与 follow session 无关；其唯一键（client_order_id、idempotency_key）不能表达 account+strategy+mode 的活动唯一性 |
| `quantpf_risk_snapshot` / `quantpf_risk_rule` | 风控事实/规则配置，与 follow 无关 |
| `quantpf_cash_ledger` / `quantpf_position` | 资金与持仓事实，follow 不直接读写；不能作为 session 状态 |
| `quantpf_strategy_signal` / `quantpf_strategy` / `quantpf_backtest_run` / `quantpf_backtest_result` | 策略信号、策略定义、回测事实；`quantpf_strategy` 仅可作为外键目标，本身不是 session |
| `quantpf_market_bar` / `quantpf_trading_calendar` / `quantpf_instrument` / `quantpf_research_news` / `quantpf_financial_statement` | 行情/日历/标的/研究事实，与 follow session 状态无关 |
| `quantpf_schema_marker` | Flyway 基线标记表，仅作版本登记，不可复用为业务状态 |

结论：当前 20 张表中没有任何表可以安全表达 follow session 的"生命周期 + 活动唯一性 + start/stop 幂等"三项事实，必须新建专用表（或由人工批准明确复用某表并承担语义变形），否则不得宣称 Follow 已持久化。

## 5. Follow 所需字段矩阵

需求字段来源：上游计划 §4.2、migration proposal §2、风险登记 FOL-SCHEMA-*。

| 需求字段 | 建议类型 | 现有表覆盖情况 | 结论 |
|---|---|---|---|
| session_id | VARCHAR(128) 唯一 | 无表表达 follow session 标识 | 需新表 |
| account_id | BIGINT UNSIGNED | `quantpf_trading_account.id` 可作外键目标，但无 session 内绑定字段 | 新表 + FK |
| strategy_id | BIGINT UNSIGNED | `quantpf_strategy.id` 可作外键目标，但无 session 内绑定字段 | 新表 + FK |
| mode | VARCHAR(16) 仅 PAPER | 无（`account_type='PAPER'` 是账户类型，`scheduler_type` 是调度器类型，语义均不同） | 需新表 |
| status（RUNNING/STOPPED/FAILED） | VARCHAR(16) | 无（`order.status`、`halt.active`、`backtest.status` 均不匹配 follow 生命周期） | 需新表 |
| start_request_id | VARCHAR(128) 唯一 | 幂等键模式已有先例，但无 follow 记录 | 新表唯一键 |
| stop_request_id | VARCHAR(128) NULL 唯一 | 同上 | 新表唯一键（可空） |
| started_by / stopped_by | VARCHAR(128) | 先例：`trading_halt.halted_by` | 新表 |
| started_at / stopped_at | DATETIME(6) | 先例：`halted_at/resumed_at`、`job_execution_audit.started_at/finished_at` | 新表 |
| stop_reason | VARCHAR(512) | 先例：`trading_halt.reason/resume_reason` | 新表 |
| last_error_code / last_error_message | VARCHAR(64)/VARCHAR(1024) | 先例：`job_execution_audit.error_code/error_message` | 新表 |
| revision | BIGINT UNSIGNED | 无乐观版本字段先例 | 新表 |
| created_at / updated_at | DATETIME(6) | 全库先例（含 ON UPDATE CURRENT_TIMESTAMP(6)） | 新表 |
| 同一 account+strategy+mode 的活动唯一性 | 条件唯一约束 | 无任何表提供；MySQL 不支持 partial unique index | 新表 + stored generated column + unique index（方案 A，需 MySQL 8.0.20 实测） |
| start/stop 与审计的事务原子性 | 同事务 | `quantpf_audit_record` 可复用，但需补 insert 路径 | 新表 + Audit insert（同 SqlSession） |

明确不在第一版引入（与上游计划一致）：`STARTING/STOPPING` 中间态、自由 JSON metadata、event/outbox、行情跟单执行器字段。

## 6. 现有 OpenAPI/错误码/事件/菜单引用

### 6.1 OpenAPI（`contract/openapi/quantpf-v29.yaml`）

- `/api/quant/paper/follow-start`：POST，tags `[paper-order]`，operationId `startPaperFollow`，响应 `202 ApiResponse`、`409 Conflict`；**无 requestBody、无 parameters**；
- `/api/quant/paper/follow-stop`：POST，tags `[paper-order]`，operationId `stopPaperFollow`，响应仅 `202 ApiResponse`；**无 requestBody、无 parameters**；
- components 中不存在 `PaperFollowStartRequest`、`PaperFollowStopRequest`、`PaperFollowSessionResponse` schema；
- 不一致点：契约登记 202，实际 Controller 返回 HTTP 200；
- 与幂等相关的既有组件：`parameters.IdempotencyKey`（header，minLength 8，maxLength 128）已存在，可被 follow 契约引用或选择 `requestId` 作为 canonical 语义（二选一，见上游计划 §8.1）。

### 6.2 错误码（`contract/error-codes/quantpf-error-codes.yaml`）

- 现有 13 个错误码，无任何 `QPF-FOLLOW-*`；
- 可复用：`QPF-IDEMP-001`（幂等键冲突 409）、`QPF-AUDIT-001`（审计写入失败 500，可重试）；
- 上游 proposal 建议新增：`QPF-FOLLOW-001`（active session conflict 409）、`QPF-FOLLOW-002`（session not found 404）、`QPF-FOLLOW-003`（invalid state transition 409）、`QPF-FOLLOW-004`（missing request id 400）；本审计未修改错误码 YAML。

### 6.3 事件 Schema（`contract/events/`）

- 现存 4 个 v1 schema：`quantpf-order-event`、`quantpf-execution-event`、`quantpf-ledger-event`、`quantpf-risk-event`；
- 没有任何 follow event，也没有对 follow 的引用；
- 结论：本阶段不新增、不伪造 follow event；未来需要时单独新增 `quantpf-follow-event.v1.schema.json` 并走独立审批（与上游计划 §8.3 一致）。

### 6.4 菜单事实源（`contract/menu/quantpf-menu-facts.yaml`）

- 现存菜单：dashboard、research、market、strategy、backtest、paper-account、paper-order、risk、audit；**没有 follow 菜单**；
- 权限清单中不存在 `quant:paper:operate`；
- 不一致点（源自 V29 设计文档）：
  - V28-2.5 的 Stage 1 菜单清单包含 `paper-follow`，但实际菜单事实源文件未包含；
  - V27-6 前端映射表把"模拟跟盘"页面（`src/views/quant/trade/follow/index.vue`）映射到权限 `quant:paper:operate`，该权限同样未进入菜单事实源；
- 结论：Follow 菜单与权限属于 Phase 1C 前端范围，后端 schema 阶段不应提前修改菜单事实源。

## 7. MySQL 版本和已知约束

- 版本证据：
  - 上一计划阶段对本地 `127.0.0.1:3306/quantpf` 的 information_schema 只读审计实测为 `8.0.20`；
  - V29 设计文档要求 MySQL 8.x / 8.0+（第 117、672 行）并提及 "MySQL 8 container"（第 6919 行）；
  - 本审计会话无数据库凭据，未独立复核；版本以迁移文件兼容性和上游实测结论为准。
- 连接与驱动证据：测试使用 `com.mysql.cj.jdbc.Driver`，JDBC URL 形如 `jdbc:mysql://127.0.0.1:3306/quantpf?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai`；mysql-connector-j 为 9.7.0（test scope）。
- 与 Follow 表相关的已知 MySQL 约束/能力（均以现有迁移文件为证）：
  1. MySQL 不支持 partial unique index（无法 `UNIQUE ... WHERE status='RUNNING'`），活动唯一性必须用 stored generated column + unique index 实现（上游方案 A）；
  2. generated column（STORED）MySQL 5.7+ 支持，8.0.20 可用，但表达式、字符集长度、与 CHECK 的组合必须在 disposable schema 实测（FOL-SCHEMA-P1-006）；
  3. CHECK 约束在 8.0.16+ 强制执行；现有迁移已大量使用 `ck_*` 字符串枚举 CHECK（V1_1_0/V1_3_0/V1_4_0/V1_5_0），可沿用该模式；数据库 CHECK 不能替代服务层状态机；
  4. 全库统一 `ENGINE=InnoDB`、`utf8mb4_unicode_ci`、`DATETIME(6)`、`BIGINT UNSIGNED`；`active_slot VARCHAR(256)`（utf8mb4 = 1024 字节）在 InnoDB DYNAMIC 行格式 3072 字节索引上限内，但需实测确认；
  5. JSON 列已有大量先例（`content`/`data`/`payload`/`definition`/`result`/`detail`），但 Follow 第一版不建议引入自由 JSON（上游已决策）；
  6. 外键 + 唯一键 + 查询索引混合模式已有先例（`quantpf_paper_order` 最接近），可直接作为新表模板；
  7. `ON UPDATE CURRENT_TIMESTAMP(6)` 用于 `updated_at` 的惯例已存在（instrument/strategy/account/position/order）。

## 8. 现有测试模式

| 模式 | 文件 | 特点 | 对 Follow 实施的意义 |
|---|---|---|---|
| 无 DB 的 standalone MockMvc | `QuantPaperApiStandaloneTest` | `standaloneSetup` + 无参构造 Controller；follow 端点当前只断言内存语义 | Follow 持久化后该测试的 follow 断言必须同步调整（当前白名单外） |
| Spring 持久化 Web 集成 | `QuantPfSpringPersistenceWebIntegrationTest`、`QuantPfRemainingApiPersistenceWebIntegrationTest` | `@SpringJUnitConfig` + 嵌套 TestConfig + `DriverManagerDataSource`；`QPF_DB_USER/QPF_DB_PASSWORD/QPF_JDBC_URL` 环境变量；`assumeTrue` 无凭据即跳过；随机前缀插入 + 前缀 DELETE 清理 | Follow 的 start/stop API 事实测试应复用该模式 |
| MyBatis Mapper 集成（domain） | `TradingAccountMapperIntegrationTest` | 手工 `SqlSessionFactory`（UnpooledDataSource + XMLMapperBuilder），rollback 清理 | Follow Mapper/XML 应补充同型测试 |
| 事务/并发集成（finance） | `PaperPersistenceTransactionIntegrationTest` | `ExecutorService + CountDownLatch + Future` 并发提交，断言幂等去重、资金冻结一次、失败整体 rollback | Follow 的并发 start/stop、同事务 audit、rollback 测试应参照该模式（上游计划 §6 已要求） |
| 纯单元测试 | `DomainValueTest`、`OrderStateMachineTest`、`PaperAccountLedgerServiceTest`、`PaperRiskGateTest` | 无 DB、无 Spring | Follow 状态机（非法转换拒绝）应补同型单元测试 |

统一约定：DB 测试一律通过环境变量读凭据、无凭据跳过而非失败；清理用随机前缀；本会话无凭据，全部 DB 事实测试处于跳过状态，运行证据需由具备凭据的环境补充。

## 9. 发现的风险

新增/确认的本清单级风险（与 `phase1b-follow-persistence-risk-register.md` 交叉引用，不重复编号）：

| ID | 等级 | 风险 | 证据 |
|---|---|---|---|
| FOL-INV-001 | P1 | 菜单事实源与设计文档不一致 | V28-2.5 列出 `paper-follow`，`quantpf-menu-facts.yaml` 无此菜单、无 `quant:paper:operate` 权限 |
| FOL-INV-002 | P1 | OpenAPI 状态码与实现不一致 | 契约登记 follow-start/stop 为 202，Controller 实际返回 200 |
| FOL-INV-003 | P1 | OpenAPI 无 request body/参数，无法表达 request id 与绑定 | 两个 follow operation 均无 parameters/requestBody；实施强幂等前必须补契约（契约变更需另行批准） |
| FOL-INV-004 | P1 | `quantpf_strategy` 尚无 Mapper/insert 路径 | domain 现有 7 个 Mapper 均不含 Strategy；Follow 事实测试需要可插入的 strategy 行（或 JDBC 直插 + 状态决策 DRAFT/ACTIVE/DISABLED） |
| FOL-INV-005 | P1 | `AuditRecordMapper` 只有只读分页 | Follow 需要同事务 audit insert，实施前必须扩白名单补 insert 路径（对应 FOL-SCHEMA-P1-002A） |
| FOL-INV-006 | P2 | 历史 STOPPED/FAILED session 无保留策略 | 新表按 session 审计要求保留历史，但无归档/保留周期定义，长期增长风险 |
| FOL-INV-007 | P2 | 本清单会话无数据库凭据，元数据复核依赖迁移文件与上游实测 | 无法独立复核 8.0.20 版本、21 张表计数、follow/session 表不存在这三项 |
| FOL-INV-008 | P2 | standalone 测试的 follow 断言基于内存行为 | 持久化实施后该测试将过期，需在其白名单放开后同步更新 |

继续有效的既有阻塞项（详见 `phase1b-follow-persistence-risk-register.md`）：FOL-SCHEMA-P0-001（无事实源）、P0-002（并发重复 RUNNING）、P1-001（API 缺参数）、P1-002/002A（审计原子性）、P1-003（partial unique index 误用）、P1-004（幂等键缺失）、P1-005（复用审计/任务表）、P1-006（generated column 未实测）。

## 10. 判定边界

本清单只做事实审计和模式对照，**不做最终 PASS/PARTIAL 判定**，不决定 Follow 是否阻塞 Paper MVP 或 Phase 1C 是否放行；最终判定由 GPT-5.6 Sol 作出。本清单的结论性事实可概括为：

```text
FOLLOW PERSISTENCE FACT SOURCE: ABSENT IN CURRENT SCHEMA (20 quantpf_* TABLES, NO follow/session TABLE)
REUSABLE: quantpf_trading_account (FK), quantpf_strategy (FK), quantpf_audit_record (transactional audit, insert path missing)
NEW TABLE REQUIRED: quantpf_paper_follow_session (per NON-EXECUTABLE migration proposal V1_9_0)
CONCURRENCY MECHANISM: stored generated column + unique index (no partial unique index on MySQL 8.0.20)
CONTRACT GAPS: OpenAPI request/response schemas missing; 202 vs 200 mismatch; no QPF-FOLLOW-* codes; no follow event; no follow menu
NO DDL / NO FLYWAY / NO CODE CHANGE PERFORMED IN THIS AUDIT
```

未执行清单：未创建 SQL/Java/XML；未修改 Controller/Flyway/契约；未执行 DDL/INSERT/UPDATE/DELETE；未输出凭据；未连接任何数据库或生产系统。
