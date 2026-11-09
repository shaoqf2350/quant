# QuantPF Phase 1A Paper Domain Risk Register

| ID | 等级 | 风险 | 证据 | 影响 | 建议 | 是否阻塞 |
|---|---|---|---|---|---|---|
| P1A-P0-001 | P0 | Paper 领域代码尚未接入数据库事务 | 本轮只实现 Entity/Repository interface 和内存逻辑 | 生产持久化和并发一致性未验证 | 下一阶段先实现 Mapper/Repository/事务边界并在隔离库验收 | 是 |
| P1A-P0-002 | P0 | Paper API 尚未实现 | 本轮未创建 Controller | 无法通过正式 OpenAPI 访问 Paper 功能 | 进入 PHASE1A-PAPER-APPLICATION-API | 是 |
| P1A-P0-003 | P0 | SnailJob/XXL-JOB 未迁移 | 底座仍为 SnailJob | 不能把 job port 当作可运行调度 | 独立调度迁移，不在 Paper 阶段启用新调度器 | 是 |
| P1A-P1-001 | P1 | PaperBrokerAdapter 当前为内存实现 | `InMemoryPaperBrokerAdapter` | 不能作为生产持久化执行器 | 下一阶段接入 repository/application service，但保持 Paper-only | 否 |
| P1A-P1-002 | P1 | 订单状态机尚未接入事件和审计 | 当前仅有状态迁移和内存成交 | 事件重放和审计闭环未完成 | 实现 event publisher port 和 audit service | 否 |
| P1A-P1-003 | P1 | 资金/持仓逻辑尚未接入事务 | PaperAccountLedgerService 为纯内存服务 | 并发下可能出现一致性问题 | Mapper/事务层实现后补充并发测试 | 否 |
| P1A-P1-004 | P1 | Flyway 尚未在隔离数据库执行 | 本轮未连接数据库 | SQL 与实际 MySQL 兼容性未验证 | 单独执行隔离库 migration 验收 | 是 |
| P1A-P2-001 | P2 | 测试覆盖仍是第一批核心路径 | 新增 11 个单元测试 | 并发、费用、部分成交边界仍不足 | 增加 property/concurrency/fee tests | 否 |
| P1A-P2-002 | P2 | Node v25.2.1 非 LTS | 基线事实 | 前端 CI 维护风险 | 后续冻结 LTS | 否 |

## 处置结论

```text
PHASE1A-PAPER-DOMAIN-IMPLEMENTATION: PASS
PHASE1A-PAPER-APPLICATION-API: NOT STARTED
```