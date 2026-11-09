# PHASE0C-ISOLATED-WEB-CONTEXT-PLAN

- 计划日期：2026-10-06
- 项目：QuantPF
- 阶段：`PHASE0C-ISOLATED-WEB-CONTEXT-PLAN`
- `planOnly`: `true`
- 本轮结论：仅生成隔离 Web 验收计划，不实施代码、POM 或配置修改。

## 1. 当前 P0-006 证据

上一轮已修改 `DromaraApplication` 的扫描边界，并在 JDK21 下完成 validate/compile/package，但完整 `ruoyi-admin` 运行时无法安全完成健康接口验收：

1. Redisson 尝试连接本机隔离端口后失败；
2. 排除 Redisson 后，Lock4j 因无 executor 初始化失败；
3. 排除 Lock4j 后，LiteFlow 因缺少 `noop` 节点/链初始化失败；
4. 应用上下文未完成刷新；
5. `GET /api/quant/health` 未获得 HTTP 200；
6. 未连接生产数据库、真实 Redis、Broker、SnailJob、XXL-JOB、QMT、CTP 或 LIVE。

结论：不能继续通过命令行堆叠排除参数把完整生产应用伪装成隔离 Web 应用，必须设计独立的测试上下文。

## 2. 三种候选方案比较

| 方案 | 说明 | 优点 | 风险/缺点 | 判定 |
|---|---|---|---|---|
| `@WebMvcTest` | 只加载 MVC slice 和指定 Controller，用 MockMvc 验证路由/序列化 | 依赖面最小；天然不启动完整应用；不创建 DataSource/Redis/Redisson/Lock4j/LiteFlow/SnailJob 上下文 | 默认是 MockMvc，不是完整生产 Jetty；不能证明生产应用全部自动配置可启动 | **首选** |
| `ApplicationContextRunner` | 以最小自动配置集合构建上下文并检查 Bean | 可精确控制 auto-configuration；适合验证 Bean 条件 | 需要自行组装 MVC、Jackson、Controller、MockMvc，测试复杂度更高 | 备选 |
| 独立 `@SpringBootConfiguration`/随机端口测试应用 | 创建专用测试启动类，启动真实 Servlet 容器后请求 HTTP | 可取得真实 TCP HTTP 200 | 容易误加载底座自动配置；需要精确 exclude 列表；测试入口与生产启动边界更容易混淆 | 不作为第一选择 |

## 3. 计划选定方案

选定：

```text
ruoyi-admin 的 @WebMvcTest + MockMvc slice test
```

理由：

- 健康 Controller 是纯静态响应，不需要数据库、Redis、Broker、调度器或业务 Service；
- 目标是证明 `com.quantpf.web.controller.QuantPfHealthController` 已被 MVC 注册并能序列化响应；
- 根 POM 已统一提供 `spring-boot-starter-test` 测试依赖，计划上不需要修改 POM；
- 不需要启动完整 `DromaraApplication`，不会触发 LiteFlow、Lock4j、Redisson、动态数据源或 SnailJob；
- 可在测试日志中明确标注：这是 Web slice contract evidence，不是生产启动验收。

## 4. 计划测试内容

计划新增测试文件：

```text
D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\src\test\java\com\quantpf\web\controller\QuantPfHealthControllerWebMvcTest.java
```

计划测试结构：

```java
@WebMvcTest(controllers = QuantPfHealthController.class)
@AutoConfigureMockMvc(addFilters = false)
class QuantPfHealthControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void healthReturnsStaticPhase0cPayload() throws Exception {
        mockMvc.perform(get("/api/quant/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("UP"))
            .andExpect(jsonPath("$.data.phase").value("PHASE0C"))
            .andExpect(jsonPath("$.data.liveEnabled").value(false));
    }
}
```

实际实现时必须先检查 `R` 的 JSON 包装字段，以实际序列化结构调整 JSONPath；不得猜测字段结构后宣称通过。

## 5. 依赖和 POM 计划

当前计划不修改任何 POM。

原因：根 `pom.xml` 已在 `<dependencies>` 中为子模块统一提供测试范围的 `spring-boot-starter-test`，因此 `ruoyi-admin` 理论上可以继承 JUnit 5、Spring Test、MockMvc 和 JSON 测试支持。

下一实施轮必须先用只读 Maven dependency tree 或现有构建结果确认：

- `spring-boot-starter-test` 是否进入 `ruoyi-admin` test classpath；
- JUnit Jupiter 是否可用；
- `spring-test`、`spring-boot-test-autoconfigure`、JSONPath 是否可用。

只有确认缺少依赖时，才另行提出最小 `ruoyi-admin/pom.xml` 测试依赖变更，并重新人工批准。本计划不自动修改 POM。

## 6. 外部能力排除边界

该方案不启动完整 `DromaraApplication`，因此以下能力不进入测试上下文：

