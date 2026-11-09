# PHASE1B-SPRING-PERSISTENCE-INTEGRATION-AND-APPLICATION-CUTOVER

- 实施日期：2026-10-06
- 阶段结果：`PARTIAL`
- 本轮目标：受控 Spring persistence context、Paper account/order API 持久化切换、隔离 MockMvc 验收
- Git：本轮未执行任何 Git 命令
- 数据库：仅连接本地 `127.0.0.1:3306/quantpf`
- Redis/Broker/SnailJob/XXL-JOB/QMT/CTP/LIVE：均未连接
- 凭据：仅通过当前进程环境变量传递，未写入源码、YAML、日志、报告或 manifest

## 1. 实施范围

本轮实际实施：

1. 审计 QuantPF 实际模块树和 Controller/Application Service 调用链；
2. 创建受控 `QuantPfPaperRuntimeConfiguration`；
3. 接入 DataSource、SqlSessionFactory、MapperScan、Repository/Application Service Bean；
4. 扩大已批准的启动扫描边界，从 `com.quantpf.web.controller` 扩展为 `com.quantpf.web`，使受控 config 包可被发现；
5. 创建数据库事实驱动的 Paper account/order Application Service；
6. 将账户和订单 Controller 在 Spring runtime 路径切换到持久化服务；
7. 创建 Spring ApplicationContext + MockMvc + 本地 MySQL 隔离测试；
8. 验收健康 API、账户查询、订单提交/成交结算和数据库事实；
9. 回归 domain/risk/execution/finance 和既有 standalone API 测试。

本轮没有实施前端、生产部署、真实交易、Redis、Broker、SnailJob、XXL-JOB、QMT、CTP 或 LIVE。

## 2. 实际 Spring 扫描边界

当前启动类：

```text
D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\src\main\java\org\dromara\DromaraApplication.java
```

实际扫描配置：

```java
@SpringBootApplication(
    scanBasePackages = {
        "org.dromara",
        "com.quantpf.web"
    }
)
```

说明：

- `com.quantpf.web.controller` 仍然被包含；
- `com.quantpf.web.config` 现在也进入受控扫描边界；
- 没有把整个 `com.quantpf` 作为 Spring component scan 根；
- domain、finance 等模块的持久化类通过配置类中的 `@MapperScan` 和显式 `@Bean` 接入；
- 未扫描或初始化 QMT、CTP、LIVE、Broker 或调度器组件。

## 3. DataSource 和 MyBatis 配置

新增配置类：

```text
D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\src\main\java\com\quantpf\web\config\QuantPfPaperRuntimeConfiguration.java
```

配置内容：

- `@Configuration(proxyBeanMethods = false)`；
- `@MapperScan(basePackages = "com.quantpf.domain.persistence.mapper")`；
- 注入 `SqlSessionFactory`；
- 创建 `PaperPersistenceTransactionService`；
- 创建 `PersistentPaperAccountApplicationService`；
- 创建 `PersistentPaperOrderApplicationService`；
- 在缺少既有事务管理器时提供 `DataSourceTransactionManager`；
- 不创建 Redis、Broker、SnailJob 或 XXL-JOB Bean。

隔离测试中的 DataSource 使用 `DriverManagerDataSource`，JDBC URL 和凭据仅从当前进程环境变量读取。生产 YAML 未修改。

隔离测试中的 MyBatis 使用：

- `SqlSessionFactoryBean`；
- `classpath*:mapper/quantpf/*.xml`；
- `@MapperScan`；
- 已有 MySQL JDBC test-scope 依赖。

## 4. 事务管理方式

当前正式接入状态：

```text
Spring DataSource / SqlSessionFactory / Mapper Bean：已在隔离 Context 验证
DataSourceTransactionManager Bean：已创建并可装配
PaperPersistenceTransactionService：已作为 Spring Bean 装配
实际订单/结算提交回滚：仍由 service 内部显式 MyBatis SqlSession commit/rollback 承载
```

因此本轮不能宣称已经完成最终 `@Transactional` 代理闭环：

- `PaperPersistenceTransactionService` 仍显式创建和管理 MyBatis `SqlSession`；
- 尚未把所有提交/结算方法转换为 Spring Proxy 生效的 `@Transactional` 事务；
- 尚未在完整 ruoyi-admin 生产启动上下文中确认 dynamic-datasource 的最终事务管理器选择；
- 本轮证明的是“Spring Bean wiring + 本地隔离数据库事实 + 显式单 SqlSession 事务路径”，不是最终生产事务代理结论。

