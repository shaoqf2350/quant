# PHASE1B Paper Order Lifecycle and Remaining API Persistence Closure Plan

- 日期：2026-10-06
- 模式：GPT-5.6 Sol 单模型端到端
- 结论：本轮优先关闭订单生命周期关键边界；剩余 follow/audit/job API 作为明确 P1，若无现有 schema 支持则不伪造完成。

## 实际架构决策

1. 继续使用显式 MyBatis `SqlSession` 单事务策略，不在本轮擅自切换 Spring `@Transactional`。
2. 使用现有 `quantpf_paper_execution` 计算累计成交数量，不修改 Flyway 或表结构。
3. 使用现有订单字段 `frozen_cash`、`frozen_quantity` 表示剩余冻结量。
4. 使用 execution 聚合计算 `PARTIALLY_FILLED`/`FILLED`，禁止直接绕过领域/事务层修改状态。
5. 新增取消事务：锁定订单、释放剩余冻结资金/持仓、更新 CANCELED，重复取消幂等返回。
6. 风险/账户/订单 Spring 路径继续使用数据库事实；follow/audit/job 尚未具备完整 schema/application adapter，不伪造 PASS。
7. 测试必须使用随机前缀；禁止无条件清理 GLOBAL 或整表数据。

## 实施顺序

1. 增加 execution 数量聚合 Mapper；
2. 扩展结算事务支持多次 execution；
3. 增加 partial fill 状态和剩余冻结量计算；
4. 增加 cancel release 事务；
5. 增加 sell settlement 和冻结持仓释放测试；
6. 增加持久化取消 API；
7. 执行全量相关回归；
8. 生成最终报告、风险登记和 handoff。

## 本轮不实施

- Flyway/SQL/DDL；
- follow/audit/job execution 的完整 schema 施工；
- 前端；
- Redis/Broker/SnailJob/XXL-JOB/QMT/CTP/LIVE；
- Git。
