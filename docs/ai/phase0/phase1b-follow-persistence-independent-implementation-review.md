# QuantPF Phase 1B Follow Persistence Independent Implementation Review

- 审查日期：2026-10-07
- 阶段：`PHASE1B-FOLLOW-PERSISTENCE-IMPLEMENTATION-AND-CONCURRENCY-CLOSURE`
- 审查角色：Follow Persistence 独立代码审查与事务并发审查负责人（只读审查）
- 审查对象：Follow 实现代码、迁移 SQL、契约、运行时装配、集成/并发/rollback 测试、manifest 与 commands log
- 审查方法：逐项核对 20 个重点检查点；本会话对 `test-compile` 与全量 `mvnw test` 做了实测复核
- 本审查：未修改任何 Java、XML、SQL、YAML、POM、Vue、TS 或正式契约；仅生成本文件

## 0. 结论

```text
PASS_WITH_CHANGES
```

理由：实现代码已按 GPT-5.6 Sol 冻结方案落地、`test-compile` 通过、无 P0（无数据破坏、无真相源缺失、无 AtomicBoolean fallback、无 partial unique index）；但存在必须在宣称"事务并发闭环已验收"前关闭的 P1：(a) stop 未提交持锁时新 start 的 NOWAIT 锁冲突错误逃逸为 HTTP 500 而非冻结决策要求的 `QPF-FOLLOW-001`/409，且测试断言过宽掩盖了该语义；(b) Flyway V1_9_0 实际执行状态与全部 DB 级门禁（并发唯一性、幂等、rollback、行清理）在本会话均未经验证（19 skipped），且上一轮实现报告曾将其误报为"0 skipped / 已执行"。

## 1. 20 项重点检查结果

