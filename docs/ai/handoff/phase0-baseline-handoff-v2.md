# QuantPF Phase 0 Baseline Handoff v2

- 日期：2026-10-03
- 当前目标：PHASE0A-BASELINE-ESTABLISHMENT
- 当前状态：**工具链基线通过；允许进入 PHASE0B-FOUNDATION-SKELETON-PLAN 计划阶段**。
- 本文件用途：下一阶段模型的唯一基线交接入口。

## 1. 本轮完成内容

- 修复 Java/Maven 工具链，使 Maven 实际运行于 JDK21；
- 将 pnpm 修复为项目 manifest 声明的 10.34.5；
- 重新确认 Node、Java、Maven、pnpm；
- 重新执行 Maven Reactor `validate`，40 个工程全部成功；
- 重新确认两个仓的 commit、分支、工作区和上游偏离；
- 重新计算后端 POM、前端 package.json、前端 lockfile 哈希；
- 生成 V29 完整方案：`D:\我的文档\私人\量化\量化交易系统完整设计方案_V29.md`；
- 更新基线报告、批准记录和命令日志；
- 未修改业务代码、根 POM、配置、SQL、前端源文件；
- 未执行数据库、调度器或生产系统操作。

## 2. 已批准决策

用户已批准：

1. JDK21/Maven/Node/pnpm 工具链基线确认；
2. 前端 RSA 私钥及其他敏感配置的轮换/撤销与安全注入方案；
3. SnailJob→XXL-JOB 的迁移窗口、任务清单、停止策略和回滚边界；
4. 禁止 SnailJob 与 XXL-JOB 双栈同时触发同一业务任务；
5. Phase 0 目标 MySQL 8.x、逻辑命名基线 `quantpf`、Flyway 版本化迁移原则；
6. 历史 SQL 进入 legacy/reference，不作为生产入口；
7. OpenAPI、事件 Schema、错误码、菜单事实源的契约审查流程。

## 3. 实际工具链状态

| 项目 | 实际状态 | 门禁 |
|---|---|---|
| `JAVA_HOME` | `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` | 通过 |
| Java | OpenJDK/Temurin 21.0.12.1 LTS | 通过 |
| Maven | 3.9.13 | 通过 |
| Maven runtime Java | 21.0.12.1，Eclipse Adoptium | 通过 |
| Node | v25.2.1 | 满足当前 Vite/Vitest engine；建议后续评估 LTS |
| pnpm | 10.34.5 | 与 packageManager 一致 |
| Maven validate | 40 个 Reactor 工程全部 SUCCESS | 通过 |

工具链 P0-001 已解除。`mvn -version` 已明确输出 Java 21.0.12.1。

## 4. 实际 Git 状态

- 后端：`D:\workspace\quant\RuoYi-Vue-Plus`；`6.X...origin/6.X`；HEAD `9227f626a3a37e1546d6c291ed398183e94df92b`；上游偏离 `0 0`；无已跟踪代码修改，仅有允许目录内的文档。
- 前端：`D:\workspace\quant\plus-ui`；`6.X-Vue...origin/6.X-Vue`；HEAD `045fc006725c014455def9fed19f93c781633d26`；上游偏离 `0 0`；工作区 clean。
- 未执行 Git commit、push、reset、clean、checkout、switch、merge 或 rebase。

## 5. 当前技术基线

- revision：6.0.0；
- Spring Boot：4.1.1；
- MyBatis：3.5.19；
- MyBatis-Plus：3.5.17；
- dynamic-datasource：4.5.0；
- Sa-Token：1.46.0；
- Redisson：4.7.0；
- SnailJob：2.0.2；
- Vue：3.5.42；
- Element Plus：2.14.5；
- ECharts：6.1.0；
- Vite：`^8.2.2`；
- TypeScript：`^6.0.3`；
- packageManager：`pnpm@10.34.5`。

关键文件哈希：

- 后端 `pom.xml`：`CBD3740F955E8B911754351C7A70B549626C2E2A20C8546155BA39772635FBEE`；
- 前端 `package.json`：`D1376599B7C4736C081EC9BEAA7354E236C1D4DE371C8B16ECB0E6F13E871BE1`；
- 前端 `pnpm-lock.yaml`：`C29BBB60A733B8F09ECAB8DE3697A51EE5340F45A1AE7E1D9739D3C3DF1C1F05`。

## 6. 调度迁移基线

- SnailJob 仍是实际底座：依赖、自动配置、服务端、任务、配置和前端页面均存在；
- XXL-JOB 当前未接入代码仓；原型 XXL-JOB SQL 仅为历史参考；
- 迁移采用先盘点、冻结新任务、隔离验证、切换窗口停旧启新、失败单调度器回滚的方式；
- 不允许 SnailJob 与 XXL-JOB 同时触发同一业务任务；
- 本轮未添加 XXL-JOB 依赖、未删除 SnailJob、未启停任何调度器。

## 7. 数据库/Flyway 基线