- `DataSource`、MyBatis、Dynamic DataSource；
- Redis、Spring Data Redis、Redisson；
- Lock4j；
- LiteFlow；
- SnailJob Client、SnailJob Server、任务 Handler、心跳；
- XXL-JOB；
- Spring Boot Admin Client；
- Broker；
- QMT、CTP、LIVE；
- Flyway 或任何数据库迁移。

`@AutoConfigureMockMvc(addFilters = false)` 仅用于移除测试请求过滤器影响，不代表生产环境关闭安全认证，也不修改生产 YAML。

## 7. HTTP 200 证据定义

计划实施轮的通过证据必须包含：

1. Maven 使用 JDK21；
2. `mvn -pl ruoyi-admin -am -DskipTests=false test` 成功；
3. 测试上下文只加载指定 Controller；
4. MockMvc 请求 `/api/quant/health` 返回 HTTP 200；
5. 响应结构中的 `status=UP`、`phase=PHASE0C`、`liveEnabled=false` 均匹配；
6. 测试日志或报告明确写出这是 Web slice 证据；
7. 不得将该测试结果表述为完整 `ruoyi-admin` 生产启动通过；
8. 仍需保留完整应用启动失败的底座风险记录。

如果人工后续要求真实 TCP HTTP 证据，必须另行设计独立测试启动类和自动配置白名单，不得把 `@WebMvcTest` 结果替代真实生产启动验收。

## 8. 计划文件和修改边界

本轮 plan-only，计划新增但尚未创建：

```text
D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\src\test\java\com\quantpf\web\controller\QuantPfHealthControllerWebMvcTest.java
```

本轮计划不修改：

- 根 `pom.xml`；
- `ruoyi-admin/pom.xml`；
- `DromaraApplication.java`；
- `QuantPfHealthController.java`；
- 任何 YAML、SQL、XML、Vue、TS、JSON；
- SnailJob/XXL-JOB 文件；
- 数据库脚本；
- 前端仓库。

## 9. MySQL 数据库脚本路径索引

用户后续配置数据库时可参考以下实际路径。当前仅提供路径索引，不执行、不迁移、不修改：

### RuoYi 底座 MySQL 脚本

```text
D:\workspace\quant\RuoYi-Vue-Plus\script\sql\ry_vue.sql
D:\workspace\quant\RuoYi-Vue-Plus\script\sql\ry_job.sql
D:\workspace\quant\RuoYi-Vue-Plus\script\sql\ry_workflow.sql
D:\workspace\quant\RuoYi-Vue-Plus\script\sql\ry_ai.sql
```

### 原型 QuantPF 历史 SQL（仅 reference/legacy，不是生产入口）

```text
D:\我的文档\私人\量化\前端功能原型设计\quantpf_v23_frontend_supplement.sql
D:\我的文档\私人\量化\前端功能原型设计\quantpf_v24_core_hardening.sql
D:\我的文档\私人\量化\前端功能原型设计\quantpf_v25_core_hardening.sql
D:\我的文档\私人\量化\前端功能原型设计\quantpf_v25_xxljob_migration.sql
```

### 其他已发现 SQL

```text
D:\我的文档\私人\量化\schema.sql
```

Oracle、PostgreSQL、SQL Server 脚本位于：

```text
D:\workspace\quant\RuoYi-Vue-Plus\script\sql\oracle\
D:\workspace\quant\RuoYi-Vue-Plus\script\sql\postgres\
D:\workspace\quant\RuoYi-Vue-Plus\script\sql\sqlserver\
```

禁止直接把历史 QuantPF SQL 当作生产迁移入口；后续应由 Phase 0D 计划转换为静态、版本化 Flyway migration。

## 10. 回滚和失败处理

- 测试文件只属于 `src/test`，失败时只回滚该测试文件，不回滚根 POM 或业务代码；
- 不执行 `git reset`、`git clean`、`Remove-Item` 或其他破坏性清理；
- 测试失败时记录具体缺失依赖、上下文加载范围和 JSON 响应结构；
- 不为使测试通过而修改生产 YAML、禁用底座功能或引入 XXL-JOB；
- 不连接任何真实数据库、Redis、Broker、调度器或交易系统。

## 11. 是否需要人工再次批准

需要。

本计划获得批准后，下一轮才允许创建一个 `src/test` 测试文件并执行测试。若 Maven test classpath 缺少依赖，POM 修改必须另行列出并再次批准。

## 12. 当前阶段结论

```text
PHASE0C-ISOLATED-WEB-CONTEXT-PLAN: COMPLETE
PHASE0C-WEB-ACCESS-GATE-IMPLEMENTATION-AND-RUNTIME-VERIFICATION: BLOCKED
PHASE0D-CONTRACT-AND-FLYWAY-PLAN: NOT ALLOWED
```