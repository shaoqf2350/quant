# QuantPF Phase 1B Runtime Transaction and Risk Persistence Closure Handoff

## 1. 当前目标

`PHASE1B-RUNTIME-TRANSACTION-PROXY-AND-RISK-PERSISTENCE-CLOSURE`

## 2. 当前结果

```text
PHASE1B-RUNTIME-TRANSACTION-PROXY-AND-RISK-PERSISTENCE-CLOSURE: PARTIAL
PHASE1C-PAPER-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```

## 3. 本轮已完成

- TradingHalt active 查询和 resume 持久化；
- PersistentPaperRiskApplicationService；
- Risk Controller Spring 持久化注入；
- RiskSnapshot 写入；
- Risk API 数据库事实验收；
- 风险暂停期间订单拒绝；
- Account/Order/Risk Spring MockMvc 验收；
- 共享 PaperRiskGate 接入持久化订单路径；
- 既有事务、幂等和 rollback 回归。

## 4. 事务结论

正式选择为显式 MyBatis SqlSession 事务：

```text
Spring @Transactional Proxy：未使用
显式 SqlSession commit/rollback：PASS
```

本轮没有伪造 Spring proxy 已生效。后续如果必须统一为 Spring proxy，需要改造为 SqlSessionTemplate 或等效方案后重新验收。

## 5. 当前证据

```text
QuantPfSpringPersistenceWebIntegrationTest: 3 passed
PaperPersistenceTransactionIntegrationTest: 5 passed
QuantPF domain/risk/execution/finance regression: PASS
Health/standalone Paper API: PASS
```

只连接本地 `127.0.0.1:3306/quantpf`，凭据没有写入任何文件。

## 6. 当前 P1 阻塞项

- 完整 ruoyi-admin production runtime/HTTP 尚未安全启动验收；
- follow/audit/job execution 尚未全部持久化；
- partial fill/cancel release/sell frozen position release 尚未完成；
- finance POM 新增 quantpf-risk 依赖需要人工复核。

## 7. 禁止范围

继续禁止：

- 前端；
- 根 POM、正式 YAML、Flyway/SQL 未获批修改；
- Redis、Broker、SnailJob、XXL-JOB、QMT、CTP、LIVE；
- 生产数据库；
- Git 命令；
- 数据库迁移、DDL、DROP、USE；
- 输出任何密码、Token、API Key、私钥或券商凭据。

## 8. 下一步目标

```text
PHASE1B-PAPER-ORDER-LIFECYCLE-AND-REMAINING-API-PERSISTENCE-CLOSURE
```

下一步优先完成：

1. partial fill；
2. cancel release；
3. sell frozen position release；
4. follow/audit/job execution 的持久化边界；
5. 再评估是否能够关闭 Phase 1B 的交易边界门禁。

## 9. Phase 1C 门禁

当前不允许进入 Phase 1C。

## 10. 人工审批

本轮新增 `quantpf-modules/quantpf-finance/pom.xml` 对 `quantpf-risk` 的模块依赖。该依赖应在正式合并前进行人工复核。未修改根 POM、正式 YAML、Flyway 或 SQL。
