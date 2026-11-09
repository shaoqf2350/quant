# QuantPF Phase 1B Spring Persistence/Application Cutover Risk Register

| ID | 等级 | 风险 | 证据 | 影响 | 建议 | 是否阻塞 |
|---|---|---|---|---|---|---|
| P1B-SP-P0-001 | P0 | 完整 ruoyi-admin production runtime 未启动验收 | 本轮仅使用受控 Spring ApplicationContext + local DataSource + bean-backed MockMvc | dynamic-datasource 最终选择、真实启动副作用和 production HTTP wiring 仍无证据 | 下一轮建立完全隔离的 admin runtime profile 或等效受控启动验收，不连接 Redis/SnailJob/Broker | 是 |
| P1B-SP-P0-002 | P0 | 不是所有 Paper API 都已切换到持久化事实 | account/order 已切换；risk/follow/audit/job 仍有内存或空实现 | 前端全面接入会出现 API 路径事实不一致 | 完成 risk/halt/snapshot 和剩余 Paper API 的持久化 cutover | 是 |
| P1B-SP-P1-001 | P1 | 事务服务仍以手工 SqlSession commit/rollback 为主 | `PaperPersistenceTransactionService` 未使用最终 Spring `@Transactional` Proxy | 事务管理器 Bean 已装配，但代理生效和多数据源选择未完整证明 | 下一轮完成受控 Spring transaction proxy 验收或明确保留显式事务的正式决策 | 否 |
| P1B-SP-P1-002 | P1 | TradingHalt/RiskSnapshot 只有 Mapper/adapter 编译证据 | 本轮未完成真实 API 读写测试 | 风险门禁数据持久化尚未形成完整证据链 | 增加本地 mapper/adapter/API 集成测试 | 否 |
| P1B-SP-P1-003 | P1 | partial fill/cancel release/sell frozen position 边界未完成 | 本轮只验证一次全量 BUY 结算 | 订单状态、冻结资金和持仓边界仍不完整 | 先扩展领域状态机和事务测试，再接入 API | 否 |
| P1B-SP-P1-004 | P1 | 测试使用 ApplicationContext + standalone MockMvc，而非完整 WebApplicationContext | Spring Bean Context 通过，但 DispatcherServlet 不是正式 admin runtime | API 路由和 Bean 事实已测，完整 web runtime 仍需复核 | 下一轮使用受控 WebApplicationContext 或隔离 HTTP 启动 | 否 |
| P1B-SP-P2-001 | P2 | 构建生成 target/flatten 产物 | Maven compile/test 日志 | 未执行 Git，产物状态未审计 | 由用户后续人工检查，不执行 Git | 否 |
| P1B-SP-P2-002 | P2 | 本地测试依赖现有 quantpf schema 和随机前缀清理 | 未使用独立 Testcontainers/schema | 并行执行隔离性有限 | 后续增加专用测试 profile 或隔离 schema | 否 |
