# QuantPF Phase 1B Remaining API Persistence and Paper MVP Closure

- 审计日期：2026-10-07
- 当前阶段：`PHASE1B-REMAINING-API-PERSISTENCE-AND-PAPER-MVP-CLOSURE`
- 执行角色：最终主控修正、代码审查、回归测试、进度评估和验收负责人
- 最终判定：`PARTIAL`
- `PHASE1C-PAPER-FRONTEND-IMPLEMENTATION`：`NOT ALLOWED`

## 1. 最终结论

本轮已把 Audit 和 Job execution 两条剩余只读 API 从固定空列表切换到本地 `quantpf` 数据库事实源，并完成分页、排序、过滤、数据库事实测试、数据库异常不 fallback 检查和全量 Maven 回归。

最终确认：

- Audit persistence：`PASS`；
- Job execution persistence：`PASS`；
- Audit/Job 数据库失败时：不返回内存数据、不返回伪造空列表，异常向上抛出；
- Follow persistence：`NOT_SUPPORTED_BY_CURRENT_SCHEMA`，仍为 `AtomicBoolean`，未伪造持久化；
- Paper 订单生命周期、事务 rollback、执行/账务幂等、重复扣款和重复持仓风险：既有测试证据保持通过；
- 本地 MySQL 数据库事实测试：2/2 通过；
- 全量 `ruoyi-admin -am test`：266 tests，0 failures，0 errors，0 skipped；
- 未执行 Git、Flyway、DDL、DROP、USE、数据库迁移；
- 未连接 Redis、Broker、SnailJob、XXL-JOB、QMT、CTP 或 LIVE；
- Follow 的独立 migration proposal 文件没有创建，因为当前 whitelist 将该文件列入 `forbiddenFiles`；proposal 内容和人工审批点已记录在本报告中；
- 因 Follow 仍无正式 schema/事实源，本阶段不能判定 PASS，也不能放行 Phase 1C。

## 2. 审计边界和安全边界

### 2.1 允许范围

本轮只处理：

- `GET /api/quant/audit/page`；
- `GET /api/quant/job/execution/page`；
- Audit/Job 的 Row、Mapper、XML、Repository、Adapter、Application Service；
- 两个 Controller 的数据库服务接入；
- 隔离 API/数据库事实测试；
- Phase 1B 报告、日志、风险登记、manifest 和 handoff。

### 2.2 明确未触碰

- `QuantPaperFollowController` 未修改；
- 根 `pom.xml`、正式 YAML、Flyway/SQL、OpenAPI、Event Schema、错误码 YAML、菜单事实源、前端未修改；
- 没有创建 `quantpf-application`；
- 没有实现真实交易、QMT、CTP、LIVE；
- 没有启动或迁移任何调度器。

### 2.3 凭据处理

本地数据库测试只使用当前开发环境已有的本地 datasource 配置；凭据值没有写入本报告、命令日志、manifest、风险登记或 handoff。没有持久化新的凭据。

## 3. 实际问题复核

### 3.1 DeepSeek 实施结果与仓库事实不一致

上一份受控实施记录声称 Audit/Job 已实现，但当时实际源代码仍分别返回 `List.of()`。本轮以实际仓库源代码为准，重新实现并复核了两条链路。缺失的 implementation manifest、implementation commands log 和 independent review 文件也没有被当作已完成证据。

### 3.2 GLM-5.3 审查记录

计划要求的独立审查文件在仓库中不存在，因此不能声称存在可验证的 GLM-5.3 签字结果。本轮执行了等效的独立静态审查，范围包括：

- Mapper/XML namespace、字段映射、过滤条件、排序和 LIMIT；
- Controller 到 Application Service 的 Spring wiring；
- 数据库异常传播和内存 fallback；
- Follow 是否被错误伪造为持久化；
- Paper 订单、现金、持仓幂等和 rollback 既有测试报告。

结论与实现结果一致：Audit/Job 可判定 PASS；Follow 仍是 schema 阻塞，不得伪造完成。

## 4. Audit 持久化闭环

正式运行时链路：

```text
QuantPaperAuditController
  -> PersistentAuditApplicationService
  -> AuditRecordRepository
  -> AuditRecordRepositoryAdapter
  -> AuditRecordMapper
  -> AuditRecordMapper.xml
  -> quantpf_audit_record
```

已确认行为：

- 表：`quantpf_audit_record`；
- 查询字段：`id、request_id、actor_id、action、aggregate_type、aggregate_id、result、detail、occurred_at`；
- 分页：`pageNum >= 1`，`pageSize` 限制在 1..100；
- offset 使用 long 计算，避免普通 int 乘法溢出；
- 排序：`occurred_at DESC, id DESC`；
- 可选过滤：`requestId/request_id`、`aggregateType/aggregate_type`、`aggregateId/aggregate_id`；
- 空字符串过滤条件归一化为未提供；
- Mapper 返回 null 或数据库异常不会被转换为空列表；
- Spring 路径不再存在 `List.of()` fallback；
- 本地 MySQL + MockMvc 事实测试验证了随机前缀测试行确实从数据库返回，并在测试后按前缀清理。

