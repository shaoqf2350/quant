# PHASE0A-BASELINE-AUDIT

- 审计时间：2026-10-03
- 审计模式：只读仓库审计；未修改 Java/XML/YAML/SQL/Vue/TS/JSON/POM；未提交 Git。
- 审计负责人：QuantPF Phase 0 主控架构师/代码仓库审计负责人

## 审计范围

1. 设计基线：`D:\我的文档\私人\量化\量化交易系统完整设计方案\_V28.md` 的正式章节 V28-0 至 V28-8。
2. 后端底座：`D:\workspace\quant\RuoYi-Vue-Plus`，包括根 POM、指定聚合/应用 POM、SnailJob common/job/server、应用配置、全仓 SQL 与工程树。
3. 前端底座：`D:\workspace\quant\plus-ui`，包括 package manifest、lockfile、Vite、router、permission/menu API、SnailJob 页面、环境文件。
4. 原型：`D:\我的文档\私人\量化\前端功能原型设计` 的 README、页面映射、运行时/样式、V25 core hardening SQL、V25 XXL-JOB migration SQL，并补充清点该目录全部 SQL。
5. 环境：Java/Maven/Node/pnpm 版本、两个代码仓的 Git 分支/工作区/上游偏离、最后提交。

> 所有配置中的密码、Token、API Key、私钥、券商凭据均未在本报告中输出；仅记录是否存在敏感材料及其风险。

## 当前状态摘要

- 后端 revision 为 `6.0.0`；根 Maven modules 仍是 RuoYi 原生五个聚合入口，没有 `quantpf-modules`。
- 根 POM 声明 Java 21、Spring Boot 4.1.1、MyBatis-Plus 3.5.17、dynamic-datasource 4.5.0、Sa-Token 1.46.0、Redisson 4.7.0、SnailJob 2.0.2；当前 Maven 实际运行在 JDK 17，直接 Java 运行时为 JDK 8，环境门禁不通过。
- 后端仍处于 SnailJob 双侧完整存在状态：客户端依赖、自动配置、任务示例、独立服务端、应用配置、前端 iframe 页面均存在；XXL-JOB 在当前底座/原型要求范围内未发现实现文件或依赖。
- `quantpf-modules`、QuantPF Java 包 `com.quantpf.*`、`quantpf-web`、正式 contract、Flyway migration 目录均不存在；没有可直接进入业务实现的 QuantPF 工程骨架。
- 后端现有 SQL 是 RuoYi 脚本集合；原型目录另外存在 4 个 QuantPF 历史/补充 SQL。V25 core hardening SQL 含 `PREPARE/EXECUTE` + `information_schema` 动态 DDL，不能直接作为 V28 Flyway 生产入口；V24/V25 之间存在重复表名体系。
- 原型页面 50 个，页面映射表含 50 个接口单元格、49 个去重后的单元格字符串；V28-2 正式最小 OpenAPI 清单是 29 个 operation。两者不是同一契约，存在路径、方法、范围和菜单差异。
- 工作区：后端和前端均 clean；两者分别在 `6.X`、`6.X-Vue` 分支，均与上游 `0/0` 偏离；本轮未提交 commit。
- 前端环境文件检测到 `VITE_APP_RSA_PRIVATE_KEY` 等敏感配置材料，未输出其值；必须在后续基线建立前处置。

## 实测版本矩阵

