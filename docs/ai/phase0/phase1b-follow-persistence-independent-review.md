# QuantPF Follow Persistence Schema Independent Review

- 审查日期：2026-10-07
- 阶段：`PHASE1B-FOLLOW-PERSISTENCE-SCHEMA-APPROVAL-AND-IMPLEMENTATION-PLAN`
- 审查角色：Follow Persistence Schema 独立审查负责人（只读审查，非最终架构主控）
- 审查对象：
  - `docs/ai/phase0/phase1b-follow-persistence-schema-approval-and-implementation-plan.md`（下称 Plan）
  - `docs/ai/phase0/phase1b-follow-persistence-migration-proposal.md`（下称 Proposal）
- 交叉参考：
  - `docs/ai/phase0/phase1b-follow-persistence-schema-inventory-draft.md`
  - `docs/ai/phase0/phase1b-remaining-api-persistence-paper-mvp-closure.md`
  - `db/migration/*.sql`（V1_0_0–V1_8_0）
  - `量化交易系统完整设计方案_V29.md`
- 审查方法：逐项对照 16 项重点检查，核对 migration SQL 事实，推演 start/stop/idempotency 并发路径与事务原子性。
- 本审查：未修改任何代码、SQL、POM、正式 YAML 或正式契约；仅生成本文件。

## 0. 结论

```text
PASS_WITH_CHANGES
```

理由：Plan/Proposal 的核心并发与原子性方向正确（generated stored column + unique index，非 partial unique index；start/stop 与 audit 同事务；无 AtomicBoolean 回退；幂等重放与冲突检测语义完整；Phase 1C 未放行；proposal 标记 NON-EXECUTABLE；人工批准点齐全）。未发现 P0 缺陷。存在若干 P1 规范空白与预验证缺口，均不要求重做核心设计，但在进入人工批准与实施前必须补齐。

```text
P0 问题：0
P1 问题：8
P2 问题：5
```

## 1. 16 项重点检查结果

| # | 检查项 | 结论 | 依据 |
|---|---|---|---|
| 1 | 是否错误使用 MySQL 不支持的 partial unique index | PASS | Plan §5.4/§5.1 明确禁止 `CREATE UNIQUE INDEX ... WHERE status='RUNNING'`；改用 `active_slot` STORED generated column + unique index，MySQL 8.0.20 支持 |
| 2 | 并发 start 是否可能产生两个 RUNNING session | PASS | `active_slot` 仅 RUNNING 时生成 `CONCAT(account_id,':',strategy_id,':',mode)`，非 RUNNING 为 NULL；unique index 拦截第二个 RUNNING；多 NULL 允许保留历史。InnoDB 唯一性检查即便对未提交行也会阻止重复插入 |
| 3 | stop 与 start 并发是否存在竞态 | PASS_WITH_OBS | 正确性安全（永不出现两个 RUNNING）；但 stop（旧 session）与新 start（同维度新 session）并发时，新 start 可能返回 `QPF-FOLLOW-001` 而非等待，可用性语义未预设默认值（见 P1-7） |
| 4 | request id 重放是否会重复创建 session | PASS | start 先按 `start_request_id`（UNIQUE）查历史；同键同参返回原 session idempotent replay；UNIQUE 约束为数据库层最后防线 |
| 5 | 相同幂等键不同参数是否被错误接受 | PASS | 同 `start_request_id` + 不同 account/strategy/mode → `QPF-IDEMP-001` 409；服务层比较同行列参数即可 |
| 6 | 重复 stop 是否具有明确语义 | PASS | 同 stop_request_id + 同 session → idempotent replay；不同 stop_request_id + 已 STOPPED → STOPPED no-op，不重复写状态 |
| 7 | 服务重启后状态是否可恢复 | PASS | RUNNING session 持久化在表；AtomicBoolean 重启丢失但不再是事实源；DB 为唯一真相 |
| 8 | 数据库失败是否会回退 AtomicBoolean | PASS | Plan §6.3 明确禁止"数据库失败后继续返回成功"与内存变量补偿；异常向上抛出 |
| 9 | Audit 是否可能先成功、状态更新失败 | PASS | 同一事务：状态 insert/update 失败 → 整事务 rollback，audit 不残留 |
| 10 | 状态更新成功、Audit 失败时如何处理 | PASS | 同一事务：audit insert 失败 → rollback session 状态变更，无 RUNNING 脏状态 |
| 11 | rollback 是否会留下 RUNNING 脏状态 | PASS | start insert 未提交即 rollback → 无 RUNNING 行，active_slot 立即释放 |
| 12 | session_id/account_id/strategy_id/mode 是否有正确唯一性 | PASS_WITH_OBS | `session_id` UNIQUE、`start_request_id` UNIQUE、`stop_request_id` UNIQUE(nullable)、`active_slot` UNIQUE 捕获 account+strategy+mode+RUNNING；account/strategy/mode 本身不应唯一，设计正确。但 `stop_request_id` 在 RUNNING 时是否必须 NULL 未被 CHECK 强制（见 P1-4） |
| 13 | 是否错误复用 Audit/Job execution 表 | PASS | 新建 `quantpf_paper_follow_session`；Proposal §9 与 Plan §2.3 明确禁止复用 Audit/Job/Halt/Order 表 |
| 14 | Flyway rollback 边界是否清晰 | PASS | Plan §9.3 / Proposal §7：forward-only、无生产 DROP TABLE 自动回滚、备份恢复或补偿 migration、无数据回填 |
| 15 | 是否错误放行 Phase 1C | PASS | Plan §12：`PHASE1C-PAPER-FRONTEND-IMPLEMENTATION: NOT ALLOWED`；Proposal §9 同 |
| 16 | 是否遗漏人工审批点 | PASS_WITH_OBS | Proposal §8 列 10 个批准点；但 strategy 状态准入、FAILED 流程、audit insert 白名单等未显式列入（见 P1-2/P1-1/P1-8） |

