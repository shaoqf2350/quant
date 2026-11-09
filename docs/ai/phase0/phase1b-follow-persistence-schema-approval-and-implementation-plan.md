# QuantPF Phase 1B Follow Persistence Schema Approval and Implementation Plan

- 日期：2026-10-07
- 阶段：`PHASE1B-FOLLOW-PERSISTENCE-SCHEMA-APPROVAL-AND-IMPLEMENTATION-PLAN`
- 最终主控判定：`PASS`
- 性质：只读审计、方案修正、非执行 migration proposal、下一阶段白名单
- `PHASE1B-FOLLOW-PERSISTENCE-IMPLEMENTATION`：`NOT STARTED`
- `PHASE1C-PAPER-FRONTEND-IMPLEMENTATION`：`NOT ALLOWED`

## 1. 最终范围和安全边界

本阶段仅允许写入 `docs/ai/phase0/` 和 `docs/ai/handoff/` 文档文件。本阶段未修改 Java、XML、YAML、SQL、POM、Vue、TS 或正式契约文件；未执行 Git、Flyway、DDL、INSERT、UPDATE、DELETE、DROP、USE、数据库迁移；未连接 Redis、Broker、SnailJob、XXL-JOB、QMT、CTP、LIVE 或生产系统。

数据库只读审计范围：`127.0.0.1:3306/quantpf`，使用 information_schema 和只读 COUNT/metadata 查询。

## 2. 实际仓库和数据库事实

- `QuantPaperFollowController` 当前仍以 `AtomicBoolean` 保存进程内状态；start/stop 无 request body、无 account、无 strategy、无 session、无幂等键。
- 当前正式 Flyway 最高版本为 `V1_8_0`。
- MySQL 实测版本为 `8.0.20`。
- 当前数据库没有名称包含 `follow` 或 `session` 的专用表。
- `quantpf_audit_record` 和 `quantpf_job_execution_audit` 只可作为审计事实，不能作为 Follow session 状态表。
- `quantpf_trading_account` 可作为 Paper account 外键目标；`quantpf_strategy` 可作为 strategy 外键目标。
- OpenAPI 当前登记 follow start/stop，但没有正式 request/response 字段；当前 Controller 实际返回 HTTP 200，而 OpenAPI 当前登记 202，下一实施阶段必须统一为同步 HTTP 200，或者另行批准异步语义。本方案选择同步 200。

## 3. 最终领域模型

### 3.1 v1 状态

本阶段最终选择只保留两个持久化状态：

| 状态 | 含义 | active slot |
|---|---|---:|
| `RUNNING` | Paper Follow session 正在运行 | 占用 |
| `STOPPED` | Paper Follow session 已停止 | 释放 |

不在 v1 引入 `FAILED`、`STARTING` 或 `STOPPING`。当前没有异步 worker、Broker 或调度器，FAILED 生命周期没有真实调用方；引入未落地的 FAILED 会造成无法验证的状态和审计路径。未来若需要运行失败状态，必须单独扩展状态机、失败写入服务、事件/审计语义和测试。

允许：

```text
不存在 --start--> RUNNING
RUNNING --stop--> STOPPED
STOPPED --new start request--> 新 RUNNING session
```

禁止：

- STOPPED 直接恢复为原 session 的 RUNNING；
- STOPPED session 再 stop 产生第二次状态变更；
- 一个 stop 请求影响其他 session；
- 任何数据库失败后回退 AtomicBoolean。

### 3.2 策略准入

start 只接受：

- `mode=PAPER`；
- `quantpf_trading_account.account_type='PAPER'` 且账户可用；
- `quantpf_strategy.status='ACTIVE'`。

`DRAFT` 和 `DISABLED` strategy 不允许启动，返回受控 409/业务错误。该规则必须在服务层校验，并由外键和数据库约束共同保护引用完整性。

## 4. 推荐专用表和字段

推荐表名：

```text
quantpf_paper_follow_session
```

字段建议：

| 字段 | 类型建议 | 必填 | 语义 |
|---|---|---:|---|
| `id` | BIGINT UNSIGNED | 是 | 主键 |
| `session_id` | VARCHAR(128) | 是 | 稳定外部会话 ID，唯一 |
| `account_id` | BIGINT UNSIGNED | 是 | Paper account 外键 |
| `strategy_id` | BIGINT UNSIGNED | 是 | ACTIVE strategy 外键 |
| `mode` | VARCHAR(16) | 是 | 只允许 PAPER |
| `status` | VARCHAR(16) | 是 | 只允许 RUNNING/STOPPED |
| `start_request_id` | VARCHAR(128) | 是 | canonical start 幂等键，唯一 |
| `stop_request_id` | VARCHAR(128) | 否 | STOPPED 后必填；唯一且可为空 |
| `started_by` | VARCHAR(128) | 否 | 操作人，缺省使用受控主体 `PAPER_API` |
| `stopped_by` | VARCHAR(128) | 否 | 停止操作人 |
| `started_at` | DATETIME(6) | 是 | 启动时间 |
| `stopped_at` | DATETIME(6) | 否 | RUNNING 必须为空，STOPPED 必须非空 |
| `stop_reason` | VARCHAR(512) | 否 | 停止原因 |
| `revision` | BIGINT UNSIGNED | 是 | 行锁下的审计递增计数，默认 0 |
| `created_at` | DATETIME(6) | 是 | 创建时间 |
| `updated_at` | DATETIME(6) | 是 | 更新时间 |
| `active_slot` | VARCHAR(256) STORED GENERATED | 否 | RUNNING 时生成的活动唯一槽位 |