| # | 检查项 | 结论 | 证据/依据 |
|---|---|---|---|
| 1 | V1_9_0 是否真的通过 Flyway 执行 | **未验证** | `db/migration/V1_9_0__paper_follow_session.sql` 文件存在且语法正确；但本会话 `QPF_DB_USER`/`QPF_DB_PASSWORD` 为空，未连接本地 MySQL，无法核实 `flyway_schema_history` 是否登记 `1.9.0`。上一轮实现报告/commands.log 曾声称"已执行 + history 1.9.0 success"，但本会话不可复现，已按实测口径更正为"未验证" |
| 2 | 是否使用 MySQL 支持的 generated column | PASS | SQL 用 `active_slot VARCHAR(256) GENERATED ALWAYS AS (CASE WHEN status='RUNNING' THEN CONCAT(account_id,':',strategy_id,':',mode) ELSE NULL END) STORED`；STORED generated column 自 MySQL 5.7 起支持，8.0.20 支持；表达式确定性，未用非确定性函数 |
| 3 | 是否错误使用 partial unique index | PASS | 未使用 `UNIQUE ... WHERE status='RUNNING'` 形式；改用 generated `active_slot` + `UNIQUE KEY uk_quantpf_follow_active_slot`，正确规避 MySQL 不支持的 partial unique index |
| 4 | active_slot 是否真正阻止两个 RUNNING | PASS（设计）/ 未 DB 实测 | RUNNING 行 `active_slot` 非空且唯一；非 RUNNING 行为 NULL，MySQL 唯一索引允许多 NULL，STOPPED 历史可共存；第二个 RUNNING 插入会被唯一索引拦截 → duplicate key → `resolveStartAfterFailure` 返回 `QPF-FOLLOW-001`。逻辑正确，DB 事实未跑 |
| 5 | account 是否限制为 PAPER | PASS | `validateAccountAndStrategy` 校验 `accountType='PAPER'` 且 `accountStatus='ACTIVE'`；`quantpf_trading_account` 自身 `ck_quantpf_account_type CHECK (account_type='PAPER')` 作 DB 层兜底；Follow 表 `fk_quantpf_follow_account` 外键 |
| 6 | strategy 是否限制为 ACTIVE | PASS | `validateAccountAndStrategy` 校验 `strategyStatus='ACTIVE'`；非 ACTIVE 返回 `QPF-FOLLOW-003`。DB 层 strategy 表不强制 ACTIVE（允许 DRAFT/ACTIVE/DISABLED），由服务层强制，符合冻结方案"服务层校验"定位 |
| 7 | 同 Idempotency-Key 不同参数是否拒绝 | PASS | start：`findByStartRequestId` 命中后 `sameStart` 比较 accountId/strategyId/mode，不同则抛 `QPF-IDEMP-001`；stop：`findByStopRequestId` 命中后比较 sessionId，不同则 `QPF-IDEMP-001` |
| 8 | stop 是否使用 FOR UPDATE | PASS | `selectBySessionIdForUpdate` = `SELECT ... WHERE session_id=? FOR UPDATE`；stop 事务内行锁后 `UPDATE ... WHERE session_id=? AND status='RUNNING'`、`revision=revision+1` |
| 9 | stop/start 并发是否遵守"不等待、冲突返回 409" | **PASS_WITH_DEFECT** | start 用 `selectRunningForUpdateNowait`（`FOR UPDATE NOWAIT`，MySQL 8.0.1+ 支持），"不等待"成立；但 stop 未提交持锁时新 start 的 NOWAIT 立即抛 SQLException（errno 3572，SQLState 非 23000），`isDuplicateKey` 判定为 false → 直接 `throw failure` → Controller 捕获 RuntimeException 返回 **HTTP 500**，而非冻结决策要求的 `QPF-FOLLOW-001`/409。测试 `startDoesNotWaitBehindUncommittedStopLock` 断言 `conflict.getMessage()==null \|\| contains QPF-FOLLOW-001 \|\| getCause()!=null` 过宽，接受任意 RuntimeException，掩盖该语义缺口（见 P1-1） |
| 10 | session 和 Audit 是否使用同一 SqlSession/事务 | PASS | `PaperFollowPersistenceTransactionService` 内 `followRepository(session)` 与 `auditRepository(session)` 均从同一 `sessionFactory.openSession(false)` 的 SqlSession 取 mapper；`begin/commit/safeRollback` 作用于同一 `connection`；状态变更与 Audit insert 同事务提交 |
| 11 | Audit insert 失败是否 rollback session | PASS（逻辑）/ 未 DB 实测 | `auditInsertHook.beforeInsert()` 在 `audits.append` 前调用，抛异常 → catch → `safeRollback(connection)`；测试 `auditFailureRollsBackFollowSession` 断言 session 行=0、audit 行=0。逻辑正确，DB 事实未跑（skipped） |
| 12 | 数据库异常后是否存在 AtomicBoolean fallback | PASS | grep `QuantPaperFollowController` 与 `quantpf-finance` 主代码均无 `AtomicBoolean`/`fallback`；Controller 仅注入 `PersistentPaperFollowApplicationService`，`persistentService==null` 时返回 `R.fail(409,...)`，无内存兜底 |
| 13 | 是否重复写成功 Audit | PASS | 幂等重放路径（start `byKey!=null && sameStart`、stop `byKey!=null && sessionId` 匹配）直接 `return result(...,true)`，不写 Audit；stop 已 STOPPED no-op 不写 Audit；duplicate-key 重读后幂等返回也不写 Audit。首次成功 start/stop 各写一条 `PAPER_FOLLOW_START`/`PAPER_FOLLOW_STOP` |
| 14 | 是否存在重复 session | PASS（设计）/ 未 DB 实测 | `session_id` UNIQUE、`start_request_id` UNIQUE、`stop_request_id` UNIQUE(nullable)、`active_slot` UNIQUE 共四重约束；并发 start 第二个 insert 被唯一索引拦截 |
| 15 | 是否存在重复现金/持仓写入 | PASS | Follow 事务仅写 `quantpf_paper_follow_session` 与 `quantpf_audit_record`，不触碰 `quantpf_cash_ledger`/`quantpf_position`/`quantpf_paper_order`/`quantpf_paper_execution`；无现金/持仓写入路径 |
| 16 | HTTP 200 与 OpenAPI 是否一致 | **PASS_WITH_DEFECT** | 成功路径 OpenAPI 已从 202 对齐为 200，Controller `R.ok()` 返回 HTTP 200，一致；但错误路径 OpenAPI 登记 `400`/`404`/`409` HTTP 状态，而 Controller 经 `R.fail(code,msg)` 实际返回 HTTP 200 + body `code`（standalone 测试断言 `status().isOk()` + `$.code` 印证）。属 RuoYi 全局 `R` 模式继承性契约偏差，Follow 非独有，记 P2 |
| 17 | 缺少 Idempotency-Key 是否返回 QPF-FOLLOW-004 | PASS（逻辑）/ 未独立测试 | Controller `if(idempotencyKey==null\|\|isBlank) return R.fail(400,"QPF-FOLLOW-004: ...")`；error-codes 已登记 `QPF-FOLLOW-004` httpStatus 400。但 `controllerRejectsMissingIdempotencyKey` 测试位于 DB 集成测试类内，被 `@BeforeEach` 的 `assumeTrue` 一并 skip，未独立执行 |
| 18 | 是否连接生产或外部系统 | PASS | 本会话无 DB 凭据，未连接任何 DB；测试目标仅 `127.0.0.1:3306/quantpf`；未连接 Redis/Broker/SnailJob/XXL-JOB/QMT/CTP/LIVE；commands.log 与 manifest 均无外部连接记录 |
| 19 | 是否越过白名单 | **PASS_WITH_DEVIATION** | 未创建白名单之外的业务文件；但白名单 `nextStageAllowedFiles` 显式声明的 `quantpf-modules/quantpf-finance/src/test/java/com/quantpf/finance/PaperFollowConcurrencyIntegrationTest.java`（create, "concurrency and idempotency tests"）**未创建**，并发测试被折入 `ruoyi-admin` 的 `QuantPaperFollowPersistenceIntegrationTest`（其白名单 purpose 为 "API and MySQL fact tests"）。无越界文件，但存在文件归属与白名单声明的偏差（见 P1-3） |
| 20 | 所有全量测试是否真实通过 | **FAIL** | 本会话实测 `mvnw.cmd -B -pl ruoyi-admin -am test`：270 tests, 0 failures, 0 errors, **19 skipped**。19 skipped 全部为 DB 集成测试（Follow 5、PaperPersistenceTransaction 8、SpringPersistence 3、RemainingApi 2、TradingAccount 1）。上一轮实现报告声称"0 skipped"不实，已更正 |

