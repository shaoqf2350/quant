# QuantPF Phase 1B Persistence Transaction and Concurrency Closure Handoff

## 1. 当前目标

`PHASE1B-PAPER-PERSISTENCE-TRANSACTION-AND-CONCURRENCY-CLOSURE`

## 2. 当前结果

```text
PHASE1B-PAPER-PERSISTENCE-TRANSACTION-AND-CONCURRENCY-CLOSURE: PARTIAL
PHASE1C-PAPER-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```

本轮已完成：

- 首批 Paper persistence records、Mapper/XML；
- PaperOrder/PaperExecution/CashLedger/Position/TradingHalt/RiskSnapshot adapter；
- 单 MyBatis SqlSession 订单提交/成交结算事务服务；
- 本地 MySQL 并发幂等和 rollback 测试；
- domain/risk/execution/finance 回归测试。

## 3. 唯一交接入口文件

- `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1b-persistence-transaction-concurrency-closure.md`
- `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1b-persistence-transaction-concurrency-closure-commands.log`
- `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1b-persistence-transaction-concurrency-closure-risk-register.md`
- `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1b-persistence-transaction-concurrency-closure-manifest.json`

## 4. 本地数据库事实

只连接本地：

```text
127.0.0.1:3306/quantpf
```

已确认关键唯一索引存在：订单幂等键、账户+clientOrderId、executionId、execution 幂等键、ledger 幂等键、账户+instrument position。未执行 DDL、Flyway migration、DROP、重建表或重建数据库。

## 5. 测试证据

```text
PaperPersistenceTransactionIntegrationTest: 5 passed
quantpf-domain: 3 passed
quantpf-risk: 4 passed
quantpf-execution: 3 passed
quantpf-finance: 7 passed
Maven BUILD SUCCESS
```

测试通过环境变量传递本地数据库凭据，凭据值没有写入任何文件。

## 6. 当前 P0 阻塞项

### P0-001：正式 Spring runtime wiring 尚未完成

`PaperPersistenceTransactionService` 当前使用手工 `SqlSessionFactory`，还没有接入正式 Spring datasource、事务管理器、Mapper 扫描和 application context。

### P0-002：Paper Application API 尚未从内存切换到持久化事务路径

现有 Paper Application Service 仍主要使用内存 Map/内存 broker。数据库测试通过不能证明 HTTP/API 已使用持久化事实。

## 7. 仍禁止的范围

继续禁止：

- 前端实现；
- 修改根 POM或生产运行时依赖；
- 修改 YAML、正式 SQL、Flyway 文件，除非后续获得单独批准；
- Redis、Broker、SnailJob、XXL-JOB、QMT、CTP、LIVE；
- 生产数据库；
- 真实交易；
- 输出密码、Token、API Key、RSA 私钥或券商凭据；
- Git 命令；
- destructive filesystem/Git commands。

## 8. 下一步目标

```text
PHASE1B-SPRING-PERSISTENCE-INTEGRATION-AND-APPLICATION-CUTOVER
```

下一步需要：

1. 建立本地隔离 Spring persistence context；
2. 接入受控 datasource、Mapper scan 和统一事务边界；
3. 将 Paper account/order/settlement application service 从内存实现切换到 persistence adapter；
4. 保持 Redis、Broker、调度器和真实交易系统关闭；
5. 通过隔离 MockMvc/HTTP 测试验证 API 返回数据库事实；
6. 重新验收 rollback、幂等和状态机路径。

## 9. Phase 1C 门禁

在上述 P0-001、P0-002 关闭前，不允许进入 `PHASE1C-PAPER-FRONTEND-IMPLEMENTATION`。

## 10. 模型建议

- 主控和事务/runtime 集成：GPT-5.6 Sol；
- 文件白名单内的批量代码实现：deepseek-v4-pro；
- SQL、并发和事务边界独立审阅：GLM-5.3。

本轮不执行 Git 命令。