## 5. Repository Bean 装配

已由 Spring configuration 装配并通过隔离测试间接使用：

- TradingAccountMapper；
- PaperOrderMapper；
- CashLedgerMapper；
- PaperExecutionMapper；
- PositionMapper；
- TradingHaltMapper；
- RiskSnapshotMapper；
- PaperPersistenceTransactionService；
- PersistentPaperAccountApplicationService；
- PersistentPaperOrderApplicationService。

现有 Repository adapter 已完成编译，但本轮 account/order API 直接使用持久化 Application Service 和 Mapper port。Controller 未直接执行 SQL。

## 6. Application Service 切换前后

### 切换前

```text
Controller
  -> PaperApiRuntime 静态单例
  -> 内存 Map Application Service
  -> InMemoryPaperBrokerAdapter
```

账户和订单 API 主要读取进程内存，不代表数据库事实。

### 本轮 Spring runtime 路径

```text
Controller
  -> PersistentPaperAccountApplicationService / PersistentPaperOrderApplicationService
  -> PaperPersistenceTransactionService
  -> MyBatis Mapper
  -> 本地 quantpf MySQL
```

已切换的路径：

- `GET /api/quant/account/page`：读取 `quantpf_trading_account`；
- `GET /api/quant/{id}/ledger`：读取 `quantpf_cash_ledger`；
- `GET /api/quant/order/page`：读取 `quantpf_paper_order`；
- `POST /api/quant/order/paper`：订单提交、资金冻结、execution、ledger、position 和 order 状态通过持久化事务服务处理。

standalone MVC 测试仍保留兼容构造器和内存替身，用于既有隔离单元测试；Spring runtime 注入路径不再依赖静态内存服务。

尚未完全切换：

- 风险状态 Controller 仍主要依赖内存 `PaperRiskApplicationService`；
- follow/audit/job execution Controller 仍不是本轮持久化切换目标；
- 生产完整 Application API 尚未全部切换为数据库事实。

## 7. Controller 调用链

已验收账户和订单调用链：

```text
QuantPaperAccountController
  -> PersistentPaperAccountApplicationService
  -> TradingAccountMapper / CashLedgerMapper
  -> 本地 MySQL
```

```text
QuantPaperOrderController
  -> PersistentPaperOrderApplicationService
  -> PaperPersistenceTransactionService
  -> PaperOrderMapper / PaperExecutionMapper / CashLedgerMapper / PositionMapper / TradingAccountMapper
  -> 本地 MySQL
```

Controller 没有直接写 SQL，也没有直接操作 Mapper。

## 8. Paper API 验收结果

测试类：

```text
com.quantpf.web.controller.QuantPfSpringPersistenceWebIntegrationTest
```

结果：

```text
Tests run: 2
Failures: 0
Errors: 0
Skipped: 0
```

覆盖：

1. Spring ApplicationContext 能够装配 SqlSessionFactory、QuantPfPaperRuntimeConfiguration 和 Controller Bean；
2. `GET /api/quant/health` 返回 200 且 `liveEnabled=false`；
3. `GET /api/quant/account/page` 返回本地测试账户；
4. `POST /api/quant/order/paper` 返回 `FILLED`；
5. 订单记录真实写入本地 `quantpf_paper_order`；
6. 账户余额真实反映成交结算；
7. 测试后删除随机前缀测试数据；
8. 没有使用内存默认账户作为数据库失败 fallback。

## 9. Health API 验收结果

隔离 Spring Context 中已验收：

```text
GET /api/quant/health: PASS
HTTP status: 200
liveEnabled: false
```

既有 standalone health 测试也通过。

## 10. TradingHalt/RiskSnapshot 结果

本轮已完成：

- TradingHalt/RiskSnapshot Mapper、XML、Row、Repository adapter 的 Spring classpath 可编译；
- `@MapperScan` 范围包含相关 Mapper。

本轮未完成：

- TradingHalt 真实 API 持久化读写验收；
- RiskSnapshot 真实 API 持久化读写验收；
- 风险 Controller 从内存服务切换到数据库服务。

这些事项保留为 P1，不伪造为 PASS。

## 11. rollback 结果

