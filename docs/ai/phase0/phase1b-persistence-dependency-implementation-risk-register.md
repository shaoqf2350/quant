# QuantPF Phase 1B Persistence Dependency Implementation Risk Register

| ID | 等级 | 风险 | 证据 | 影响 | 建议 | 是否阻塞 |
|---|---|---|---|---|---|---|
| P1B-DI-P0-001 | P0 | MySQL JDBC driver 缺失导致的 MyBatis 集成测试阻塞 | test dependency tree 已解析 `com.mysql:mysql-connector-j:9.7.0:test`；目标测试 1/1 通过 | 本轮阻塞已解除 | 保持 test scope；禁止将 driver 无审查地提升为生产依赖 | 否 |
| P1B-DI-P1-001 | P1 | Paper 订单/执行/账务/持仓尚未形成完整持久化事务 | 当前仅 TradingAccount Mapper/XML 和 adapter 完成首批验收 | 不能证明资金、持仓和订单状态跨表原子性 | 下一轮实现事务 application service 与相应 adapter，并使用本地隔离库测试 | 是 |
| P1B-DI-P1-002 | P1 | 并发幂等尚未验收 | 尚无两线程 idempotency-key/clientOrderId/executionId/ledger event 测试 | 可能重复下单、重复扣款或重复持仓变更 | 下一轮增加唯一约束/冲突处理/并发测试 | 是 |
| P1B-DI-P1-003 | P1 | 当前集成测试为手工 MyBatis SqlSession，不是完整 Spring Boot 上下文 | `TradingAccountMapperIntegrationTest` 使用 `UnpooledDataSource` 和 `XMLMapperBuilder` | 尚不能证明正式运行时 datasource/transaction/mybatis wiring | 后续在隔离 profile 下补充 Spring 集成验收 | 否 |
| P1B-DI-P1-004 | P1 | 空库完整 Flyway migrate 尚未执行 | 本轮只连接已有本地 `quantpf` schema | 新环境可重复建库能力未验收 | 单独建立受控空库/容器验收，不在本轮扩大范围 | 否 |
| P1B-DI-P2-001 | P2 | Maven/测试会写入既有 target 与 flattened POM 产物 | 构建日志出现 target/flatten 处理 | 工作区可能出现构建产物变化 | 后续统一采用明确的构建产物隔离策略，由用户自行执行 Git 检查 | 否 |
| P1B-DI-P2-002 | P2 | Redis、调度器、Broker、QMT、CTP、LIVE 未连接 | 本轮明确隔离 | 不影响本轮 MyBatis 验收，但不能据此宣称全链路可运行 | 保持外部系统禁用，另行审批 | 否 |
