# QuantPF Phase 1B Follow Persistence Schema Plan Handoff

- 交接日期：2026-10-07
- 唯一交接入口：本文件
- 当前阶段：`PHASE1B-FOLLOW-PERSISTENCE-SCHEMA-APPROVAL-AND-IMPLEMENTATION-PLAN`
- 计划阶段结果：`PASS`
- 下一阶段：`PHASE1B-FOLLOW-PERSISTENCE-IMPLEMENTATION-AND-CONCURRENCY-CLOSURE`
- Follow 实际持久化：`NOT STARTED`
- Phase 1C：`NOT ALLOWED`

## 1. 最终复核结论

DeepSeek-v4-pro 已完成 inventory；GLM-5.3 independent review 初判为 `PASS_WITH_CHANGES` 并提出 8 个 P1。GPT-5.6 Sol 主控已逐项处理：

1. v1 移除无真实调用方的 FAILED 状态；
2. strategy 只允许 ACTIVE；
3. 跨列 CHECK 纳入 MySQL 8.0.20 disposable schema 必测项；
4. stop_request_id 状态不变量明确；
5. revision 固定为行锁下递增审计计数，不做 CAS；
6. 测试使用随机前缀 JDBC 夹具；
7. stop+start 不等待语义固定为 active conflict 409；
8. Audit insert Mapper/XML/Repository/Adapter 和同 SqlSession 事务适配进入下一阶段白名单。

因此计划阶段最终判定为 PASS；实现阶段仍未开始。

## 2. 实际事实

- MySQL：8.0.20；目标仅本地 `127.0.0.1:3306/quantpf`；
- V1_0_0 至 V1_8_0 业务迁移定义 20 张 QuantPF 表；实际 schema 还包含 Flyway 管理表，因此历史文档中的 21 张总表计数不等于业务表数量；
- Follow/session 专用表不存在；
- `QuantPaperFollowController` 仍为 AtomicBoolean；
- Audit/Job 只读闭环已通过；Follow 尚未持久化。

## 3. Canonical schema

```text
quantpf_paper_follow_session
```

v1 状态：

```text
RUNNING
STOPPED
```

v1 不实现 FAILED/STARTING/STOPPING。account 必须是 PAPER，strategy 必须是 ACTIVE，mode 必须是 PAPER。

活动唯一性：

```text
stored generated active_slot + unique index
account_id + strategy_id + mode
```

禁止使用 partial unique index，禁止复用 Audit/Job/Halt 表。

## 4. Canonical 幂等和 API

唯一幂等来源：

```text
Idempotency-Key request header
```

start body：accountId、strategyId、mode=PAPER；stop body：sessionId、reason。

同步成功 HTTP 状态统一为 200；当前 OpenAPI 的 202 必须在下一实施阶段修正。

## 5. Canonical 事务

```text
校验 -> 幂等查询 -> 行锁/active_slot 唯一性 -> session 变更 -> Audit insert -> commit
```

Follow session 和 Audit 必须使用同一个 MyBatis SqlSession/数据库事务。当前 Audit mapper 只有 selectPage，下一阶段必须补 Audit insert mapper/XML/repository/adapter；不得分离提交。

revision v1 只在 stop 的行锁事务中递增，不做乐观 CAS。

## 6. Canonical 并发语义

- 两个并发 start 最多一个 RUNNING；
- 唯一冲突后重新读事实，同 key 同参数幂等，不同 key active conflict；
- stop 未提交时并发新 start 不等待，返回 `QPF-FOLLOW-001`；
- stop 提交后下一次 start 才允许成功；
- 不允许旧 session 被新 session 覆盖。

## 7. 文件位置

```text
D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1b-follow-persistence-schema-approval-and-implementation-plan.md
D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1b-follow-persistence-migration-proposal.md
D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1b-follow-persistence-file-whitelist.json
D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1b-follow-persistence-risk-register.md
D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1b-follow-persistence-commands.log
D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1b-follow-persistence-manifest.json
D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\handoff\phase1b-follow-persistence-schema-plan-handoff.md
```

Migration proposal 必须保持：

```text
NON-EXECUTABLE
NO DDL EXECUTED
NO FLYWAY EXECUTED
HUMAN APPROVAL REQUIRED
```

## 8. 下一阶段顺序

1. 人工批准 schema、proposal、V1_9_0 和下一阶段白名单；
2. 在 disposable schema 验证 MySQL 8.0.20 generated column、唯一键、跨列 CHECK、外键和冲突行为；
3. 获批后创建并执行 V1_9_0；
4. 创建 Follow Row/Mapper/XML/Repository/Adapter/Application Service；
5. 同批补 Audit insert mapper/XML/repository/adapter；
6. 使用同一 SqlSession 实现 start/stop + Audit；
7. 更新 DTO、Controller、runtime wiring、OpenAPI 和错误码；
8. 编写事实、并发、幂等、rollback、MockMvc 和全量回归测试；
9. 独立审查并重新判定 Paper MVP/Phase 1C。

## 9. 下一阶段禁止

- 人工批准前创建或执行 V1_9_0；
- 任何手工 DDL；
- 保留 AtomicBoolean 作为正式事实源；
- 复用 Audit/Job 表；
- 启用 LIVE、QMT、CTP；
- 启动 SnailJob/XXL-JOB 双栈；
- 进入 Phase 1C。

## 10. 当前门禁

```text
PHASE1B-FOLLOW-PERSISTENCE-SCHEMA-APPROVAL-AND-IMPLEMENTATION-PLAN: PASS
PHASE1B-FOLLOW-PERSISTENCE-IMPLEMENTATION: NOT STARTED
PHASE1C-PAPER-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```
