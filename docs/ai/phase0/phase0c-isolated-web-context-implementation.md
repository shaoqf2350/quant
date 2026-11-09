# PHASE0C-ISOLATED-WEB-CONTEXT-IMPLEMENTATION-AND-TEST

- 实施日期：2026-10-06
- 项目：QuantPF
- 后端：`D:\workspace\quant\RuoYi-Vue-Plus`
- 前端：`D:\workspace\quant\plus-ui`
- 实施结论：`PASS`（隔离 MVC 契约测试）
- 生产应用启动结论：仍为 `NOT VERIFIED / BLOCKED`

## 1. 本轮实际实施

本轮创建一个独立的 `src/test` 测试文件，用于验证静态健康 Controller 的 Web MVC 契约：

```text
D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\src\test\java\com\quantpf\web\controller\QuantPfHealthControllerWebMvcTest.java
```

测试使用：

```text
JUnit 5
Spring Test MockMvc
MockMvcBuilders.standaloneSetup
```

没有启动完整 `DromaraApplication`，没有加载生产 Spring Boot ApplicationContext。

## 2. 为什么未使用 @WebMvcTest

计划阶段首选 `@WebMvcTest`，但实际检查当前 Spring Boot 4.1.1 本地测试依赖后发现，当前解析到的 `spring-boot-test-autoconfigure-4.1.1.jar` 不包含 `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest` 和 `AutoConfigureMockMvc` 类。

第一次测试编译因此失败，错误为：

```text
package org.springframework.boot.test.autoconfigure.web.servlet does not exist
cannot find symbol: WebMvcTest
cannot find symbol: AutoConfigureMockMvc
```

本轮没有为追求注解形式擅自添加或升级 Boot 测试依赖，也没有修改 POM。随后采用更小、更安全的等效隔离方式：

```java
MockMvcBuilders.standaloneSetup(new QuantPfHealthController()).build()
```

该方式只注册目标 Controller 和 Spring MVC MockMvc 基础设施，不加载数据库、Redis、调度器或其他自动配置。

## 3. 测试断言

测试请求：

```text
GET /api/quant/health
```

断言：

```text
HTTP 200
$.code = 200
$.data.status = UP
$.data.phase = PHASE0C
$.data.liveEnabled = false
```

`R` 的实际字段为 `code`、`msg`、`data`，因此使用 `$.data` 路径是基于实际源代码确认，而不是猜测。

## 4. POM 状态

本轮没有修改：

- 根 `pom.xml`；
- `ruoyi-admin/pom.xml`；
- 任何 QuantPF 模块 POM。

当前根 POM 和 admin POM SHA-256 保持：

```text
root pom.xml: CEEC6E47C73229ED7D7C6B6AF9BD35CD4313981012B2DBC48DE71C48434F3FD5
ruoyi-admin/pom.xml: 57DF02F266F1C2B236E3C4742395626FB091A960A47D420DA5C1ACAADB628DCB
```

根 POM 已有 test scope 的 `spring-boot-starter-test`，实际已足够提供 JUnit 5、Spring Test 和 MockMvc 的底层类，因此不需要 POM 变更。

## 5. JDK21/Maven 验收

使用显式临时 JDK21：

```text
C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot
```

结果：

| 验收项 | 结果 |
|---|---|
| JDK | Temurin 21.0.12.1 |
| Maven | 3.9.13 |
| `test-compile` | PASS，exit 0 |
| dependency tree（test scope） | PASS，exit 0 |
| 指定测试（含 `-am`） | PASS，exit 0 |
| 测试用例 | 1 run / 0 failures / 0 errors / 0 skipped |

## 6. MockMvc 验收

Surefire 报告确认：

