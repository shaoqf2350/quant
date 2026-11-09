# PHASE0B-FOUNDATION-SKELETON-PLAN

- 计划日期：2026-10-03
- 阶段：`PHASE0B-FOUNDATION-SKELETON-PLAN`
- 计划性质：只读架构设计、文件级施工计划、人工审批包
- `planOnly`：`true`
- `codeChangesApplied`：`false`
- `pomChangesApplied`：`false`
- `sqlChangesApplied`：`false`
- `contractChangesApplied`：`false`
- `frontendChangesApplied`：`false`

## 1. 计划结论

Phase 0A 工具链基线已经通过：Maven 实际运行于 Temurin JDK21.0.12.1，pnpm 为 10.34.5，Maven Reactor `validate` 的 40 个工程全部成功。

本轮只完成 Foundation Skeleton 的架构计划，不修改代码仓。当前允许进入后续阶段的唯一条件是：本计划经人工审查批准后，进入 `PHASE0C-FOUNDATION-SKELETON-IMPLEMENTATION`。

本轮明确不创建：

- `quantpf-modules`；
- QuantPF Java 业务代码；
- 根 POM 变更；
- Flyway SQL；
- OpenAPI/事件 Schema/错误码/菜单事实源；
- XXL-JOB 依赖或实现；
- QMT/CTP/LIVE 代码。

## 2. 实际仓库基线

### 2.1 后端

- 根：`D:\workspace\quant\RuoYi-Vue-Plus`
- 分支：`6.X...origin/6.X`
- HEAD：`9227f626a3a37e1546d6c291ed398183e94df92b`
- 上游偏离：`0 0`
- revision：`6.0.0`
- 根 POM SHA-256：`CBD3740F955E8B911754351C7A70B549626C2E2A20C8546155BA39772635FBEE`
- 当前根 modules：`ruoyi-admin`、`ruoyi-common`、`ruoyi-extend`、`ruoyi-modules`、`ruoyi-api`
- 当前不存在：`quantpf-modules`、`com.quantpf.*`、`contract/`、`db/migration/`

### 2.2 前端

- 根：`D:\workspace\quant\plus-ui`
- 分支：`6.X-Vue...origin/6.X-Vue`
- HEAD：`045fc006725c014455def9fed19f93c781633d26`
- 上游偏离：`0 0`
- `package.json` SHA-256：`D1376599B7C4736C081EC9BEAA7354E236C1D4DE371C8B16ECB0E6F13E871BE1`
- `pnpm-lock.yaml` SHA-256：`C29BBB60A733B8F09ECAB8DE3697A51EE5340F45A1AE7E1D9739D3C3DF1C1F05`
- packageManager：`pnpm@10.34.5`
- 当前动态菜单 API：`/system/menu/getRouters`
- 当前 SnailJob 页面：`src/views/monitor/snailjob/index.vue`

### 2.3 现有调度证据

后端存在：

- `ruoyi-common/ruoyi-common-job/pom.xml`；
- `ruoyi-common/ruoyi-common-job/src/main/java/org/dromara/common/job/config/SnailJobConfig.java`；
- `ruoyi-modules/ruoyi-job/src/main/java/org/dromara/job/snailjob/*.java`；
- 8 个 `@JobExecutor`；
- `ruoyi-extend/ruoyi-snailjob-server/**`；
- `ruoyi-admin` dev/prod `snail-job` 配置；
- `ruoyi-extend/pom.xml` 中的 `ruoyi-snailjob-server` module。

当前代码仓不含 XXL-JOB 依赖、Executor、Admin 或有效配置。原型中的 `quantpf_v25_xxljob_migration.sql` 仍是历史参考文件。

## 3. V29/V28 权威规则

以 V29 新增章节和 V28-0 至 V28-8 为正式规则，V25/V26/V27 只作为历史详细参考。

必须保持：

1. `quantpf-common` 不放 Entity、Mapper、Mapper XML；
2. `quantpf-domain` 统一拥有 QuantPF Entity、Mapper、Mapper XML、Repository；
3. `quantpf-job` 不直接访问 Mapper、不拼 SQL、不执行 Shell、不直接调用 Broker；
4. Controller 只放在 `ruoyi-admin/src/main/java/com/quantpf/web/controller`；
5. QuantPF 新代码包名统一 `com.quantpf.*`；
6. Stage 1 只注册 `PaperBrokerAdapter`；
7. `LiveOrderGateway`、QMT、CTP submit/cancel 不作为 Spring Bean；
8. Stage 1 菜单事实源不得包含真实交易权限；
9. 不允许 SnailJob 与 XXL-JOB 双栈同时触发同一业务任务；
10. 不允许直接执行历史 SQL；
11. 不允许在 Phase 0 进入完整业务实现、真实交易或生产迁移。