v1 不添加自由 JSON metadata，不添加 last_error 字段，不添加未使用的 FAILED 语义。

必须验证的约束：

- `session_id` UNIQUE；
- `start_request_id` UNIQUE；
- `stop_request_id` UNIQUE nullable；
- account/strategy 外键；
- mode CHECK 为 PAPER；
- status CHECK 为 RUNNING/STOPPED；
- `status='RUNNING'` 时 `stopped_at IS NULL AND stop_request_id IS NULL`；
- `status='STOPPED'` 时 `stopped_at IS NOT NULL AND stop_request_id IS NOT NULL`；
- `started_at <= stopped_at OR stopped_at IS NULL`；
- `active_slot` 仅在 RUNNING 时生成。

## 5. MySQL 并发唯一性

推荐方案：stored generated `active_slot` + unique index。逻辑表达式为：

```text
CASE WHEN status='RUNNING'
     THEN CONCAT(account_id, ':', strategy_id, ':', mode)
     ELSE NULL
END
```

唯一性维度是 `account_id + strategy_id + mode`。STOPPED 行的 generated value 为 NULL，因此历史 session 可以保留。

不采用：

- MySQL 不支持的 `UNIQUE ... WHERE status='RUNNING'` partial unique index；
- 普通 `(account_id,strategy_id,mode)` 唯一键，因为它无法保留历史 STOPPED session；
- 依赖内存锁或单节点 AtomicBoolean 的方案。

实施阶段的默认并发语义：

- 两个并发 start 最多一个 RUNNING；
- 唯一冲突后重新读取事实；同 start key 同参数返回幂等结果，不同 key 返回 `QPF-FOLLOW-001`；
- stop 与新 start 并发时不等待：如果 stop 尚未提交且 active_slot 仍被占用，新 start 返回 `QPF-FOLLOW-001`；stop 提交释放 slot 后，后续新 start 才能成功；
- 新 start 不得覆盖旧 session；
- 该不等待语义必须由并发集成测试固定下来。

实现前必须在 MySQL 8.0.20 disposable schema 预验证：generated expression、索引长度、CHECK 执行效果、跨列 CHECK、外键顺序、唯一冲突返回行为。DB CHECK 不能替代服务层状态机。

## 6. 幂等和 API 规范

v1 选择 `Idempotency-Key` 请求头作为唯一 canonical 幂等键，持久化到 `start_request_id`/`stop_request_id`。不同时接受 body `requestId` 和 header 作为两个独立事实。

正式 API：

```text
POST /api/quant/paper/follow-start
Header: Idempotency-Key (required)
Body: accountId, strategyId, mode=PAPER

POST /api/quant/paper/follow-stop
Header: Idempotency-Key (required)
Body: sessionId, reason
```

同步成功响应统一为 HTTP 200，响应至少包含：sessionId、accountId、strategyId、mode、status、startedAt、stoppedAt、idempotent。

语义：

| 场景 | 结果 |
|---|---|
| 同 start key + 同参数 | 原 RUNNING session，`idempotent=true` |
| 同 start key + 不同参数 | `QPF-IDEMP-001`，HTTP 409 |
| 不同 start key + 同维度已有 RUNNING | `QPF-FOLLOW-001`，HTTP 409 |
| 同 stop key + 同 session | 原 STOPPED session，`idempotent=true` |
| 同 stop key + 不同 session | `QPF-IDEMP-001`，HTTP 409 |
| 不同 stop key + 已 STOPPED | 返回 STOPPED no-op，不重复更新 |
| session 不存在 | `QPF-FOLLOW-002`，HTTP 404 |
| 缺少 Idempotency-Key | `QPF-FOLLOW-004`，HTTP 400 |
| 数据库异常 | 明确 5xx/事务错误，不返回成功，不回退内存 |

首次成功 start/stop 各写一条 Audit。幂等重放不重复写状态，也不重复写成功 Audit；如需记录 replay，必须另行批准审计动作，不得影响事务语义。

## 7. 事务实现落点