## 5. Job execution 持久化闭环

正式运行时链路：

```text
QuantPaperJobExecutionController
  -> PersistentJobExecutionApplicationService
  -> JobExecutionAuditRepository
  -> JobExecutionAuditRepositoryAdapter
  -> JobExecutionAuditMapper
  -> JobExecutionAuditMapper.xml
  -> quantpf_job_execution_audit
```

已确认行为：

- 表：`quantpf_job_execution_audit`；
- 查询字段：`id、job_name、execution_id、scheduler_type、status、request_id、started_at、finished_at、error_code、error_message、created_at`；
- 分页：`pageNum >= 1`，`pageSize` 限制在 1..100；
- 排序：`created_at DESC, id DESC`；
- 可选过滤：`jobName/job_name`、`executionId/execution_id`、`status`；
- `execution_id` 的既有唯一约束保持不变；本轮没有增加调度器写入路径；
- 不注册、不触发、不初始化 SnailJob 或 XXL-JOB；
- 数据库异常不 fallback 到内存或空列表；
- 本地 MySQL + MockMvc 事实测试验证了随机前缀测试行从数据库返回，并在测试后按前缀清理。

## 6. Follow 现状和非执行 migration proposal

### 6.1 当前事实

`QuantPaperFollowController` 仍以 `AtomicBoolean` 保存运行状态。当前 schema 中没有能够完整表达 Follow 生命周期的正式事实源，因此不能将该实现称为持久化。

本轮没有：

- 复用 `quantpf_audit_record` 作为 Follow 状态表；
- 复用 `quantpf_job_execution_audit` 作为 Follow 状态表；
- 修改 Flyway/SQL；
- 执行 DDL；
- 修改 Follow Controller。

### 6.2 非执行 proposal 内容

以下仅是供下一阶段审批的设计草案，不是已执行 SQL：

建议新增专用表，例如 `quantpf_paper_follow_session`，至少表达：

- `id`：主键；
- `session_id`：唯一 Follow 会话标识；
- `account_id`：Paper 账户绑定；
- `strategy_id` 或策略标识；
- `mode`：固定为 PAPER，禁止 LIVE；
- `status`：RUNNING、STOPPED、FAILED 等受控状态；
- `started_at`、`stopped_at`；
- `started_by`、`stopped_by`；
- `start_request_id`、`stop_request_id`；
- `stop_reason`；
- `created_at`、`updated_at`；
- `session_id` 唯一约束，以及按账户/策略/模式限制并发 RUNNING 会话的数据库约束或事务策略。

必须由人工批准的内容：

1. 表名、字段命名和 Flyway 版本号；
2. 一个账户是否允许多个并行 Follow session；
3. RUNNING 状态唯一性采用数据库唯一索引、状态条件写入还是事务锁；
4. start/stop 幂等键和重复 stop 的返回语义；
5. Follow session 是否必须绑定 strategy、instrument universe 和 Paper account；
6. 停止原因、操作人和审计记录的字段要求；
7. 是否允许先实现 Paper-only session，再扩展其他模式。

当前 whitelist 明确禁止单独创建 `docs/ai/phase0/phase1b-follow-persistence-migration-proposal.md`，因此本报告是本轮唯一的非执行 proposal 记录。没有人工批准和 whitelist 扩展前，不得施工。

## 7. 内存 fallback、重复扣款和重复持仓审查

### 7.1 fallback

| 路径 | 结论 | 证据 |
|---|---|---|
| Audit | 已消除 | Controller 仅调用持久化服务；服务直接调用 Repository；无 `List.of()` fallback |
| Job execution | 已消除 | Controller 仅调用持久化服务；服务直接调用 Repository；无 `List.of()` fallback |
| Follow | 仍存在正式内存状态 | `AtomicBoolean`；因无 schema 按边界未修改 |
| Account/Order/Risk | Spring 持久化路径保持 | 既有 Spring persistence tests 通过；standalone 兼容构造不作为正式 Spring 事实源 |

### 7.2 重复扣款/重复持仓

本轮没有引入任何现金或持仓写操作。既有本地事务和并发测试继续通过，覆盖：

- 相同 idempotency key；
- 相同 clientOrderId；
- 相同 executionId；
- 相同 ledger event id；
- 订单提交 rollback；
- 成交结算 rollback；
- buy settlement；
- sell settlement；
- frozen cash release；
- frozen position release；
- partial fill 30 + 20 + 50。