## 4. 目标工程树

计划目标，不在本轮创建：

```text
RuoYi-Vue-Plus/
├── pom.xml                                  # 计划接入 quantpf-modules
├── BASELINE.md                              # Phase 0C/后续基线文件
├── ruoyi-admin/
│   └── src/main/java/com/quantpf/web/controller/
├── ruoyi-api/
├── ruoyi-common/
├── ruoyi-extend/
├── ruoyi-modules/
├── quantpf-modules/
│   ├── pom.xml
│   ├── quantpf-common/
│   │   └── pom.xml
│   ├── quantpf-domain/
│   │   └── pom.xml
│   ├── quantpf-marketdata/
│   │   └── pom.xml
│   ├── quantpf-research/
│   │   └── pom.xml
│   ├── quantpf-strategy/
│   │   └── pom.xml
│   ├── quantpf-risk/
│   │   └── pom.xml
│   ├── quantpf-finance/
│   │   └── pom.xml
│   ├── quantpf-execution/
│   │   └── pom.xml
│   ├── quantpf-backtest/
│   │   └── pom.xml
│   └── quantpf-job/
│       └── pom.xml
├── contract/
├── db/
├── deploy/
├── tests/
└── docs/
```

## 5. 模块职责与边界

| 模块 | 负责 | 不负责 | 数据库 | Broker | Controller |
|---|---|---|---|---|---|
| `quantpf-common` | 枚举、通用值对象、错误抽象、事件 envelope、幂等/追踪接口、纯工具 | Entity、Mapper、XML、业务流程、Controller | 否 | 否 | 否 |
| `quantpf-domain` | Entity、Mapper、Mapper XML、Repository、领域状态和值对象组合 | Web、调度编排、Broker SDK、真实下单 | 是，通过 RuoYi persistence | 否 | 否 |
| `quantpf-marketdata` | 行情、K线、盘口、逐笔、交易日历的数据访问用例和 provider 抽象 | 订单、策略决策、真实交易 | 通过 domain/repository | 只读数据 provider，不得下单 | 否 |
| `quantpf-research` | 新闻、财报、因子、标的池、研究用例 | 订单执行、风险放行、调度基础设施 | 通过 domain/repository | 否 | 否 |
| `quantpf-strategy` | 策略配置、信号生成、策略运行时抽象 | 直接写数据库、直接下单、绕过风险 | 通过 domain/repository | 否 | 否 |
| `quantpf-risk` | 风险规则、风险状态、halt/resume、订单前置检查接口 | 直接持久化 SQL、直接调用 Broker | 通过 domain/repository | 否 | 否 |
| `quantpf-finance` | 财报分析、账户/账务查询用例、财务数据编排 | 订单执行、Broker submit/cancel | 通过 domain/repository | 否 | 否 |
| `quantpf-execution` | Paper order、订单生命周期、PaperBrokerAdapter、撮合接口、幂等编排 | LIVE submit/cancel、QMT/CTP Bean、直接拼 SQL | 通过 domain/repository | 只允许 PaperBrokerAdapter | 否 |
| `quantpf-backtest` | 回测用例、快照、性能分析、回测执行模型 | 生产订单、真实账户、LIVE | 通过 domain/repository | 否 | 否 |
| `quantpf-job` | 调度用例编排、Job handler 适配、任务幂等、执行审计调用 | Mapper、SQL、Shell、Broker 直调、业务 Controller | 只能调用 application service | 不得直调 | 否 |

### 5.1 包命名规则

- `com.quantpf.common.*`
- `com.quantpf.domain.*`
- `com.quantpf.marketdata.*`
- `com.quantpf.research.*`
- `com.quantpf.strategy.*`
- `com.quantpf.risk.*`
- `com.quantpf.finance.*`
- `com.quantpf.execution.*`
- `com.quantpf.backtest.*`
- `com.quantpf.job.*`
- Controller 统一使用 `com.quantpf.web.controller.*`，只位于 `ruoyi-admin`。

