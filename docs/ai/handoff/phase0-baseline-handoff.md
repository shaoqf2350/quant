# QuantPF Phase 0 Baseline Handoff

> 唯一交接入口。下一位模型必须先读取本文件，再读取 `phase0a-baseline-audit.md`、`phase0a-risk-register.md`、`phase0a-file-inventory.json` 和 `phase0a-commands.log`。

## 1. 当前目标

本轮已完成 `PHASE0A-BASELINE-AUDIT` 的只读审计，目标是确认 V28 设计规范与两个实际代码仓/原型目录之间的差异，并生成 Phase 0 第一个交接包。

审计日期：2026-10-03。未修改业务代码、POM、配置、SQL、前端源文件；未连接数据库；未执行迁移；未提交 Git。

## 2. 当前仓库事实

- 后端：`D:\workspace\quant\RuoYi-Vue-Plus`，revision `6.0.0`，根 POM SHA-256 `CBD3740F955E8B911754351C7A70B549626C2E2A20C8546155BA39772635FBEE`。
- 前端：`D:\workspace\quant\plus-ui`，package version `6.0.0`，`package.json` SHA-256 `D1376599B7C4736C081EC9BEAA7354E236C1D4DE371C8B16ECB0E6F13E871BE1`，`pnpm-lock.yaml` SHA-256 `C29BBB60A733B8F09ECAB8DE3697A51EE5340F45A1AE7E1D9739D3C3DF1C1F05`。
- 原型：`D:\我的文档\私人\量化\前端功能原型设计`，50 个页面，原型 API 映射 50 行/49 个去重单元格。
- V28：`D:\我的文档\私人\量化\量化交易系统完整设计方案\_V28.md`，V28-2.1 正式最小 API 清单为 29 个 operation。
- 当前没有 `quantpf-modules`、`quantpf-web`、后端 `ruoyi-ui`、`com.quantpf.*` Java、正式 contract 或 Flyway migration。

## 3. 当前分支和工作区状态

- 后端分支：`6.X...origin/6.X`；HEAD `9227f626a3a37e1546d6c291ed398183e94df92b`；工作区 clean；与上游 `0/0` 偏离。
- 前端分支：`6.X-Vue...origin/6.X-Vue`；HEAD `045fc006725c014455def9fed19f93c781633d26`；工作区 clean；与上游 `0/0` 偏离。
- 禁止提交 commit。任何后续修改前必须重新记录状态和哈希。

## 4. 当前技术基线

- 根 POM：Java 21、Spring Boot 4.1.1、MyBatis 3.5.19、MyBatis-Plus 3.5.17、dynamic-datasource 4.5.0、Sa-Token 1.46.0、Redisson 4.7.0、SnailJob 2.0.2。
- 实际环境：`java -version` 为 OpenJDK 8.0.482；`mvn -version` 为 Maven 3.9.13 + Java 17.0.1；Node v25.2.1；pnpm 11.19.0。
- 前端：Vue 3.5.42、Element Plus 2.14.5、ECharts 6.1.0、Vite `^8.2.2`、TypeScript `^6.0.3`；packageManager 声明 pnpm 10.34.5；无 `test` script。
- 不得把当前 Java8/Java17 环境称为已满足 JDK21；不得在未建立 JDK21 基线前运行后端构建验收。

## 5. 当前调度状态

- SnailJob 仍是实际底座：客户端依赖、`SnailJobConfig`（`@AutoConfiguration` + `@EnableSnailJob`）、独立 `ruoyi-snailjob-server`、应用 dev/prod 配置、前端 iframe 页面均存在。
- `ruoyi-modules/ruoyi-job` 有 8 个 `@JobExecutor` 命中及多类 SnailJob 示例/账单任务。
- XXL-JOB 代码、依赖、Executor、Admin、配置在当前代码仓未发现；原型的 `quantpf_v25_xxljob_migration.sql` 只是历史参考 SQL。
- 当前不能新增 XXL-JOB 依赖、不能删除 SnailJob、不能同时启用两个调度器。调度迁移须经人工审批和单调度器门禁。

## 6. 当前工程树

