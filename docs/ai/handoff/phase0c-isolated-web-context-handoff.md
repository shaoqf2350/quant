# QuantPF Phase 0C Isolated Web Context Handoff

## 1. 当前目标

当前阶段：`PHASE0C-ISOLATED-WEB-CONTEXT-PLAN`。

本轮是 plan-only，不创建测试代码、不修改 POM、不修改 YAML、不启动应用。

## 2. 当前状态

```text
PHASE0C-ISOLATED-WEB-CONTEXT-PLAN: COMPLETE
PHASE0C-WEB-ACCESS-GATE-IMPLEMENTATION-AND-RUNTIME-VERIFICATION: BLOCKED
PHASE0D-CONTRACT-AND-FLYWAY-PLAN: NOT ALLOWED
```

## 3. P0-006 证据

完整 `ruoyi-admin` 启动依次受到 Redisson、Lock4j 和 LiteFlow 自动配置阻塞。健康接口未获得 HTTP 200。不能继续堆叠生产自动配置排除参数。

## 4. 选定隔离方式

下一实施轮首选：

```text
@WebMvcTest(controllers = QuantPfHealthController.class)
@AutoConfigureMockMvc(addFilters = false)
```

使用 MockMvc 验证：

```text
GET /api/quant/health -> HTTP 200
status=UP
phase=PHASE0C
liveEnabled=false
```

该证据只证明 Web MVC slice contract，不证明完整生产应用启动通过。

## 5. 计划新增文件

实施前计划新增：

```text
D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\src\test\java\com\quantpf\web\controller\QuantPfHealthControllerWebMvcTest.java
```

本轮没有创建该文件。

## 6. POM 结论

根 `pom.xml` 已统一声明 test scope 的 `spring-boot-starter-test`，计划先验证 `ruoyi-admin` 是否继承该测试 classpath。本轮不修改 POM。只有依赖缺失时，才提出最小 POM 变更并重新审批。

## 7. 外部能力边界

隔离测试不得加载或连接：

- 数据库、MyBatis、Dynamic DataSource；
- Redis、Redisson；
- Lock4j、LiteFlow；
- SnailJob、XXL-JOB；
- Broker；
- QMT、CTP、LIVE；
- Flyway。

用户后续负责 YAML 数据库、真实 Redis、Broker、SnailJob、XXL-JOB、QMT、CTP、LIVE 配置；本阶段不读取、不输出、不连接这些真实配置。

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

这些历史 QuantPF SQL 不是生产迁移入口，不能直接执行。

## 9. 下一步人工审批点

实施 `@WebMvcTest` 前需要再次批准：

1. 创建一个 `src/test` 测试文件；
2. 使用根 POM 已提供的 test scope 依赖；
3. 执行 `mvn -pl ruoyi-admin -am -DskipTests=false test`；
4. 仅接受 Web slice 的 MockMvc 证据，不将其等同于生产启动通过。

## 10. 下一步目标

```text
PHASE0C-ISOLATED-WEB-CONTEXT-IMPLEMENTATION-AND-TEST
```

## 11. 下一步建议模型

```text
GPT-5.6 Sol
```

原因：实施轮仍需控制 Spring Test slice、依赖继承、MockMvc 响应结构和禁止外部连接边界。

## 12. 下一步验收命令

```powershell
$jdk21 = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
$oldHome = $env:JAVA_HOME
$oldPath = $env:Path
$env:JAVA_HOME = $jdk21
$env:Path = "$jdk21\bin;$oldPath"
mvn -f "D:\workspace\quant\RuoYi-Vue-Plus\pom.xml" -pl ruoyi-admin -am -DskipTests=false test
$env:JAVA_HOME = $oldHome
$env:Path = $oldPath
```

不得在该实施轮启动完整 `ruoyi-admin`，不得连接真实基础设施。

## 13. 未解决问题

- `R` 实际 JSON 包装字段需要由测试运行结果确认；
- `spring-boot-starter-test` 的 test classpath 需要实际 Maven test 验证；
- 是否需要 `@Import` 或额外 MVC 配置要以测试失败证据决定；
- 完整生产启动仍受底座自动配置影响，不能由 Web slice 测试关闭该风险。