# PHASE1B-PAPER-ORDER-LIFECYCLE-AND-REMAINING-API-PERSISTENCE-CLOSURE

- 日期：2026-10-06
- 执行模式：GPT-5.6 Sol 单模型端到端
- 当前结果：`PARTIAL`
- 本地数据库：仅 `127.0.0.1:3306/quantpf`
- Git：本轮未执行任何 Git 命令
- 外部系统：Redis、Broker、SnailJob、XXL-JOB、QMT、CTP、LIVE 均未连接
- 凭据：仅通过当前进程环境变量注入，未写入任何文件

## 1. 本轮完成内容

1. 增加 Paper execution 按订单累计成交数量查询；
2. 将成交事务扩展为多次 execution；
3. 支持 `PARTIALLY_FILLED` 和 `FILLED` 状态；
4. 检查 execution 数量不得超过订单剩余数量；
5. 部分成交时按实际 execution 更新资金、持仓和剩余冻结量；
6. 最终成交时释放剩余冻结资金/持仓；
7. 新增 cancel release 事务；
8. 取消订单时释放剩余冻结现金或冻结持仓；
9. 重复取消返回幂等结果；
10. 完成卖出成交结算测试；
11. 增加持久化订单取消 API；
12. 增加 partial fill、cancel、sell settlement 集成测试；
13. 执行 QuantPF 和 admin API 回归测试。

## 2. 实际修改文件

### Domain Mapper

- `quantpf-modules/quantpf-domain/src/main/java/com/quantpf/domain/persistence/mapper/PaperExecutionMapper.java`
- `quantpf-modules/quantpf-domain/src/main/resources/mapper/quantpf/PaperExecutionMapper.xml`

新增：

```text
sumExecutedQuantity(orderId)
```

### Finance 事务服务

- `quantpf-modules/quantpf-finance/src/main/java/com/quantpf/finance/PaperPersistenceTransactionService.java`

新增/修正：

- 多次 execution；
- partial fill；
- 剩余成交量检查；
- 买入剩余冻结现金计算；
- 卖出剩余冻结持仓计算；
- cancel release；
- 重复 cancel 幂等；
- sell settlement；
- execution/ledger/account/position/order 单事务提交和 rollback。

### Application Service 和 Controller

- `quantpf-modules/quantpf-finance/src/main/java/com/quantpf/finance/PersistentPaperOrderApplicationService.java`
- `ruoyi-admin/src/main/java/com/quantpf/web/controller/QuantPaperOrderController.java`

新增持久化取消路径：

```text
POST /api/quant/order/{id}/cancel
```

### 测试

- `quantpf-modules/quantpf-finance/src/test/java/com/quantpf/finance/PaperPersistenceTransactionIntegrationTest.java`

新增覆盖：

- 3 次 execution：30 + 20 + 50；
- `PARTIALLY_FILLED` → `FILLED`；
- 最终冻结现金释放；
- cancel release；
- 重复 cancel 幂等；
- sell execution；
- frozen position 释放；
- sell settlement 增加可用资金。

## 3. Partial fill 结论

```text
PARTIAL FILL：PASS
```

已验证：

```text
订单数量：100
Execution 1：30
Execution 2：20
Execution 3：50
```

结果：

- 第一次成交后状态为 `PARTIALLY_FILLED`；
- 第二次成交后仍为 `PARTIALLY_FILLED`；
- 第三次成交后为 `FILLED`；
- 总 execution 数量为 100；
- execution 数量超过剩余订单数量时拒绝；
- 最终成交后冻结现金归零。

## 4. Cancel release 结论

```text
CANCEL RELEASE：PASS
```

已验证：

- 未成交订单取消；
- 剩余冻结资金释放回 available cash；
- 订单状态变为 `CANCELED`；
- 重复取消不会再次释放资金；
- 已完成成交的终态订单不能取消；
- 卖出订单的剩余冻结持仓可释放。

## 5. 买入/卖出结算结论

### 买入

```text
BUY SETTLEMENT：PASS
```

验证：

- 资金冻结；
- execution 写入；
- cash ledger 写入；
- position 增加；
- average cost 更新；
- partial fill 的剩余冻结量正确；
- final fill 后冻结资金归零。