本轮静态审查未发现新增的重复扣款或重复持仓写路径。结论为：在当前已测试 Paper 持久化路径中，`duplicateCashMutation=ABSENT`、`duplicatePositionMutation=ABSENT`；不表示尚未实现的 Follow 持久化或未来实盘链路已完成。

## 8. 最终验收证据

### 8.1 定向回归

- `QuantPaperApiStandaloneTest`：6/6 PASS；
- `QuantPfHealthControllerWebMvcTest`：1/1 PASS；
- `QuantPfRemainingApiNoFallbackUnitTest`：2/2 PASS；
- `QuantPfRemainingApiPersistenceWebIntegrationTest`：2/2 PASS，使用本地 `127.0.0.1:3306/quantpf` 事实源；
- `QuantPfSpringPersistenceWebIntegrationTest`：3/3 PASS；
- `PaperPersistenceTransactionIntegrationTest`：8/8 PASS；
- `OrderStateMachineTest`：3/3 PASS；
- `PaperRiskGateTest`：4/4 PASS。

### 8.2 全量回归

命令：

```powershell
mvnw.cmd -B -pl ruoyi-admin -am test -q
```

结果：

- exit code：0；
- 69 个 Surefire XML 报告；
- 266 tests；
- failures：0；
- errors：0；
- skipped：0。

日志中出现的 LiteFlow checked exception 和 Mockito/Byte Buddy agent warning 是既有测试日志/运行时 warning，不是失败。

### 8.3 数据库事实测试

第一次使用环境变量时因本地 datasource 值的引号未剥离，连接测试返回 `Access denied`；没有成功插入测试行，也没有执行 DDL。修正为从 `application-dev.yml` 读取并在进程内去除包装引号后重新执行：

- Audit API：PASS；
- Job execution API：PASS；
- 总计：2/2 PASS；
- 测试行使用随机 `RAP_` 前缀；
- AfterEach 按该前缀清理；
- 凭据值未输出、未写入文件。

## 9. 阶段判定

```text
PHASE1B-REMAINING-API-PERSISTENCE-AND-PAPER-MVP-CLOSURE: PARTIAL
PHASE1C-PAPER-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```

判定原因只有一个主阻塞：Follow 没有正式 schema/持久化事实源，而且 whitelist 同时要求 proposal 又禁止创建 proposal 文件。Audit、Job execution、内存 fallback、数据库事实测试和既有 Paper 事务/幂等路径已达到本轮门禁。

## 10. 进度评估

以下是基于 V29 当前范围和已验证代码的工程进度估算，不是按代码行数计算：

| 范围 | 估算完成度 | 说明 |
|---|---:|---|
| Phase 0 基础、模块骨架、契约/Flyway 静态文件、隔离 Web 验收 | 85%～90% | 核心底座已建立；调度迁移、CI/发布和部分前端接入仍未完成 |
| Phase 1A Paper 领域和 Application API | 100% | 领域、状态机、风险闸门、内存/持久化 API 基础已完成 |
| Phase 1B Paper 后端 | 86%～90% | 账户/订单/风险、事务并发、订单生命周期、Audit/Job 已完成；Follow 仍阻塞 |
| Phase 1C Paper 前端 | 0% | 按当前门禁禁止进入 |
| 面向“基本可交付源代码工程”的整体进度 | 76%（合理区间 73%～80%） | Paper 后端已接近闭环，但 Follow schema、前端、调度迁移和交付级环境门禁仍未完成 |

“基本可交付源代码工程”当前仍不能宣称完成。当前可交付的是：Paper 后端的大部分可审计源码、数据库适配、事务/幂等测试和明确的阻塞清单；不是完整前后端产品，也不是生产可部署版本。

## 11. 后续建议顺序

1. 人工确认 Follow schema、状态机、并发唯一性和 migration proposal 白名单；
2. 单独进入 `PHASE1B-FOLLOW-PERSISTENCE-SCHEMA-APPROVAL-AND-IMPLEMENTATION-PLAN`；
3. 获批后创建专用 Flyway migration，并执行独立数据库验收；
4. 将 Follow 从 `AtomicBoolean` 切换为正式 Repository/Application Service；
5. 补齐 Follow 幂等、并发 start/stop、rollback 和 Spring API 测试；
6. 重新验收整个 Paper MVP；
7. 只有 Paper MVP 和 Phase 0 交付门禁都通过后，才重新申请 `PHASE1C-PAPER-FRONTEND-IMPLEMENTATION`。

## 12. 不允许的下一步捷径

- 不得把 AtomicBoolean 改名后宣称持久化；
- 不得把 Audit/Job 表当作 Follow 状态表；
- 不得为放行前端而降低数据库事实、幂等或回滚门禁；
- 不得启用 LIVE、QMT、CTP；
- 不得引入 SnailJob/XXL-JOB 双栈触发；
- 不得在未审批时执行 DDL/Flyway。
