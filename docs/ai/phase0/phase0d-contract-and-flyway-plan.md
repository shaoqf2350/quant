# PHASE0D-CONTRACT-AND-FLYWAY-PLAN

- 日期：2026-10-06
- 阶段：`PHASE0D-CONTRACT-AND-FLYWAY-PLAN`
- 计划性质：plan-only；本轮不创建正式契约文件、不创建 Flyway SQL、不执行迁移。
- 用户额外批准动作：仅更新 `ruoyi-admin/src/main/resources/application-dev.yml` 的本地 dev 数据库/Redis配置；未连接外部系统。

## 1. 当前结论

```text
PHASE0D-CONTRACT-AND-FLYWAY-PLAN: COMPLETE
BUSINESS-CODE-IMPLEMENTATION: NOT STARTED
DATABASE-MIGRATION: NOT EXECUTED
```

本轮优先完成契约、数据库、事件和 Paper 领域逻辑的施工顺序设计。实际 Paper 业务代码必须等本计划审批后按 manifest 分阶段实施。

## 2. 本地开发配置变更

已按用户指令更新：

```text
D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\src\main\resources\application-dev.yml
```

变更范围：

- dev 主数据源指向本机 MySQL `quantpf`；
- dev 用户名设置为本地开发用户；
- dev Redis 使用本机 6379；
- Redis logical database 设置为 6；
- Redis 无密码；
- 未修改 `application.yml`、`application-prod.yml`；
- 未连接 MySQL；
- 未连接 Redis；
- 未执行 SQL 或 Flyway。

本报告不记录密码值。后续不得把本地密码提交到 Git 或复制到其他配置文件。

## 3. 正式 OpenAPI 版本

正式文件名冻结为：

```text
contract/openapi/quantpf-v29.yaml
```

规则：

- `quantpf-v28.yaml` 仅作为历史 reference；
- V29 是当前生成源；
- 所有正式 DTO、Controller、前端 API/types 必须从 V29 契约生成或按契约实现；
- 原型 API 不得直接复制。

## 4. Paper 功能逻辑优先级

后续实现顺序：

1. Instrument 与交易日历基础模型；
2. Paper TradingAccount、CashLedger、Position；
3. Order、Execution/Fill 和状态机；
4. PaperBrokerAdapter；
5. 风控 halt 和下单前门禁；
6. StrategySignal 与回测信号边界；
7. Paper account/order HTTP API；
8. 审计和 JobExecutionAudit；
9. 前端契约生成和页面接入。

Stage 1 只允许 `PaperBrokerAdapter`。禁止 QMT、CTP、LIVE submit/cancel。

## 5. Paper 订单状态机

```text
NEW
→ ACCEPTED
→ PARTIALLY_FILLED
→ FILLED
→ CANCEL_REQUESTED
→ CANCELED
→ REJECTED
```

必须具备：

- clientOrderId；
- requestId；
- eventId；
- idempotencyKey；
- 状态迁移校验；
- 余额冻结/释放；
- 持仓冻结/释放；
- 成交后账务更新；
- 拒绝原因；
- 幂等重放；
- 审计事件。

## 6. Flyway 版本计划

目标：MySQL 8.x，逻辑数据库 `quantpf`。

规划版本：

```text
V1_0_0__quantpf_baseline.sql
V1_1_0__instrument_and_market.sql
V1_2_0__research_and_signal.sql
V1_3_0__strategy_and_backtest.sql
V1_4_0__paper_account_and_ledger.sql
V1_5_0__paper_order_and_execution.sql
V1_6_0__risk_and_halt.sql
V1_7_0__job_and_audit.sql
V1_8_0__indexes_and_constraints.sql
```

本轮不创建上述 SQL 文件，不执行数据库迁移。

## 7. 事件 Schema 计划

```text
quantpf-order-event.v1.schema.json
quantpf-execution-event.v1.schema.json
quantpf-ledger-event.v1.schema.json
quantpf-risk-event.v1.schema.json
```

共同 envelope：

- eventId；
- eventType；
- occurredAt；
- requestId；
- aggregateType；
- aggregateId；
- schemaVersion；
- source；
- payload；
- traceId；
- idempotencyKey。

版本只增不覆盖。

## 8. 错误码和菜单事实源计划

错误码：

```text
contract/error-codes/quantpf-error-codes.yaml
```

字段：

```text
code, category, message, retryable, httpStatus, owner, deprecated
```

菜单：

```text
contract/menu/quantpf-menu-facts.yaml
```

Stage 1 菜单只允许：

- dashboard；
- research；
- market；
- strategy；
- backtest；
- paper account；
- paper order；
- risk；
- audit。

禁止 LIVE、QMT、CTP 和真实券商权限。

## 9. 数据库/Redis目标

非敏感环境目标：

```text
MySQL logical database: quantpf
Redis host: 127.0.0.1
Redis port: 6379
Redis database: 6
```

凭据不进入契约、Flyway、Java、POM 或文档。数据库/Redis连接验收另行执行。

## 10. 当前允许的后续代码实现

Phase 0D 计划审批后，优先进入：

```text
PHASE1A-PAPER-DOMAIN-SKELETON-IMPLEMENTATION
```

实现顺序：

1. `quantpf-domain` 实体/枚举/Mapper 契约；
2. `quantpf-execution` PaperBrokerAdapter 和订单状态机；
3. `quantpf-finance` 账户/账务服务；
4. `quantpf-risk` 风控门禁；
5. `quantpf-backtest` 最小回测执行边界；
6. `quantpf-job` 只调用 application service，不直接 SQL/Broker；
7. Controller 和 API 生成物最后接入。

DeepSeek-v4-pro 只能按批准 manifest 实施明确文件，不得自行修改数据库、调度或 LIVE 边界。