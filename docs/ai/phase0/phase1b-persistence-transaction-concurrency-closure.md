# PHASE1B-PAPER-PERSISTENCE-TRANSACTION-AND-CONCURRENCY-CLOSURE

- 实施日期：2026-10-06
- 当前结果：`PARTIAL`
- 本轮已完成的子目标：持久化记录/Mapper/XML、Repository adapter、单一 MyBatis SqlSession 事务服务、本地并发幂等测试
- Git：本轮未执行任何 Git 命令
- 数据库：仅连接本地 `127.0.0.1:3306/quantpf`
- 外部系统：Redis、Broker、SnailJob、XXL-JOB、QMT、CTP、LIVE 均未连接
- 凭据：只通过当前进程环境变量注入，未写入源码、日志、报告或 manifest

## 1. 实施范围

本轮围绕 Paper 持久化和本地事务并发闭环实施：

1. 扩展账户、订单、执行、现金账务、持仓、交易暂停和风险快照的持久化记录、Mapper 和 XML；
2. 增加 PaperOrder、PaperExecution、CashLedger、Position、TradingHalt、RiskSnapshot repository port/adapter；
3. 新增 `PaperPersistenceTransactionService`，将订单提交和成交结算放在单一 MyBatis `SqlSession` 事务边界内；
4. 使用本地 MySQL 验证幂等键、clientOrderId、executionId、ledger event id、失败 rollback；
5. 执行 domain、risk、execution、finance 回归测试。

本轮没有实现前端、实盘交易、QMT/CTP/LIVE、调度迁移或生产部署。

## 2. 实际修改和新增文件

### POM

- `D:\workspace\quant\RuoYi-Vue-Plus\quantpf-modules\quantpf-domain\pom.xml`
  - 已有 test-scope `com.mysql:mysql-connector-j` 保持不变。
- `D:\workspace\quant\RuoYi-Vue-Plus\quantpf-modules\quantpf-finance\pom.xml`
  - 新增 test-scope `com.mysql:mysql-connector-j`，原因是 Maven test scope 不会从 domain 模块传递到 finance 测试 classpath；该依赖仅服务本地集成测试，不进入生产运行时。

未修改根 `pom.xml`、`ruoyi-admin/pom.xml` 或其他生产运行时依赖。

### Domain record

- `quantpf-domain/src/main/java/com/quantpf/domain/persistence/record/PaperOrderRow.java`
- `quantpf-domain/src/main/java/com/quantpf/domain/persistence/record/PaperExecutionRow.java`
- `quantpf-domain/src/main/java/com/quantpf/domain/persistence/record/CashLedgerRow.java`
- `quantpf-domain/src/main/java/com/quantpf/domain/persistence/record/PositionRow.java`
- `quantpf-domain/src/main/java/com/quantpf/domain/persistence/record/TradingHaltRow.java`
- `quantpf-domain/src/main/java/com/quantpf/domain/persistence/record/RiskSnapshotRow.java`

### Domain Mapper 和 XML

- `quantpf-domain/src/main/java/com/quantpf/domain/persistence/mapper/TradingAccountMapper.java`
- `quantpf-domain/src/main/java/com/quantpf/domain/persistence/mapper/PaperOrderMapper.java`
- `quantpf-domain/src/main/java/com/quantpf/domain/persistence/mapper/PaperExecutionMapper.java`
- `quantpf-domain/src/main/java/com/quantpf/domain/persistence/mapper/CashLedgerMapper.java`
- `quantpf-domain/src/main/java/com/quantpf/domain/persistence/mapper/PositionMapper.java`
- `quantpf-domain/src/main/java/com/quantpf/domain/persistence/mapper/TradingHaltMapper.java`
- `quantpf-domain/src/main/java/com/quantpf/domain/persistence/mapper/RiskSnapshotMapper.java`
- `quantpf-domain/src/main/resources/mapper/quantpf/TradingAccountMapper.xml`
- `quantpf-domain/src/main/resources/mapper/quantpf/PaperOrderMapper.xml`
- `quantpf-domain/src/main/resources/mapper/quantpf/PaperExecutionMapper.xml`
- `quantpf-domain/src/main/resources/mapper/quantpf/CashLedgerMapper.xml`
- `quantpf-domain/src/main/resources/mapper/quantpf/PositionMapper.xml`
- `quantpf-domain/src/main/resources/mapper/quantpf/TradingHaltMapper.xml`
- `quantpf-domain/src/main/resources/mapper/quantpf/RiskSnapshotMapper.xml`

### Repository port/adapter

