# PHASE1B Remaining API Persistence and Paper MVP Closure Plan

- 计划生成日期：2026-10-07
- 用户提示阶段日期：2026-10-06
- 当前阶段：`PHASE1B-REMAINING-API-PERSISTENCE-AND-PAPER-MVP-CLOSURE-PLAN`
- 执行模式：只读审计和实施规划
- 本轮业务代码修改：无
- Git：未执行
- 数据库：仅对本地 `127.0.0.1:3306/quantpf` 做只读 metadata/COUNT 核对

## 1. 计划边界

本轮只审计并规划以下剩余 Paper API：

- `GET /api/quant/audit/page`
- `GET /api/quant/job/execution/page`
- `POST /api/quant/paper/follow-start`
- `POST /api/quant/paper/follow-stop`

本轮不修改 Java、XML、YAML、SQL、POM、Vue、TS 或正式契约文件，不执行 Flyway/DDL，不启动 Redis、Broker、SnailJob、XXL-JOB、QMT、CTP、LIVE，不执行 Git。

## 2. 实际代码事实

### Audit

当前：

```text
QuantPaperAuditController -> R.ok(List.of())
```

结论：

```text
正式数据库路径：不存在
当前实现：固定空列表骨架
```

应改为：

```text
Audit Controller
  -> Persistent Audit Application Service
  -> Audit Repository
  -> quantpf_audit_record
```

### Job execution

当前：

```text
QuantPaperJobExecutionController -> R.ok(List.of())
```

结论：

```text
正式数据库路径：不存在
当前实现：固定空列表骨架
```

应改为只读查询：

```text
Job Execution Controller
  -> Persistent Job Execution Application Service
  -> Job Execution Repository
  -> quantpf_job_execution_audit
```

本阶段不启动任何调度器，也不连接 SnailJob 或 XXL-JOB。

### Follow

当前：

```text
QuantPaperFollowController -> AtomicBoolean running
```

结论：

```text
正式数据库事实源：当前 schema 中未发现 follow session/state 表
当前实现：内存 AtomicBoolean
```

不得把 `quantpf_audit_record` 或 `quantpf_job_execution_audit` 强行当作 follow 状态表。需要单独的 follow 持久化事实源或正式 migration proposal。

## 3. 数据库事实源审计

本地 schema 只读核对结果：

### 3.1 Audit

表：

```text
quantpf_audit_record
```

已确认字段：

- id
- request_id
- actor_id
- action
- aggregate_type
- aggregate_id
- result
- detail JSON
- occurred_at

已确认索引：

- PRIMARY(id)
- idx_quantpf_audit_request(request_id)
- idx_quantpf_audit_aggregate(aggregate_type, aggregate_id)
- idx_quantpf_audit_occurred_at(occurred_at)

当前行数核对：

```text
AUDIT_ROWS=0
```

结论：Audit 可以直接复用现有表，不需要新的 Flyway/SQL 变更。

### 3.2 Job execution

表：

```text
quantpf_job_execution_audit
```

已确认字段：

- id
- job_name
- execution_id
- scheduler_type
- status
- request_id
- started_at
- finished_at
- error_code
- error_message
- created_at

已确认索引：

- PRIMARY(id)
- uk_quantpf_job_execution(execution_id)

当前行数核对：

```text
JOB_ROWS=0
```

结论：Job execution 查询可以直接复用现有表，不需要启动调度器，也不需要新的 Flyway/SQL 变更。

### 3.3 Follow

在当前已确认表体系中未发现专用 follow state/session 表。已有表不能安全表达：

- follow session id；
- follow mode；
- strategy/account 绑定；
- started_at/stopped_at；
- running 状态的唯一事实；
- start/stop 幂等键；
- 操作人和停止原因的完整生命周期。

结论：

```text
Follow persistence：NOT SUPPORTED BY CURRENT SCHEMA
```

本轮不修改 Flyway/SQL，不执行 DDL。必须生成后续 migration proposal，或由后续人工批准明确复用哪一个正式表；在此之前不得宣称 Follow 已持久化。