禁止：

- `org.dromara.*` 下新增 QuantPF 业务包；
- 在 `quantpf-common` 放业务 Entity/Mapper；
- 在业务模块放 Controller；
- `quantpf-job` 依赖 Mapper 或拼接 SQL；
- `quantpf-execution` 注册 QMT/CTP/LIVE Bean。

## 6. 模块依赖 DAG

```text
ruoyi-admin
 ├── quantpf-job
 ├── quantpf-execution
 ├── quantpf-backtest
 ├── quantpf-research
 ├── quantpf-finance
 ├── quantpf-marketdata
 └── ruoyi-common-*

quantpf-job        → quantpf-execution, quantpf-research, quantpf-marketdata, quantpf-backtest
quantpf-backtest   → quantpf-strategy, quantpf-risk, quantpf-domain, quantpf-common
quantpf-execution  → quantpf-risk, quantpf-domain, quantpf-common
quantpf-finance    → quantpf-research, quantpf-domain, quantpf-common
quantpf-research   → quantpf-marketdata, quantpf-domain, quantpf-common
quantpf-strategy   → quantpf-domain, quantpf-common
quantpf-risk       → quantpf-domain, quantpf-common
quantpf-marketdata → quantpf-domain, quantpf-common
quantpf-domain     → quantpf-common + RuoYi common persistence modules
```

### 6.1 DAG 审查结果

- 当前目标 DAG 无环；
- `quantpf-common` 为最底层纯共享模块；
- `quantpf-domain` 是唯一 QuantPF persistence owner；
- Controller 不下沉至业务模块；
- `quantpf-job` 不可通过间接依赖获得 Mapper 访问权，Phase 0C 必须通过依赖树和架构扫描复核；
- `quantpf-execution` 不引入 QMT/CTP SDK；
- `quantpf-research`、`quantpf-marketdata`、`quantpf-finance` 的 provider 与业务编排必须保持接口隔离；
- `quantpf-backtest` 可以调用 strategy/risk，但不得调用生产执行路由。

### 6.2 依赖禁止矩阵

| 来源 | 禁止依赖 |
|---|---|
| `quantpf-common` | 所有业务模块、Web、Mapper、RuoYi Admin |
| `quantpf-domain` | `ruoyi-admin`、`quantpf-job`、Web Controller、Broker SDK |
| `quantpf-job` | Mapper、Mapper XML、SQL、Shell、QMT/CTP、LIVE gateway |
| `quantpf-execution` | QMT/CTP submit/cancel SDK、SnailJob API、Web Controller |
| `quantpf-marketdata` | execution、broker submit/cancel |
| `quantpf-strategy` | execution 具体实现、Web、调度实现 |
| `ruoyi-admin` | 直接访问 Mapper、绕过 application service |

## 7. POM 计划变更矩阵

本节只描述未来变更，不执行修改。

| 文件 | 计划变更 | 原因 | 依赖影响 | 风险 | 人工批准 |
|---|---|---|---|---|---|
| 根 `pom.xml` | 增加 `quantpf-modules` module；增加 `com.quantpf` 版本管理；按批准方案加入必要依赖管理 | 让新聚合器进入 Reactor | 影响全仓 Reactor | P0，构建树变化 | 是 |
| `ruoyi-modules/pom.xml` | 原则上不增加 QuantPF 模块；仅确认旧 `ruoyi-job` 兼容入口边界 | 防止 QuantPF 与底座任务混杂 | 低至中 | P1 | 是 |
| `ruoyi-common/pom.xml` | 原则上不加入 QuantPF 业务模块；调度迁移阶段才处理 `ruoyi-common-job` | 保持底座 common 边界 | 中 | P1 | 是 |
| `ruoyi-common/ruoyi-common-job/pom.xml` | 迁移阶段删除 SnailJob 依赖并加入经验证的 XXL-JOB Core/公共封装 | 调度框架切换 | 影响 admin/job 依赖树 | P0 | 是 |
| `ruoyi-extend/pom.xml` | 迁移阶段移除 `ruoyi-snailjob-server` module | 清理旧调度服务端 | 影响 Reactor/部署 | P0 | 是 |
| `ruoyi-admin/pom.xml` | Phase 0C 只加入批准的 QuantPF application modules；调度迁移另行处理 | Controller/Application 接入 | 启动 classpath 变化 | P0 | 是 |
| 新 `quantpf-modules/pom.xml` | 创建聚合器和模块顺序 | 形成 QuantPF 子 Reactor | 新增 11 个 POM | P0 | 是 |
| 十个 QuantPF 模块 POM | 创建最小依赖，按 DAG 约束 | 形成可编译骨架 | 影响全新模块 | P0 | 是 |
| `ruoyi-admin` 自动配置入口 | 计划增加 QuantPF 最小扫描/健康接入；不加载 QMT/CTP | 启动验证 | 影响启动上下文 | P0 | 是 |
| Flyway 依赖位置 | 计划由已批准的 persistence/application 入口承载；具体 artifact/version 需 Phase 0C 前冻结 | 迁移框架 | 影响启动和数据库 | P0 | 是 |
| XXL-JOB 依赖位置 | 只在独立调度迁移阶段加入 `ruoyi-common-job`/Executor 入口 | 调度替换 | 影响任务执行 | P0 | 是 |
| SnailJob 依赖 | 只能在迁移验收通过后删除 | 防止运行时断裂 | 高 | P0 | 是 |

