# QuantPF Follow Persistence Schema Approval and Implementation Plan - Risk Register

- 日期：2026-10-07
- 最终主控判定：`PASS`（计划阶段）
- 实施阶段：`NOT STARTED`

| ID | 等级 | 风险 | 证据 | 最终处理 | 是否阻塞实施 |
|---|---|---|---|---|---|
| FOL-SCHEMA-P0-001 | P0 | Follow 当前没有持久化事实源 | Controller 使用 AtomicBoolean；数据库无 follow/session 表 | 下一阶段创建专用 `quantpf_paper_follow_session` 并切换 Spring 持久化路径 | 是 |
| FOL-SCHEMA-P0-002 | P0 | 并发 start 可能重复创建 RUNNING | 当前没有唯一 slot 和事务锁 | generated active_slot + unique index；并发测试固定最多一条 RUNNING | 是 |
| FOL-SCHEMA-P1-001 | P1 | v1 若保留 FAILED 但没有失败写入流程，会产生悬空状态 | 当前没有异步 worker/运行失败调用方 | v1 移除 FAILED，仅保留 RUNNING/STOPPED；未来另行设计 | 否，已解决 |
| FOL-SCHEMA-P1-002 | P1 | strategy 准入状态未定义 | strategy 表有 DRAFT/ACTIVE/DISABLED | 明确只允许 ACTIVE；服务层校验，测试使用 ACTIVE 夹具 | 否，已解决 |
| FOL-SCHEMA-P1-003 | P1 | 跨列 CHECK 可能未在 MySQL 8.0.20 验证 | stopped_at/stop_request_id 与 status 联动 | 下一阶段 disposable schema 必须实测全部跨列 CHECK，并保留服务层校验 | 否，实施前门禁 |
| FOL-SCHEMA-P1-004 | P1 | stop_request_id 状态不变量缺失 | nullable stop key 需要与状态联动 | RUNNING 必须 NULL，STOPPED 必须非 NULL；proposal 已明确 | 否，已解决 |
| FOL-SCHEMA-P1-005 | P1 | revision 语义不清 | 方案同时提到行锁和乐观版本 | v1 只使用 `SELECT FOR UPDATE`；revision 仅为行锁下递增审计计数，不做 CAS | 否，已解决 |
| FOL-SCHEMA-P1-006 | P1 | Follow 测试需要 strategy/account 外键夹具 | 当前没有 Follow fixture | 下一阶段通过 JDBC 随机前缀插入 PAPER account 和 ACTIVE strategy；不新增无关 Mapper | 否，实施前门禁 |
| FOL-SCHEMA-P1-007 | P1 | stop 与新 start 并发语义未固定会导致实现差异 | 可能等待，也可能立即冲突 | v1 明确不等待；旧 stop 未提交时新 start 返回 QPF-FOLLOW-001 | 否，已解决 |
| FOL-SCHEMA-P1-008 | P1 | Audit 只有读 Mapper，无法实现同事务写入 | 当前 AuditRecordMapper 仅 selectPage | 下一阶段白名单显式允许 Audit insert Mapper/XML/Repository/Adapter 和同 SqlSession transaction adapter | 否，已解决 |
| FOL-SCHEMA-P1-009 | P1 | 幂等键存在双事实风险 | body requestId 与 header Idempotency-Key 尚未统一 | v1 选择 Idempotency-Key header 为唯一 canonical 键，持久化为 start/stop_request_id | 否，已解决 |
| FOL-SCHEMA-P2-001 | P2 | OpenAPI 当前 202 与同步 Controller 200 不一致 | inventory 已发现 | 下一阶段统一为同步 HTTP 200 并更新契约 | 否，实施前门禁 |
| FOL-SCHEMA-P2-002 | P2 | 历史 STOPPED session 无归档策略 | session 会累积 | 作为后续运维/数据保留任务，不阻塞 v1 | 否 |
| FOL-SCHEMA-P2-003 | P2 | 当前 20 张迁移表与 schema 21 张 QuantPF 表的计数差异 | 额外表可能是 flyway_schema_history | 记录为 schema 审计说明；实施前只读复核，不影响设计 | 否 |
| FOL-SCHEMA-P2-004 | P2 | Follow event/outbox 未设计 | 当前没有异步事件需求 | 本阶段明确不引入；未来单独立项 | 否 |
| FOL-SCHEMA-P2-005 | P2 | 独立 review 初版为 PASS_WITH_CHANGES | GLM review 提出 P1 项 | 主控已逐项处理并在 canonical plan §11 记录；不冒充 GLM 二次签字 | 否 |
