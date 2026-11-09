# QuantPF Phase 1B Persistence Transaction and Concurrency Closure Risk Register

| ID | 等级 | 风险 | 证据 | 影响 | 建议 | 是否阻塞 |
|---|---|---|---|---|---|---|
| P1B-TC-P0-001 | P0 | 持久化事务服务尚未接入正式 Spring Boot runtime wiring | 当前使用手工 `SqlSessionFactory`；未启动 `ruoyi-admin` 完整上下文 | 不能证明正式 datasource、事务管理器、Mapper 扫描和 API 运行时一致 | 下一阶段在隔离 Spring 上下文完成受控接入和 HTTP/API 验收 | 是 |
| P1B-TC-P0-002 | P0 | Paper Application API 仍主要使用内存服务 | 已有 Paper API 未切换到 `PaperPersistenceTransactionService` | 前端接入会看到内存事实与数据库事实不一致 | 先完成 application service cutover，再允许 Phase 1C | 是 |
| P1B-TC-P1-001 | P1 | 空库完整 Flyway migrate 未执行 | 本轮只使用已有本地 `quantpf` schema | 新环境可重复建库能力仍未验收 | 单独建立受控空 schema 验收，不在本轮直接迁移 | 否 |
| P1B-TC-P1-002 | P1 | Halt/RiskSnapshot adapter 缺少真实本地集成测试 | adapter 已编译，暂无对应 SQL 集成测试 | 风险门禁和暂停状态的数据库读写仍缺证据 | 下一阶段增加 mapper/adapter integration tests | 否 |
| P1B-TC-P1-003 | P1 | 部分成交、多次合法 execution、撤单释放冻结资金未完成 | 当前只验证一次全量成交/FILLED和失败 rollback | 后续订单状态和账务边界不完整 | 增加状态机驱动的 partial fill/cancel 测试与实现 | 否 |
| P1B-TC-P1-004 | P1 | finance 模块新增 test-scope MySQL driver 属于本轮必要扩展 | domain test scope 不会传递给 finance test classpath | 不影响生产运行时，但需在审阅中确认依赖边界 | 保持 test scope；后续可迁移到独立 integration-test profile | 否 |
| P1B-TC-P2-001 | P2 | 构建生成 target/flatten 产物 | Maven 编译日志显示 target 和 flattened POM 处理 | 需要用户自行确认工作区状态 | 不执行 Git；后续由用户人工检查 | 否 |
| P1B-TC-P2-002 | P2 | 未使用 Testcontainers/独立测试库 | 当前测试连接本地 quantpf 并使用随机前缀和清理 | 并行测试隔离能力有限 | 后续增加隔离 profile 或专用测试 schema | 否 |