### 7.1 POM 变更原则

- Phase 0B 不修改任何 POM；
- Phase 0C 只允许修改 manifest 中明确列出的 POM；
- 任何额外 POM 必须先追加至 manifest 并重新审批；
- 不得以“顺手整理”方式升级 Spring Boot、MyBatis-Plus、Sa-Token、Redisson 或 Node 相关依赖；
- 不得在 Phase 0C 同时完成 SnailJob 删除和业务代码实现；
- 不得将 XXL-JOB 和 SnailJob 同时作为有效执行栈。

## 8. Controller、Service、DTO 和持久化边界

### 8.1 Controller

唯一允许位置：

```text
ruoyi-admin/src/main/java/com/quantpf/web/controller/
```

Controller 只负责：

- 参数接收和校验入口；
- 权限注解；
- 幂等 header 传递；
- 调用 application service/use case；
- 返回统一 `Result<T>`。

Controller 禁止：

- 直接调用 Mapper；
- 直接拼 SQL；
- 直接调用 Broker；
- 直接执行任务；
- 将 LIVE/QMT/CTP 路由注册为有效执行路径。

### 8.2 DTO/VO

- 外部请求/响应 DTO：优先放在 `ruoyi-admin` 的 `com.quantpf.web.dto` 或由契约生成的边界包；具体生成策略在 Phase 0C 前冻结；
- 领域 DTO/命令：放在对应 QuantPF 模块 application/use-case 包；
- Entity、Mapper、Repository：只归属 `quantpf-domain`；
- Common 只放跨模块无业务持久化含义的 value object、enum、event envelope、错误抽象。

### 8.3 OpenAPI 唯一性

- Phase 0 计划基线使用 V28-2.1 的 29 个正式 operation；
- V29 OpenAPI 文件计划命名为 `contract/openapi/quantpf-v29.yaml`；
- 原型旧路径不直接生成 Controller；
- 兼容别名必须 deprecated、覆盖测试并登记删除版本；
- 原型 49 个 API 单元格必须逐项映射到正式 operation、兼容别名或明确淘汰项。

## 9. SnailJob → XXL-JOB 迁移计划

本轮只建立文件级计划，不迁移。

### 9.1 SnailJob 文件分组

| 分组 | 当前范围 | 计划动作 | 执行阶段 |
|---|---|---|---|
| 客户端依赖 | 根 POM、`ruoyi-common-job/pom.xml` | 迁移验收后删除，加入经验证 XXL-JOB 依赖 | 调度迁移阶段 |
| 自动配置 | `ruoyi-common-job/.../SnailJobConfig.java`、AutoConfiguration imports | 迁移后删除/替换为 XxlJobConfig | 调度迁移阶段 |
| 任务实现 | `ruoyi-modules/ruoyi-job/.../snailjob/*.java`，8 个 `@JobExecutor` | 逐个判定迁移、停用或保留非 QuantPF 兼容入口 | 任务盘点阶段 |
| 服务端 | `ruoyi-extend/ruoyi-snailjob-server/**`、`ruoyi-extend/pom.xml` | 新调度验收后移出 Reactor/部署 | 调度迁移阶段 |
| 应用配置 | `ruoyi-admin/application-dev.yml`、`application-prod.yml` | 迁移窗口后删除 snail-job，增加 xxl.job | 调度迁移阶段 |
| 前端 | `plus-ui/src/views/monitor/snailjob/index.vue`、`.env.*`、`src/types/env.d.ts` | 替换为 XXL-JOB 入口或受控内网外链 | 前端调度迁移阶段 |
| 原型 SQL | `quantpf_v25_xxljob_migration.sql` | 保留为 legacy/reference，不作为迁移入口 | 文档/数据库阶段 |

