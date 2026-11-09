# QuantPF Phase 0C Web Access Gate Handoff

## 1. 当前目标

目标：`PHASE0C-WEB-ACCESS-GATE-IMPLEMENTATION-AND-RUNTIME-VERIFICATION`。

当前状态：

```text
PHASE0C-WEB-ACCESS-GATE-IMPLEMENTATION-AND-RUNTIME-VERIFICATION: BLOCKED
PHASE0D-CONTRACT-AND-FLYWAY-PLAN: NOT ALLOWED
```

## 2. 用户批准事项

用户批准了三种候选方案，并明确批准本轮采用方案 A：修改启动类扫描边界。本轮没有同时实施 AutoConfiguration 或调整 Controller 位置。

## 3. 原始扫描边界

原启动类：

```text
D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\src\main\java\org\dromara\DromaraApplication.java
```

原注解：

```java
@SpringBootApplication
```

默认扫描根为 `org.dromara`。健康 Controller 位于 `com.quantpf.web.controller`，原本不在扫描树内。

## 4. 实际选择的接入方案

采用方案 A：

```java
@SpringBootApplication(scanBasePackages = {"org.dromara", "com.quantpf.web.controller"})
```

只增加精确健康 Controller 包，不扫描整个 `com.quantpf`。

## 5. 实际代码变更

修改：

```text
D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\src\main\java\org\dromara\DromaraApplication.java
```

未修改：

- 根 POM；
- ruoyi-admin POM；
- QuantPfHealthController；
- YAML/XML/SQL/Vue/TS/JSON；
- SnailJob/XXL-JOB；
- 前端仓库。

## 6. JDK21/Maven 验收结果

使用显式临时 JDK21：

```text
C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot
```

结果：

- Maven 3.9.13 实际 Java 21.0.12.1；
- `mvn validate`：exit 0；
- `mvn compile`：exit 0；
- `DromaraApplication.class` 和 `QuantPfHealthController.class` 均存在。

## 7. 健康接口运行时结果

接口：

```text
GET /api/quant/health
```

状态：

```text
NOT VERIFIED / BLOCKED
```

本轮没有安全启动应用，也没有发送 HTTP，因为当前没有经批准的无数据库、无 Redis、无 SnailJob 心跳的隔离 profile。不能使用现有开发/生产配置进行未经隔离的启动。

## 8. 当前 Git 状态

后端：

```text
## 6.X...origin/6.X
 M pom.xml
 M ruoyi-admin/pom.xml
 M ruoyi-admin/src/main/java/org/dromara/DromaraApplication.java
?? BASELINE.md
?? docs/
?? quantpf-modules/
?? ruoyi-admin/src/main/java/com/
```

前端：

```text
## 6.X-Vue...origin/6.X-Vue
```

未提交 Git。

## 9. 当前工程骨架

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

## 10. 当前调度状态

SnailJob 仍存在；XXL-JOB 未添加；未启动、注册、发送心跳或触发任何任务；禁止双栈。

## 11. 当前数据库和契约状态

未创建或执行 Flyway；未执行 SQL；未连接数据库。正式 OpenAPI、事件 Schema、错误码 YAML、菜单事实源均未创建。

## 12. 已关闭 P0

- 原 P0-002 的**静态扫描边界缺陷**已通过方案 A 修复。
- JDK21 显式进程 validate/compile 已通过。

## 13. 未关闭 P0

- P0-002 的运行时 HTTP 验收仍未完成；
- P0-003 SnailJob 尚未迁移；
- P0-004 正式契约/Flyway 尚未建立；
- P0-005 敏感配置轮换/撤销执行记录尚未形成。

## 14. 本轮未执行内容

未启动应用；未发送 HTTP；未修改 YAML/POM；未创建 AutoConfiguration；未移动 Controller；未执行前端构建；未执行数据库、Flyway、调度或生产连接；未清理 target；未提交 Git。

## 15. 下一步目标

