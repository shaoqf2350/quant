# PHASE1B-PAPER-PERSISTENCE-AND-CONCURRENCY-CLOSURE

- 日期：2026-10-06
- 结果：`PARTIAL`
- Git：本轮未执行任何 Git 命令
- 数据库：仅本地 `quantpf`
- Redis：未连接

## 1. Flyway engine 结果

已使用本地 Maven repository 中的 Flyway 11.7.2 core/mysql jar 通过临时 JDK21 runner 执行：

```text
Flyway baselineOnMigrate=true
baselineVersion=1.8.0
```

结果：

```text
BASELINE_MIGRATE=0
VALIDATE=PASS
```

说明：此前已通过临时 JDBC runner 创建 V1.0.0 至 V1.8.0 schema。本轮正式 Flyway engine 对已有本地 schema 执行 baseline 1.8.0 和 validate，未重复执行或破坏已有表。

因此：

- Flyway engine baseline：PASS；
- Flyway validate：PASS；
- 从空库完整 Flyway migrate：NOT EXECUTED；
- 生产数据库：未连接。

本轮没有修改 POM，Flyway 使用临时 runner 依赖，不代表项目 POM 已接入 Flyway。

## 2. 本地数据库结果

目标仅为：

```text
127.0.0.1:3306/quantpf
```

通过检查：

- `quantpf_schema_marker`；
- `quantpf_instrument`；
- `quantpf_trading_account`；
- `quantpf_cash_ledger`；
- `quantpf_position`；
- `quantpf_paper_order`；
- `quantpf_paper_execution`；
- `quantpf_trading_halt`；
- `quantpf_audit_record`。

本地事务 rollback 检查通过。

## 3. Mapper/XML 结果

已完成并编译：

- TradingAccount persistence row；
- TradingAccountMapper；
- PaperOrderMapper；
- TradingHaltMapper；
- TradingAccountMapper.xml；
- PaperOrderMapper.xml；
- TradingHaltMapper.xml；
- TradingAccountRepositoryAdapter。

MyBatis XML 可以被编译资源处理，但实际 SqlSession 数据库测试被本模块测试 classpath 缺少 MySQL JDBC driver 阻塞并自动 skip：

```text
MySQL JDBC driver is not on the Maven test classpath
```

没有自行修改 POM。

## 4. 事务和并发结果

当前已完成：

- JDBC 临时事务插入/rollback；
- 领域资金和持仓单元测试；
- Repository adapter 编译；
- MyBatis XML 资源加载前的测试结构编译。

尚未完成：

- Paper order 真实持久化事务；
- account/order/execution/ledger 同事务提交；
- 并发 Idempotency-Key；
- clientOrderId 并发唯一性；
- executionId 重复写入；
- ledger event 重复写入；
- 订单状态并发更新。

## 5. POM 状态

本轮未修改任何 POM。

若下一轮要运行 MyBatis + MySQL 集成测试，需要在测试 classpath 引入 MySQL driver，必须先生成 POM 变更建议并人工批准，不能自行修改 POM。

## 6. 连接安全

本轮：

- 只连接本地 MySQL `quantpf`；
- 未连接 Redis；
- 未连接 Broker；
- 未启动 SnailJob/XXL-JOB；
- 未连接 QMT/CTP/LIVE；
- 未输出数据库密码；
- 未输出 Redis 密码。

## 7. 当前判定

```text
PHASE1B-PAPER-INTEGRATION-TEST: PARTIAL
PHASE1C-PAPER-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```

完整 PASS 还需要：

1. MySQL driver 测试依赖审批；
2. MyBatis SqlSession integration test；
3. PaperOrder/Risk/Halt repository；
4. 事务服务；
5. 并发幂等测试。