### 9.2 任务迁移顺序

1. 形成任务清单：任务名、负责人、触发频率、参数、幂等键、读写表、外部副作用、失败补偿；
2. 将测试示例与实际业务任务分开；
3. 先在隔离环境验证 XXL-JOB Admin/Executor 注册、心跳、测试 Handler；
4. 对每个任务先实现/验证幂等，再切换调度注解；
5. 切换窗口停止 SnailJob 触发；
6. 验证没有旧任务继续发起业务副作用；
7. 启用 XXL-JOB 单调度器；
8. 观察窗口通过后，才清理 SnailJob 资产。

### 9.3 双栈禁止门禁

- 依赖树不得同时包含有效 SnailJob 和 XXL-JOB 执行入口；
- 任务扫描不得同时发现有效 SnailJob executor 与 XXL-JOB handler；
- 配置不得同时启用 `snail-job.enabled=true` 和 `xxl.job.enabled=true`；
- 部署入口不得同时启动两套 Admin/Server；
- 前端有效菜单不得同时提供 SnailJob 和 XXL-JOB 执行入口；
- 所有迁移检查失败时，禁止切换生产调度。

### 9.4 回滚条件

- 新调度器注册/心跳失败；
- 任务执行结果与旧基线不一致；
- 发现重复触发或幂等冲突；
- 作业审计记录缺失；
- 任务失败率、延迟或副作用超出批准阈值。

回滚只允许“停止当前调度器 → 记录窗口 → 单一调度器恢复”，绝不允许双栈并行。

## 10. Flyway V1.x 计划

只做计划，不创建 SQL。

```text
db/migration/
├── V1_0_0__quantpf_baseline.sql
├── V1_1_0__instrument_calendar_market.sql
├── V1_2_0__news_finance_factor_pool.sql
├── V1_3_0__strategy_signal_risk.sql
├── V1_4_0__paper_account_order_fill_ledger.sql
├── V1_5_0__backtest_snapshot_performance.sql
├── V1_6_0__order_event_outbox_reconciliation.sql
├── V1_7_0__job_execution_audit_quality.sql
└── V1_8_0__menu_fact_source.sql
```

| 版本 | 计划职责 | 依赖 |
|---|---|---|
| V1_0_0 | Flyway history 对齐、最小基线、扩展 schema 约束 | 无 |
| V1_1_0 | `qm_instrument`、交易日历、基础行情表 | V1_0_0 |
| V1_2_0 | 新闻、财报、因子、标的池 | V1_1_0 |
| V1_3_0 | 策略、信号、风险规则和风险事件 | V1_2_0 |
| V1_4_0 | `qt_sim_account`、订单、成交、账务 | V1_3_0 |
| V1_5_0 | 回测快照、性能结果 | V1_4_0 |
| V1_6_0 | 订单事件、outbox、对账 | V1_4_0 |
| V1_7_0 | `qo_job_execution`、审计、数据质量 | V1_6_0 |
| V1_8_0 | `quantpf_menu_fact` 和菜单约束 | V1_7_0 |

必须禁止：

- `CREATE DATABASE`；
- `USE quantpf`；
- `DROP TABLE`；
- 动态 `PREPARE ALTER`；
- 用 `IF NOT EXISTS` 隐藏 schema drift；
- 生产环境手工 source 历史 SQL。

旧 `quantpf_v23_frontend_supplement.sql`、`quantpf_v24_core_hardening.sql`、`quantpf_v25_core_hardening.sql`、`quantpf_v25_xxljob_migration.sql` 只能在后续阶段归档至 `docs/legacy/sql/` 或等效 reference 位置；移动前必须人工批准并更新 inventory。

验收矩阵：