| 组件 | 实测/声明版本 | 证据 | 结论 |
|---|---:|---|---|
| RuoYi revision | 6.0.0 | `RuoYi-Vue-Plus/pom.xml` `<revision>`；前端 package version 6.0.0 | 实际底座 revision |
| POM Java 要求 | 21 | 根 POM `<java.version>21</java.version>` | 目标要求 JDK21 |
| `java -version` | OpenJDK 8.0.482 / Zulu 8.92 | 命令实测 | 直接 Java 不满足 |
| `mvn -version` Java | 17.0.1 / Oracle | 命令实测 | Maven 未使用 JDK21 |
| Maven | 3.9.13 | 命令实测 | Maven 本身可执行 |
| Spring Boot | 4.1.1 | 根 POM | 实际声明 |
| MyBatis | 3.5.19 | 根 POM | 实际声明 |
| MyBatis-Plus | 3.5.17 | 根 POM | 实际声明 |
| dynamic-datasource | 4.5.0 | 根 POM | 实际声明 |
| Sa-Token | 1.46.0 | 根 POM | 实际声明 |
| Redisson | 4.7.0 | 根 POM | 实际声明 |
| SnailJob | 2.0.2 | 根 POM；server POM | 实际声明/使用 |
| Vue | 3.5.42 | `plus-ui/package.json` + lockfile | 实际前端依赖 |
| Element Plus | 2.14.5 | package manifest + lockfile | 实际前端依赖 |
| ECharts | 6.1.0 | package manifest + lockfile | 实际前端依赖 |
| Vite | `^8.2.2` | devDependencies | 版本范围而非锁定声明 |
| TypeScript | `^6.0.3` | devDependencies；lockfile importer | 版本范围而非 manifest 固定值 |
| Node | v25.2.1 | 命令实测 | 非项目文档中冻结的 LTS 基线 |
| pnpm | 11.19.0 | 命令实测 | 与 packageManager `pnpm@10.34.5` 不一致 |

## 实测哈希矩阵

| 文件 | 大小（bytes） | SHA-256 |
|---|---:|---|
| `D:\workspace\quant\RuoYi-Vue-Plus\pom.xml` | 25073 | `CBD3740F955E8B911754351C7A70B549626C2E2A20C8546155BA39772635FBEE` |
| `D:\workspace\quant\plus-ui\package.json` | 2339 | `D1376599B7C4736C081EC9BEAA7354E236C1D4DE371C8B16ECB0E6F13E871BE1` |
| `D:\workspace\quant\plus-ui\pnpm-lock.yaml` | 175408 | `C29BBB60A733B8F09ECAB8DE3697A51EE5340F45A1AE7E1D9739D3C3DF1C1F05` |

完整文件级清单、存在性、大小和哈希见 `phase0a-file-inventory.json`。

## 实际 Maven 模块树

```text
root modules
├─ ruoyi-admin
├─ ruoyi-common
├─ ruoyi-extend
├─ ruoyi-modules
└─ ruoyi-api

ruoyi-common modules
├─ ruoyi-common-bom
├─ ruoyi-common-social
├─ ruoyi-common-core
├─ ruoyi-common-doc
├─ ruoyi-common-excel
├─ ruoyi-common-job
├─ ruoyi-common-log
├─ ruoyi-common-mail
├─ ruoyi-common-mybatis
├─ ruoyi-common-oss
├─ ruoyi-common-redis
├─ ruoyi-common-satoken
├─ ruoyi-common-security
├─ ruoyi-common-sms
├─ ruoyi-common-elasticsearch
├─ ruoyi-common-web
├─ ruoyi-common-translation
├─ ruoyi-common-sensitive
├─ ruoyi-common-json
├─ ruoyi-common-encrypt
├─ ruoyi-common-push
├─ ruoyi-common-liteflow
├─ ruoyi-common-mqtt
├─ ruoyi-common-ai
└─ ruoyi-common-mcp

ruoyi-modules modules
├─ ruoyi-demo
├─ ruoyi-gen
├─ ruoyi-job
├─ ruoyi-system
├─ ruoyi-workflow
└─ ruoyi-ai

ruoyi-extend modules
├─ ruoyi-monitor-admin
├─ ruoyi-snailai-server
└─ ruoyi-snailjob-server
```

- 根 POM 实际共 5 个直接 module；全仓发现 40 个 `pom.xml` 文件。
- `quantpf-modules`：不存在。
- `quantpf-web`：不存在。
- `ruoyi-ui`：后端仓内不存在；实际前端底座是独立仓 `D:\workspace\quant\plus-ui`。
- QuantPF Java 代码：未发现 `com.quantpf.*` package/import 或含 QuantPF 的后端文件。

## 实际前端结构