当前 `AuditRecordMapper` 只有只读分页方法。下一阶段必须显式补齐 Audit 写入能力：

- AuditRecordMapper insert；
- AuditRecordMapper.xml insert；
- AuditRecordRepository append/save；
- AuditRecordRepositoryAdapter append/save；
- Follow session mapper 与 Audit mapper 必须使用同一 MyBatis `SqlSession`/连接；
- Follow 状态变更和 Audit insert 必须同一事务提交；
- 任一失败整体 rollback；
- 禁止先提交 session 再独立写 Audit；
- 禁止数据库失败后返回成功或回退 AtomicBoolean。

start 事务：校验 PAPER account 和 ACTIVE strategy -> 按 start key 查询 -> 查询 RUNNING -> 插入 session -> 写 start Audit -> commit。

stop 事务：按 stop key 查询 -> `SELECT ... FOR UPDATE` 锁 session -> 校验 session -> RUNNING 更新为 STOPPED、写 stop key/时间/原因、revision+1 -> 写 stop Audit -> commit。

revision v1 只作为行锁下的审计递增计数，不采用乐观 CAS；行锁是并发控制机制。未来如采用 CAS，必须单独变更计划和测试。

## 8. 契约、错误码、事件和菜单

下一实施阶段需要另行获批修改 OpenAPI：

- 将 follow start/stop 的成功响应从当前登记的 202 对齐为同步 200；
- 增加 `Idempotency-Key` required header；
- 增加 start/stop request 和 session response schema；
- 增加过滤/错误响应定义。

错误码：

- 复用 `QPF-IDEMP-001`；
- 建议新增 `QPF-FOLLOW-001` active conflict、`QPF-FOLLOW-002` not found、`QPF-FOLLOW-003` invalid transition、`QPF-FOLLOW-004` missing idempotency key；
- strategy not ACTIVE 可复用 risk/business conflict，或另行增加错误码，实施阶段必须固定一个 canonical code。

事件：本阶段不新增 Follow event schema，不引入 outbox，不升级已有 order/execution/ledger/risk v1。未来异步事件必须单独设计。

菜单：本阶段不修改菜单事实源；Phase 1C 再单独审批 Follow 页面和权限。

## 9. Flyway 非执行提案

建议下一迁移版本：

```text
V1_9_0__paper_follow_session.sql
```

仅建议创建专用表、约束、索引和外键；无数据回填，无 legacy SQL 搬迁。正式 migration forward-only，不使用生产自动 DROP TABLE 回滚；生产回滚采用应用版本回退、备份恢复或补偿 migration。

执行前需要：人工批准 schema、在 disposable schema 预验证 MySQL 8.0.20 行为、确认备份和窗口、由 Flyway 统一执行。不能手工建表后声称完成迁移。

## 10. 下一阶段白名单

下一阶段可申请的文件类别：

- Follow Row/Mapper/XML/Repository/Adapter/Application Service；
- Follow request/response DTO 和 Controller；
- Runtime configuration；
- AuditRecordMapper insert、XML insert、Repository append、Adapter append；
- 同一 SqlSession 的 Follow/Audit transaction adapter；
- `V1_9_0__paper_follow_session.sql`；
- Follow API、MySQL 事实、并发、幂等、rollback、Spring MockMvc 测试；
- OpenAPI 和错误码文件（必须在实施阶段单独批准）。

本阶段不创建上述代码和 SQL 文件。

## 11. 复核结论

DeepSeek inventory 与 GLM independent review 的主要 P1 发现已由主控修正：

- v1 移除未落地 FAILED 状态；
- strategy 只允许 ACTIVE；
- 明确跨列 CHECK 必须 disposable schema 实测；
- 明确 stop_request_id 状态不变量；
- 明确 revision 只作行锁下递增计数；
- 明确 strategy 测试夹具使用随机前缀 JDBC 直插，不新增无关 Mapper；
- 明确 stop+start 不等待、冲突返回 409；
- 明确 Audit insert 文件和同 SqlSession 事务适配已进入下一阶段白名单；
- 明确 Idempotency-Key 为唯一 canonical 键；
- 明确 OpenAPI 202/200 对齐为下一实施任务；
- 明确历史 STOPPED session 的保留/归档应作为后续运维任务，不阻塞 v1。

## 12. 阶段验收标准和结论

本计划阶段满足：schema、状态机、MySQL 并发、幂等、事务、契约影响、迁移边界、白名单和人工审批点均已明确；未修改业务代码、正式 SQL 或正式契约；未执行 DDL/Flyway。

```text
PHASE1B-FOLLOW-PERSISTENCE-SCHEMA-APPROVAL-AND-IMPLEMENTATION-PLAN: PASS
PHASE1B-FOLLOW-PERSISTENCE-IMPLEMENTATION: NOT STARTED
PHASE1C-PAPER-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```