1. MySQL 8 clean database migrate；
2. upgrade fixture migrate；
3. repeat application startup；
4. Flyway validate；
5. schema diff；
6. 注入失败 migration 后 forward recovery；
7. 确认无动态 DDL、DROP、USE、CREATE DATABASE；
8. 不连接生产数据库。

## 11. 正式契约计划

本轮只做计划，不创建契约文件。

```text
contract/
├── openapi/quantpf-v29.yaml
├── events/order-event.v1.schema.json
├── events/market-data-event.v1.schema.json
├── events/risk-alert-event.v1.schema.json
├── events/job-event.v1.schema.json
├── error-codes/quantpf-error-codes.yaml
└── menu/quantpf-menu-facts.yaml
```

### 11.1 OpenAPI operation 基线

计划继承 V28-2.1 的 29 个正式 operation：

```text
GET  /dashboard/summary
GET  /instrument/page
GET  /market/kline
GET  /market/order-book/latest
POST /market/import
GET  /news/page
POST /news/analyze
GET  /finance/statements
GET  /finance/factors
POST /finance/analyze
GET  /pool/page
POST /pool/screening
GET  /strategy/page
GET  /signal/page
POST /backtest
GET  /backtest/{id}
GET  /account/page
GET  /account/{id}/ledger
GET  /position/page
GET  /order/page
POST /order/paper
POST /order/{orderNo}/cancel
POST /paper/follow-start
POST /paper/follow-stop
GET  /risk/status
POST /risk/halt
POST /risk/resume
GET  /job/execution/page
GET  /audit/page
```

每个 operation 必须定义：operationId、permission、请求 DTO、响应 DTO、分页结构、错误码、幂等要求、二次确认要求、重试策略、Stage 1 模式限制。

### 11.2 映射原则

- 原型旧路径不直接作为生产 API；
- `/instrument/list` → `/instrument/page`；
- `/kline/query` → `/market/kline`；
- `/orderbook/latest/{symbol}` → `/market/order-book/latest`；
- `GET/POST` 复合单元必须拆成独立 operation；
- 原型扩展接口若不在 V28/V29 正式闭环中，标记为 deferred、deprecated 或 reference-only；
- API/原型映射必须逐行可审计。

### 11.3 事件、错误码和菜单

- 事件 schema 只增版本，不覆盖旧版本；
- 错误码必须唯一、稳定、可映射到 Result 错误响应；
- 菜单事实源必须显式拒绝 `real-trade-order`、`real-trade-cancel`、`qmt-submit`、`ctp-submit`、`qmt-live`、`ctp-live`；
- 事件 envelope 至少包含 eventId、eventType、schemaVersion、occurredAt、traceId、aggregateType、aggregateId、producer、payload；
- 契约 CI 必须执行 OpenAPI lint、JSON Schema validate、错误码唯一性、菜单 parent/permission 检查和原型映射一致性检查。

## 12. 前端接入计划

本轮不修改前端。

### 12.1 计划目录

```text
plus-ui/src/views/quant/
plus-ui/src/api/quant/
plus-ui/src/types/quant/
```

### 12.2 接入原则

- 生产页面使用 Vue 3 + TypeScript + Element Plus + ECharts；
- API/types 以正式 OpenAPI 生成物为准；
- `permission.ts` 仍负责 RuoYi 动态路由过滤，但菜单事实源必须可与契约校验；
- `/system/menu/getRouters` 是底座运行时菜单接口，不替代 QuantPF 菜单事实源；
- 原型页面只作字段、交互和状态参考；
- 生产页面必须补充 loading、空数据、错误、stale、断线、无权限和二次确认状态；
- 不创建真实交易、QMT、CTP、LIVE 页面；
- SnailJob 页面只能在调度迁移阶段替换为受控 XXL-JOB 入口，不能在本轮修改。

### 12.3 页面/API 映射计划

- `dashboard` → `/dashboard/summary`；
- `instrument` → `/instrument/page`；
- `market` → `/market/kline`、`/market/order-book/latest`；
- `news` → `/news/page`、`/news/analyze`；
- `finance` → `/finance/statements`、`/finance/factors`、`/finance/analyze`；
- `pool` → `/pool/page`、`/pool/screening`；
- `strategy/signal` → `/strategy/page`、`/signal/page`；
- `backtest` → `/backtest`、`/backtest/{id}`；
- `paper-account` → `/account/page`、`/account/{id}/ledger`；
- `paper-order` → `/order/page`、`/order/paper`；
- `paper-follow` → `/paper/follow-start`、`/paper/follow-stop`；
- `risk` → `/risk/status`、`/risk/halt`、`/risk/resume`；
- `job-execution/audit` → `/job/execution/page`、`/audit/page`。

