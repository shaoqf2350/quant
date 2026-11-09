# PHASE0D-CONTRACT-AND-FLYWAY-IMPLEMENTATION 实施报告

- 日期：2026-10-06
- 阶段：PHASE0D-CONTRACT-AND-FLYWAY-IMPLEMENTATION
- 结果：PASS（正式文件创建和静态检查通过）
- 数据库迁移：未执行
- 外部连接：未执行
- 业务 Java：未创建
- POM：未修改

## 1. 实际创建文件

已创建：

- contract/openapi/quantpf-v29.yaml；
- 4 个 v1 Event JSON Schema；
- contract/error-codes/quantpf-error-codes.yaml；
- contract/menu/quantpf-menu-facts.yaml；
- 9 个静态 Flyway SQL 文件；
- Phase 0D 实施报告、命令日志、风险登记、manifest 和 handoff。

## 2. OpenAPI

正式文件：

`	ext
D:\workspace\quant\RuoYi-Vue-Plus\contract\openapi\quantpf-v29.yaml
`

SHA-256：$openHash

覆盖：health、dashboard、instrument、market、research、strategy、backtest、paper account、paper order、paper follow、risk、job execution、audit。

未包含 QMT、CTP、LIVE submit/cancel operation。

## 3. 事件 Schema

已创建：

`	ext
quantpf-order-event.v1.schema.json
quantpf-execution-event.v1.schema.json
quantpf-ledger-event.v1.schema.json
quantpf-risk-event.v1.schema.json
`

4 个 JSON Schema 均通过 PowerShell JSON 解析。事件 envelope 包含 eventId、eventType、occurredAt、requestId、aggregateType、aggregateId、schemaVersion、source、payload、traceId、idempotencyKey。

## 4. 错误码和菜单

错误码文件已覆盖请求、幂等、订单、账户、资金、持仓、风控、行情、审计和内部错误。

菜单事实源只包含 Stage 1 允许的 dashboard、research、market、strategy、backtest、paper account、paper order、risk、audit；明确禁止 LIVE、QMT、CTP 和真实券商操作。

## 5. Flyway

目标：MySQL 8.x，逻辑数据库 quantpf。

已创建：

`	ext
V1_0_0__quantpf_baseline.sql
V1_1_0__instrument_and_market.sql
V1_2_0__research_and_signal.sql
V1_3_0__strategy_and_backtest.sql
V1_4_0__paper_account_and_ledger.sql
V1_5_0__paper_order_and_execution.sql
V1_6_0__risk_and_halt.sql
V1_7_0__job_and_audit.sql
V1_8_0__indexes_and_constraints.sql
`

SQL 静态扫描通过：未发现 CREATE DATABASE、USE、DROP TABLE、动态 PREPARE/EXECUTE。

Flyway SQL 未执行，未连接 MySQL。

## 6. Dev YAML

按用户额外授权更新：

`	ext
D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\src\main\resources\application-dev.yml
`

SHA-256：$yamlHash

目标为本地 MySQL quantpf 和本地 Redis 6379/database 6。密码没有写入报告、manifest、命令日志或 handoff。

未修改 pplication.yml 和 pplication-prod.yml。

## 7. POM 和业务代码

- 根 POM：未修改；
- uoyi-admin/pom.xml：未修改；
- QuantPF POM：未修改；
- Entity/Mapper/Repository/Service/Controller：本轮未创建；
- PaperBrokerAdapter：本轮未创建；
- QMT/CTP/LIVE：未创建。

本轮不需要新增 Flyway 或契约插件依赖；后续如需 CI 校验插件，必须另行审批。

## 8. 外部连接

本轮未连接：

- MySQL；
- Redis；
- Broker；
- SnailJob；
- XXL-JOB；
- QMT；
- CTP；
- LIVE。

未执行 SQL、Flyway、调度注册、心跳或任务。

## 9. 当前阶段判定

`	ext
PHASE0D-CONTRACT-AND-FLYWAY-IMPLEMENTATION: PASS
PHASE1A-PAPER-DOMAIN-IMPLEMENTATION: ALLOWED TO START AFTER HUMAN REVIEW
`

这里的 PASS 仅表示正式契约和静态 Flyway 文件已创建并通过静态检查，不表示数据库迁移已执行，也不表示完整生产应用已启动。

## 10. 回滚

本轮新增文件均可通过文件级审阅回滚；不使用 git reset、git clean 或 Remove-Item。Dev YAML 回滚必须由人工审阅后按文件级差异恢复。