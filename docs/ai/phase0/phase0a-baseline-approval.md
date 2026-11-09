# PHASE0A Baseline Approval Record — REACCEPTED

- 日期：2026-10-03
- 当前目标：PHASE0A-BASELINE-ESTABLISHMENT
- 基线状态：工具链门禁通过；Phase 0 全量尚未完成。

## 1. 已批准事项

用户已批准：

1. JDK21、Maven、Node、pnpm 工具链基线确认；
2. 前端 RSA 私钥及其他敏感配置的轮换/撤销与安全注入方案；
3. SnailJob→XXL-JOB 的迁移窗口、任务清单、停止策略和回滚边界；
4. 单调度器规则，禁止 SnailJob 与 XXL-JOB 双栈同时触发同一业务任务；
5. Phase 0 目标 MySQL 8.x、逻辑数据库/Schema 命名基线 `quantpf` 和 Flyway 版本化迁移策略；
6. 历史 SQL 进入 legacy/reference，不作为生产入口；
7. OpenAPI、事件 Schema、错误码、菜单事实源的契约审查流程。

## 2. 工具链修复结果

- JDK：Eclipse Temurin 21.0.12.1 LTS；
- Maven：3.9.13，实际 Java 21.0.12.1；
- Node：v25.2.1；
- pnpm：10.34.5，与 `package.json` 一致；
- Maven `validate`：成功，40 个 Reactor 工程全部 SUCCESS；
- 根 POM、前端 package.json、前端 lockfile 哈希与原基线一致。

## 3. 批准边界

本次批准和工具链修复不等于批准以下事项已经完成：

- QuantPF 工程骨架施工；
- 根 POM 修改；
- XXL-JOB 依赖加入；
- SnailJob 删除；
- Flyway migration 创建/执行；
- 正式契约文件创建；
- 敏感凭据实际轮换/撤销；
- QMT/CTP/LIVE 能力创建。

这些事项必须按 V29/V28 阶段门禁逐项执行。

## 4. 放行结论

`PHASE0A-TOOLCHAIN-BASELINE: PASS`

允许进入：

`PHASE0B-FOUNDATION-SKELETON-PLAN`

只允许计划级架构审查，不允许跳过计划阶段直接修改业务代码或根 POM。