- 目标引擎：MySQL 8.x；
- 逻辑命名基线：`quantpf`；
- Flyway 负责版本化迁移；数据库/schema provisioning 不由 migration 完成；
- 禁止 `CREATE DATABASE`、`USE quantpf`、`DROP TABLE`、动态 `PREPARE ALTER` 和以 `IF NOT EXISTS` 掩盖 schema drift；
- 历史 QuantPF SQL 只能进入 legacy/reference；
- V28/V29 目标迁移目录为 `db/migration/`，本轮未创建；
- 本轮未连接数据库、未执行 SQL、未执行 Flyway。

## 8. 契约基线

- 正式 OpenAPI、事件 JSON Schema、错误码 YAML、菜单事实源仍不存在；
- 原型有 50 个页面、49 个去重 API 单元格；V28 正式最小闭环有 29 个 operation；
- 下一阶段施工前必须先完成契约文件计划和 API/原型映射计划；
- 本轮未创建契约文件、未生成接口、未修改菜单。

## 9. 敏感配置处置基线

- 前端 `.env.development`、`.env.production` 存在 RSA private-key 配置键；
- 后端配置存在密码/Token 类配置键；
- 用户已批准轮换/撤销和安全注入方向；
- 本轮没有读取、复制、输出、轮换或删除任何敏感值；
- 后续只允许记录敏感键、状态、时间、指纹和审批信息，不记录原始值。

## 10. 仍存在的 P0 阻塞项

工具链 P0-001 已解除，但以下 P0 仍存在：

1. QuantPF 工程骨架不存在；
2. SnailJob 迁移未实施，XXL-JOB 尚未接入；
3. 正式契约和 Flyway 入口不存在；
4. 敏感配置尚未完成轮换/撤销/安全注入；
5. 历史 SQL 尚未完成 legacy/reference 隔离。

## 11. 本轮未执行内容

- 未修改 Java、XML、YAML、SQL、Vue、TS、JSON、POM；
- 未创建 `quantpf-modules`；
- 未修改根 POM；
- 未删除 SnailJob；
- 未添加 XXL-JOB 依赖；
- 未执行完整后端 compile/test/package/verify/install/deploy；
- 未执行 `pnpm install`、`pnpm lint`、`pnpm build`、`pnpm test`；
- 未执行数据库/Flyway/Docker；
- 未连接生产数据库或真实交易系统；
- 未输出任何真实敏感值；
- 未提交 Git commit。

## 12. 下一步目标

下一目标：

```text
PHASE0B-FOUNDATION-SKELETON-PLAN
```

工具链基线已允许进入该阶段，但下一阶段必须是计划级架构审查，不是批量代码实现。

## 13. 下一步禁止

- 直接创建 QuantPF 业务代码；
- 直接修改根 POM；
- 直接创建 `quantpf-modules`；
- 直接修改 SnailJob；
- 直接添加 XXL-JOB 依赖；
- 直接创建数据库迁移；
- 直接创建 OpenAPI 实现代码；
- 创建 QMT/CTP；
- 启用 LIVE。

## 14. 下一步输入文件

1. 本文件；
2. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase0a-baseline-establishment.md`；
3. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase0a-baseline-approval.md`；
4. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase0a-baseline-establishment-commands.log`；
5. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase0a-baseline-audit.md`；
6. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase0a-risk-register.md`；
7. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase0a-file-inventory.json`；
8. `D:\我的文档\私人\量化\量化交易系统完整设计方案_V29.md`；
9. `D:\我的文档\私人\量化\量化交易系统完整设计方案\_V28.md`。

## 15. 下一步验收命令

```powershell
java -version
mvn -version
node --version
pnpm --version

mvn -f "D:\workspace\quant\RuoYi-Vue-Plus\pom.xml" -DskipTests=true validate

git -C "D:\workspace\quant\RuoYi-Vue-Plus" status --short --branch
git -C "D:\workspace\quant\plus-ui" status --short --branch
Get-FileHash "D:\workspace\quant\RuoYi-Vue-Plus\pom.xml" -Algorithm SHA256
Get-FileHash "D:\workspace\quant\plus-ui\package.json" -Algorithm SHA256
Get-FileHash "D:\workspace\quant\plus-ui\pnpm-lock.yaml" -Algorithm SHA256
```

## 16. 未解决问题

- Node v25.2.1 不是长期支持版本，Phase 0 CI 计划需决定是否冻结到受支持 LTS；
- 敏感配置轮换/撤销的执行人、时间、系统和指纹记录尚未产生；
- XXL-JOB Admin/Executor 的部署位置、版本和隔离数据库尚未冻结；
- MySQL 8.x 的具体 minor/patch 版本尚未记录；
- `quantpf` schema provisioning 尚未执行；
- 正式契约文件、Flyway 文件和 QuantPF 模块骨架仍不存在；
- 前端 lint/build/test 尚未执行。

## 17. 交接结论

`PHASE0A-TOOLCHAIN-BASELINE: PASS`

下一目标：`PHASE0B-FOUNDATION-SKELETON-PLAN`

建议模型：`GPT-5.6 Sol`

原因：Phase 0 工程骨架、模块边界、POM 变更清单、调度迁移文件清单、契约和 Flyway 施工顺序仍需要架构审查，不能交给批量实现模型直接修改。
