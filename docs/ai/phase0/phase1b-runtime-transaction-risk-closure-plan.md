# PHASE1B Runtime Transaction Proxy and Risk Persistence Closure Plan

- 日期：2026-10-06
- 模式：GPT-5.6 Sol 单模型端到端执行
- 目标：关闭 Paper Risk/Halt/Snapshot 持久化和正式运行时 fallback 风险

## 架构决策

1. 本轮使用受控 Spring ApplicationContext + bean-backed MockMvc 作为隔离 HTTP 验收上下文。完整 ruoyi-admin 启动会触发未隔离的 Redis/SnailJob/dynamic-datasource 外部配置，因此不在本轮启动。
2. 本轮正式事务策略继续采用 `PaperPersistenceTransactionService` 的单一显式 MyBatis `SqlSession` commit/rollback。该服务不是 Spring `@Transactional` 代理事务；不伪造为已完成的 Spring proxy 闭环。通过 rollback、并发和单 session 证据验收显式事务边界。
3. Risk/Halt/Snapshot 通过 `TradingHaltRepository` 和 `RiskSnapshotRepository` 接入持久化 Paper Risk Application Service。正式 Controller 注入持久化服务；不得在数据库异常时 fallback 到内存状态。
4. account/order 的 Spring runtime 持久化路径保持；standalone compatibility constructor 仅供既有 standalone MVC 测试使用。
5. 不修改 Flyway、SQL、YAML、根 POM，不执行 DDL、迁移或 Git。

## 实施流程

1. 扩展 halt mapper/repository 的 active 查询和 resume；
2. 新增持久化 Risk Application Service；
3. 将 Risk Controller 改为 Spring 注入持久化服务，并保留包内 standalone compatibility constructor；
4. 在 runtime configuration 中装配 Risk Service、Repository adapter 和共享 PaperRiskGate；
5. 让持久化 Order Application Service 在提交前使用共享 risk gate；
6. 增加风险 API 本地 MySQL + MockMvc 测试；
7. 增加事务/幂等回归和 fallback 审查；
8. 执行 domain/risk/execution/finance/admin 相关测试；
9. 生成最终报告和 handoff。

## 门禁结论规则

- 若 Risk API 仍使用内存服务、Spring runtime 测试失败、出现重复资金/持仓变更或无法证明 rollback，则结果必须为 PARTIAL。
- 本轮不因 account/order 通过而放行 Phase 1C。
