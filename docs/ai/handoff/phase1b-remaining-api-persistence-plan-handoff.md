# QuantPF Phase 1B Remaining API Persistence Plan Handoff

## 1. 当前目标

`PHASE1B-REMAINING-API-PERSISTENCE-AND-PAPER-MVP-CLOSURE`

## 2. 当前结果

```text
PLAN COMPLETE
IMPLEMENTATION NOT STARTED
PHASE1C-PAPER-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```

## 3. 当前事实

### Audit

- Controller 当前返回固定空列表；
- `quantpf_audit_record` 已存在；
- 字段和索引足够支持最小只读分页查询；
- 当前行数为 0。

### Job execution

- Controller 当前返回固定空列表；
- `quantpf_job_execution_audit` 已存在；
- `execution_id` 已有唯一约束；
- 字段足够支持最小只读分页查询；
- 当前行数为 0。

### Follow

- Controller 当前使用 AtomicBoolean；
- 当前 schema 未发现 follow session/state 表；
- 不允许复用 audit/job 表伪造 Follow 状态；
- 必须单独生成 migration proposal 并等待人工批准。

## 4. 唯一实施入口

先阅读：

- `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1b-remaining-api-persistence-paper-mvp-plan.md`
- `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1b-remaining-api-persistence-file-whitelist.json`
- `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1b-remaining-api-persistence-risk-register.md`
- `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1b-remaining-api-persistence-commands.log`

## 5. 下一模型实施顺序

1. Audit Row/Mapper/XML/Repository/Application Service；
2. Job execution Row/Mapper/XML/Repository/Application Service；
3. Controller 和 Spring runtime wiring；
4. Audit/Job MockMvc + MySQL 事实测试；
5. 数据库失败不 fallback 测试；
6. Follow 只做 schema 评估和 migration proposal，不实现假持久化；
7. 旧测试回归；
8. GLM-5.3 只读审查；
9. GPT-5.6 Sol 最终验收。

## 6. 严格禁止

- 修改根 POM、正式 YAML、Flyway/SQL；
- 执行迁移、DDL、DROP、USE；
- Redis/Broker/SnailJob/XXL-JOB/QMT/CTP/LIVE；
- Git；
- 创建 quantpf-application；
- 前端实现；
- 输出凭据；
- 将 AtomicBoolean 当成正式 Follow 持久化。

## 7. 当前门禁

在 Audit、Job execution 完成且 Follow 的 schema/审批边界明确前：

```text
PHASE1C-PAPER-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```