- `src/router/index.ts` 使用 RuoYi 常规常量路由 + 后台动态路由；动态路由由 `src/api/menu.ts` 的 `/system/menu/getRouters` 获取，`src/store/modules/permission.ts` 负责过滤、添加和权限路由。
- `src/views/monitor/snailjob/index.vue` 以 iframe 包装 `VITE_APP_SNAILJOB_ADMIN`；开发环境值指向 SnailJob 8800/context，生产环境值为 `/snail-job`。
- package scripts 有 `lint`、`build`、`build:dev`、`build:prod`、`dev`、`preview`，没有 `test` script。
- 前端依赖实际为 Vue 3.5.42、Element Plus 2.14.5、ECharts 6.1.0、Vite `^8.2.2`、TypeScript `^6.0.3`；packageManager 声明 pnpm 10.34.5，但机器实测 pnpm 11.19.0。
- `.env.development` / `.env.production` 均包含 RSA 公钥/私钥配置键；值未在审计输出中复制。将私钥放入 Vite `VITE_` 前端环境会进入客户端构建边界，属于 P0 敏感配置风险。

## SnailJob 现状

| 项目 | 实际证据 | 结论 |
|---|---|---|
| 依赖 | 根 POM 管理 `com.aizuda:snail-job-client-starter/client-retry-core/client-job-core` 2.0.2；server POM 引入 `snail-job-server-starter` 2.0.2 | SnailJob 已是底座现行调度栈 |
| 自动配置 | `ruoyi-common/ruoyi-common-job/.../SnailJobConfig.java` 使用 `@AutoConfiguration`、`@ConditionalOnProperty(prefix="snail-job", name="enabled", havingValue="true")`、`@EnableSnailJob`、`@EnableScheduling` | 运行期开关和自动接入存在 |
| 服务端 | `ruoyi-extend/ruoyi-snailjob-server` 存在 server application、filter/security、application 配置；默认 context `/snail-job`、server port 8800、RPC port 17888 | 独立 SnailJob 服务端存在 |
| 任务 | `ruoyi-modules/ruoyi-job` 下有 8 个 `@JobExecutor` 命中，另有 Alipay/Summary/Wechat 等示例任务，Java 文件共 9 个与 SnailJob 相关 | 示例/业务任务仍在 |
| 配置 | `ruoyi-admin` dev/prod 都存在 `snail-job` 段，含 group、token、namespace、server host/port 等键；敏感值未输出 | 应用默认仍接 SnailJob |
| 前端 | `src/views/monitor/snailjob/index.vue` + `VITE_APP_SNAILJOB_ADMIN` | 页面未清理 |

## XXL-JOB 现状

- 当前后端扫描未发现 `xxl-job`/`XxlJob` 依赖、Java 配置、Executor、Admin 模块或配置文件。
- 原型目录存在 `quantpf_v25_xxljob_migration.sql`，但它是历史原型 SQL，不代表代码仓已接入 XXL-JOB。
- 因此当前不是“SnailJob + XXL-JOB 已并存的运行双栈”，而是“SnailJob 已运行资产存在、XXL-JOB 仅有迁移设计/SQL 参考”的迁移前状态；若直接新增 XXL-JOB 而不先冻结/审批，会形成双栈风险。

## 数据库/迁移现状

### 当前 SQL

- 后端 `script/sql` 下发现 14 个 SQL：RuoYi MySQL/Oracle/PostgreSQL/SQL Server 基础脚本及 job/ai/workflow 脚本。
- 原型目录发现 4 个 QuantPF SQL：`quantpf_v23_frontend_supplement.sql`、`quantpf_v24_core_hardening.sql`、`quantpf_v25_core_hardening.sql`、`quantpf_v25_xxljob_migration.sql`。
- 当前没有 `db/migration`、Flyway V1.x 文件或 `contract` 目录。

### 补充 SQL 与安全检查

- `quantpf_v25_core_hardening.sql` 创建 10 张 `qt/qm/qs/qa` 前缀表，并包含针对既有表的 14 条动态 `ALTER TABLE` 构造，使用 `information_schema`、`PREPARE`、`EXECUTE`、`DEALLOCATE PREPARE`。
- `quantpf_v25_xxljob_migration.sql` 创建 `qo_job_execution`、`qo_job_migration_audit` 两表。
- 实际可执行语句未发现 `USE`、`CREATE DATABASE` 或 `DROP TABLE`；文件注释中出现禁止语句文字，不能据此误判为执行动作。
- 原型 SQL 的 V24/V25 之间重复创建 10 个同名 QuantPF 表，说明存在重复表名体系；V23 另有 `qn_*`、`qs_*`、`qt_*`、`quantpf_ui_menu` 等体系。
- 当前没有可确认的“QuantPF 业务数据库”或生产数据库连接；应用配置指向本地 `ry-vue` 数据库形态，审计未连接任何数据库。

