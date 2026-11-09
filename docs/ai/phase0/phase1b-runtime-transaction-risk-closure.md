# PHASE1B-RUNTIME-TRANSACTION-PROXY-AND-RISK-PERSISTENCE-CLOSURE

- 日期：2026-10-06
- 执行模式：GPT-5.6 Sol 单模型端到端
- 当前结果：`PARTIAL`
- 数据库：仅本地 `127.0.0.1:3306/quantpf`
- Git：本轮未执行任何 Git 命令
- 外部系统：Redis、Broker、SnailJob、XXL-JOB、QMT、CTP、LIVE 均未连接
- 凭据：仅通过当前进程环境变量注入，未写入源码、YAML、日志、报告或 manifest

## 1. 本轮实施范围

本轮完成：

1. 读取并审计上一阶段交接、报告、风险登记、manifest、V29 和当前实际代码；
2. 生成内部实施方案和文件白名单；
3. 扩展 TradingHalt persistence port、Mapper、XML 和 adapter，支持 active 查询和 resume；
4. 新增 `PersistentPaperRiskApplicationService`；
5. 将 Risk Controller 的 Spring runtime 路径切换为持久化服务；
6. 在受控 Spring configuration 中装配 TradingHalt/RiskSnapshot repository、Persistent Risk Service 和共享 `PaperRiskGate`；
7. 将持久化订单 Application Service 接入共享 risk gate，风险暂停时拒绝订单；
8. 增加本地 MySQL + Spring MockMvc 风险持久化测试；
9. 执行 QuantPF 和 admin API 回归测试；
10. 进行内存 fallback、重复扣款、重复持仓和事务边界审查；
11. 生成最终报告、命令日志、风险登记、manifest 和 handoff。

本轮没有实施前端、生产部署、实盘交易、Flyway、DDL、Redis、Broker、SnailJob、XXL-JOB、QMT、CTP 或 LIVE。

## 2. 内部方案和白名单

已生成：

- `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1b-runtime-transaction-risk-closure-plan.md`
- `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1b-runtime-transaction-risk-closure-file-whitelist.json`

实际修改均在白名单范围内。未修改根 POM、正式 YAML、Flyway/SQL、前端和调度配置。

## 3. 实际 Spring 扫描边界

启动类：

```text
D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\src\main\java\org\dromara\DromaraApplication.java
```

当前扫描：

```java
@SpringBootApplication(scanBasePackages = {"org.dromara", "com.quantpf.web"})
```

说明：

- `com.quantpf.web.controller` 和 `com.quantpf.web.config` 均在受控边界内；
- 没有把整个 `com.quantpf` 作为 component scan 根；
- QuantPF Mapper 通过 `@MapperScan` 接入；
- 没有扫描 QMT、CTP、LIVE、Broker 或调度器组件。

实际仓库中不存在 `quantpf-application` 模块，本轮没有创建该模块。

## 4. Spring Context 和 HTTP 验收方案

本轮使用：

```text
SpringJUnitConfig
+ 受控 TestConfig
+ 本地 DriverManagerDataSource
+ SqlSessionFactoryBean
+ MapperScan
+ bean-backed standalone MockMvc
```

这是不触发完整 ruoyi-admin 外部自动配置的等效隔离 HTTP 上下文。

本轮没有启动完整 ruoyi-admin WebApplicationContext/真实 HTTP 服务，因为那会触发未隔离的 Redis、SnailJob、dynamic-datasource 和其他外部配置。

测试只允许使用：

```text
127.0.0.1:3306/quantpf
```

测试结果：

```text
QuantPfSpringPersistenceWebIntegrationTest: 3 passed, 0 failed, 0 skipped
```

已验证：

- Spring ApplicationContext 能装配 SqlSessionFactory；
- QuantPfPaperRuntimeConfiguration 能装配；
- Account/Order/Risk Controller 能装配；
- Health API 可访问；
- Account API 读取数据库事实；
- Order API 写入并读取数据库事实；
- Risk API 写入并读取 TradingHalt/RiskSnapshot 数据库事实。

## 5. DataSource、MyBatis 和 Bean 装配

配置文件：

```text
D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\src\main\java\com\quantpf\web\config\QuantPfPaperRuntimeConfiguration.java
```

当前装配：

- `@MapperScan("com.quantpf.domain.persistence.mapper")`；
- `PaperRiskGate`；
- `TradingHaltRepositoryAdapter`；
- `RiskSnapshotRepositoryAdapter`；
- `PersistentPaperRiskApplicationService`；
- `PaperPersistenceTransactionService`；
- `PersistentPaperAccountApplicationService`；
- `PersistentPaperOrderApplicationService`；
- 缺少既有事务管理器时的 `DataSourceTransactionManager`。