- `quantpf-domain/src/main/java/com/quantpf/domain/repository/PaperOrderRepository.java`
- `quantpf-domain/src/main/java/com/quantpf/domain/repository/PaperExecutionRepository.java`
- `quantpf-domain/src/main/java/com/quantpf/domain/repository/CashLedgerRepository.java`
- `quantpf-domain/src/main/java/com/quantpf/domain/repository/TradingHaltRepository.java`
- `quantpf-domain/src/main/java/com/quantpf/domain/repository/RiskSnapshotRepository.java`
- `quantpf-finance/src/main/java/com/quantpf/finance/PaperOrderRepositoryAdapter.java`
- `quantpf-finance/src/main/java/com/quantpf/finance/PaperExecutionRepositoryAdapter.java`
- `quantpf-finance/src/main/java/com/quantpf/finance/CashLedgerRepositoryAdapter.java`
- `quantpf-finance/src/main/java/com/quantpf/finance/PositionRepositoryAdapter.java`
- `quantpf-finance/src/main/java/com/quantpf/finance/TradingHaltRepositoryAdapter.java`
- `quantpf-finance/src/main/java/com/quantpf/finance/RiskSnapshotRepositoryAdapter.java`
- `quantpf-domain/src/main/java/com/quantpf/domain/entity/Position.java`
  - 增加受控的持久化状态恢复方法。

### 事务服务和测试

- `quantpf-finance/src/main/java/com/quantpf/finance/PaperPersistenceTransactionService.java`
- `quantpf-finance/src/test/java/com/quantpf/finance/PaperPersistenceTransactionIntegrationTest.java`

## 3. 实际数据库结构核对

只读核对了本地 `quantpf` schema 的相关表、字段和索引。未执行 DDL、Flyway migration、DROP、重建数据库或重建表。

相关表：

- `quantpf_trading_account`
- `quantpf_cash_ledger`
- `quantpf_position`
- `quantpf_paper_order`
- `quantpf_paper_execution`
- `quantpf_risk_snapshot`
- `quantpf_trading_halt`

已确认的关键唯一约束：

- `uk_quantpf_account_code`
- `uk_quantpf_cash_ledger_idempotency`
- `uk_quantpf_position_account_instrument`
- `uk_quantpf_order_client`
- `uk_quantpf_order_idempotency`
- `uk_quantpf_execution_id`
- `uk_quantpf_execution_idempotency`

因此本轮没有新增 SQL/Flyway 约束提案，也没有执行数据库迁移。现有 schema 已具备本轮并发幂等测试需要的关键唯一性。

## 4. Mapper/XML 验收

已通过 Maven 编译和 MyBatis XML 资源加载：

- 账户：普通查询、`FOR UPDATE` 查询、插入、余额更新、期望余额条件更新；
- 订单：主键查询、`FOR UPDATE` 查询、幂等键查询、客户订单号查询、插入、状态和冻结金额更新；
- 执行：executionId 查询、幂等键查询、插入、订单计数；
- 现金账务：幂等键查询、插入、引用查询；
- 持仓：账户+标的查询、`FOR UPDATE` 查询、插入、状态更新；
- 交易暂停：active 计数、插入、resume；
- 风险快照：插入、按账户获取最新快照。

## 5. Repository adapter 验收

已实现首批 adapter：

- `TradingAccountRepositoryAdapter`
- `PaperOrderRepositoryAdapter`
- `PaperExecutionRepositoryAdapter`
- `CashLedgerRepositoryAdapter`
- `PositionRepositoryAdapter`
- `TradingHaltRepositoryAdapter`
- `RiskSnapshotRepositoryAdapter`

本轮已完成编译验收。完整 Spring Bean 装配和正式 Application API 持久化接入仍未完成，因此不能将 adapter 编译通过等同于生产运行时闭环通过。

## 6. 事务边界

### 6.1 订单提交

当前事务服务执行以下单事务序列：

```text
account SELECT FOR UPDATE
  -> idempotency/clientOrderId 查询
  -> 可用资金或持仓校验
  -> 账户余额/冻结金额更新
  -> 订单插入
  -> COMMIT
```

相同幂等键或相同账户 clientOrderId 会返回已有订单结果，不重复冻结资金。

### 6.2 成交结算

当前事务服务执行以下单事务序列：

```text
execution/ledger idempotency 查询
  -> order SELECT FOR UPDATE
  -> account SELECT FOR UPDATE
  -> execution 插入
  -> cash ledger 插入
  -> account 更新
  -> position 插入或更新
  -> order 状态更新为 FILLED
  -> COMMIT
```

任何步骤抛出运行时异常都会回滚同一 `SqlSession` 中的 execution、ledger、account、position 和 order 更新。

当前边界仍是本地 MyBatis service，不是 Spring `@Transactional` 运行时 Bean；这是本轮的主要未闭环项。

## 7. 幂等策略