```text
root: ruoyi-admin, ruoyi-common, ruoyi-extend, ruoyi-modules, ruoyi-api
common: ruoyi-common-bom, social, core, doc, excel, job, log, mail, mybatis, oss, redis, satoken, security, sms, elasticsearch, web, translation, sensitive, json, encrypt, push, liteflow, mqtt, ai, mcp
modules: ruoyi-demo, ruoyi-gen, ruoyi-job, ruoyi-system, ruoyi-workflow, ruoyi-ai
extend: ruoyi-monitor-admin, ruoyi-snailai-server, ruoyi-snailjob-server
```

实际全仓有 40 个 `pom.xml`。V28 目标树中的 QuantPF 十模块尚未创建；本轮没有创建任何业务代码目录。

## 7. 当前数据库和迁移状态

- 后端 `script/sql` 有 14 个 RuoYi SQL 文件；原型有 4 个 QuantPF SQL 文件。
- 没有 `db/migration` 或 Flyway V1.x 文件；没有连接生产数据库，也未执行任何 SQL。
- `quantpf_v25_core_hardening.sql` 有 10 张表及动态 `ALTER TABLE`（`information_schema` + `PREPARE/EXECUTE`）；不能直接作为 Flyway 生产入口。
- V24/V25 重复创建 10 个同名表；必须建立唯一的版本化迁移和 legacy/reference 隔离。
- 当前 SQL 事实中没有可执行 `USE`、`CREATE DATABASE`、`DROP TABLE`；文件注释包含禁止语句文字，不应当当作执行证据。

## 8. 当前契约状态

- 正式 OpenAPI：不存在。
- 事件 JSON Schema：不存在。
- 错误码 YAML：不存在。
- 菜单事实源 YAML：不存在。
- 前端权限/菜单仍由 `/system/menu/getRouters` + `permission.ts` 动态路由机制驱动。
- 原型 49 个去重 API 单元格与 V28 29 个正式 operation 不一致；存在旧路径、复合方法单元、扩展接口和缺失正式接口。必须先形成映射矩阵，不得按原型旧路径直接写 Controller。

## 9. 当前前端和原型状态

- 前端已有 RuoYi 动态路由、菜单 API、SnailJob 页面；不存在 QuantPF 正式 `src/views/quant` / `src/api/quant` 实现。
- 原型 50 页覆盖行情、新闻、财报、策略、回测、账户/订单、风控、券商、数据源、作业、审计；仅作 UI/字段/交互参考。
- 原型 README 声称 XXL-JOB 唯一调度，与底座实际 SnailJob 冲突；以实际底座和 V28 调度门禁为准。
- `.env.development` 和 `.env.production` 检测到 RSA private-key 配置键；不在此交接中输出值。前端 `VITE_` 变量边界使其成为 P0 风险。

## 10. 已确认P0阻塞项

1. JDK/Maven 不满足 Java 21 基线。
2. QuantPF 模块/包/契约/Flyway/BASELINE 均不存在。
3. SnailJob 全套运行资产仍在，XXL-JOB 尚未接入；迁移方案和单调度器门禁未审批。
4. 历史 SQL 存在动态 DDL和重复表体系，不能直接迁移。
5. 前端环境存在私钥配置键及后端密码/Token 类配置风险，需人工决定轮换和安全注入。

## 11. 不允许修改的文件和能力

下一位模型必须继续遵守：

