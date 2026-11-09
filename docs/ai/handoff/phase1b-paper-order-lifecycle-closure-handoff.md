# QuantPF Phase 1B Paper Order Lifecycle Closure Handoff

## 1. 当前目标

`PHASE1B-PAPER-ORDER-LIFECYCLE-AND-REMAINING-API-PERSISTENCE-CLOSURE`

## 2. 当前结果

```text
PHASE1B-PAPER-ORDER-LIFECYCLE-AND-REMAINING-API-PERSISTENCE-CLOSURE: PARTIAL
PHASE1C-PAPER-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```

## 3. 本轮完成

- partial fill；
- 30+20+50 多次 execution；
- `PARTIALLY_FILLED` 到 `FILLED`；
- 剩余冻结现金处理；
- cancel release；
- 重复 cancel 幂等；
- sell settlement；
- frozen position release；
- persistent cancel API；
- execution quantity aggregate；
- lifecycle integration tests。

## 4. 测试证据

```text
PaperPersistenceTransactionIntegrationTest: 8 passed
QuantPfSpringPersistenceWebIntegrationTest: 3 passed
Health/standalone Paper API: PASS
QuantPF domain/risk/execution/finance regression: PASS
ruoyi-admin compile: PASS
```

只连接本地 `127.0.0.1:3306/quantpf`，未执行 Git、Flyway、DDL 或外部系统连接。

## 5. 未完成 P1

- follow persistence；
- audit persistence；
- job execution persistence；
- 完整 production runtime/HTTP 隔离启动；
- Spring @Transactional Proxy；
- 更完整的手续费/异常组合矩阵。

## 6. 下一步目标

```text
PHASE1B-REMAINING-API-PERSISTENCE-AND-PAPER-MVP-CLOSURE
```

优先完成 follow/audit/job execution 的持久化事实源；若 schema 不足，先生成 migration proposal，不直接修改 Flyway/SQL。

## 7. Phase 1C 门禁

当前仍不允许进入 Phase 1C。

## 8. 人工审批

`quantpf-finance/pom.xml` 新增 `quantpf-risk` 依赖需要人工复核。没有新的 Flyway/YAML/根 POM 批准需求。