必须先完成安全隔离启动条件和一次健康 HTTP 验证。若验证成功，下一目标为：

```text
PHASE0D-CONTRACT-AND-FLYWAY-PLAN
```

当前不允许直接进入 Phase 0D。

## 16. 下一步建议模型

```text
GPT-5.6 Sol
```

原因：下一步仍需判断隔离启动、外部依赖禁用、Spring 运行时和 Phase 0 放行门禁；不适合批量实现模型主控。

## 17. 下一步输入文件

1. 本文件；
2. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase0c-web-access-gate-implementation.md`；
3. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase0c-web-access-gate-risk-register.md`；
4. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase0c-web-access-gate-commands.log`；
5. `D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\handoff\phase0c-baseline-repair-handoff.md`；
6. `D:\我的文档\私人\量化\量化交易系统完整设计方案_V29.md`；
7. `D:\我的文档\私人\量化\量化交易系统完整设计方案\_V28.md`。

## 18. 下一步验收命令

```powershell
$jdk21 = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
$oldHome = $env:JAVA_HOME
$oldPath = $env:Path
$env:JAVA_HOME = $jdk21
$env:Path = "$jdk21\bin;$oldPath"
mvn -version
mvn -f "D:\workspace\quant\RuoYi-Vue-Plus\pom.xml" -DskipTests=true validate
mvn -f "D:\workspace\quant\RuoYi-Vue-Plus\pom.xml" -DskipTests=true compile
$env:JAVA_HOME = $oldHome
$env:Path = $oldPath
```

只有在确认隔离启动不会连接真实基础设施、不会触发 SnailJob 后，才允许执行临时端口启动和一次 `GET /api/quant/health`。

## 19. 人工审批点

1. 批准无外部依赖的本地隔离启动方式；
2. 批准临时端口和启动参数；
3. 确认 SnailJob 客户端不会初始化或发送心跳；
4. 仍需单独批准调度迁移、Flyway、正式契约和敏感配置轮换；
5. 保持不连接生产、不启用 LIVE、不创建 QMT/CTP、不提交 Git。

## 20. 未解决问题

- 如何在不修改 YAML/POM、不连接真实数据库/Redis、不触发 SnailJob 的条件下启动完整 `ruoyi-admin`；
- 是否需要单独批准隔离 profile 或测试启动入口；
- 默认 shell Java8/17 漂移的持久化治理；
- 35 个 target 目录的清理方案；
- Phase 0D 的 OpenAPI/Flyway 版本和目标数据库计划。

下一模型必须先解决安全隔离启动问题，不得把编译通过当作 HTTP 运行时验收通过。
## 21. 2026-10-06 实际运行时补充

已按批准的本地隔离条件执行 fat jar 启动尝试，使用临时端口 `18081`、JDK21、SnailJob/SnailAI/管理客户端关闭参数，以及仅指向本机未监听端口的数据库/Redis 参数。

结果依次为：

1. Redisson 连接本机隔离端口失败；
2. 排除 Redisson 后，Lock4j `LockTemplate` 因无 executor 失败；
3. 排除 Lock4j 后，LiteFlow 规则初始化因缺少 `noop` 节点/链失败。

所有进程均已退出，未遗留监听端口；健康接口未获得 HTTP 响应。

因此当前状态维持：

```text
PHASE0C-WEB-ACCESS-GATE-IMPLEMENTATION-AND-RUNTIME-VERIFICATION: BLOCKED
PHASE0D-CONTRACT-AND-FLYWAY-PLAN: NOT ALLOWED
```

## 22. 下一步唯一目标

```text
PHASE0C-ISOLATED-WEB-CONTEXT-PLAN
```

下一轮只做“隔离 Web 验收上下文”的计划和最小实现审批，不进入业务开发、契约/Flyway 或调度迁移。

建议模型：

```text
GPT-5.6 Sol
```

原因：需要决定测试启动上下文、自动配置裁剪边界、无数据库/Redis/调度副作用和回滚方案，属于架构门禁任务。