- 不修改任何 Java、XML、YAML、SQL、Vue、TS、JSON、POM 文件，除非用户在后续任务明确批准且该任务进入允许阶段；
- 不创建 `quantpf-modules`；
- 不修改根 `pom.xml`；
- 不删除 SnailJob；
- 不添加 XXL-JOB 依赖；
- 不执行数据库迁移，不连接生产数据库；
- 不读取或输出真实密码、Token、API Key、私钥、券商凭据；
- 不修改 JDK、JAVA_HOME、Maven、Node、pnpm；
- 不生成真实交易、QMT、CTP、LIVE 代码；
- 不执行删除/重置类命令，不提交 Git commit；
- 当前交接之后的写入仍只允许在 `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\` 和 `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\handoff\`，除非后续人工审批明确改变范围。

## 12. 下一步目标

下一目标：
**PHASE0A-BASELINE-ESTABLISHMENT**

## 13. 下一步建议模型

建议模型：
**GPT-5.6 Sol**

原因：
这是环境、分支、基线和人工批准任务，不能交给批量实现模型直接决定。

下一步禁止：

- 修改业务代码；
- 修改POM；
- 删除SnailJob；
- 创建数据库迁移；
- 创建QMT/CTP；
- 启用LIVE。

## 14. 下一步输入文件

1. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\handoff\phase0-baseline-handoff.md`（本文件，唯一入口）
2. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase0a-baseline-audit.md`
3. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase0a-file-inventory.json`
4. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase0a-commands.log`
5. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase0a-risk-register.md`
6. `D:\我的文档\私人\量化\量化交易系统完整设计方案\_V28.md` 的 V28-0 至 V28-8
7. 两个仓的实际根 POM、package manifest/lockfile、Git 状态和工具链输出

## 15. 下一步验收命令

下一步只能在人工批准后执行，并且先记录命令/时间/退出码；当前交接阶段不要自动执行写入型命令：

```powershell
java -version
mvn -version
node --version
pnpm --version
git -C "D:\workspace\quant\RuoYi-Vue-Plus" status --short --branch
git -C "D:\workspace\quant\plus-ui" status --short --branch
git -C "D:\workspace\quant\RuoYi-Vue-Plus" log -1 --format="%H%n%ad%n%s" --date=iso
git -C "D:\workspace\quant\plus-ui" log -1 --format="%H%n%ad%n%s" --date=iso
Get-FileHash "D:\workspace\quant\RuoYi-Vue-Plus\pom.xml" -Algorithm SHA256
Get-FileHash "D:\workspace\quant\plus-ui\package.json" -Algorithm SHA256
Get-FileHash "D:\workspace\quant\plus-ui\pnpm-lock.yaml" -Algorithm SHA256
```

验收重点：JDK21 是否真正被 Maven 使用；JAVA_HOME/Maven/Node/pnpm 组合是否经人工确认；两个仓是否仍 clean；baseline 是否记录实际 commit/hash/toolchain/database version；不得以版本声明替代实测。

## 16. 人工审批点

- [ ] 批准 JDK21/Maven/Node/pnpm 的具体版本和执行方式（不允许模型自行修改系统环境）。
- [ ] 批准是否轮换/撤销前端环境中的私钥和其他凭据，并批准安全注入方案。
- [ ] 批准 SnailJob→XXL-JOB 的迁移窗口、单调度器策略、任务逐项迁移和回滚边界。
- [ ] 批准 Phase 0 目标数据库类型/版本、Flyway V1.x 版本化策略和历史 SQL 的 legacy 隔离。
- [ ] 批准正式 OpenAPI/事件/错误码/菜单契约的生成/审查流程。
- [ ] 在上述审批前，不允许开始业务代码或任何真实交易能力。

## 17. 未解决问题

- 具体 JDK21 安装路径、Maven toolchain 配置和 CI runner 尚未确定。
- 前端 RSA 私钥是否已被使用/共享、是否需要轮换，尚未由人工确认。
- SnailJob 当前生产/测试是否有活跃任务、任务负责人、触发窗口和数据落库影响，未连接系统核实。
- XXL-JOB Admin 独立部署位置、数据库隔离方式、Executor 注册参数、回滚版本尚未批准。
- Phase 0 目标 MySQL 版本和 schema/database 命名尚未冻结；当前审计未连接任何数据库。
- V28 29 个正式 operation 与原型 49 个 API 单元格的最终映射、弃用别名和菜单权限矩阵尚未形成。
- 前端 pnpm 10.34.5 lockfile 安装是否可复现、Node 版本是否需降到 LTS，尚未在批准环境验证。

## 交接完成判定

本交接包仅证明“实际仓库已审计并且阻塞事实已记录”，不证明 Phase 0 已完成；下一位模型必须从 `PHASE0A-BASELINE-ESTABLISHMENT` 开始，先做基线和人工审批，不得直接实现 QuantPF 业务。

