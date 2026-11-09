# QuantPF Phase 1B Follow Persistence Final Risk Register

- 日期：2026-10-07
- 阶段结果：PASS

| ID | 等级 | 风险 | 证据 | 处理 | 是否阻塞 |
|---|---|---|---|---|---|
| FOL-IMP-P0-001 | P0 | Follow 无数据库事实源 | V1.9.0 与实际表存在 | 专用 Follow 表已由 Flyway 建立并验证 | 否，已关闭 |
| FOL-IMP-P0-002 | P0 | 并发 start 重复 RUNNING | generated active_slot 唯一约束、并发测试 | DB 事实测试通过 | 否，已关闭 |
| FOL-IMP-P0-003 | P0 | Follow 与 Audit 部分提交 | Audit failure rollback 测试 | 同一 connection 显式 rollback 通过 | 否，已关闭 |
| FOL-IMP-P0-004 | P0 | 数据库失败回退 AtomicBoolean | 生产 Follow 路径静态复核 | 无内存 fallback，定向测试通过 | 否，已关闭 |
| FOL-IMP-P1-001 | P1 | 普通异常误判 duplicate recovery | 前序问题及修正 | 仅 duplicate key recovery；全量回归通过 | 否，已关闭 |
| FOL-IMP-P1-002 | P1 | NOWAIT 锁冲突错误映射不稳定 | 前序定向测试暴露消息断言不一致 | 统一包含 QPF-FOLLOW-001 的受控错误消息；定向/全量通过 | 否，已关闭 |
| FOL-IMP-P1-003 | P1 | STOPPED 历史增长 | 表保留历史 session | 后续设计 retention/archival | 否 |
| FOL-IMP-P2-001 | P2 | 未使用 Testcontainers | 当前为本地 quantpf 事实测试 | 后续增加 disposable schema | 否 |
| FOL-IMP-P2-002 | P2 | Mockito/Byte Buddy warning | 全量测试日志 | 不改 POM，记录为工具链改进项 | 否 |
