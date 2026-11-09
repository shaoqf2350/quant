# QuantPF Phase 0B Foundation Handoff

- 日期：2026-10-03
- 当前阶段：`PHASE0B-FOUNDATION-SKELETON-PLAN`
- 计划状态：`plan-only=true`
- 代码变更：未应用
- POM 变更：未应用
- SQL/Flyway 变更：未应用
- 契约变更：未应用
- 前端变更：未应用

## 1. 当前目标

完成 QuantPF Phase 0B Foundation Skeleton 的计划级架构设计、模块依赖审查、POM 文件级变更清单、调度迁移计划、Flyway 计划、契约计划、前端接入计划和人工审批包。

## 2. 本轮完成内容

- 读取 V29/V28 正式方案及 Phase 0A 交接包；
- 重新核对当前 Maven module tree、SnailJob 资产、前端动态菜单和原型边界；
- 形成十个 QuantPF 模块的职责和禁止边界；
- 形成无环依赖 DAG 和依赖禁止矩阵；
- 形成 POM 变更矩阵；
- 形成 50 条文件级计划 manifest；
- 形成 SnailJob→XXL-JOB 的逐阶段迁移计划；
- 形成 Flyway V1_0_0 至 V1_8_0 计划；
- 形成 V29 OpenAPI、事件 Schema、错误码、菜单事实源计划；
- 形成前端页面/API/类型目录计划；
- 形成风险登记和下一阶段唯一入口。

## 3. 实际仓库基线

- 后端：`D:\workspace\quant\RuoYi-Vue-Plus`，分支 `6.X`，HEAD `9227f626a3a37e1546d6c291ed398183e94df92b`，上游偏离 `0 0`。
- 前端：`D:\workspace\quant\plus-ui`，分支 `6.X-Vue`，HEAD `045fc006725c014455def9fed19f93c781633d26`，上游偏离 `0 0`。
- 后端 revision：6.0.0。
- JDK/Maven/pnpm Phase 0A 工具链门禁已通过。
- 当前没有 `quantpf-modules`、`com.quantpf.*`、`contract/`、`db/migration/`。

## 4. QuantPF 目标模块树

```text
quantpf-modules/
├── pom.xml
├── quantpf-common/
├── quantpf-domain/
├── quantpf-marketdata/
├── quantpf-research/
├── quantpf-strategy/
├── quantpf-risk/
├── quantpf-finance/
├── quantpf-execution/
├── quantpf-backtest/
└── quantpf-job/
```

当前仍未创建。十个模块的职责见 `phase0b-foundation-skeleton-plan.md`。

## 5. 模块依赖 DAG

```text
quantpf-job        → quantpf-execution, quantpf-research, quantpf-marketdata, quantpf-backtest
quantpf-backtest   → quantpf-strategy, quantpf-risk, quantpf-domain, quantpf-common
quantpf-execution  → quantpf-risk, quantpf-domain, quantpf-common
quantpf-finance    → quantpf-research, quantpf-domain, quantpf-common
quantpf-research   → quantpf-marketdata, quantpf-domain, quantpf-common
quantpf-strategy   → quantpf-domain, quantpf-common
quantpf-risk       → quantpf-domain, quantpf-common
quantpf-marketdata → quantpf-domain, quantpf-common
quantpf-domain     → quantpf-common + RuoYi common persistence modules
```

Controller 只允许位于 `ruoyi-admin/src/main/java/com/quantpf/web/controller`；domain 是唯一 persistence owner；job 禁止 Mapper/SQL/Shell/Broker 直连。

## 6. POM 计划变更

计划修改但本轮未修改：

- 根 `pom.xml`：接入 `quantpf-modules`；
- 新增 `quantpf-modules/pom.xml` 和十个模块 POM；
- `ruoyi-admin/pom.xml`：只接入批准的最小 application modules；
- 调度迁移阶段才处理 `ruoyi-common-job/pom.xml`、`ruoyi-extend/pom.xml` 和 SnailJob 依赖；
- XXL-JOB 依赖、Flyway 依赖必须分别人工批准；
- 不升级现有 Spring Boot、MyBatis-Plus、Sa-Token、Redisson。

具体 50 条 manifest 见 `phase0b-file-change-manifest.json`。

## 7. 调度迁移计划

当前 SnailJob 资产：客户端依赖、自动配置、8 个 `@JobExecutor`、独立服务端、应用配置、前端 iframe 页面均存在；XXL-JOB 代码仓资产不存在。

计划顺序：

1. 任务盘点和 owner/幂等/副作用记录；
2. 隔离环境验证 XXL-JOB Admin/Executor、注册/心跳/测试 Handler；
3. 逐任务完成幂等验证；
4. 切换窗口停止 SnailJob；
5. 单独启用 XXL-JOB；
6. 观察窗口验证；
7. 验收后清理 SnailJob。

任何时刻不得双栈同时触发同一业务任务。本轮未添加 XXL-JOB、未删除 SnailJob、未启动调度器。

## 8. Flyway 计划

计划目录：`db/migration/`，版本 V1_0_0 至 V1_8_0。

目标数据库：MySQL 8.x，逻辑命名基线 `quantpf`。

