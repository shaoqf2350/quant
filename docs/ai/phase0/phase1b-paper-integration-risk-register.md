# QuantPF Phase 1B Paper Integration Risk Register

| ID | 等级 | 风险 | 证据 | 影响 | 建议 | 是否阻塞 |
|---|---|---|---|---|---|---|
| P1B-P0-001 | P0 | 正式 Flyway engine 尚未执行 | 本轮使用临时 JDBC runner 执行静态 SQL，POM 未引入 Flyway | 不能宣称 Flyway migrate/validate 完成 | 另行批准 Flyway dependency/CLI，执行隔离库 migrate/validate | 是 |
| P1B-P0-002 | P0 | MyBatis Repository 尚未端到端验证 | Mapper/XML 已创建并编译，但未在 MyBatis session 中执行 | 持久化适配可能存在映射问题 | 增加本地隔离 MyBatis integration test | 是 |
| P1B-P0-003 | P0 | Paper order/account/risk 尚未形成完整事务边界 | 当前主要为领域和第一批 adapter | 订单、资金、持仓一致性未完成 | 下一轮实现 transaction application service | 是 |
| P1B-P0-004 | P0 | 并发幂等未验证 | 尚无并发测试 | 重复订单/重复成交风险 | 增加并发 idempotency/unique key tests | 是 |
| P1B-P1-001 | P1 | 本地 dev 数据库配置存在凭据风险 | application-dev.yml 已由用户配置本地数据库 | 误提交会泄露本地凭据 | 密码不进入文档/日志；Git 提交前人工审查 | 否 |
| P1B-P1-002 | P1 | Redis 未纳入本轮集成 | 本轮明确不连接 Redis | 缓存/锁行为未验证 | 单独规划 Redis integration，不能阻塞数据库核心闭环 | 否 |
| P1B-P1-003 | P1 | 正式生产应用仍未启动验证 | 既有 Redisson/Lock4j/LiteFlow 启动阻塞 | API runtime 尚未完成 | 独立完整应用隔离启动阶段 | 否 |
| P1B-P2-001 | P2 | 迁移测试使用临时 runner | 非正式 Flyway CLI | 工具链证据需要补强 | 后续切换正式 Flyway engine | 否 |

## 处置结论

```text
PHASE1B-PAPER-INTEGRATION-TEST: PARTIAL
PHASE1C-PAPER-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```