本轮 API 订单路径复用了上一阶段已通过的单 SqlSession rollback 机制，并由本轮 Spring 隔离 Context 调用。

上一阶段已验证：

- execution、ledger、position、account、order 在异常后整体 rollback；
- 相同 idempotencyKey 不重复创建订单；
- 相同 clientOrderId 不重复创建订单；
- 相同 executionId 不重复结算；
- 相同 ledger event id 不重复写入。

本轮新增 Spring Context 订单路径验证了正常提交和数据库事实；尚未把异常 rollback 改写为 Spring `@Transactional` Proxy 专项测试，因此最终事务代理仍为 P1/P0 边界项。

## 12. 回归测试结果

### Admin 编译

```text
mvn -f D:\workspace\quant\RuoYi-Vue-Plus\pom.xml -pl ruoyi-admin -am -DskipTests compile
BUILD SUCCESS
```

### Spring persistence API test

```text
QuantPfSpringPersistenceWebIntegrationTest: 2 passed, 0 failed, 0 skipped
```

### 既有 standalone API/Health tests

```text
QuantPfHealthControllerWebMvcTest: PASS
QuantPaperApiStandaloneTest: PASS
```

### QuantPF 回归

```text
quantpf-domain: PASS
quantpf-risk: PASS
quantpf-execution: PASS
quantpf-finance: PASS
Maven BUILD SUCCESS
```

## 13. 未执行命令

- Git status/log/diff/commit/push：未执行，遵守本轮禁止 Git 命令要求；
- 完整 `ruoyi-admin` 真实启动：未执行，避免读取/触发生产 Redis、SnailJob、Broker 和其他外部配置；
- 空库完整 Flyway migrate：未执行；
- DDL、DROP、USE、数据库重建：未执行；
- Redis、Broker、SnailJob、XXL-JOB、QMT、CTP、LIVE：未连接；
- 前端 lint/build/test：未执行；
- 生产数据库：未连接。

## 14. 当前判定

```text
PHASE1B-SPRING-PERSISTENCE-INTEGRATION-AND-APPLICATION-CUTOVER: PARTIAL
PHASE1C-PAPER-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```

本轮已经关闭“Spring 隔离 Context 完全未建立”和“account/order API 完全依赖内存”的直接阻塞，但没有关闭全部正式 runtime / application cutover 风险。

## 15. P0/P1/P2 风险摘要

### P0

- P0-001：完整 ruoyi-admin 生产运行时没有启动验收，dynamic-datasource 的最终数据源和事务管理器选择尚未获得 HTTP 级证据；
- P0-002：风险状态、follow、audit、job API 尚未全部切换到持久化 Application Service，不能宣称整个 Paper API 已完全脱离内存路径。

### P1

- P1-001：`PaperPersistenceTransactionService` 仍以显式 MyBatis SqlSession commit/rollback 为主，未完成最终 Spring `@Transactional` Proxy 专项闭环；
- P1-002：TradingHalt/RiskSnapshot 真实数据库读写和 API 验收未完成；
- P1-003：partial fill、cancel release、卖出冻结持仓释放等边界尚未完成；
- P1-004：完整 production datasource 配置没有在本轮修改或启动验证；
- P1-005：本轮隔离测试使用 standalone MockMvc + Spring Bean Context，而非完整 WebApplicationContext。

### P2

- P2-001：Maven 生成 target/flatten 构建产物；本轮未执行 Git 检查；
- P2-002：尚未建立独立 test profile/Testcontainers 隔离数据库。

## 16. Phase 1C 门禁

```text
不允许进入 PHASE1C-PAPER-FRONTEND-IMPLEMENTATION
```

必须先完成 P0-001、P0-002，并完成正式 Spring transaction/application cutover 的进一步验收。

## 17. 是否需要人工批准新的 POM/YAML/Flyway 变更

```text
不需要新的 Flyway/SQL/DML 变更批准。
```

本轮：

- 未修改正式 YAML；
- 未修改 Flyway SQL；
- 未执行数据库迁移；
- 未修改根 POM；
- 未新增生产运行时数据库依赖；
- 未新增 XXL-JOB 依赖。

本轮新增 finance/application runtime 代码未要求新的人工批准；如下一轮要把 Spring `@Transactional`、dynamic-datasource 或测试 profile 正式写入生产配置，必须重新提出文件级审批。
