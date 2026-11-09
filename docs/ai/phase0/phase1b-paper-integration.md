# PHASE1B-PAPER-INTEGRATION-TEST-AND-PERSISTENCE-ADAPTER

- 日期：2026-10-06
- 项目：QuantPF
- 结果：`PARTIAL`
- 说明：本地隔离数据库 schema 和持久化第一批编译通过；尚未使用正式 Flyway 引擎，Repository 端到端 MyBatis 集成尚未完成。

## 1. 本轮完成内容

### Flyway SQL 隔离执行

由于当前 POM 未引入 Flyway 运行时依赖，且本轮禁止修改 POM，本轮使用临时 JDK21 JDBC runner 在本地 `quantpf` 数据库执行静态 migration 文件，未修改 SQL 源文件。

执行结果：

- V1_0_0 至 V1_8_0 全部执行成功；
- `quantpf_schema_marker` 存在；
- 9 个核心表存在；
- 事务回滚测试通过；
- 未连接生产数据库；
- 未连接 Redis。

这不是 Flyway API 运行结果，因此不能把它表述为“Flyway engine validate/migrate PASS”。正式 Flyway 引擎验收仍需后续补充依赖或使用受控 CLI，并重新审批 POM/工具链边界。

### Mapper 与 XML

已创建第一批 persistence 文件：

- `TradingAccountRow`；
- `TradingAccountMapper`；
- `PaperOrderMapper`；
- `TradingHaltMapper`；
- 对应 MyBatis XML；
- `TradingAccountRepositoryAdapter`。

Mapper/XML 位于 `quantpf-domain` persistence owner 范围。

### 领域恢复能力

为 `TradingAccount` 增加持久化恢复余额能力，用于 Repository adapter 将数据库余额恢复为领域对象。

## 2. 数据库执行结果

本地数据库连接目标仅为：

```text
127.0.0.1:3306/quantpf
```

验证通过：

```text
quantpf_schema_marker
quantpf_instrument
quantpf_trading_account
quantpf_cash_ledger
quantpf_position
quantpf_paper_order
quantpf_paper_execution
quantpf_trading_halt
quantpf_audit_record
```

事务验证：

- 开启本地事务；
- 插入临时账户记录；
- 执行 rollback；
- rollback 成功；
- 未保留测试账户数据。

本轮没有执行 Redis 操作。

## 3. Maven/JDK21

使用显式 Temurin JDK21.0.12.1 和 Maven 3.9.13。

执行结果：

```text
quantpf-domain / quantpf-finance testCompile: PASS
quantpf-domain / quantpf-finance test: PASS
```

新增/已有测试均通过：

```text
Failures: 0
Errors: 0
Skipped: 0
```

## 4. POM 状态

本轮没有修改：

- 根 `pom.xml`；
- `ruoyi-admin/pom.xml`；
- QuantPF 模块 POM。

因此没有引入 Flyway dependency。正式 Flyway engine 验收需另行批准 POM 或独立 CLI 方案。

## 5. 连接和安全边界

本轮：

- 只连接本地 MySQL `quantpf`；
- 未连接生产数据库；
- 未连接 Redis；
- 未连接 Broker；
- 未启动 SnailJob；
- 未启动 XXL-JOB；
- 未连接 QMT/CTP/LIVE；
- 未读取或输出密码；
- 未修改 dev YAML；
- 未修改 production YAML。

## 6. 当前判定

```text
PHASE1B-PAPER-INTEGRATION-TEST: PARTIAL
PHASE1C-PAPER-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```

原因：

1. 本地 schema 已执行并验证；
2. 第一批 Mapper/XML 编译通过；
3. 领域和 finance 单元测试通过；
4. 但正式 Flyway engine 未执行；
5. MyBatis Repository 尚未端到端集成测试；
6. Paper order/account/risk 与持久化事务尚未完整打通；
7. 并发幂等尚未完成。

## 7. 下一步

下一步应继续完成：

- 正式 Flyway engine/CLI 验收；
- MyBatis session + Mapper XML 集成测试；
- TradingAccountRepositoryAdapter 本地数据库测试；
- PaperOrderRepositoryAdapter；
- Risk/Halt repository；
- Application Service 事务；
- 并发幂等和唯一键测试；
- 只有完成后才能进入 Phase 1C。