1. 订单使用数据库唯一约束 `idempotency_key` 和 `(account_id, client_order_id)`；
2. 提交前使用账户行锁和业务查询，数据库唯一约束作为最终保护；
3. execution 使用 `execution_id` 和 execution 幂等键唯一约束；
4. cash ledger 使用 `idempotency_key` 唯一约束；
5. position 使用 `(account_id, instrument_id)` 唯一约束；
6. settlement 在写 execution 前检查 execution 幂等键和 ledger event 幂等键；
7. 重复请求返回 duplicate 结果，不再次变更余额、持仓或订单状态。

## 8. 并发测试结果

测试类：

```text
com.quantpf.finance.PaperPersistenceTransactionIntegrationTest
```

结果：

```text
Tests run: 5
Failures: 0
Errors: 0
Skipped: 0
```

覆盖：

- 相同 `idempotencyKey` 两线程提交：PASS；
- 相同 `clientOrderId`、不同幂等键两线程提交：PASS；
- 相同 `executionId` 两线程结算：PASS；
- 相同 ledger event id、不同 executionId：PASS；
- 结算在 ledger 后故障时整体 rollback：PASS。

验证结果包括：

- 订单最多创建一条；
- 资金只冻结一次；
- execution 最多一条；
- ledger 最多一条；
- 余额不会重复扣减；
- 持仓不会重复增加；
- 失败后不留下孤立 execution/ledger/position；
- 订单状态不会在失败事务中被推进。

## 9. 回归测试结果

使用 JDK21 执行了相关 Reactor 测试：

```text
quantpf-domain: 3 tests passed
quantpf-risk: 4 tests passed
quantpf-execution: 3 tests passed
quantpf-finance: 7 tests passed
Maven BUILD SUCCESS
```

finance 的 7 个测试包括既有 `PaperAccountLedgerServiceTest` 和本轮 5 个本地 MySQL 集成测试。

## 10. 未执行命令和原因

- Git status/log/diff/commit/push：未执行，遵守本阶段“不执行任何 Git 命令”。
- `Remove-Item`、`rm`、`git reset`、`git clean`、checkout/switch/merge/rebase：未执行。
- 空库完整 Flyway migrate：未执行。本轮只使用已有本地 schema，避免扩大数据库施工范围。
- 生产数据库、Redis、Broker、SnailJob、XXL-JOB、QMT、CTP、LIVE：未连接或启动。
- 完整 `ruoyi-admin` Spring Boot 启动：未执行，避免触发真实外部配置和运行时基础设施。
- 前端 lint/build/test：未执行，不属于本轮。

## 11. 当前风险和未解决问题

### P0

- P0-001：正式运行时尚未将本轮 `PaperPersistenceTransactionService` 接入 Spring Bean、正式 datasource、事务管理器和 Application API。当前只能证明隔离 MyBatis SqlSession 事务行为，不能证明生产运行时 wiring 安全。
- P0-002：Paper 领域 API 当前仍主要使用内存 Application Service，尚未切换到本轮持久化事务服务。直接进入前端会造成前端调用路径与数据库事实不一致。

### P1

- P1-001：当前未执行空库完整 Flyway migrate，只验证既有本地 schema。
- P1-002：TradingHalt 和 RiskSnapshot adapter 已编译，但缺少真实本地集成测试覆盖。
- P1-003：当前 service 使用手工 MyBatis `SqlSessionFactory`，后续必须转换为受控 Spring `@Transactional` 或等效统一事务组件。
- P1-004：PaperOrderRepositoryAdapter 的完整领域状态恢复和冻结金额恢复仍需增强；当前首批映射主要服务本地闭环测试。
- P1-005：执行结算目前按全量成交/`FILLED`路径验证，部分成交、多次合法 execution、撤单释放冻结资金等边界尚未完成。

### P2

- P2-001：本轮构建会产生既有 Maven `target`/flatten 产物；未执行 Git 检查。
- P2-002：未建立独立 test profile 或 Testcontainers 隔离环境。

## 12. Phase 1C 判定

```text
PHASE1B-PAPER-PERSISTENCE-TRANSACTION-AND-CONCURRENCY-CLOSURE: PARTIAL
PHASE1C-PAPER-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```

在 P0-001 和 P0-002 关闭前，不允许进入前端实现。

## 13. 下一步建议

下一步应为：

```text
PHASE1B-SPRING-PERSISTENCE-INTEGRATION-AND-APPLICATION-CUTOVER
```

重点：

1. 在不连接 Redis、调度器、Broker 或真实交易系统的前提下，把事务服务接入受控 Spring 隔离上下文；
2. 将 Paper Application Service 从内存路径切换到持久化 adapter；
3. 增加 TradingHalt、RiskSnapshot、部分成交、撤单释放冻结金额测试；
4. 再次验收健康接口和 Paper API 的持久化路径；
5. 关闭 P0 后再评估 Phase 1C。