## 2. P0 问题

无。

## 3. P1 问题（进入批准/实施前必须补齐）

### P1-1 FAILED 状态转换流程未指定
- 现象：Plan §3.2 允许 `RUNNING --runtime failure--> FAILED`，但 §6 只定义 start/stop 两个事务流程，没有 `markFailed()` 流程、没有对应 audit action（§8.4 列出 START/STOP/IDEMPOTENT/REJECTED，无 `PAPER_FOLLOW_FAILED`），也未说明 FAILED 写入的事务边界与 active_slot 释放。
- 影响：Phase 1B 同步路径无 worker/broker，FAILED 在 v1 实际无来源；若保留 FAILED 却不规定其写入原子性，未来引入异步运行时后可能出现"状态置 FAILED 但 audit 未写"或"FAILED 未释放 active_slot"的不一致。
- 建议：二选一并写入 Plan——(a) v1 状态机只保留 RUNNING/STOPPED，FAILED 留到引入异步运行时时连同独立流程与事件一并新增；或 (b) 现即补 `markFailed()` 流程：同事务 `status=FAILED`、`stopped_at`/`last_error_*`、release active_slot（FAILED→active_slot NULL）、写 `PAPER_FOLLOW_FAILED` audit、revision+1、rollback 边界。

### P1-2 strategy 状态准入未定义
- 现象：Plan §6.1 步骤 1 要求"strategy 存在且可用"，但未定义"可用"对应 `quantpf_strategy.status` 的哪一档。`V1_3_0` 的 `ck_quantpf_strategy_status` 允许 `DRAFT/ACTIVE/DISABLED`。
- 影响：Follow 是否可绑定 DRAFT 策略未明确；DISABLED 是否必须拒绝未明确；与 strategy 版本唯一键 `uk(strategy_code, version)` 的关系未说明。
- 建议：在 Plan/Proposal 显式规定允许绑定的 strategy.status 集合（建议仅 `ACTIVE`），并列入 Proposal §8 人工批准点。

### P1-3 跨列 CHECK 预验证范围未明确
- 现象：Plan §4.3 引入非平凡跨列 CHECK（`stopped_at` 在 RUNNING 为 NULL、在 STOPPED/FAILED 非 NULL；`started_at <= stopped_at`）。Plan §9.2 与风险 FOL-SCHEMA-P1-006 要求"generated expression、CHECK、外键顺序、索引长度"在 disposable schema 预验证，但未把上述跨列 CHECK 列为必须实测项。
- 影响：MySQL 8.0.20 支持跨列比较 CHECK（8.0.16+ 强制执行），但 NULL 语义与 `<=` 组合在具体表达式上需实测；若表达式书写不当，约束可能不生效或建表失败。
- 建议：在 Plan §9.2 增加"跨列不变量 CHECK 表达式必须在 MySQL 8.0.20 disposable schema 实测生效"，并明确"DB CHECK 不能替代服务层状态机"（Plan §4.3 已有此句，保留）。

### P1-4 stop_request_id 可空性不变量缺失
- 现象：Proposal §2.1 `stop_request_id VARCHAR(128) NULL UNIQUE`；Plan §4.3 的 CHECK 只约束 `stopped_at`，未约束 `stop_request_id`。因此理论上可能出现 `status='RUNNING'` 且 `stop_request_id` 非空的自相矛盾行。
- 影响：幂等键语义与状态不一致；若 RUNNING 行已写入 stop_request_id，后续 stop 重放判断会被污染。
- 建议：增加 CHECK：`status='RUNNING'` 时 `stop_request_id IS NULL`；`status IN ('STOPPED','FAILED')` 时若 stop_request_id 为 canonical 停止键则 `stop_request_id IS NOT NULL`（若以 Idempotency-Key 为 canonical，则需在文档中明确二者关系）。