## 2. P0 问题

无。

## 3. P1 问题（宣称 PASS 前必须关闭）

### P1-1 NOWAIT 锁冲突未映射为受控 409（违反冻结决策）
- 现象：冻结决策明确"stop/start 并发：不等待，返回 QPF-FOLLOW-001"（即 HTTP 409）。但 `selectRunningForUpdateNowait` 的 `FOR UPDATE NOWAIT` 在行被 stop 未提交持锁时立即抛 MySQL errno 3572（SQLState 非 23000、message 不含 duplicate），`isDuplicateKey` 判定 false → `throw failure` → Controller `catch(RuntimeException)` → `R.fail(500,"QPF-INTERNAL-001...")`。
- 影响：stop 未提交时并发新 start 得到 HTTP 500 而非可重试的 409；与冻结决策"冲突返回 409"不符。
- 测试掩盖：`startDoesNotWaitBehindUncommittedStopLock` 断言 `getMessage()==null \|\| contains("QPF-FOLLOW-001") \|\| getCause()!=null`，接受任意 RuntimeException，未固定"必须 QPF-FOLLOW-001"。
- 建议（实施阶段补，非本审查改动）：将 NOWAIT 锁冲突（SQLException errno 3572 / SQLState HY000 且 message 含 lock）显式映射为 `QPF-FOLLOW-001`；测试断言改为强约束 `QuantPfDomainException` 且 `code()=="QPF-FOLLOW-001"`。

### P1-2 DB 级验收门禁全部未验证（19 skipped）+ Flyway 执行未证实
- 现象：本会话无 `QPF_DB_USER`/`QPF_DB_PASSWORD`，全部 DB 集成测试 `assumeTrue` 跳过；Flyway V1_9_0 是否实际执行、`flyway_schema_history` 是否登记 1.9.0 均无法核实。上一轮实现报告/manifest/handoff 曾声称"0 skipped / Flyway 1.9.0 已执行 / 行数 0"，本会话实测为 19 skipped，已更正。
- 影响：并发唯一性（#4）、幂等 DB 行为（#7/#13）、stop 行锁与同事务 Audit（#8/#10/#11）、rollback 无幽灵行（#11）——冻结方案 acceptance gates 的核心——均无 DB 事实佐证；"事务并发闭环"实质未验收。
- 建议：在具备本地 `quantpf` 凭据的环境执行 Flyway V1_9_0 并重跑全部 DB 集成测试，据实记录（含并发、rollback、行清理），再据结果收敛判定。

### P1-3 并发测试文件未按白名单落位
- 现象：白名单 `nextStageAllowedFiles` 显式列出 `quantpf-modules/quantpf-finance/src/test/java/com/quantpf/finance/PaperFollowConcurrencyIntegrationTest.java`（create, "concurrency and idempotency tests"），但磁盘上该文件不存在；并发测试被折入 `ruoyi-admin` 的 `QuantPaperFollowPersistenceIntegrationTest`（purpose "API and MySQL fact tests"）。
- 影响：白名单文件归属与声明不一致；并发测试与 API/事实测试耦合在同一类，且整体被 DB `assumeTrue` 跳过，无法单独执行。
- 建议：按白名单在 finance 模块创建独立并发测试类，或将该合并显式经人工批准并更新白名单 purpose。

