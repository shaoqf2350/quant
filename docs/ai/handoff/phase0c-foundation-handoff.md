# QuantPF Phase 0C Foundation Handoff

## 1. 当前目标

当前阶段：`PHASE0C-FOUNDATION-SKELETON-IMPLEMENTATION`。目标是完成 QuantPF 最小 Maven 工程骨架、最小 admin 接入、静态健康接口和可追踪基线，不进入业务实现。

当前状态：`IMPLEMENTED-BUT-BLOCKED`。

## 2. 本轮实际修改

已按批准范围修改/创建：

- `D:\workspace\quant\RuoYi-Vue-Plus\pom.xml`：增加 `quantpf-modules`；
- `D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\pom.xml`：增加六个 QuantPF application-facing 依赖；
- `D:\workspace\quant\RuoYi-Vue-Plus\quantpf-modules\pom.xml`；
- `quantpf-modules` 下十个模块 POM；
- `D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\src\main\java\com\quantpf\web\controller\QuantPfHealthController.java`；
- `D:\workspace\quant\RuoYi-Vue-Plus\BASELINE.md`；
- Phase 0C 报告、命令日志、风险登记和本交接文件。

未修改前端仓库、既有 SnailJob 文件、既有启动类、SQL、Flyway、契约或业务 Java。

## 3. 实际模块树

```text
quantpf-modules/
├── pom.xml
├── quantpf-common/pom.xml
├── quantpf-domain/pom.xml
├── quantpf-marketdata/pom.xml
├── quantpf-research/pom.xml
├── quantpf-strategy/pom.xml
├── quantpf-risk/pom.xml
├── quantpf-finance/pom.xml
├── quantpf-execution/pom.xml
├── quantpf-backtest/pom.xml
└── quantpf-job/pom.xml
```

## 4. 实际依赖 DAG

```text
quantpf-job        -> quantpf-execution, quantpf-research, quantpf-marketdata, quantpf-backtest
quantpf-backtest   -> quantpf-strategy, quantpf-risk, quantpf-domain, quantpf-common
quantpf-execution  -> quantpf-risk, quantpf-domain, quantpf-common
quantpf-finance    -> quantpf-research, quantpf-domain, quantpf-common
quantpf-research   -> quantpf-marketdata, quantpf-domain, quantpf-common
quantpf-strategy   -> quantpf-domain, quantpf-common
quantpf-risk       -> quantpf-domain, quantpf-common
quantpf-marketdata -> quantpf-domain, quantpf-common
quantpf-domain     -> quantpf-common
```

## 5. 实际 POM 变更

- 根 Reactor 已包含 `quantpf-modules`。
- `ruoyi-admin` 已接入 `quantpf-job`、`quantpf-execution`、`quantpf-backtest`、`quantpf-research`、`quantpf-finance`、`quantpf-marketdata`。
- 未增加 XXL-JOB、Flyway、QMT 或 CTP 依赖。
- POM XML 解析和 Maven Reactor 验收通过。

## 6. ruoyi-admin 接入

新增的健康接口是 `GET /api/quant/health`，返回静态 `UP`/`PHASE0C`/`liveEnabled=false`。它不访问数据库、Redis、Broker、调度器或敏感配置。

注意：`DromaraApplication` 默认扫描根为 `org.dromara`，而 Controller 包为 `com.quantpf.web.controller`。因此仅有编译成功，不能证明运行时会注册；该问题是当前 P0 阻塞。

## 7. BASELINE.md 状态

`BASELINE.md` 已修复，包含实际仓库 HEAD、分支、上游偏离、当前 POM/package/lockfile 哈希、工具链实测和禁止事项。原先的控制字符及 `$rootHash` 等占位符已清除。文件不包含任何敏感值。

## 8. 健康接口状态

编译：通过。

运行时：未验证。

不允许下一模型在未获批准前擅自修改 `DromaraApplication.java`、扩大 component scan 或创建自动配置。

## 9. SnailJob/XXL-JOB 状态

SnailJob 仍存在且未修改；XXL-JOB 尚未接入。本阶段没有启动调度器或触发任务。任何迁移必须单独建立任务清单、停止策略、单调度器切换和回滚边界。

## 10. 数据库/Flyway 状态

未创建或执行 Flyway；未执行历史 SQL；未连接生产或其他真实业务数据库。目标数据库、版本、schema 和 migration 文件仍属于下一阶段计划/审批范围。

## 11. 契约状态