## 4. 推荐实现方案

### 4.1 Audit

新增最小只读链路：

```text
QuantPaperAuditController
  -> PersistentAuditApplicationService
  -> AuditRepository
  -> AuditMapper
  -> quantpf_audit_record
```

推荐查询字段：

- id；
- request_id；
- actor_id；
- action；
- aggregate_type；
- aggregate_id；
- result；
- detail；
- occurred_at。

推荐分页：

```text
offset = (max(pageNum,1)-1) * clamp(pageSize,1,100)
ORDER BY occurred_at DESC, id DESC
LIMIT offset, limit
```

数据库异常必须向上抛出并返回明确错误，禁止返回 `List.of()` 作为 fallback。

### 4.2 Job execution

新增最小只读链路：

```text
QuantPaperJobExecutionController
  -> PersistentJobExecutionApplicationService
  -> JobExecutionRepository
  -> JobExecutionMapper
  -> quantpf_job_execution_audit
```

推荐分页：

```text
ORDER BY created_at DESC, id DESC
LIMIT offset, limit
```

只查询审计表，不注册、不触发、不初始化任何调度器。

`execution_id` 已有唯一约束，后续如果增加写入 API，必须以该约束作为幂等边界；本计划阶段只实现安全查询路径。

### 4.3 Follow

当前推荐：

```text
本轮不实施正式 Follow persistence
```

应生成后续 proposal，至少包含：

- 建议表名，例如 `quantpf_paper_follow_session`；
- session_id；
- account_id；
- strategy_id 或 strategy_code；
- mode；
- status；
- started_at；
- stopped_at；
- start/stop idempotency_key；
- started_by/stopped_by；
- reason；
- 唯一约束；
- 回滚方案；
- Flyway 版本建议。

未经人工批准，不得修改 SQL/Flyway 或执行 DDL。

## 5. 内存 fallback 结论

当前发现：

| API | 当前问题 | 正式路径结论 |
|---|---|---|
| Account | 已有持久化 Spring 路径，standalone compatibility 仍存在 | 需保持 Spring 路径无 fallback |
| Order | 已有持久化 Spring 路径，standalone compatibility 仍存在 | 需保持 Spring 路径无 fallback |
| Risk | 已有持久化 Spring 路径 | 已完成主要风险路径切换 |
| Follow | AtomicBoolean | 仍存在正式内存状态风险，必须标记 P1 |
| Audit | 固定 List.of() | 必须切换数据库查询 |
| Job execution | 固定 List.of() | 必须切换数据库查询 |

## 6. 下一阶段实施顺序

1. 实现 Audit Row/Mapper/XML/Repository/Application Service；
2. 实现 Job execution Row/Mapper/XML/Repository/Application Service；
3. 将两个 Controller 接入持久化服务；
4. 增加本地数据库 + MockMvc 查询测试；
5. 增加数据库失败不 fallback 测试；
6. 复核 Follow schema；
7. 生成 Follow migration proposal；
8. 不在无 schema 情况下实现假持久化 Follow；
9. 执行全量旧测试；
10. 由 GPT-5.6 Sol 最终判定 Paper MVP 是否达到基本可交付门槛。

## 7. PASS/PARTIAL 门禁

### 可以判定本阶段 PASS 的最低条件

1. Audit API 从 `quantpf_audit_record` 查询；
2. Job execution API 从 `quantpf_job_execution_audit` 查询；
3. 两者不再返回固定空列表；
4. 数据库异常不 fallback 到内存/空列表；
5. Follow 已有正式 schema 并完成持久化，或已由人工批准接受单独 Follow migration proposal 并明确不阻塞当前 Paper MVP；
6. Account/Order/Risk 旧路径继续通过；
7. 订单生命周期旧测试继续通过；
8. 测试清理使用随机前缀；
9. 未执行 Git、Flyway、DDL、外部系统连接；
10. 生成完整交接包。

由于当前 Follow schema 不存在，若没有新的人工批准，本阶段默认只能：

```text
PARTIAL
PHASE1C: NOT ALLOWED
```
