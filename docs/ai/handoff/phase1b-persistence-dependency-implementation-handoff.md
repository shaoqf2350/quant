# QuantPF Phase 1B Persistence Dependency Implementation Handoff

## 1. 当前唯一入口

请先阅读：

- `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1b-persistence-dependency-implementation.md`
- `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1b-persistence-dependency-implementation-commands.log`
- `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1b-persistence-dependency-implementation-risk-register.md`
- `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1b-persistence-dependency-implementation-manifest.json`

## 2. 当前目标和结果

```text
PHASE1B-PERSISTENCE-DEPENDENCY-IMPLEMENTATION-AND-MYBATIS-INTEGRATION: PASS
PHASE1B-PAPER-PERSISTENCE-AND-CONCURRENCY-CLOSURE: PARTIAL
PHASE1C-PAPER-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```

已解除 MySQL JDBC test classpath 阻塞。不要把本轮 PASS 误判为完整 Paper 持久化和并发闭环 PASS。

## 3. 实际变更

仅修改：

```text
D:\workspace\quant\RuoYi-Vue-Plus\quantpf-modules\quantpf-domain\pom.xml
```

新增：`com.mysql:mysql-connector-j`，`scope=test`，Maven 解析版本 `9.7.0`。根 POM 未修改。

## 4. 已验收文件

首批 Mapper/XML、TradingAccountRow、TradingAccountRepositoryAdapter 和 TradingAccountMapperIntegrationTest 已编译/执行；目标测试通过并显式 rollback。

本地目标库仅为 `127.0.0.1:3306/quantpf`。密码没有写入任何仓库文件或文档。

## 5. 验收证据

```text
TradingAccountMapperIntegrationTest: 1 passed, 0 failed, 0 skipped
quantpf-domain tests: 3 passed, 0 failed, 0 skipped
quantpf-finance tests: 2 passed, 0 failed, 0 skipped
Maven BUILD SUCCESS
```

## 6. 未完成 P0/P1

- PaperOrderRepositoryAdapter；
- TradingHalt/Risk adapter；
- execution/cash ledger/position persistence；
- account/order/execution/ledger atomic transaction service；
- idempotency-key/clientOrderId/executionId/ledger event concurrent duplicate tests；
- empty-schema Flyway migrate；
- full Spring Boot datasource/MyBatis integration.

## 7. 严格边界

继续禁止：

- 修改根 `pom.xml` 或未获批 POM；
- 修改 YAML/SQL/Flyway/contract/frontend；
- Redis、Broker、SnailJob、XXL-JOB、QMT、CTP、LIVE；
- 生产数据库；
- 输出任何密码、Token、API Key 或凭据；
- Git 命令（用户本阶段明确要求不执行 Git 命令）；
- destructive filesystem/Git commands。

## 8. 下一步目标

```text
PHASE1B-PAPER-PERSISTENCE-TRANSACTION-AND-CONCURRENCY-CLOSURE
```

下一步先实现 PaperOrder/Risk/Halt/Execution/Ledger/Position 的最小持久化 adapter 和事务服务，再补充本地并发幂等验收；仍不进入前端和真实交易接入。

## 9. 推荐模型

- 主控/事务边界/验收：GPT-5.6 Sol；
- 批量实现：deepseek-v4-pro（必须严格按文件白名单）；
- 独立 SQL/并发审阅：GLM-5.3。
