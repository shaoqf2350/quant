# QuantPF Follow Persistence Migration Proposal

- 日期：2026-10-07
- 状态：`NON-EXECUTABLE`
- `NO DDL EXECUTED`
- `NO FLYWAY EXECUTED`
- `HUMAN APPROVAL REQUIRED`

## 1. 建议版本和目标

建议后续迁移文件：

```text
V1_9_0__paper_follow_session.sql
```

目标是新增 `quantpf_paper_follow_session` 作为 Paper Follow 的唯一持久化事实源。当前没有创建此 SQL 文件，也没有执行建表。

## 2. 建议模型

状态只包含 `RUNNING` 和 `STOPPED`。v1 不实现 FAILED/STARTING/STOPPING，因为当前没有异步运行时、Broker 或调度器来产生这些状态。

建议字段：id、session_id、account_id、strategy_id、mode、status、start_request_id、stop_request_id、started_by、stopped_by、started_at、stopped_at、stop_reason、revision、created_at、updated_at、active_slot。

`mode` 只允许 PAPER；strategy 只允许引用 ACTIVE strategy；account 必须是 PAPER account。

## 3. 约束和索引摘要

- session_id UNIQUE；
- start_request_id UNIQUE；
- nullable stop_request_id UNIQUE；
- account_id/strategy_id 外键；
- `status='RUNNING'` 时 stopped_at 和 stop_request_id 必须 NULL；
- `status='STOPPED'` 时 stopped_at 和 stop_request_id 必须非 NULL；
- started_at <= stopped_at 或 stopped_at NULL；
- stored generated active_slot：RUNNING 时为 account_id:strategy_id:mode，否则 NULL；
- active_slot UNIQUE，保证同维度最多一个 RUNNING；
- account_id、strategy_id、status、updated_at 查询索引。

禁止使用不受 MySQL 8.0.20 支持的 partial unique index。

## 4. 事务和幂等提案

正式入口使用 `Idempotency-Key` header，持久化为 start_request_id/stop_request_id；不同时接受另一个 body requestId 作为第二事实。

start：校验 PAPER account 和 ACTIVE strategy，查询幂等键和 RUNNING，插入 session，写 Audit，单事务提交。

stop：查询 stop 幂等键，`SELECT ... FOR UPDATE` 锁 session，RUNNING 更新 STOPPED，写 stop Audit，单事务提交。

状态和 Audit 必须通过同一 MyBatis SqlSession/连接完成。当前 Audit mapper 只有分页查询，下一阶段必须补充 Audit insert mapper/XML/repository/adapter。任一失败整体 rollback，不得回退 AtomicBoolean。

并发 stop+start v1 不等待：旧 stop 未提交时，新 start 遇 active_slot 冲突返回 QPF-FOLLOW-001；stop 提交后下一次 start 才允许成功。

## 5. API 和错误码提案

保持路由：

```text
POST /api/quant/paper/follow-start
POST /api/quant/paper/follow-stop
```

下一阶段统一为同步 HTTP 200，并增加 required `Idempotency-Key` header、start/stop body 和 session response。

建议新增：

```text
QPF-FOLLOW-001 active session conflict, HTTP 409
QPF-FOLLOW-002 session not found, HTTP 404
QPF-FOLLOW-003 invalid state transition, HTTP 409
QPF-FOLLOW-004 missing idempotency key, HTTP 400
```

复用 `QPF-IDEMP-001` 表示同 key 不同参数。

## 6. Audit、事件和菜单

首次成功 start/stop 各写一条 Audit；幂等重放不重复写成功 Audit。当前不新增 Follow event schema、不引入 outbox、不修改菜单事实源。

## 7. 运维和回滚边界

无数据回填，无 legacy/reference 搬迁。正式 Flyway forward-only，不采用生产自动 DROP TABLE 回滚。执行前需人工批准、disposable schema 验证 MySQL 8.0.20、备份确认和变更窗口。

## 8. 明确状态

本文件绝不是 SQL，不能复制执行：

```text
NON-EXECUTABLE
NO DDL EXECUTED
NO FLYWAY EXECUTED
HUMAN APPROVAL REQUIRED
```