## 契约现状

- OpenAPI 正式文件：不存在（V28 期望 `contract/openapi/quantpf-v28.yaml`）。
- 事件 JSON Schema：不存在（V28 期望 4 个 `.v1.schema.json`）。
- 错误码 YAML：不存在（V28 期望 `contract/error-codes/quantpf-error-codes.yaml`）。
- 菜单事实源：不存在（V28 期望 `contract/menu/quantpf-menu-facts.yaml`）。
- 菜单实际事实源目前仍是 RuoYi 的数据库菜单接口 `/system/menu/getRouters` + 前端动态路由过滤。
- 原型 API：50 个页面映射行、50 个接口单元格、49 个去重字符串单元格；其中部分单元格包含多个方法或聚合描述，不能直接当作 49 个 operation。
- V28 API：V28-2.1 正式最小闭环列出 29 个 method/path operation。
- 不一致项（代表性）：
  - 原型 `/instrument/list`、`/kline/query`、`/orderbook/latest/{symbol}` 与 V28 `/instrument/page`、`/market/kline`、`/market/order-book/latest` 不一致；
  - 原型包含 news latest/detail/risk、finance calendar/peers、broker config/status/log、data source/quality、strategy runtime/performance 等扩展接口，V28-2.1 29 项未覆盖这些原型扩展；
  - V28 明确的 `/account/{id}/ledger`、`GET /signal/page` 等，在原型映射中没有同名正式项；
  - 原型中 `GET/PUT`、`GET/POST`、`actuator/prometheus + 聚合API`、`RuoYi角色/菜单接口` 等复合单元不能直接生成唯一 OpenAPI operation。

## 原型现状

- 原型 README 明确说明：使用模拟数据、不发起真实交易/网络请求，XXL-JOB 是唯一调度框架，SnailJob 只在迁移说明中出现；该声明与当前实际 RuoYi 底座仍保留 SnailJob 的事实冲突。
- `pages/` 实际有 50 个 HTML 页面；页面覆盖驾驶舱、行情、新闻、财报、策略、回测、账户/订单、风控、券商连接、数据源、作业和审计等。
- `assets/app.js` 约 54 KB、94 行（高度压缩/长行）并包含原型模拟数据/交互；`assets/styles.css` 约 18 KB、单行。
- 原型只能作为 UI/字段/交互参考，不得直接作为生产 API、SQL 或调度事实源。

## 环境构建现状

| 门禁 | 结果 | 证据/原因 |
|---|---|---|
| Maven 是否实际使用 JDK21 | **否** | 根 POM 要求 21，但 `mvn -version` 显示 Java 17.0.1；`java -version` 另显示 Java 8.0.482 |
| 后端构建 | **不可批准执行** | 本轮只读限制下未执行会生成 `target/`/缓存的构建；在当前 Maven JDK17 与 POM Java21 不匹配时，不能宣称可构建 |
| 前端 lint | **未执行** | 为遵守仅允许向两个 docs 目录写入的限制，不运行可能触碰缓存/工作区的工具；静态上有 `lint` script |
| 前端 build | **未执行/不可批准** | `build` 会写出产物目录；当前 pnpm 11 与 manifest pnpm 10.34.5 不一致 |
| 前端 test | **不可用** | `package.json` 没有 `test` script；存在 vitest 依赖但没有 test 命令入口 |
| 工作区修改 | **通过** | 后端/前端 `git status --short --branch` 均无未提交文件 |
| 分支冲突 | **未发现** | 后端 `6.X...origin/6.X`，前端 `6.X-Vue...origin/6.X-Vue`；两仓 `HEAD...@{u}` 均 `0 0` |
| 敏感配置 | **P0 风险** | 前端 `.env.*` 存在 RSA private-key 配置键；后端配置存在密码/Token 类配置键。值未输出 |

## P0 阻塞项