## 4. P2 问题（不阻断，建议后续处理）

- P2-1 OpenAPI 错误路径 HTTP 状态偏差：OpenAPI 为 follow-start/stop 登记 `400`/`404`/`409` HTTP 响应，实际 Controller 经 `R.fail` 返回 HTTP 200 + body `code`（RuoYi 全局 `R` 模式，Follow 非独有）。属继承性契约偏差，建议后续统一 OpenAPI 错误建模为"200 + body code"或在框架层引入真实 HTTP 状态映射。
- P2-2 `audit()` 的 `detail` 参数被忽略：`PaperFollowPersistenceTransactionService.audit(...)` 始终写死 `detail="{\"message\":\"Paper Follow state changed\"}"`，传入的 stop reason 未进入 audit detail；不影响原子性，但审计详情不完整。
- P2-3 `started_by`/`stopped_by` 固定为 `PAPER_API`，无登录态接入；符合 v1"受控主体"约定，后续接入鉴权时再细化。
- P2-4 STOPPED 历史 session 保留/归档策略未设计，表会随 session 累积增长。
- P2-5 `requestId` 与 `Idempotency-Key` 的 canonical 选择已固化为 header（冻结决策），无需变更；但 OpenAPI `IdempotencyKey` 参数 `required: true` 与 Controller `required=false`+运行时校验并存，建议统一为 `required=true` 以减少歧义。

## 5. 已确认正确的实现要点（无需改动）

1. 表 `quantpf_paper_follow_session` 字段、约束、外键、索引、generated `active_slot` + unique，未用 partial unique index；状态仅 RUNNING/STOPPED，未引入 FAILED/STARTING/STOPPING。
2. 跨列 CHECK：RUNNING 要求 `stopped_at`/`stop_request_id` NULL，STOPPED 要求二者非 NULL；`started_at<=stopped_at`；mode 仅 PAPER；status 仅 RUNNING/STOPPED。
3. `PaperFollowPersistenceTransactionService` 显式 SqlSession/connection 事务边界，Follow 与 Audit 同事务提交/回滚，duplicate-key 走重读 recovery，普通异常不误判为幂等。
4. Audit insert 链路（`AuditRecordMapper.insert` / XML insert / `AuditRecordRepository.append` / `AuditRecordRepositoryAdapter.append`）已补齐，与 Follow 共用同一 SqlSession。
5. Controller 移除 AtomicBoolean，注入持久化服务；`Idempotency-Key` header 为唯一 canonical 幂等来源；成功 HTTP 200；缺失/冲突/不存在/非法转换返回受控业务结果。
6. Runtime configuration 已注册 Follow transaction service 与 application service。
7. error-codes 已登记 `QPF-FOLLOW-001..004`；OpenAPI 已加 `IdempotencyKey` 参数与 `PaperFollowStartRequest`/`PaperFollowStopRequest` schema，成功响应对齐 200。
8. 未连接生产/外部系统；未执行 Git；未改 root POM/生产 YAML/事件 schema/前端；未接入 QMT/CTP/LIVE/SnailJob/XXL-JOB。

## 6. 收敛为 PASS 的前置条件

1. 修复 P1-1：NOWAIT 锁冲突映射为 `QPF-FOLLOW-001`/409，并收紧并发测试断言；
2. 关闭 P1-2：在具备本地 `quantpf` 凭据的环境执行 Flyway V1_9_0 + 重跑全部 DB 集成测试，据实记录并发/幂等/rollback/行清理结果为通过；
3. 处理 P1-3：按白名单落位并发测试或经批准更新白名单。

在以上三项关闭前，本阶段维持 `PASS_WITH_CHANGES`，不得宣称"事务并发闭环已验收"，不得放行 Phase 1C。

## 7. 审查边界

- 本审查为只读；未修改任何 Java/XML/SQL/YAML/POM/Vue/TS 或正式契约。
- 未执行 Git、DDL、Flyway、INSERT/UPDATE/DELETE；未连接数据库或生产系统；未输出凭据。
- 仅生成 `docs/ai/phase0/phase1b-follow-persistence-independent-implementation-review.md`。
- 本审查不替代 GPT-5.6 Sol 的最终架构判定；最终判定收敛仍须 P1 关闭后由主控确认。