禁止：`CREATE DATABASE`、`USE`、`DROP TABLE`、动态 `PREPARE ALTER`、以 `IF NOT EXISTS` 掩盖 schema drift。旧 SQL 只能进入 legacy/reference，不能作为生产入口。

本轮未创建或执行任何 migration。

## 9. 契约计划

计划文件：

- `contract/openapi/quantpf-v29.yaml`；
- 四个 `.v1.schema.json` 事件 schema；
- `contract/error-codes/quantpf-error-codes.yaml`；
- `contract/menu/quantpf-menu-facts.yaml`。

OpenAPI 基线为 V28-2.1 的 29 个正式 operation；原型 49 个去重 API 单元格必须逐项映射、弃用或标记 deferred/reference-only。契约文件本轮未创建。

## 10. 前端计划

计划目录：

```text
plus-ui/src/views/quant/
plus-ui/src/api/quant/
plus-ui/src/types/quant/
```

正式页面使用 OpenAPI 生成类型；菜单事实源与 RuoYi 动态菜单 API 做校验；原型仅作为字段/交互参考；真实交易、QMT、CTP、LIVE 页面不进入 Stage 1。SnailJob 页面只能在独立调度迁移阶段替换。

## 11. 已确认 P0/P1 风险

P0：

- 根 POM/新 Reactor 变化；
- 模块边界和依赖越权；
- SnailJob/XXL-JOB 重复触发；
- XXL-JOB API 尚未在代码仓验证；
- Flyway/历史 SQL 冲突；
- 正式契约缺失；
- 前端私钥配置尚未处置；
- manifest 漏项导致越权修改。

P1：

- ruoyi-admin 一次性引入过多模块；
- quantpf-job 间接获得 Mapper 权限；
- V28 `quantpf-v28.yaml` 与 V29 `quantpf-v29.yaml` 版本命名决策；
- Node v25.2.1 的 LTS/CI 策略；
- 原型扩展 API 超出正式闭环。

## 12. 本轮禁止事项

- 不修改代码/POM/YAML/SQL/Vue/TS/JSON；
- 不创建 QuantPF 模块或业务目录；
- 不添加 XXL-JOB；
- 不删除 SnailJob；
- 不创建/执行 Flyway；
- 不创建正式契约；
- 不执行数据库或前端/后端写入型构建；
- 不连接生产；
- 不提交 Git。

## 13. 下一步目标

下一目标：

```text
PHASE0C-FOUNDATION-SKELETON-IMPLEMENTATION
```

## 14. 下一步建议模型

建议模型：

```text
GPT-5.6 Sol
```

原因：Phase 0C 将涉及根 POM、quantpf-modules、十个模块骨架和 ruoyi-admin 接入，属于高耦合工程变更，必须由架构模型按 manifest 逐项实施，不能交给批量实现模型自由发挥。

## 15. 下一步输入文件

1. 本文件；
2. `phase0b-foundation-skeleton-plan.md`；
3. `phase0b-module-dependency-matrix.md`；
4. `phase0b-file-change-manifest.json`；
5. `phase0b-risk-register.md`；
6. `phase0b-plan-commands.log`；
7. V29/V28 正式方案；
8. 实际根/common/modules/extend/admin POM。

## 16. 下一步验收命令

```powershell
java -version
mvn -version
node --version
pnpm --version

mvn -DskipTests=true validate
mvn -DskipTests dependency:tree

rg -n "com.quantpf.web.controller" ruoyi-admin quantpf-modules
rg -n "@LiveEnabled|qmtSubmitEnabled|ctpSubmitEnabled|LiveOrderGateway" ruoyi-admin quantpf-modules
rg -n "snailjob|snail-job|xxl-job|XxlJob" .

git status --short --branch
git diff --stat
```

执行前必须重新确认 manifest、审批和敏感配置边界。不得连接生产数据库或触发调度。

## 17. 人工审批点

必须批准：

1. 根 POM 和 `quantpf-modules` 创建；
2. 十个模块 POM 创建；
3. ruoyi-admin POM/健康 Controller 接入；
4. 调度迁移依赖和文件变更；
5. Flyway 依赖和 migration；
6. contract 文件版本和生成策略；
7. 历史 SQL 归档；
8. 前端正式目录和生成类型接入；
9. 敏感配置轮换/撤销；
10. 任何数据库、调度或生产连接。

## 18. 未解决问题

- V28 OpenAPI 文件名与 V29 OpenAPI 文件名的最终版本策略；
- XXL-JOB 3.1.0 的实际 artifact/API 组合；
- Node v25.2.1 是否改为 LTS CI 版本；
- Flyway 具体版本、MySQL 8.x minor/patch 和 provisioning 方式；
- 前端 RSA 私钥轮换/撤销执行记录；
- 8 个 SnailJob 任务中哪些迁移、停用或保留；
- `ruoyi-job` 兼容入口最终保留边界。

## 19. 最终状态

`PHASE0B-FOUNDATION-SKELETON-PLAN: COMPLETE`

只允许在人工审批通过后进入 `PHASE0C-FOUNDATION-SKELETON-IMPLEMENTATION`。