### 卖出

```text
SELL SETTLEMENT：PASS
```

验证：

- 初始持仓冻结；
- execution 写入；
- position quantity 减少；
- frozen position 减少；
- realized PnL 更新；
- cash ledger 增加卖出收入；
- final fill 后冻结持仓归零；
- 重复 execution 不重复增加资金或减少持仓。

## 6. 多次 execution 结论

```text
MULTIPLE EXECUTION：PASS
```

已验证：

- 每个 executionId 唯一；
- 每个 execution 的 ledger event 唯一；
- 成交累计数量正确；
- 订单状态不回退；
- 重放 executionId 不重复结算；
- ledger event 重放不重复变更资金。

## 7. 事务和 rollback 结论

当前正式策略仍为：

```text
显式 MyBatis SqlSession 单事务边界
```

不是 Spring `@Transactional` Proxy。

已通过：

- order submit；
- buy settlement；
- sell settlement；
- partial fill；
- cancel release；
- execution 幂等；
- ledger 幂等；
- failure after ledger rollback；
- account rollback；
- position rollback；
- order rollback。

## 8. 剩余 API 持久化结论

本轮没有完成以下 API 的完整持久化：

- follow；
- audit；
- job execution。

当前状态：

```text
remainingApiPersistence：PARTIAL
```

原因：

- 当前 Controller 仍存在 `AtomicBoolean`、空列表或骨架实现；
- 本轮没有修改 Flyway/SQL；
- 若要完成这些 API，需要单独定义数据库事实源、Mapper、Repository 和 schema 使用边界；
- 不允许为了测试通过而继续使用内存默认值作为正式路径。

## 9. 测试结果

### Paper 事务生命周期集成测试

```text
PaperPersistenceTransactionIntegrationTest
Tests run: 8
Failures: 0
Errors: 0
Skipped: 0
```

### Spring/API 测试

```text
QuantPfSpringPersistenceWebIntegrationTest: 3 passed
QuantPfHealthControllerWebMvcTest: PASS
QuantPaperApiStandaloneTest: PASS
```

### QuantPF 回归

```text
quantpf-domain: PASS
quantpf-risk: PASS
quantpf-execution: PASS
quantpf-finance: PASS
```

### 编译

```text
ruoyi-admin -am compile: BUILD SUCCESS
```

## 10. 测试数据安全

本轮新增测试使用随机前缀：

- account code；
- instrument code；
- order clientOrderId；
- idempotencyKey；
- executionId；
- ledger event id。

测试清理使用随机账户/标的前缀关联数据，不执行：

- DROP；
- USE；
- DROP DATABASE；
- DROP TABLE；
- 无条件整表删除。

## 11. P0/P1/P2 风险

### P0

当前没有新增 P0。

### P1

- P1-001：follow/audit/job execution 尚未全部持久化；
- P1-002：完整 production runtime/HTTP 尚未启动验收；
- P1-003：Spring `@Transactional` Proxy 未使用，当前依赖显式 SqlSession 事务；
- P1-004：完整撤单 API 的 standalone compatibility path 仍不是本轮重点；
- P1-005：finance POM 的 `quantpf-risk` 依赖仍需人工复核；
- P1-006：本轮没有完成 Testcontainers/独立测试 schema。

### P2

- P2-001：Maven 构建产生 target/flatten 产物，本轮未执行 Git 检查；
- P2-002：follow/audit/job API 的契约事实源需要后续明确。

## 12. 是否需要人工批准

不需要新的：

- Flyway 批准；
- SQL/DDL 批准；
- YAML 批准；
- 根 POM 批准。

需要人工复核：

```text
quantpf-modules/quantpf-finance/pom.xml
```

新增 `quantpf-risk` 模块依赖的边界。

## 13. 本阶段判定

```text
PHASE1B-PAPER-ORDER-LIFECYCLE-AND-REMAINING-API-PERSISTENCE-CLOSURE: PARTIAL
PHASE1C-PAPER-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```

原因：核心订单生命周期已经明显推进，但 follow/audit/job execution 仍未完成持久化闭环，因此不允许进入前端阶段。
