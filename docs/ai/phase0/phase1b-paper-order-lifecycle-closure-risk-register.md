# QuantPF Paper Order Lifecycle Closure Risk Register

| ID | 等级 | 风险 | 证据 | 影响 | 建议 | 是否阻塞 |
|---|---|---|---|---|---|---|
| P1B-LC-P1-001 | P1 | follow/audit/job execution 尚未持久化 | 当前 Controller 仍为 AtomicBoolean/空列表骨架 | 剩余 Paper API 事实不统一 | 单独定义 schema/Mapper/Repository/Application Service 后实施 | 是（Phase 1C 门禁） |
| P1B-LC-P1-002 | P1 | 完整 production runtime/HTTP 未启动验收 | 当前使用受控 Spring Context + MockMvc | dynamic-datasource/外部自动配置仍需隔离验证 | 后续建立安全 runtime profile | 否 |
| P1B-LC-P1-003 | P1 | 正式事务为显式 SqlSession，不是 Spring @Transactional Proxy | 8 个生命周期/事务测试通过 | Spring Proxy 语义未验证 | 若后续强制采用 Spring Proxy，迁移 SqlSessionTemplate 后专项验收 | 否 |
| P1B-LC-P1-004 | P1 | partial fill/cancel/sell 仍缺更多边界组合 | 本轮覆盖核心 30+20+50、cancel、sell；未覆盖所有手续费/异常排列 | 极端交易边界仍需扩展 | 后续增加费用、部分撤单和异常矩阵 | 否 |
| P1B-LC-P1-005 | P1 | finance POM 新增 quantpf-risk 模块依赖需要人工复核 | 本轮 POM 变更 | 需要确认模块依赖边界 | 正式合并前人工复核 | 否 |
| P1B-LC-P1-006 | P1 | 测试使用现有本地 schema 而非独立 schema | 使用随机前缀清理 | 并行测试隔离性有限 | 建立专用测试 schema 或 Testcontainers | 否 |
| P1B-LC-P2-001 | P2 | Maven target/flatten 产物未执行 Git 检查 | 本轮禁止 Git | 工作区产物状态未审计 | 用户后续人工检查 | 否 |