```text
Test set: com.quantpf.web.controller.QuantPfHealthControllerWebMvcTest
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

Spring Test DispatcherServlet 初始化成功，目标 Controller 返回预期响应，测试通过。

这证明：

- `QuantPfHealthController` 的映射可被 MockMvc 注册；
- `/api/quant/health` 的 MVC 请求处理成功；
- `R` 响应 JSON 结构符合预期；
- 静态 Phase 0C 健康契约成立。

这不证明：

- 完整 `ruoyi-admin` 可启动；
- 数据库配置正确；
- Redis 配置正确；
- SnailJob/XXL-JOB 可运行；
- 生产 Spring 自动配置无问题。

## 7. 外部连接安全结论

本轮测试使用 standalone MockMvc，不启动完整应用上下文，不读取生产 YAML 中的真实外部配置，不连接：

- MySQL；
- Redis；
- Broker；
- SnailJob；
- XXL-JOB；
- QMT；
- CTP；
- LIVE。

用户提供的本地 MySQL/Redis 信息没有写入仓库、文档或命令参数，本轮也没有使用这些连接信息。

## 8. MySQL 脚本路径

RuoYi MySQL 底座脚本：

```text
D:\workspace\quant\RuoYi-Vue-Plus\script\sql\ry_vue.sql
D:\workspace\quant\RuoYi-Vue-Plus\script\sql\ry_job.sql
D:\workspace\quant\RuoYi-Vue-Plus\script\sql\ry_workflow.sql
D:\workspace\quant\RuoYi-Vue-Plus\script\sql\ry_ai.sql
```

QuantPF 历史原型 SQL：

```text
D:\我的文档\私人\量化\前端功能原型设计\quantpf_v23_frontend_supplement.sql
D:\我的文档\私人\量化\前端功能原型设计\quantpf_v24_core_hardening.sql
D:\我的文档\私人\量化\前端功能原型设计\quantpf_v25_core_hardening.sql
D:\我的文档\私人\量化\前端功能原型设计\quantpf_v25_xxljob_migration.sql
```

以上 QuantPF 历史 SQL 只能作为 `legacy/reference-only`，不能直接执行或作为生产迁移入口。

## 9. P0/P1/P2 风险

### P0

- P0-003：SnailJob 尚未迁移，XXL-JOB 未接入；
- P0-004：OpenAPI、事件 Schema、错误码、菜单事实源和 Flyway 尚未建立；
- P0-005：敏感配置轮换/撤销执行记录尚未形成；
- P0-006：完整 `ruoyi-admin` 仍无法在无外部依赖条件下启动；
- P0-007：本轮通过的是 standalone MVC 契约测试，不是生产启动验收；
- P0-009：数据库、Redis、Broker、调度和交易配置由后续人工提供，本轮未验证。

### P1

- 默认 shell 仍可能解析到 Java 8/17，显式 JDK21 构建和测试已通过；
- Node v25.2.1 非 LTS；
- `target/` 和 jar 构建产物仍存在；
- QuantPF 十个模块仍为空骨架。

### P2

- 前端尚未执行 lint/build/test；
- POM 既有换行/尾部空白差异仍存在；
- Spring Boot 4.1.1 当前测试依赖不提供预期 `WebMvcTest` 注解包，因此使用 standalone MockMvc 替代。

## 10. 当前阶段判定

```text
PHASE0C-ISOLATED-WEB-CONTEXT-IMPLEMENTATION-AND-TEST: PASS
PHASE0D-CONTRACT-AND-FLYWAY-PLAN: STILL NOT ALLOWED
```

本轮只关闭“隔离 Web MVC 契约测试”这一项，不关闭整个 Phase 0 的所有 P0。

## 11. 回滚说明

本轮代码变更仅为新增测试文件：

```text
D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\src\test\java\com\quantpf\web\controller\QuantPfHealthControllerWebMvcTest.java
```

回滚时只需通过审阅后的文件级变更移除该新增测试文件，不使用 `git reset`、`git clean` 或 `Remove-Item`。本轮没有 POM、YAML、SQL、生产 Java 或前端修改。