正式 OpenAPI、事件 JSON Schema、错误码 YAML 和菜单事实源尚未创建。原型 API 仍是 reference，不得直接生成业务代码。

## 12. 验收结果

- Maven validate：exit 0；
- Maven compile：exit 0；
- 正确引用 `-Dincludes=com.quantpf:*` 的 dependency tree：exit 0；
- POM XML 和 Phase 0C manifest：可解析；
- 后端 QuantPF 模块为 10 个，Reactor 已接入；
- compile 生成 35 个 `target/` 目录，未删除；
- 当前 shell 工具链：Java8/Maven实际Java17，不满足 Java21 门禁；
- Git commit/push：未执行。

## 13. P0/P1 风险

P0：

1. 当前 shell 未满足 JDK21，必须重新建立可复现的 JDK21 进程基线；
2. 健康 Controller 未被现有默认扫描根覆盖，运行时未验证；
3. SnailJob 尚未迁移，不能添加 XXL-JOB 或形成双栈；
4. Flyway 和正式契约尚未创建；
5. 敏感配置轮换/撤销执行记录仍未形成。

P1：Node v25.2.1 非 LTS；QuantPF 模块仍为空骨架；compile 产物未清理；ruoyi-admin 接入边界需持续审查。

## 14. 本轮未执行内容

未启动应用、未做 HTTP 验证、未修改启动类、未做前端构建/测试、未做数据库/调度操作、未做 Flyway/契约、未做破坏性清理、未提交 Git。

## 15. 下一步目标

下一目标：

```text
PHASE0D-CONTRACT-AND-FLYWAY-PLAN
```

但必须先关闭本交接中的 P0-001 和 P0-002；P0-003 至 P0-005 继续作为 Phase 0 后续独立门禁。

## 16. 下一步建议模型

建议模型：

```text
GPT-5.6 Sol
```

原因：下一步仍是环境、契约、Flyway、扫描边界和迁移门禁的架构审查，不能交给批量实现模型直接决定。

## 17. 下一步输入文件

1. 本文件；
2. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase0c-foundation-skeleton-implementation.md`；
3. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase0c-risk-register.md`；
4. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase0c-file-manifest.json`；
5. `D:\我的文档\私人\量化\量化交易系统完整设计方案_V29.md`；
6. Phase 0A/0B 交接、审计、计划和命令日志；
7. 当前实际 root/common/modules/extend/admin POM；
8. 原型 README、页面映射及历史 SQL（只读 reference）。

## 18. 下一步验收命令

```powershell
java -version
mvn -version
node --version
pnpm --version
mvn -f "D:\workspace\quant\RuoYi-Vue-Plus\pom.xml" -DskipTests=true validate
mvn -f "D:\workspace\quant\RuoYi-Vue-Plus\pom.xml" -DskipTests=true compile
mvn -f "D:\workspace\quant\RuoYi-Vue-Plus\pom.xml" -DskipTests=true -pl quantpf-modules/quantpf-job -am dependency:tree '-Dincludes=com.quantpf:*'
git -C "D:\workspace\quant\RuoYi-Vue-Plus" status --short --branch
git -C "D:\workspace\quant\plus-ui" status --short --branch
git -C "D:\workspace\quant\RuoYi-Vue-Plus" diff --stat
```

执行前必须确认 JDK21 进程环境，且不得连接生产、触发调度或执行数据库迁移。

## 19. 人工审批点

1. 选择并批准健康 Controller 的运行时扫描边界；
2. 重新批准/确认 JDK21、Maven、JAVA_HOME 的实际进程环境；
3. 批准 Phase 0D 仅做契约与 Flyway 计划，不执行迁移；
4. 后续另行批准 SnailJob→XXL-JOB 的任何依赖、任务停用和切换；
5. 后续另行批准敏感配置轮换/撤销执行；
6. 保持“不连接生产、不启用 LIVE、不创建 QMT/CTP、不提交 Git”的边界。

## 20. 未解决问题

- 当前 shell 为什么回落到 Java 8/17，需由环境负责人确认，不得在本任务中擅自修改系统配置；
- `com.quantpf.web.controller` 的合规 Spring 扫描方案；
- Flyway 版本、目标数据库实例和 schema provisioning；
- OpenAPI v28/v29 文件名和兼容策略；
- SnailJob 八个任务的逐任务迁移/停用清单；
- 前端敏感配置轮换/撤销的执行凭证（只记录结果，不记录秘密）；
- `target/` 清理的非破坏性审批方案。

下一模型只能以本文件为唯一交接入口，先解决 P0，不得直接进入业务开发。