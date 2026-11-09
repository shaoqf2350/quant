# QuantPF Phase 0C Baseline Repair Handoff

## 1. 当前目标

当前目标：`PHASE0C-BASELINE-REPAIR-AND-RUNTIME-GATE-CLOSURE`。本轮仅修复/验收工具链并确认健康接口运行时门禁，不实施业务代码、契约、Flyway 或调度迁移。

当前状态：

```text
PHASE0C-BASELINE-REPAIR-AND-RUNTIME-GATE-CLOSURE: BLOCKED
PHASE0D-CONTRACT-AND-FLYWAY-PLAN: NOT ALLOWED
```

## 2. 原始基线

- 后端：`D:\workspace\quant\RuoYi-Vue-Plus`，分支 `6.X`，HEAD `9227f626a3a37e1546d6c291ed398183e94df92b`，上游偏离 `0/0`；
- 前端：`D:\workspace\quant\plus-ui`，分支 `6.X-Vue`，HEAD `045fc006725c014455def9fed19f93c781633d26`，上游偏离 `0/0`，工作区 clean；
- 根 POM 已接入 `quantpf-modules`；十个 QuantPF POM、健康 Controller 和 BASELINE 已存在；
- 前端 package/lockfile 未修改；
- SnailJob、历史 SQL 和底座配置均未修改。

## 3. 工具链修复结果

默认 shell 仍为 Java 8 / Maven 实际 Java 17；本轮未修改系统环境。已发现并验证本机 JDK21：

```text
C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot
```

仅在临时 PowerShell 进程设置 `JAVA_HOME` 和 PATH 前缀后，Maven 实际使用 Temurin 21.0.12.1。临时环境已恢复，不持久化。

Node 为 v25.2.1，pnpm 为 10.34.5。

## 4. JDK21/Maven 验收结果

- JDK21 `java -version`：Temurin 21.0.12.1 LTS；
- 临时 JDK21 `mvn -version`：Maven 3.9.13，Java 21.0.12.1，Eclipse Adoptium；
- JDK21 `mvn validate`：exit 0；
- JDK21 `mvn compile`：exit 0；
- Reactor 包含十个 QuantPF 模块并全部成功；
- compile 产物 `target/` 仍为 35 个，未清理。

P0-001 的显式 JDK21 构建门禁已通过，但默认 shell 环境漂移仍作为非阻塞 P1/P0-001 残余记录。

## 5. Spring 扫描边界结果

`DromaraApplication` 位于 `org.dromara`，只使用默认 `@SpringBootApplication`，没有 `@ComponentScan` 或 `scanBasePackages`。`QuantPfHealthController` 位于 `com.quantpf.web.controller`，不在默认扫描树内。未发现 QuantPF AutoConfiguration 或组件索引。

## 6. 健康接口运行时结果

`GET /api/quant/health`：`NOT VERIFIED / BLOCKED`。

本轮没有启动应用或发送 HTTP 请求，因为：

- Controller 扫描边界未解决；
- application 配置存在 datasource、Redis、SnailJob 等外部依赖入口；
- 没有经批准的隔离 profile/基础设施证明；
- 启动可能触发外部连接或调度初始化；
- 本轮禁止修改启动类、Controller、YAML、POM 或创建 AutoConfiguration。

## 7. 当前 Git 状态

后端：

```text
## 6.X...origin/6.X
 M pom.xml
 M ruoyi-admin/pom.xml
?? BASELINE.md
?? docs/
?? quantpf-modules/
?? ruoyi-admin/src/main/java/com/
```

前端：

```text
## 6.X-Vue...origin/6.X-Vue
```

后端无 commit/push；前端 clean。

## 8. 当前工程骨架

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

实际 DAG 保持不变：`job -> execution/research/marketdata/backtest`，业务模块向下依赖 domain/common，domain 为 persistence owner。

## 9. 当前调度状态

SnailJob client、自动配置、服务端、任务和配置仍存在；XXL-JOB 未添加。没有启动、注册、心跳或触发任何调度任务。禁止双栈。

## 10. 当前数据库/契约状态

Flyway、正式 OpenAPI、事件 JSON Schema、错误码 YAML 和菜单事实源均未创建。未执行 SQL、Flyway、数据库迁移或生产连接。历史 SQL 仍为 reference-only。

## 11. 已关闭 P0

- P0-001 的 JDK21 显式进程验收：已关闭；validate/compile 在 Java 21.0.12.1 中均成功。

## 12. 未关闭 P0

- P0-002：健康 Controller 不在默认扫描树，运行时未验证；
- P0-003：SnailJob 尚未迁移；
- P0-004：正式契约/Flyway 尚未建立；
- P0-005：敏感配置轮换/撤销执行记录尚未形成。

## 13. 本轮未执行内容

未修改系统工具链；未修改任何代码/POM/YAML/SQL/Vue/TS/JSON；未启动应用；未做 HTTP；未做前端构建；未做数据库、调度、生产或真实交易连接；未清理 target；未提交 Git。

## 14. 下一步目标

在 P0-002 解决前，不允许进入 Phase 0D。下一步应先获得健康接口扫描边界的明确批准，并在隔离、无外部副作用条件下重新进行运行时 HTTP 验证。

若 P0-002 关闭，下一目标为：

```text
PHASE0D-CONTRACT-AND-FLYWAY-PLAN
```

## 15. 下一步建议模型

建议模型：

```text
GPT-5.6 Sol
```

原因：仍涉及 Spring 扫描边界、隔离启动安全、契约和 Flyway 计划门禁，需架构级判断，不适合批量实现模型直接决定。

## 16. 下一步输入文件

1. 本文件；
2. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase0c-baseline-repair.md`；
3. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase0c-baseline-repair-risk-register.md`；
4. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase0c-baseline-repair-commands.log`；
5. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0c-foundation-handoff.md`；
6. `D:\我的文档\私人\量化\量化交易系统完整设计方案_V29.md`；
7. `D:\workspace\quant\量化交易系统完整设计方案\_V28.md`。

## 17. 下一步验收命令

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn -version
mvn -f "D:\workspace\quant\RuoYi-Vue-Plus\pom.xml" -DskipTests=true validate
mvn -f "D:\workspace\quant\RuoYi-Vue-Plus\pom.xml" -DskipTests=true compile
```

只有在人工批准扫描边界并确认隔离环境后，才允许增加健康 HTTP 验证命令。当前不得运行应用。

## 18. 人工审批点

1. 批准 `com.quantpf.web.controller` 的 Spring 扫描接入策略，或批准延期健康接口；
2. 批准隔离启动所需的 profile、临时端口和无外部依赖条件；
3. 批准是否将默认 shell 的 JAVA_HOME/PATH 纳入环境修复任务；
4. 仍需单独批准 SnailJob→XXL-JOB 迁移；
5. 仍需单独批准 Flyway、契约和敏感配置处置。

## 19. 未解决问题

- 健康 Controller 应通过启动类扫描、受控 AutoConfiguration 还是延后到 Web 接入阶段；
- 如何保证启动时不连接 datasource/Redis、不触发 SnailJob；
- 默认 shell JDK8/17 漂移的持久化修复责任人；
- `target/` 的安全清理方案；
- OpenAPI v28/v29 文件名、Flyway 版本和目标数据库 provisioning；
- SnailJob 八个任务的迁移/停用清单。

下一模型必须先读取本文件，不得把显式 JDK21 构建通过误判为运行时健康接口已通过。