# PHASE0C-WEB-ACCESS-GATE-IMPLEMENTATION-AND-RUNTIME-VERIFICATION

- 实施日期：2026-10-04
- 项目：QuantPF
- 后端：`D:\workspace\quant\RuoYi-Vue-Plus`
- 前端：`D:\workspace\quant\plus-ui`
- 本轮选择方案：方案 A——修改启动类扫描边界
- 本轮结论：`BLOCKED`

## 1. 目标与范围

本轮只解决 QuantPF 健康接口的 Spring Boot 扫描边界问题，并在满足安全隔离前提时进行运行时 HTTP 验收。本轮不实施 Phase 0D 契约/Flyway，不迁移调度器，不修改前端，不连接数据库或真实交易系统。

## 2. 用户批准事项

用户已批准以下三种候选方案：

1. 修改启动类扫描边界；
2. 创建受控 AutoConfiguration；
3. 调整健康接口接入位置。

本轮只实施其中一种，避免重复接入和扫描边界叠加。

## 3. 实际选择方案

选择：

```text
方案 A：在 DromaraApplication 上显式增加最小扫描边界
```

实际修改为：

```java
@SpringBootApplication(scanBasePackages = {"org.dromara", "com.quantpf.web.controller"})
```

选择理由：

- 变更最小，仅修改既有启动类的一行注解；
- 只扫描健康 Controller 所在的 `com.quantpf.web.controller`，没有扩大到整个 `com.quantpf`；
- 不需要增加 AutoConfiguration 元数据或额外资源文件；
- 不改变健康接口路径和响应结构；
- 易于审查和回滚。

未选择方案 B：本轮不创建 AutoConfiguration，因为方案 A 已满足最小扫描需求，继续创建 AutoConfiguration 会增加重复接入面。

未选择方案 C：本轮不移动或调整 Controller 位置，因为现有 `com.quantpf.web.controller` 已符合 Phase 0B 规定的 Controller 包边界。

## 4. 实际代码变更

修改文件：

```text
D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\src\main\java\org\dromara\DromaraApplication.java
```

修改内容：

```diff
-@SpringBootApplication
+@SpringBootApplication(scanBasePackages = {"org.dromara", "com.quantpf.web.controller"})
```

本轮没有修改：

- 根 `pom.xml`；
- `ruoyi-admin/pom.xml`；
- `QuantPfHealthController.java`；
- YAML、XML、SQL、Vue、TS、JSON；
- SnailJob 源码或配置；
- 前端仓库。

## 5. 扫描边界结果

修改前：

- 启动类包：`org.dromara`；
- 使用默认 `@SpringBootApplication`；
- `com.quantpf.web.controller` 不在默认扫描树内；
- 健康接口只能确认编译存在，不能确认运行时注册。

修改后：

- 保留底座扫描：`org.dromara`；
- 增加精确 QuantPF Web 扫描：`com.quantpf.web.controller`；
- 未扫描整个 `com.quantpf`；
- 未扫描 QuantPF 业务模块；
- 未新增 AutoConfiguration；
- 未增加数据库、Redis、Broker、SnailJob、XXL-JOB、QMT、CTP 或 LIVE 扫描入口。

## 6. JDK21/Maven 验收

使用已确认的临时 JDK21 进程：

```text
C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot
```

验收结果：

| 命令 | Exit | 结果 |
|---|---:|---|
| `mvn -version` | 0 | Maven 3.9.13，实际 Java 21.0.12.1，Eclipse Adoptium |
| `mvn -DskipTests=true validate` | 0 | BUILD SUCCESS |
| `mvn -DskipTests=true compile` | 0 | BUILD SUCCESS |

编译后的关键 class 文件存在：

- `ruoyi-admin/target/classes/org/dromara/DromaraApplication.class`；
- `ruoyi-admin/target/classes/com/quantpf/web/controller/QuantPfHealthController.class`。

## 7. 应用启动安全评估

当前 `ruoyi-admin` 的开发/生产配置包含 datasource、Redis 和 SnailJob 等外部依赖入口。当前没有已经批准并验证的“无数据库、无 Redis、无调度心跳”的隔离 profile，也没有可确认不会连接真实外部系统的启动证明。

因此本轮没有启动完整 `ruoyi-admin`，理由是：

1. 启动可能初始化底座外部连接；
2. 启动可能触发 SnailJob 客户端初始化或心跳；
3. 启动可能读取或传播敏感配置；
4. 不允许通过修改 YAML/POM 临时关闭这些能力；
5. 不允许连接生产或任何未经批准的真实基础设施。

## 8. 健康接口运行时结果

接口定义仍为：

```text
GET /api/quant/health
```

运行时状态：

```text
NOT VERIFIED / BLOCKED
```

