# PHASE0C-BASELINE-REPAIR-AND-RUNTIME-GATE-CLOSURE

- 审计日期：2026-10-04
- 最后执行时间：2026-10-04 17:13:48 +08:00
- 项目：QuantPF
- 后端：`D:\workspace\quant\RuoYi-Vue-Plus`
- 前端：`D:\workspace\quant\plus-ui`
- 方案：`D:\我的文档\私人\量化\量化交易系统完整设计方案_V29.md`
- 本轮结论：`BLOCKED`

## 1. 修复目标

本轮只处理 Phase 0C 的工具链和运行时门禁，不实施 Phase 0D，不修改业务代码、POM、配置、数据库、调度器、前端或原型。

目标包括：

1. 在不修改系统环境的前提下，确认可用 JDK21 并让 Maven 在临时进程中实际使用 JDK21；
2. 在 JDK21 进程中重新执行 Maven validate/compile；
3. 静态确认 `QuantPfHealthController` 的 Spring Boot 组件扫描边界；
4. 判断是否满足安全启动和 HTTP 验证条件；
5. 更新 P0 风险和下一阶段交接文件。

## 2. 原始工具链结果

默认 shell 结果仍为：

| 项目 | 实测结果 | 判定 |
|---|---|---|
| `java -version` | OpenJDK 8.0.482（Zulu） | 不满足 Java 21 |
| `mvn -version` | Maven 3.9.13，实际 Java 17.0.1（Oracle） | 不满足 Java 21 |
| `JAVA_HOME` | 指向 Java 17.0.1 | 不满足根 POM 要求 |
| `node --version` | v25.2.1 | 可运行，但非 LTS |
| `pnpm --version` | 10.34.5 | 与 packageManager 声明一致 |

默认 shell 的 JDK/JAVA_HOME/PATH 未被本轮修改。

## 3. 显式 JDK21 检查结果

发现可用 JDK21：

```text
C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot
```

显式执行 `java.exe -version` 结果：

```text
OpenJDK 21.0.12.1
Eclipse Temurin 21.0.12.1 LTS
```

随后仅在当前 PowerShell 进程临时设置：

```text
JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot
PATH 前置该 JDK 的 bin 目录
```

没有写入用户环境变量、系统环境变量或仓库文件。临时进程结束后恢复原环境。

## 4. JDK21/Maven 验收结果

临时 JDK21 进程中的 `mvn -version` 确认：

- Maven：3.9.13；
- Java：21.0.12.1；
- Vendor：Eclipse Adoptium；
- Runtime：`C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`。

在该进程中执行：

| 命令 | Exit | 结果 |
|---|---:|---|
| `mvn -f "D:\workspace\quant\RuoYi-Vue-Plus\pom.xml" -DskipTests=true validate` | 0 | `BUILD SUCCESS` |
| `mvn -f "D:\workspace\quant\RuoYi-Vue-Plus\pom.xml" -DskipTests=true compile` | 0 | `BUILD SUCCESS` |

Reactor 包含原有 RuoYi 工程和十个 QuantPF 模块，全部成功。compile 使用既有构建目录并确认源码可编译；当前仓库仍保留 35 个 `target/` 目录，本轮没有执行清理。

### 工具链门禁判定

```text
JDK21 可用：PASS
Maven 实际使用 JDK21：PASS（显式临时进程）
JDK21 validate：PASS
JDK21 compile：PASS
默认 shell 永久环境已修复：未执行/不在本轮授权范围
```

因此原 P0-001 的“可在 JDK21 进程中构建”部分已关闭；默认 shell 漂移作为残余 P1 环境风险保留。

## 5. Spring 扫描边界判断

启动类：

```text
D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\src\main\java\org\dromara\DromaraApplication.java
```

确认结果：

- 主包：`org.dromara`；
- 使用 `@SpringBootApplication`；
- 没有显式 `@ComponentScan`；
- 没有 `scanBasePackages`；
- 没有本轮新建的 AutoConfiguration 或组件索引；
- 默认组件扫描边界为 `org.dromara` 及其子包。

健康 Controller：

```text
D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\src\main\java\com\quantpf\web\controller\QuantPfHealthController.java
```

其包为 `com.quantpf.web.controller`，不属于默认 `org.dromara` 扫描树。静态结论：该 Controller 编译可见，但不能依据当前启动类断言会被 Spring 自动注册。

## 6. 健康接口运行时状态

状态：`NOT VERIFIED / BLOCKED`。

本轮未启动完整 `ruoyi-admin`，原因如下：

1. Controller 扫描边界尚未获批准解决；启动后即使应用正常运行，也不能证明该 Controller 已注册；
2. application 配置包含 datasource、Redis、SnailJob 等外部依赖入口；当前没有经批准的隔离 profile、临时数据库、临时 Redis 或调度禁用证明；
3. 启动可能触发底座自动配置或 SnailJob 相关初始化，违反本轮不触发调度和不连接真实外部系统的安全边界；
4. 本轮禁止修改 `DromaraApplication.java`、Controller、YAML、POM 或创建 AutoConfiguration。

未执行 HTTP 请求，未记录任何 Cookie、Token、密码或配置值。

## 7. 当前 P0/P1/P2 状态

### 已关闭/部分关闭

- P0-001：**部分关闭为“JDK21 显式进程门禁通过”**。默认 shell 仍为 Java 8/17，不能称为系统默认环境已修复。

### 未关闭 P0

- P0-002：健康 Controller 未被默认扫描边界覆盖，运行时未验证；
- P0-003：SnailJob 仍在，XXL-JOB 尚未迁移；
- P0-004：Flyway 和正式契约尚未创建；
- P0-005：前端敏感配置轮换/撤销执行记录仍未形成。

### P1/P2

- P1：Node v25.2.1 非 LTS；
- P1：QuantPF 仍为空骨架；
- P1：35 个 `target/` 目录仍存在；
- P2：前端未执行 lint/build/test；
- P2：approved POM 存在换行/尾部空白差异，未擅自格式化。

## 8. 是否允许进入 Phase 0D

不允许。当前唯一结论：

```text
PHASE0C-BASELINE-REPAIR-AND-RUNTIME-GATE-CLOSURE: BLOCKED
PHASE0D-CONTRACT-AND-FLYWAY-PLAN: NOT ALLOWED
```

阻塞原因是 P0-002 仍未解决。JDK21 显式构建已通过，但不能用它掩盖健康接口的扫描边界和安全运行时验证缺口。

## 9. 未执行内容

- 未修改系统 JDK、JAVA_HOME、PATH、Maven、Node 或 pnpm；
- 未修改任何 Java、XML、YAML、SQL、Vue、TS、JSON 或 POM 文件；
- 未启动应用或发送 HTTP 请求；
- 未修改启动类、扩大组件扫描或创建 AutoConfiguration；
- 未连接数据库、Redis、Broker、SnailJob、XXL-JOB、QMT、CTP 或 LIVE；
- 未执行 Flyway、历史 SQL 或数据库迁移；
- 未执行前端 install/lint/build/test；
- 未清理 target；
- 未执行 Git commit、push、reset、clean、checkout、switch、merge 或 rebase。

## 10. 回滚说明

本轮没有代码、POM、配置、数据库或调度器变更。仅写入 Phase 0 文档目录；临时 JAVA_HOME/PATH 修改只存在于单次 PowerShell 进程并已恢复，不需要系统回滚。