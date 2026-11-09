# PHASE1B-PERSISTENCE-DEPENDENCY-IMPLEMENTATION-AND-MYBATIS-INTEGRATION

- 审计/实施日期：2026-10-06
- 阶段结果：`PASS（本轮目标）`
- 下游闭环状态：`PHASE1B-PAPER-PERSISTENCE-AND-CONCURRENCY-CLOSURE: PARTIAL`
- Git：本轮未执行任何 Git 命令
- 外部系统：仅连接本地 MySQL `127.0.0.1:3306/quantpf`
- 凭据：通过当前进程环境变量注入；未写入文件、日志或报告

## 1. 本轮范围

本轮只实施和验收：

1. `quantpf-domain/pom.xml` 的 test-scope MySQL JDBC driver；
2. 第一批 TradingAccount/PaperOrder/TradingHalt MyBatis Mapper 与 XML；
3. `TradingAccountRepositoryAdapter` 的编译和映射验证；
4. 本地 MyBatis `SqlSession` 插入、查询和事务 rollback 集成测试；
5. domain/finance 的 JDK21 编译和测试回归。

本轮未实施 Paper 订单完整持久化、执行/账务/持仓事务、并发幂等、Redis、Broker、调度器、QMT、CTP、LIVE 或前端。

## 2. 获批变更与实际变更

唯一获批 POM 变更：

```text
D:\workspace\quant\RuoYi-Vue-Plus\quantpf-modules\quantpf-domain\pom.xml
```

新增依赖：

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>test</scope>
</dependency>
```

版本未在模块 POM 中硬编码，由现有依赖管理解析为 `9.7.0`。未修改根 `pom.xml`、其他 POM、YAML、SQL、Flyway、前端或调度配置。

## 3. 第一批持久化文件

已存在并完成本轮编译/资源验收：

- `quantpf-domain/src/main/java/com/quantpf/domain/persistence/record/TradingAccountRow.java`
- `quantpf-domain/src/main/java/com/quantpf/domain/persistence/mapper/TradingAccountMapper.java`
- `quantpf-domain/src/main/java/com/quantpf/domain/persistence/mapper/PaperOrderMapper.java`
- `quantpf-domain/src/main/java/com/quantpf/domain/persistence/mapper/TradingHaltMapper.java`
- `quantpf-domain/src/main/resources/mapper/quantpf/TradingAccountMapper.xml`
- `quantpf-domain/src/main/resources/mapper/quantpf/PaperOrderMapper.xml`
- `quantpf-domain/src/main/resources/mapper/quantpf/TradingHaltMapper.xml`
- `quantpf-finance/src/main/java/com/quantpf/finance/TradingAccountRepositoryAdapter.java`
- `quantpf-domain/src/test/java/com/quantpf/domain/persistence/TradingAccountMapperIntegrationTest.java`

## 4. 依赖验收

Maven test dependency tree 已确认：

```text
com.mysql:mysql-connector-j:jar:9.7.0:test
```

依赖范围为 `test`，不会进入生产运行时依赖边界。

## 5. MyBatis 本地集成验收

目标库：

```text
127.0.0.1:3306/quantpf
```

测试通过进程环境变量读取：

```text
QPF_DB_USER
QPF_DB_PASSWORD
QPF_JDBC_URL
```

测试结果：

```text
TradingAccountMapperIntegrationTest: PASS
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

已验证：

- MySQL JDBC driver 可加载；
- Mapper XML 可被 `XMLMapperBuilder` 解析；
- `TradingAccountMapper.insert` 可写入本地表；
- 自增 ID 可回填；
- `TradingAccountMapper.selectById` 可读取；
- 账户余额字段映射正确；
- 测试事务显式 rollback；
- 未执行生产数据库连接。

## 6. 回归验收

JDK21 下执行 domain/finance 及所需上游模块测试：

```text
Maven BUILD SUCCESS
quantpf-domain: 3 tests, 0 failures, 0 errors, 0 skipped
quantpf-finance: 2 tests, 0 failures, 0 errors, 0 skipped
```

目标 MyBatis 测试在 POM 格式规范化后再次执行，退出码为 `0`。

## 7. 未完成项与边界

以下仍属于后续 Phase 1B persistence/concurrency closure，不因本轮通过而宣告完成：

- PaperOrderRepositoryAdapter；
- TradingHalt/Risk repository adapter；
- Paper execution、cash ledger、position settlement 持久化；
- account/order/execution/ledger 单事务服务；
- idempotency-key、clientOrderId、executionId、ledger event 并发/重复写入测试；
- 订单状态并发更新与余额/持仓不重复扣减；
- 空库完整 Flyway migrate；
- 正式 Spring Boot + MySQL 集成上下文；
- Redis、SnailJob、XXL-JOB、Broker、QMT、CTP、LIVE。

## 8. 判定

```text
PHASE1B-PERSISTENCE-DEPENDENCY-IMPLEMENTATION-AND-MYBATIS-INTEGRATION: PASS
PHASE1B-PAPER-PERSISTENCE-AND-CONCURRENCY-CLOSURE: PARTIAL
PHASE1C-PAPER-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```

本轮成功解除“缺少 MySQL test-scope driver 导致 MyBatis 测试自动 skip”的阻塞，但不允许将本轮结果误判为完整持久化与并发闭环。