没有创建 Redis、Broker、SnailJob、XXL-JOB、QMT、CTP 或 LIVE Bean。

## 6. 事务方案和代理结论

### 6.1 实际选择

本轮正式选择：

```text
显式 MyBatis SqlSession 单事务边界
```

原因：

- 当前 `PaperPersistenceTransactionService` 已经以一个 `SqlSession` 承载 account/order/execution/ledger/position 的提交和回滚；
- 当前持久化 Mapper 使用手工 MyBatis `SqlSessionFactory`；
- 直接叠加 Spring `@Transactional` 而不改为 `SqlSessionTemplate` 可能造成连接/事务边界混用；
- 本轮不扩大到根 POM、框架基础设施或完整 dynamic-datasource 改造。

### 6.2 代理结论

```text
Spring @Transactional Proxy：NOT USED
正式显式 SqlSession 事务：PASS（既有事务测试证据）
```

本轮没有伪造 Spring `@Transactional` 代理已经生效。

已复核：

- 订单提交和成交结算在单个显式 SqlSession 中执行；
- 异常路径调用 rollback；
- execution、ledger、position、account、order 的既有 rollback 测试通过；
- 本轮 Spring runtime 调用路径能够进入持久化 service；
- 未声明最终 Spring proxy 已闭环。

因此事务代理专项结论为：

```text
显式事务策略：PASS
Spring @Transactional Proxy：NOT_APPLICABLE_TO_CURRENT_FORMAL_STRATEGY
```

## 7. Risk/Halt/Snapshot 持久化

### 7.1 新增和修改文件

- `quantpf-domain/src/main/java/com/quantpf/domain/repository/TradingHaltRepository.java`
- `quantpf-domain/src/main/java/com/quantpf/domain/persistence/mapper/TradingHaltMapper.java`
- `quantpf-domain/src/main/resources/mapper/quantpf/TradingHaltMapper.xml`
- `quantpf-finance/src/main/java/com/quantpf/finance/TradingHaltRepositoryAdapter.java`
- `quantpf-finance/src/main/java/com/quantpf/finance/PersistentPaperRiskApplicationService.java`
- `ruoyi-admin/src/main/java/com/quantpf/web/config/QuantPfPaperRuntimeConfiguration.java`
- `ruoyi-admin/src/main/java/com/quantpf/web/controller/QuantPaperRiskController.java`

### 7.2 实际调用链

```text
QuantPaperRiskController
  -> PersistentPaperRiskApplicationService
  -> TradingHaltRepository / RiskSnapshotRepository
  -> MyBatis Mapper/XML
  -> 本地 quantpf MySQL
```

### 7.3 已验证

```text
GET  /api/quant/risk/status: PASS
POST /api/quant/risk/halt: PASS
POST /api/quant/risk/resume: PASS
```

数据库验证：

- GLOBAL active halt 成功写入；
- HALTED RiskSnapshot 成功写入；
- status 从数据库 active halt 读取；
- resume 将 active halt 更新为非 active；
- CLEAR RiskSnapshot 成功写入；
- 测试后清理随机/全局测试风险记录；
- 风险数据库失败时没有静默 fallback 到内存状态。

风险暂停期间，持久化订单路径通过共享 `PaperRiskGate` 拒绝订单，测试已验证 HTTP 返回业务冲突码。

## 8. Paper API 持久化和 fallback 审查

### 8.1 正式 Spring 路径

以下路径使用持久化服务：

- Account API；
- Order API；
- Risk API。

### 8.2 `PaperApiRuntime`

`PaperApiRuntime` 仍被 standalone compatibility constructor 引用，但：

- Spring Controller 使用带 `@Autowired` 的持久化构造器；
- compatibility constructor 为包内构造器；
- 受控 Spring Context 测试确认注入的是持久化 Service；
- 正式 Spring account/order/risk 路径没有使用静态内存单例。

### 8.3 fallback 结论

```text
Account Spring path memory fallback：ABSENT
Order Spring path memory fallback：ABSENT
Risk Spring path memory fallback：ABSENT
```

未完成或仍为非持久化实现的其他 API：

- follow；
- audit；
- job execution。

这些路径不在本轮核心 Risk/Halt/Snapshot 持久化闭环中，登记为 P1，不宣称全部 Paper API 已完成持久化。

## 9. 重复扣款和重复持仓审查

已有并重新回归的数据库约束/测试覆盖：

- idempotency key；
- account + clientOrderId；
- executionId；
- execution idempotency key；
- ledger idempotency key；
- account + instrument position 唯一性。

结论：