本轮没有发送 HTTP 请求，因此不能宣称 HTTP 200 或 Spring 运行时注册已经完成。静态扫描边界问题已经修复，但运行时门禁仍缺少安全隔离启动条件。

## 9. 调度、数据库、契约和前端状态

### SnailJob/XXL-JOB

- SnailJob 仍存在；
- XXL-JOB 未添加；
- 未启动调度器；
- 未注册任务、发送心跳或执行 Handler；
- 未形成双栈。

### 数据库/Flyway

- 未创建 Flyway；
- 未执行 SQL 或数据库迁移；
- 未连接生产或其他真实数据库。

### 契约

- OpenAPI 未创建；
- 事件 JSON Schema 未创建；
- 错误码 YAML 未创建；
- 菜单事实源未创建。

### 前端

- 前端仓库未修改；
- 未执行 pnpm install、lint、build 或 test。

## 10. P0/P1/P2 风险

### P0

- P0-002：代码扫描边界已修复，但健康接口尚未完成安全运行时 HTTP 验收；
- P0-003：SnailJob 尚未迁移；
- P0-004：正式契约和 Flyway 尚未建立；
- P0-005：敏感配置轮换/撤销执行记录尚未形成。

### P1

- 默认 shell 仍可能解析到 Java 8/17，虽然显式 JDK21 构建已通过；
- Node v25.2.1 非 LTS；
- 35 个 target 目录仍存在；
- QuantPF 十个模块仍为空骨架。

### P2

- 前端未执行质量命令；
- POM 已批准差异存在换行/尾部空白噪声；
- 应用缺少已批准的本地无外部依赖运行配置。

## 11. 是否允许进入 Phase 0D

不允许。当前结论：

```text
PHASE0C-WEB-ACCESS-GATE-IMPLEMENTATION-AND-RUNTIME-VERIFICATION: BLOCKED
PHASE0D-CONTRACT-AND-FLYWAY-PLAN: NOT ALLOWED
```

阻塞原因不是扫描注解本身，而是无法在不连接真实外部基础设施、不触发 SnailJob 的前提下完成 HTTP 运行时验收。

## 12. 回滚说明

本轮代码回滚范围仅为：

```text
D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\src\main\java\org\dromara\DromaraApplication.java
```

回滚内容是将显式扫描注解恢复为默认 `@SpringBootApplication`。本轮没有执行 Git commit、reset、clean 或任何破坏性回滚命令。
## 13. 2026-10-06 runtime isolation recheck

在不修改 YAML/POM 的前提下，使用 JDK21 和已打包的 `ruoyi-admin.jar` 进行了本地临时启动尝试：

```text
server.port=18081
server.address=127.0.0.1
snail-job.enabled=false
snail-ai.enabled=false
snail-ai.open-api.enabled=false
spring.boot.admin.client.enabled=false
spring.task.scheduling.enabled=false
```

数据库参数被指向本机未监听的隔离地址 `127.0.0.1:1`，Redis 参数同样指向本机未监听端口；未连接生产或任何真实基础设施。

### 启动尝试结果

1. 第一次尝试：应用启动在 Redisson 初始化阶段因本机隔离端口拒绝连接而失败；
2. 第二次尝试：通过命令行排除 Redisson 自动配置后，应用继续在 Lock4j `LockTemplate` 初始化阶段失败；
3. 第三次尝试：进一步排除 Lock4j 自动配置后，应用继续在底座 LiteFlow 本地规则初始化阶段失败，错误为缺少 `noop` 节点/链；
4. 每次进程均自行退出，没有遗留运行进程；端口 `18081` 未保持监听；
5. 健康请求没有获得 HTTP 响应，最终状态为 `NOT VERIFIED`。

上述失败证明：仅依靠命令行隔离参数不能在不修改既有 YAML/POM/底座配置的情况下，把完整 `ruoyi-admin` 缩减为“只加载静态健康 Controller”的安全运行上下文。未为排除 LiteFlow、Lock4j、Redisson、数据源和其他底座自动配置继续扩大修改范围。

## 14. 修订后的结论

方案 A 的扫描边界修改已通过 JDK21 编译，且静态 class 文件存在；但是完整应用运行时受到底座外部基础设施和自动配置链影响，健康接口未完成 HTTP 200 验收。因此本阶段仍为：

```text
PHASE0C-WEB-ACCESS-GATE-IMPLEMENTATION-AND-RUNTIME-VERIFICATION: BLOCKED
PHASE0D-CONTRACT-AND-FLYWAY-PLAN: NOT ALLOWED
```

下一步必须单独设计并批准一个真正的隔离 Web 验收入口，例如独立测试启动配置、受控测试 profile 或只加载健康 Controller 的测试上下文；不得继续通过临时命令行参数堆叠生产底座自动配置排除项。