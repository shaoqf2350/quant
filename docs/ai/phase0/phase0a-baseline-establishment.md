# PHASE0A-BASELINE-ESTABLISHMENT — FINAL REACCEPTANCE

- 日期：2026-10-03
- 目标：PHASE0A-BASELINE-ESTABLISHMENT
- 结果：**工具链基线通过；允许进入 PHASE0B-FOUNDATION-SKELETON-PLAN 计划阶段**。
- V29：D:\我的文档\私人\量化\量化交易系统完整设计方案_V29.md

## 1. 本轮修复内容

1. 安装 Eclipse Temurin JDK 21.0.12.1 LTS：
   `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`
2. 当前用户 `JAVA_HOME` 已持久化为 JDK21；机器级 `JAVA_HOME` 经安装器核验也已指向 JDK21。
3. JDK21 `bin` 位于机器 PATH 的旧 Java 入口之前；旧 JDK 未删除。
4. 安装 pnpm 10.34.5，使实际 pnpm 与前端 `package.json` 的 `packageManager: pnpm@10.34.5` 一致。
5. Node v25.2.1 保持不变；当前满足 Vite 8.2.2 的 Node engine 范围。

本轮没有修改后端或前端仓库中的 Java、XML、YAML、SQL、Vue、TS、JSON 或 POM 文件。

## 2. 重新验收结果

| 门禁 | 实测结果 | 结论 |
|---|---|---|
| Java | OpenJDK/Temurin 21.0.12.1 LTS | 通过 |
| Maven | 3.9.13 | 通过 |
| Maven 实际运行 JDK | 21.0.12.1，Eclipse Adoptium | 通过 |
| `JAVA_HOME` | `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` | 通过 |
| Node | v25.2.1 | 通过当前前端依赖运行时范围；建议后续评估 LTS |
| pnpm | 10.34.5 | 通过，与 manifest 一致 |
| Maven Reactor validate | exit code 0，40 个工程全部 SUCCESS | 通过 |
| 后端 POM 哈希 | `CBD3740F955E8B911754351C7A70B549626C2E2A20C8546155BA39772635FBEE` | 与既有基线一致 |
| 前端 package 哈希 | `D1376599B7C4736C081EC9BEAA7354E236C1D4DE371C8B16ECB0E6F13E871BE1` | 与既有基线一致 |
| 前端 lockfile 哈希 | `C29BBB60A733B8F09ECAB8DE3697A51EE5340F45A1AE7E1D9739D3C3DF1C1F05` | 与既有基线一致 |

## 3. Maven 验收证据

执行命令：

```powershell
mvn -f "D:\workspace\quant\RuoYi-Vue-Plus\pom.xml" -DskipTests=true validate
```

结果：

- exit code：0；
- Maven 输出 `Java version: 21.0.12.1`；
- RuoYi-Vue-Plus 根工程和全部 39 个子工程进入 Reactor；
- Reactor Summary 中 40 个工程均为 `SUCCESS`；
- 验收后未发现仓库内 `target` 目录；
- 未执行完整 compile/test/package/verify/install/deploy；
- Maven 下载的依赖仅进入本机 Maven 缓存，不修改代码仓文件。

之前一次探测使用了错误的 `-Dmaven.main.skip=true` 参数，exit code 1；该失败仅为命令参数错误，无仓库文件修改。修正命令后已成功。

## 4. Git 和工作区复核

### 后端

- 路径：`D:\workspace\quant\RuoYi-Vue-Plus`
- 分支：`6.X...origin/6.X`
- HEAD：`9227f626a3a37e1546d6c291ed398183e94df92b`
- 上游偏离：`0 0`
- 无已跟踪代码修改；仅存在本轮允许目录下的 `docs/ai` 文档。

### 前端

- 路径：`D:\workspace\quant\plus-ui`
- 分支：`6.X-Vue...origin/6.X-Vue`
- HEAD：`045fc006725c014455def9fed19f93c781633d26`
- 上游偏离：`0 0`
- 工作区 clean。

## 5. 关键文件哈希

| 文件 | SHA-256 |
|---|---|
| `D:\workspace\quant\RuoYi-Vue-Plus\pom.xml` | `CBD3740F955E8B911754351C7A70B549626C2E2A20C8546155BA39772635FBEE` |
| `D:\workspace\quant\plus-ui\package.json` | `D1376599B7C4736C081EC9BEAA7354E236C1D4DE371C8B16ECB0E6F13E871BE1` |
| `D:\workspace\quant\plus-ui\pnpm-lock.yaml` | `C29BBB60A733B8F09ECAB8DE3697A51EE5340F45A1AE7E1D9739D3C3DF1C1F05` |
| `D:\我的文档\私人\量化\量化交易系统完整设计方案_V29.md` | `BD49F35D2028005D8F7727255B93A65A6A29036A0E722D6CAC6DE323E06A9D7E` |

## 6. 基线门禁结论

### 已通过

- **P0-001 JDK/Maven 工具链门禁：已解除**；
- Maven 实际使用 JDK21；
- pnpm 与 manifest 版本一致；
- Maven Reactor validate 通过；
- 关键仓库文件哈希未因工具链修复发生变化；
- 当前用户新进程可解析 JDK21、Maven 3.9.13、Node v25.2.1、pnpm 10.34.5。

### 仍未完成的 Phase 0 工作

- QuantPF 工程骨架仍未创建；
- SnailJob→XXL-JOB 迁移仍未实施；
- XXL-JOB 仍未接入；
- Flyway 和正式契约仍未创建；
- 历史 SQL 尚未隔离到 legacy/reference；
- 前端敏感配置轮换/撤销和安全注入尚未执行；
- 前端 lint/build/test 尚未验收；
- Phase 0 全量验收尚未完成。

## 7. 进入下一步条件

工具链基线现已允许进入：

```text
PHASE0B-FOUNDATION-SKELETON-PLAN
```

下一步仍应先做计划级架构审查，不得直接进入业务代码实施。

下一步建议模型：GPT-5.6 Sol。

## 8. 本轮安全边界

- 未读取或输出真实密码、Token、API Key、RSA 私钥或券商凭据；
- 未执行数据库迁移或连接生产数据库；
- 未启动或切换 SnailJob/XXL-JOB；
- 未创建 `quantpf-modules`；
- 未修改根 POM；
- 未创建 QMT/CTP/LIVE 代码；
- 未执行 Git commit、push、reset、clean、checkout、switch、merge 或 rebase。


