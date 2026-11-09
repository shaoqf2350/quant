# Remaining API Persistence and Paper MVP Closure Risk Register

| ID | 等级 | 风险 | 证据 | 影响 | 建议 | 是否阻塞 |
|---|---|---|---|---|---|---|
| P1B-RAP-P0-001 | P0 | Follow 没有当前数据库事实源 | 只读核对现有 schema 未发现 follow session/state 表；当前代码使用 AtomicBoolean | 无法安全宣称 Follow 持久化；可能丢失运行状态 | 生成单独 migration proposal，等待人工批准；禁止伪造持久化 | 是 |
| P1B-RAP-P1-001 | P1 | Audit API 固定返回空列表 | `QuantPaperAuditController` 当前直接返回 `List.of()` | 审计事实不可查询 | 使用 `quantpf_audit_record` 实现只读 Mapper/Repository/Application Service | 是 |
| P1B-RAP-P1-002 | P1 | Job execution API 固定返回空列表 | `QuantPaperJobExecutionController` 当前直接返回 `List.of()` | 任务执行审计不可查询 | 使用 `quantpf_job_execution_audit` 实现只读查询链路 | 是 |
| P1B-RAP-P1-003 | P1 | Follow 正式路径使用 AtomicBoolean | `QuantPaperFollowController` 当前使用 AtomicBoolean | 重启丢状态，多实例不一致 | 只有 schema/迁移批准后实施；当前保持 PARTIAL | 是 |
| P1B-RAP-P1-004 | P1 | Follow/Audit/Job API 与数据库事实不统一 | Account/Order/Risk 已持久化，剩余 API 仍是骨架 | Paper MVP 交付边界不完整 | 先完成 Audit/Job，单独处理 Follow | 是 |
| P1B-RAP-P2-001 | P2 | Audit/Job 当前数据库行数为 0 | 本地只读 COUNT：AUDIT_ROWS=0，JOB_ROWS=0 | API 测试需要随机测试数据或空结果基线 | 集成测试中插入随机前缀测试行并清理 | 否 |
| P1B-RAP-P2-002 | P2 | 不能执行 Git 状态检查 | 用户/阶段禁止 Git | 构建产物状态未审计 | 用户后续人工检查 | 否 |
| P1B-RAP-P2-003 | P2 | 未使用独立测试 schema | 当前只能使用本地 quantpf + 随机前缀 | 并行测试隔离性有限 | 后续 Testcontainers/专用 schema | 否 |