## 13. CI、验收和回滚计划

### 13.1 当前阶段不执行

以下命令在本轮不执行：

```powershell
mvn clean verify
pnpm install --frozen-lockfile
pnpm lint
pnpm build
pnpm test
```

原因：本轮是 plan-only；这些命令可能写入 target、node_modules、dist、缓存或生成物，并且 Phase 0C 尚未获批。

### 13.2 Phase 0C 计划命令

```powershell
mvn -version
mvn -DskipTests=true validate
mvn -DskipTests=false clean verify
mvn -DskipTests dependency:tree

node --version
pnpm --version
pnpm install --frozen-lockfile
pnpm lint
pnpm build
pnpm test
```

契约门禁：

```text
OpenAPI lint
JSON Schema validate
error-code uniqueness
menu parent/permission check
API/PROTOTYPE_PAGE_MAP consistency
generated diff
```

Flyway 门禁：

```text
MySQL 8 clean database
migrate
validate
repeat startup
upgrade fixture
failed migration recovery
schema diff
```

所有构建/迁移/调度命令必须：

- 先记录 commit、状态和哈希；
- 明确是否会写入仓库或本机缓存；
- 禁止连接生产数据库；
- 禁止触发真实调度；
- 禁止产生 LIVE/QMT/CTP submit/cancel 路由。

### 13.3 回滚原则

- 代码：使用版本化 Git 变更和人工审核，不执行破坏性 reset/clean；
- POM：按文件级 manifest 回滚，不扩散修改；
- Flyway：停止写入、备份、前向修复、schema diff，不使用 DROP 回滚；
- 调度：停止当前调度器，单调度器恢复，不允许双栈；
- 契约：向后兼容、字段先 deprecated、事件只增版本。

## 14. Phase 0C 施工顺序

### C0：施工前冻结

- 人工批准本计划；
- 重新计算三份基线哈希；
- 记录两个仓 clean 状态；
- 确认 JDK21/Maven/Node/pnpm；
- 确认本次允许修改文件 manifest。

### C1：Maven 骨架

- 创建 `quantpf-modules` 聚合器；
- 创建十个最小模块 POM；
- 按 DAG 添加依赖；
- 根 POM 接入聚合器；
- `ruoyi-admin` 只接入最小健康入口；
- 不创建业务 Entity/Mapper/Controller 实现。

### C2：骨架验收

- Maven validate/compile/verify；
- 依赖树循环检查；
- 包名扫描；
- Controller 路径扫描；
- LIVE/QMT/CTP Bean 扫描；
- 确认未引入 XXL-JOB 或删除 SnailJob，除非进入独立调度迁移阶段。

### C3：调度迁移计划实施

- 另行审批；
- 逐任务迁移；
- 验证单调度器；
- 验收后才清理 SnailJob。

### C4：Flyway 与契约

- 另行审批；
- 创建 V1.x 和正式契约；
- 执行隔离环境 CI；
- 不连接生产数据库。

## 15. 人工审批点

必须人工批准：

1. 根 `pom.xml` 增加 `quantpf-modules`；
2. 新增 `quantpf-modules` 和十个模块目录；
3. `ruoyi-admin/pom.xml` 接入 QuantPF 模块；
4. 任何 Flyway 依赖和 migration 文件；
5. 任何 XXL-JOB 依赖、部署文件和 SnailJob 删除；
6. 任何 contract 文件和生成策略；
7. 任何前端正式 QuantPF 页面/API 文件；
8. 任何历史 SQL 移动/归档；
9. 任何敏感配置轮换、撤销或前端注入方案；
10. 任何数据库连接、调度切换或生产环境动作。

## 16. 本轮最终状态

- plan-only：是；
- 业务代码修改：否；
- POM 修改：否；
- QuantPF 模块创建：否；
- SQL/Flyway 创建：否；
- 正式契约创建：否；
- XXL-JOB 添加：否；
- SnailJob 删除：否；
- 数据库迁移：否；
- Git commit：否。

下一阶段只有在人工审批后才能进入：

`PHASE0C-FOUNDATION-SKELETON-IMPLEMENTATION`
