# QuantPF BASELINE

- 生成日期：2026-10-04
- 阶段：PHASE0C-FOUNDATION-SKELETON-IMPLEMENTATION
- 代码状态：QuantPF 最小 Maven 骨架已创建；业务代码、调度迁移、数据库迁移和正式契约未实施。
- 基线判定：骨架文件和 Maven 编译验收通过；工具链当前 shell 仍未满足 JDK21 门禁；运行时健康接口尚未验证。

## 仓库基线

| 项目 | 后端 | 前端 |
|---|---|---|
| 路径 | D:\workspace\quant\RuoYi-Vue-Plus | D:\workspace\quant\plus-ui |
| 分支 | 6.X | 6.X-Vue |
| HEAD | 9227f626a3a37e1546d6c291ed398183e94df92b | 045fc006725c014455def9fed19f93c781633d26 |
| 上游偏离 | 0 ahead / 0 behind | 0 ahead / 0 behind |
| 工作区 | 有 Phase 0C 受控修改，未提交 | clean |

## 关键文件哈希

| 文件 | SHA-256 |
|---|---|
| D:\workspace\quant\RuoYi-Vue-Plus\pom.xml | CEEC6E47C73229ED7D7C6B6AF9BD35CD4313981012B2DBC48DE71C48434F3FD5 |
| D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\pom.xml | 57DF02F266F1C2B236E3C4742395626FB091A960A47D420DA5C1ACAADB628DCB |
| D:\workspace\quant\plus-ui\package.json | D1376599B7C4736C081EC9BEAA7354E236C1D4DE371C8B16ECB0E6F13E871BE1 |
| D:\workspace\quant\plus-ui\pnpm-lock.yaml | C29BBB60A733B8F09ECAB8DE3697A51EE5340F45A1AE7E1D9739D3C3DF1C1F05 |
| D:\workspace\quant\RuoYi-Vue-Plus\BASELINE.md | 由本文件写入后重新计算 |

## 工具链实测

- `java -version`：当前 shell 解析为 OpenJDK 8.0.482（Zulu）。
- `mvn -version`：Maven 3.9.13，实际运行 Java 17.0.1（Oracle）。
- `JAVA_HOME`：当前 shell 为 `C:\Program Files\Java\jdk-17.0.1`。
- `node --version`：v25.2.1。
- `pnpm --version`：10.34.5，与 `package.json` 的 packageManager 声明一致。
- 根 POM 要求 Java 21；因此当前 shell 的工具链门禁仍为 **未通过**。本文件不修改系统工具链。

## Phase 0C 状态

- `quantpf-modules`：已创建。
- 十个 QuantPF POM：已创建。
- 根 Reactor 接入：已完成。
- `ruoyi-admin` 最小 QuantPF 模块依赖接入：已完成。
- `QuantPfHealthController`：已创建并通过 Maven compile；运行时注册未验证。
- `BASELINE.md`：已创建并修复为无控制字符、无占位符版本。
- Flyway：未创建。
- OpenAPI/事件 Schema/错误码/菜单事实源：未创建。
- XXL-JOB：未添加。
- SnailJob：未删除、未修改。
- QMT/CTP/LIVE：未创建，真实交易未启用。

## 当前已知风险

1. 当前 shell 的 Java 8/17 与根 POM Java 21 要求不一致，Phase 0D 前必须在不修改仓库的前提下由人工确认 JDK21 进程环境。
2. `DromaraApplication` 默认组件扫描根为 `org.dromara`，而 QuantPF Controller 位于 `com.quantpf.web.controller`；本阶段未修改既有启动类，健康接口尚未完成运行时注册验收。
3. Node v25.2.1 不是长期支持版本，CI 冻结策略待后续确认。
4. SnailJob 仍是现行调度底座；本阶段没有引入 XXL-JOB。
5. 正式契约和 Flyway 尚未创建。
6. `mvn compile` 已生成 35 个模块的 `target/` 构建目录；本轮不执行破坏性删除，后续清理必须单独审批。

## 当前禁止事项

- 不创建业务 Entity、Mapper、Mapper XML、Repository；
- 不创建订单、账户、风控、回测、Paper 撮合或行情实现；
- 不创建 QMT/CTP/LIVE；
- 不删除 SnailJob；
- 不添加 XXL-JOB；
- 不执行数据库迁移；
- 不连接生产数据库；
- 不把密码、Token、API Key、RSA 私钥或券商凭据写入本文件。