# QuantPF Runtime Transaction and Risk Persistence Closure Risk Register

| ID | 等级 | 风险 | 证据 | 影响 | 建议 | 是否阻塞 |
|---|---|---|---|---|---|---|
| P1B-RTR-P1-001 | P1 | 完整 ruoyi-admin production runtime/HTTP 尚未启动验收 | 本轮使用受控 Spring ApplicationContext + bean-backed MockMvc | dynamic-datasource/外部自动配置最终行为仍缺完整 HTTP 证据 | 后续建立完全隔离 runtime profile，再决定是否启动 | 否 |
| P1B-RTR-P1-002 | P1 | 正式事务策略是显式 MyBatis SqlSession，不是 Spring @Transactional proxy | `PaperPersistenceTransactionService` 显式 openSession/commit/rollback；5 个事务/幂等测试通过 | 不具备 Spring proxy 专项证据，但避免了当前连接混用 | 后续若要求 Spring proxy，改造为 SqlSessionTemplate 并专项验收 | 否 |
| P1B-RTR-P1-003 | P1 | follow/audit/job execution API 尚未全部持久化 | 本轮只关闭 Account/Order/Risk 核心路径 | 全部 Paper API 仍非数据库统一事实 | 后续单独持久化切换 | 是（Phase 1C 门禁） |
| P1B-RTR-P1-004 | P1 | partial fill/cancel/sell release 尚未完成 | 当前主要覆盖全量 BUY settlement | 交易边界仍不完整 | 后续扩展领域状态机和事务测试 | 是（Phase 1C 门禁） |
| P1B-RTR-P1-005 | P1 | finance POM 新增 quantpf-risk 依赖需要人工复核 | 本轮修改 `quantpf-modules/quantpf-finance/pom.xml` | 需要确认模块依赖边界 | 正式合并前人工复核 POM diff | 否 |
| P1B-RTR-P2-001 | P2 | 构建生成 target/flatten 产物 | Maven compile/test 执行 | 工作区状态未通过 Git 检查 | 用户后续人工检查 | 否 |
| P1B-RTR-P2-002 | P2 | 使用已有本地 schema 和随机前缀清理 | 未使用独立测试 schema/Testcontainers | 并行运行隔离性有限 | 后续增加专用测试 profile | 否 |
| P1B-RTR-P1-006 | P1 | 中间测试迭代曾使用过宽的 GLOBAL 风险记录清理条件 | 后续已改为随机前缀限定清理，并重新通过测试 | 中间迭代可能影响本地 quantpf 中既有 GLOBAL 风险测试记录；未连接生产库 | 后续所有测试必须使用随机前缀或专用测试 schema；不得使用无条件 GLOBAL 清理 | 是（测试数据安全） |
