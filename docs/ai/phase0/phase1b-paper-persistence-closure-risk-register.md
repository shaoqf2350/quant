# QuantPF Phase 1B Persistence Closure Risk Register

| ID | 等级 | 风险 | 证据 | 影响 | 建议 | 是否阻塞 |
|---|---|---|---|---|---|---|
| P1B-C-P0-001 | P0 | MyBatis MySQL integration test 被测试 classpath 缺少 MySQL driver 阻塞 | 测试自动 skip：`MySQL JDBC driver is not on the Maven test classpath` | Mapper/XML 未完成真实数据库端到端验证 | 另行审批最小 test-scope MySQL driver POM 依赖 | 是 |
| P1B-C-P0-002 | P0 | Paper order/account/risk 尚未形成完整事务闭环 | 目前仅有 schema、第一批 Mapper 和内存领域逻辑 | 资金、持仓、订单一致性未完成 | 实现 transaction application service 后再验收 | 是 |
| P1B-C-P0-003 | P0 | 并发幂等尚未验证 | 尚无并发 integration test | 重复订单/成交/账务风险 | 增加并发唯一键和状态迁移测试 | 是 |
| P1B-C-P0-004 | P0 | 空库完整 Flyway migrate 尚未执行 | 当前 schema 先由 JDBC runner 创建，Flyway仅baseline/validate | 不能证明从空库可重建 | 单独建立隔离空库或审批清理策略后执行 | 是 |
| P1B-C-P1-001 | P1 | Redis 未集成 | 本轮明确不连接 Redis | 锁/缓存未验证 | Redis 单独阶段，不阻塞数据库核心闭环 | 否 |
| P1B-C-P1-002 | P1 | 完整应用未启动验证 | 已知底座自动配置阻塞 | API生产运行时未闭环 | 单独完整应用隔离验收 | 否 |
| P1B-C-P2-001 | P2 | 本轮未执行 Git 命令 | 用户明确要求不执行 Git | Git状态未重新采集 | 后续由人工需要时自行检查 | 否 |

## 处置结论

```text
PHASE1B-PAPER-INTEGRATION-TEST: PARTIAL
PHASE1C-PAPER-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```
## 2026-10-06 dependency approval update

| ID | 等级 | 风险 | 证据 | 影响 | 建议 | 是否阻塞 |
|---|---|---|---|---|---|---|
| P1B-C-P0-005 | P0 | MySQL JDBC test dependency 尚未获批 | `TradingAccountMapperIntegrationTest` 因 driver 不在 test classpath 自动 skip；已生成 proposal | 无法执行 MyBatis SqlSession 真实本地数据库测试 | 人工批准 test-scope `mysql-connector-j` 后再修改 POM | 是 |
| P1B-C-P2-002 | P2 | 本轮未执行 Git 命令 | 用户明确要求不执行 Git 命令 | Git 状态未刷新 | 后续由用户自行检查 | 否 |