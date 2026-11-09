# QuantPF Phase 1B Remaining API Persistence and Paper MVP Closure Handoff

- 交接日期：2026-10-07
- 唯一交接入口：本文件
- 当前阶段：`PHASE1B-REMAINING-API-PERSISTENCE-AND-PAPER-MVP-CLOSURE`
- 阶段结果：`PARTIAL`
- Phase 1C：`NOT ALLOWED`

## 1. 当前目标

完成剩余 Paper API 的数据库事实接入，确认没有固定空列表和数据库失败后的静默内存 fallback，复核幂等/事务安全，并决定是否允许进入 Paper 前端阶段。

## 2. 本轮最终事实

### Audit

`GET /api/quant/audit/page` 已接入：

```text
Controller
 -> PersistentAuditApplicationService
 -> AuditRecordRepository
 -> AuditRecordRepositoryAdapter
 -> AuditRecordMapper/XML
 -> quantpf_audit_record
```

已验证：分页 1..100、long offset、`occurred_at DESC, id DESC`、request/aggregate 过滤、数据库异常传播、随机测试数据清理。

### Job execution

`GET /api/quant/job/execution/page` 已接入：

```text
Controller
 -> PersistentJobExecutionApplicationService
 -> JobExecutionAuditRepository
 -> JobExecutionAuditRepositoryAdapter
 -> JobExecutionAuditMapper/XML
 -> quantpf_job_execution_audit
```

已验证：分页 1..100、long offset、`created_at DESC, id DESC`、job/execution/status 过滤、数据库异常传播。没有初始化或连接 SnailJob/XXL-JOB。

### Follow

`QuantPaperFollowController` 仍为 `AtomicBoolean`。当前 schema 没有 Follow session/state 表。不得将它宣称为持久化，也不得复用 Audit/Job 表伪造 Follow 状态。

## 3. 当前验收结果

- `QuantPaperApiStandaloneTest`：6/6 PASS；
- `QuantPfHealthControllerWebMvcTest`：1/1 PASS；
- `QuantPfRemainingApiNoFallbackUnitTest`：2/2 PASS；
- `QuantPfRemainingApiPersistenceWebIntegrationTest`：2/2 PASS；
- `QuantPfSpringPersistenceWebIntegrationTest`：3/3 PASS；
- `PaperPersistenceTransactionIntegrationTest`：8/8 PASS；
- `OrderStateMachineTest`：3/3 PASS；
- `PaperRiskGateTest`：4/4 PASS；
- 全量 `mvnw.cmd -B -pl ruoyi-admin -am test -q`：69 reports、266 tests、0 failures、0 errors、0 skipped；
- 本地 DB 范围：`127.0.0.1:3306/quantpf`；
- Git：本阶段没有执行任何 Git 命令；
- Flyway/DDL：没有执行；
- 凭据：没有写入任何报告或日志。

## 4. 当前分支和工作区状态

本阶段按用户要求没有运行 Git status、log 或 diff，因此不能在本交接中声称工作区 clean 或给出分支偏离数。需要由用户在不涉及修改的人工检查阶段自行执行 Git 审核。

## 5. 当前技术基线

- JDK21 Maven 基线已由前阶段确认；
- QuantPF 模块骨架和 `ruoyi-admin` 受控 Spring wiring 已存在；
- Audit/Job Mapper XML 位于 `quantpf-domain` 资源路径；
- Audit/Job Repository adapter 和 Application Service 位于 `quantpf-finance`；
- Controller 位于 `ruoyi-admin`；
- 本轮没有修改根 POM、正式 YAML、Flyway/SQL 或前端。

## 6. 当前 Paper 持久化状态

| 能力 | 状态 |
|---|---|
| Account persistence | 既有 Spring 路径通过 |
| Order persistence | 既有 Spring 路径通过 |
| Risk/Halt/Snapshot persistence | 既有 Spring 路径通过 |
| Order lifecycle | PASS |
| Partial fill | PASS |
| Cash freeze/release | PASS |
| Position freeze/release | PASS |
| Buy/sell settlement | PASS |
| Execution/ledger idempotency | PASS |
| Rollback | PASS |
| Audit read persistence | PASS |
| Job execution read persistence | PASS |
| Follow persistence | BLOCKED / NOT_SUPPORTED_BY_CURRENT_SCHEMA |

## 7. 已确认风险

P0/P1 阻塞项：

1. Follow 没有正式 schema/事实源；
2. Follow 仍是 AtomicBoolean；
3. whitelist 要求 migration proposal 但同时禁止独立 proposal 文件；
4. 独立 GLM-5.3 review 文件缺失，当前只有等效静态审查，不应称为 GLM 签字。

## 8. 下一步目标

```text
PHASE1B-FOLLOW-PERSISTENCE-SCHEMA-APPROVAL-AND-IMPLEMENTATION-PLAN
```

下一阶段只能先做：

1. Follow schema 和状态机审批；
2. start/stop 幂等键、并发唯一性和事务边界设计；
3. whitelist 扩展或 proposal 文件落点审批；
4. 非执行 migration proposal；
5. 验收标准和回滚边界。

未经批准不得：

- 创建 Flyway SQL；
- 执行 DDL 或迁移；
- 修改 Follow Controller；
- 进入 Phase 1C 前端实现。

## 9. 下一步输入文件

必须先读取：

1. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1b-remaining-api-persistence-paper-mvp-closure.md`
2. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1b-remaining-api-persistence-paper-mvp-closure-risk-register.md`
3. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1b-remaining-api-persistence-paper-mvp-closure-manifest.json`
4. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1b-remaining-api-persistence-paper-mvp-closure-commands.log`
5. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase1b-remaining-api-persistence-file-whitelist.json`
6. `D:\我的文档\私人\量化\量化交易系统完整设计方案_V29.md`

## 10. 下一步验收命令

在不写入凭据的前提下，先进行静态审计：

```powershell
rg -n "AtomicBoolean|follow-start|follow-stop" "D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\src\main\java"
rg -n "quantpf_audit_record|quantpf_job_execution_audit" "D:\workspace\quant\RuoYi-Vue-Plus\quantpf-modules" "D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin"
```

Follow 施工获得审批后，必须重新执行：

```powershell
mvnw.cmd -B -pl ruoyi-admin -am test -q
```

并补充：

- Follow start/stop API + 本地 MySQL 事实测试；
- 并发 start 只有一个 RUNNING session；
- 重复 start/stop 幂等；
- rollback 不留下半状态；
- Audit/Job/Account/Order/Risk 全量回归。

## 11. 人工审批点

必须由人工明确批准：

1. Follow 表和字段；
2. 一个账户/策略是否允许多个 RUNNING session；
3. 唯一约束和锁策略；
4. start/stop 幂等语义；
5. Flyway 版本和回滚边界；
6. proposal 文件是否允许从 forbiddenFiles 移出；
7. 何时重新开放 Phase 1C。

## 12. 当前唯一门禁结论

```text
PHASE1B-REMAINING-API-PERSISTENCE-AND-PAPER-MVP-CLOSURE: PARTIAL
PHASE1C-PAPER-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```

不要为了前端进度而弱化 Follow schema、数据库事实、幂等、事务或回滚门禁。