```text
重复扣款风险：在已覆盖的重复请求/重复 execution/重复 ledger 场景中未发现
重复持仓变更风险：在已覆盖的重复 execution 场景中未发现
订单重复创建：未发现
订单状态回退：未发现
```

边界限制：

- partial fill 尚未完成；
- cancel release 尚未完成；
- sell frozen position release 尚未完成；
- 多次合法 execution 尚未完成。

因此不能宣称所有交易边界均已闭环。

## 10. 回滚结论

既有 `PaperPersistenceTransactionIntegrationTest`：

```text
5 passed, 0 failed, 0 skipped
```

已覆盖：

- order submit；
- execution settlement；
- same execution idempotency；
- same ledger event idempotency；
- failure after ledger 的整体 rollback。

本轮 Spring Risk API 测试通过；风险 halt/resume 使用 Mapper DML，测试后完成清理。

结论：

```text
显式 SqlSession rollback：PASS
Spring @Transactional Proxy rollback：NOT_EXECUTED
```

## 11. 测试结果

### Maven 编译

```text
mvn -f D:\workspace\quant\RuoYi-Vue-Plus\pom.xml -pl ruoyi-admin -am -DskipTests compile
BUILD SUCCESS
```

### Spring Risk/Application Context 测试

```text
QuantPfSpringPersistenceWebIntegrationTest: 3 passed, 0 failed, 0 skipped
```

### 既有 Health/Paper API 测试

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

## 12. 未执行命令

- 所有 Git 命令：明确禁止；
- 完整 ruoyi-admin production WebApplicationContext/HTTP 启动：为避免真实 Redis、SnailJob、dynamic-datasource 等外部副作用，未执行；
- Flyway migrate、DDL、DROP、USE、数据库重建：明确禁止；
- Redis、Broker、SnailJob、XXL-JOB、QMT、CTP、LIVE：未连接；
- 前端 pnpm lint/build/test：不属于本轮；
- 生产数据库连接：未执行。

## 13. P0/P1/P2 风险

### P0

- 无新增数据库结构 P0。

### P1

- P1-001：完整 ruoyi-admin production runtime/HTTP 启动尚未隔离验收；当前已使用等效受控 Spring Context + MockMvc 证据；
- P1-002：本轮正式事务策略为显式 MyBatis SqlSession，Spring `@Transactional` proxy 未使用；若后续强制采用 Spring proxy，需要单独改造为 SqlSessionTemplate/受控事务方案；
- P1-003：follow、audit、job execution API 尚未全部切换到持久化事实；
- P1-004：partial fill、cancel release、sell frozen position release 尚未完成；
- P1-005：finance POM 新增 `quantpf-risk` 模块依赖，虽不引入外部运行时系统，但需要纳入后续 POM 审阅。

### P2

- P2-001：构建会生成 target/flatten 产物，本轮未执行 Git 检查；
- P2-002：本地测试仍使用已有 quantpf schema 和随机前缀清理，没有 Testcontainers/独立测试 schema。

## 14. 人工审批结论

本轮没有执行 Flyway、DDL 或正式 YAML 修改，不需要新的数据库迁移批准。

由于修改了：

```text
quantpf-modules/quantpf-finance/pom.xml
```

新增 `quantpf-risk` 模块依赖。该依赖只用于共享 PaperRiskGate 和 risk application 逻辑，不引入 Redis、Broker、调度器或实盘系统。建议在正式合并前进行一次 POM 变更人工复核。

## 15. 最终判定

```text
PHASE1B-RUNTIME-TRANSACTION-PROXY-AND-RISK-PERSISTENCE-CLOSURE: PARTIAL
PHASE1C-PAPER-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```

原因：

1. 核心 Risk/Halt/Snapshot 持久化和 Spring 隔离 API 已通过；
2. 显式 SqlSession 事务及既有幂等/rollback 已通过；
3. 但 Spring `@Transactional` proxy 未执行；
4. 完整 production runtime 尚未安全启动；
5. follow/audit/job API 尚未全部持久化；
6. partial fill/cancel/sell release 尚未完成。

不能据此放行 Phase 1C。

## 16. 测试数据安全修正

中间测试迭代的初版 Risk API 测试曾使用过宽的 GLOBAL 风险记录清理条件。发现该问题后，测试已修正为使用随机前缀限定 `reason LIKE prefix%` 清理，并重新执行通过：

```text
QuantPfSpringPersistenceWebIntegrationTest: 3 passed, 0 failed, 0 skipped
```

该中间迭代只连接本地 `quantpf`，但由于无法恢复未知的历史本地记录，后续测试数据安全登记为 P1。今后禁止使用无条件 GLOBAL 清理，必须使用随机前缀或专用测试 schema。