### P1-5 revision 字段用途未在流程中落地
- 现象：Plan §4.2 定义 `revision BIGINT UNSIGNED DEFAULT 0`，注释"用于乐观版本审计"；但 §6.1/§6.2 的 start/stop 流程仅使用 `SELECT ... FOR UPDATE` 行锁，未使用 revision 做 CAS（`WHERE id=? AND revision=?` + revision+1）。
- 影响：字段声明与流程不一致；实施者可能误以为已有乐观锁保护而忽略行锁，或反之。
- 建议：明确二选一——(a) stop/update 使用 `revision` CAS 并 `revision+1`，与行锁二选一或叠加；或 (b) v1 仅用行锁，移除 `revision` 或将其降级为纯审计计数。在 Plan §6 写明最终选择。

### P1-6 strategy 测试夹具路径未指定
- 现象：domain 现有 Mapper 不含 Strategy；Follow 事实/并发测试需要可绑定的 strategy 行（account_id/strategy_id 为 NOT NULL FK）。Plan §10 白名单列出 Follow Row/Mapper/Repository/Adapter/Service/Controller/Runtime/SQL/测试，但未说明 strategy FK 行如何准备。
- 影响：实施阶段测试可能在 strategy 插入上卡壳，或被迫越权新增 StrategyMapper（不在白名单）。
- 建议：在 Plan §10 或 Proposal 显式规定 Follow 测试通过 JDBC 直插 `quantpf_strategy`（参照既有 account/instrument 测试的 JDBC 夹具模式，随机前缀），并确认 strategy.status 取值与 P1-2 一致；不得为此新增 StrategyMapper 除非另行批准。

### P1-7 stop 与新 start 并发的可用性默认值未预设
- 现象：Plan §6.2 把"stop 提交后才允许新 start，或返回冲突"推迟到实施阶段并发测试决定，未预设安全默认。
- 影响：正确性已安全（永不两个 RUNNING），但 API 可用性语义未定；实施者可能选择"新 start 立即 409"或"新 start 等待"，二者对外行为不同。
- 建议：预设强制不变量"并发 stop+start 永不产生两个 RUNNING；当新 start 在 stop 未提交时命中 active_slot，返回 `QPF-FOLLOW-001`（不阻塞等待）"，并在 Plan §6.2 写为默认语义，并发测试据此断言。

### P1-8 Audit insert 路径未显式列入下一阶段白名单
- 现象：Plan §8.4/Proposal §6 要求 start/stop 同事务写 `quantpf_audit_record`，但当前 `AuditRecordMapper` 仅有 `selectPage`（只读）。Plan §10 白名单文本列出 Follow 相关文件，未显式包含"Audit insert mapper/XML/repository/adapter"文件。风险登记 FOL-SCHEMA-P1-002A 已标此为阻塞。
- 影响：若下一阶段白名单未显式放行 Audit insert 文件，实施将无法在同事务完成审计，或被迫越权。
- 建议：在 Plan §10 白名单显式追加"AuditRecordMapper insert 方法/XML 片段、AuditRecordRepository save/port、AuditRecordRepositoryAdapter insert、同 SqlSession 事务适配"为下一阶段允许文件，并与 Follow 文件同批批准。

## 4. P2 问题（建议补齐，不阻塞批准）

- P2-1 OpenAPI `follow-start`/`follow-stop` 登记响应 202，而当前 Controller 实际返回 HTTP 200；Plan §8.1 列出契约待补但未显式把"202↔200 状态码对齐"列为必需变更。（对应 inventory FOL-INV-002）
- P2-2 `started_by` 默认值约定未指定；可参照 `trading_halt.halted_by='PAPER_API'` 的先例明确默认主体。
- P2-3 `requestId` 与 `Idempotency-Key` 的 canonical 选择仍未决（Plan §8.1 列为批准点但未给建议）；建议 v1 选其一并文档化。
- P2-4 历史 STOPPED/FAILED session 无保留/归档策略；表会随 session 累积增长，建议在 Plan 增加保留周期或归档迁移占位说明。
- P2-5 Plan §2.2 记录"QuantPF 表数量 21 张"，而 `db/migration/` 定义 20 张 `quantpf_*`（差值最可能为 `flyway_schema_history`）；Plan 未显式核对此差异，建议补一句说明以消除事实歧义。

## 5. 方向性正确性确认（无需改动项）

下列经推演确认正确，无需变更：