1. **P0-001 工具链不满足 V28 门禁**：POM 需要 Java 21，Maven 实际 Java 17，直接 Java 实际 Java 8；必须先建立并人工批准 JDK21/Maven 基线，禁止在此状态声称后端可构建。
2. **P0-002 QuantPF 正式骨架不存在**：无 `quantpf-modules`、无 `com.quantpf.*` Java、无 `quantpf-web`，业务代码没有合规落点。
3. **P0-003 调度迁移尚未开始**：SnailJob 客户端/服务端/任务/配置/页面全部存在，XXL-JOB 仅有历史 SQL；必须先形成迁移设计、停双栈门禁和人工审批。
4. **P0-004 正式迁移/契约缺失**：无 Flyway `db/migration`、无 OpenAPI、无事件 Schema、无错误码 YAML、无菜单事实源；不得直接实现业务 Controller/Mapper 或执行历史 SQL。
5. **P0-005 敏感配置泄漏风险**：前端环境文件包含私钥配置键且 `VITE_` 暴露边界不适合私钥；后续必须由人工确认轮换/移除/改为安全注入，报告不输出值。

## P1 问题

- pnpm 实测 11.19.0 与 manifest 声明 10.34.5 不一致；Node 25.2.1 也未形成项目冻结基线。
- 原型 50 页/49 个 API 单元格与 V28 29 个正式 operation 不一致，必须建立映射矩阵和弃用策略。
- 原型 README 的“XXL-JOB 唯一调度”与底座实际 SnailJob 状态冲突。
- V24/V25 SQL 重复创建同名表，且 V25 动态 DDL 不能直接纳入 Flyway；历史 SQL 尚未隔离到 legacy/reference。
- 任务模块仍保留 8 个 `@JobExecutor` 示例和多个 SnailJob 任务类，迁移前不能增加 QuantPF 调度业务。

## P2 问题

- `assets/app.js`、`assets/styles.css` 为压缩/单行原型资源，审计可读性较低；正式前端不应复制原型实现。
- 前端当前菜单事实源仍依赖后端菜单 API，尚无契约生成与 CI 校验链。
- 后端 SQL 同时维护 MySQL/Oracle/PostgreSQL/SQL Server 脚本，数据库基线尚未被 QuantPF Phase 0 约束为单一可迁移入口。
- 项目有 SnailAI 等扩展资产，会增加迁移/模块边界审计成本；本轮未触碰其业务能力。

## 明确结论：能否直接开始业务代码

**不能。** 当前只能进入 `PHASE0A-BASELINE-ESTABLISHMENT` 的环境/分支/基线建立和人工审批阶段；在 JDK21、调度迁移策略、Flyway/契约正式文件、敏感配置处置和 QuantPF 模块骨架完成前，不允许开始股票/期货业务代码，不允许生成真实交易/QMT/CTP/LIVE 代码，也不允许直接执行历史 SQL。

## 建议的 Phase 0 执行顺序

1. `PHASE0A-BASELINE-ESTABLISHMENT`：确认两个仓的 revision、commit、分支、工作区、JDK21/Maven/Node/pnpm、数据库版本；建立 `BASELINE.md`，人工批准后再继续。
2. 处理敏感配置：轮换/撤销前端暴露的私钥及相关凭据，确认安全注入方式；只做人工批准后的受控修改。
3. 调度门禁设计：冻结 SnailJob 资产，完成 SnailJob→XXL-JOB 迁移方案、双栈禁止规则和回滚方案；本轮不要添加 XXL-JOB 依赖。
4. 建立 QuantPF 最小工程骨架：根 POM/`quantpf-modules`/最小模块，严格遵守 V28-0 包名和依赖边界。
5. 建立 Flyway Phase 0：把历史 SQL 放入 legacy/reference，编写可验证的 V1.x 迁移，禁止动态 DDL/破坏性回滚/直接生产执行。
6. 建立正式契约：OpenAPI、事件 Schema、错误码、菜单事实源，并生成/校验前后端样板。
7. 建立健康接口、CI 和验收门禁；全部通过后才可进入 Phase 1 Paper MVP。

## 只读审计产物

- `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase0a-baseline-audit.md`
- `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase0a-file-inventory.json`
- `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase0a-commands.log`
- `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase0a-risk-register.md`
- `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\handoff\phase0-baseline-handoff.md`