1. 唯一性机制：generated stored `active_slot` + unique index，非 partial unique index；多 NULL 允许保留历史 STOPPED/FAILED；MySQL 8.0.20 可实现。
2. 并发 start 重复键处理：两并发 start 同时通过前置查询时，仅一个 insert 成功，另一个被 unique key 拦截后重新读事实；同键同参→idempotent，同键异参→`QPF-IDEMP-001`，异键同维度→`QPF-FOLLOW-001`。InnoDB 对未提交行的唯一性冲突检测保证不会漏过第二个 RUNNING。
3. 事务原子性：session 状态变更 + audit insert 同一 SqlSession/事务，任一失败整体 rollback，无"audit 成功但状态失败"或反之的残留。
4. 幂等：`start_request_id` 与 `stop_request_id` 的 UNIQUE 约束为数据库层最后防线；服务层先查历史再决定 replay/conflict。
5. 无内存回退：明确禁止 DB 失败后回退 AtomicBoolean 或返回成功。
6. 不复用 Audit/Job/Halt/Order 表；新建专用 `quantpf_paper_follow_session`。
7. Flyway forward-only、无生产 DROP TABLE 自动回滚、无数据回填。
8. Phase 1C 未放行；proposal 标记 `NON-EXECUTABLE`/`NO DDL EXECUTED`/`HUMAN APPROVAL REQUIRED`。
9. 不引入 outbox/event、不伪造 follow event、不升级既有 v1 event schema。
10. audit detail 不保存凭据。

## 6. 进入人工批准与实施的"变更前/变更后"门禁

变更前（当前 Plan/Proposal 状态）：

```text
PASS_WITH_CHANGES
PHASE1B-FOLLOW-PERSISTENCE-IMPLEMENTATION: NOT STARTED
PHASE1C-PAPER-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```

变更后（补齐 P1 后可进入人工批准）：

```text
P1-1 FAILED 流程或移除 FAILED 状态
P1-2 strategy.status 准入集合明确
P1-3 跨列 CHECK 列入 disposable schema 预验证清单
P1-4 stop_request_id 可空性 CHECK 不变量补齐
P1-5 revision 用途与流程对齐或移除
P1-6 strategy 测试夹具路径（JDBC 直插）明确
P1-7 stop+start 并发默认语义预设
P1-8 Audit insert 路径显式列入下一阶段白名单
```

完成上述 P1 补齐后，本审查可升级为 PASS；在此之前保持 PASS_WITH_CHANGES，不阻塞"计划/提案"本身的可批准性，但阻塞"下一阶段实施"的启动。

## 7. 审查边界

- 本审查为只读；未修改任何 Java/XML/YAML/SQL/POM/Vue/TS 或正式契约。
- 未执行 Git、DDL、Flyway、INSERT、UPDATE、DELETE；未连接数据库或生产系统；未输出凭据。
- 仅生成 `docs/ai/phase0/phase1b-follow-persistence-independent-review.md`。
- 本审查不替代 GPT-5.6 Sol 的最终架构判定，也不做 Paper MVP 的 PASS/PARTIAL 终判。

## 8. GPT-5.6 Sol 主控修正后的复核处置

本节不是对原 GLM-5.3 输出的改写，而是记录主控对原 `PASS_WITH_CHANGES` 结果的逐项处置：

| 原 P1 | 主控处置 | 状态 |
|---|---|---|
| FAILED 状态没有真实写入流程 | v1 移除 FAILED，仅保留 RUNNING/STOPPED；未来失败状态另行立项 | 已解决 |
| strategy.status 准入未定义 | 明确只允许 ACTIVE | 已解决 |
| 跨列 CHECK 未列入实测 | 纳入 MySQL 8.0.20 disposable schema 必测门禁 | 已解决 |
| stop_request_id 不变量缺失 | RUNNING 必须 NULL，STOPPED 必须非 NULL | 已解决 |
| revision 语义不清 | v1 使用行锁；revision 仅递增审计计数，不使用 CAS | 已解决 |
| strategy 测试夹具未定义 | 下一阶段使用随机前缀 JDBC 直插 PAPER account/ACTIVE strategy，不新增无关 Mapper | 已解决 |
| stop+start 并发语义未固定 | v1 不等待；active slot 未释放时返回 QPF-FOLLOW-001 | 已解决 |
| Audit insert 不在白名单 | 已将 Audit insert mapper/XML/repository/adapter 和同 SqlSession transaction adapter 纳入下一阶段白名单 | 已解决 |

补充固定决策：

- `Idempotency-Key` header 是唯一 canonical 幂等键；
- 同步 Follow API 成功响应统一为 HTTP 200，下一阶段同步 OpenAPI 当前的 202；
- 首次成功 start/stop 写 Audit，幂等重放不重复写成功 Audit；
- 当前计划阶段没有执行 DDL/Flyway，也没有修改业务代码。

因此，原独立 review 的 `PASS_WITH_CHANGES` 在主控修正完成后可作为“计划阶段已满足、实现阶段仍须门禁”的审查证据；不代表 Follow 已实施。
