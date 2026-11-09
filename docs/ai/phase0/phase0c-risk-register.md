# QuantPF Phase 0C Risk Register

- 日期：2026-10-04
- 阶段：`PHASE0C-FOUNDATION-SKELETON-IMPLEMENTATION`
- 状态：骨架已实施，但存在 P0 阻塞；未提交 Git。

| ID | 等级 | 风险 | 证据 | 影响 | 建议 | 是否阻塞 |
| -- | -- | -- | -- | -- | -- | ---- |
| P0-001 | P0 | 当前 shell 未满足 JDK21 工具链门禁 | `java -version`=OpenJDK 8.0.482；`mvn -version` 实际 Java=17.0.1；根 POM `java.version=21` | 后续构建和运行时基线不可复现，不能把当前编译成功视为 Java21 验收 | 不修改仓库；由人工在明确 JDK21 进程中重新执行 java/mvn/validate/compile，并记录新基线 | 是 |
| P0-002 | P0 | 健康 Controller 可能不在 Spring 默认扫描边界内 | `DromaraApplication` 仅使用默认 `@SpringBootApplication`；Controller 在 `com.quantpf.web.controller` | `/api/quant/health` 可能编译存在但运行时 404 | 单独批准扫描边界方案：启动类扫描、受控 AutoConfiguration、或延后健康接口 | 是 |
| P0-003 | P0 | SnailJob 仍是现行调度底座，XXL-JOB 未迁移 | SnailJob 客户端/自动配置/服务端/任务/配置/前端页面仍存在；本轮没有 XXL-JOB 依赖 | 后续若直接新增 XXL-JOB，可能双栈重复触发任务 | 另行建立任务清单、停止策略、单调度器切换和回滚验收；本轮保持现状 | 是 |
| P0-004 | P0 | Flyway 和正式契约尚未建立 | 当前没有 `db/migration`、正式 OpenAPI、事件 Schema、错误码 YAML、菜单事实源 | 无法进入契约优先和可重复 schema 施工 | 进入 Phase 0D 前先完成计划、版本策略和隔离环境门禁 | 是 |
| P0-005 | P0 | 前端敏感配置处置仍未形成执行记录 | Phase 0A 已发现前端环境文件存在敏感键风险；本轮未读取或输出值，也未修改前端 | 私钥/凭据可能进入前端构建或版本库 | 由人工批准的轮换/撤销和安全注入流程单独执行；审计只记录键存在性 | 是 |
| P1-001 | P1 | Node v25.2.1 非 LTS | 当前 Node=v25.2.1，Vite 运行范围可满足但 CI 长期维护性较弱 | 前端构建可复现性和维护成本增加 | 后续 CI 评估 LTS 版本，未经批准不改本机 Node | 否 |
| P1-002 | P1 | QuantPF 模块当前为空骨架 | 十个模块只有 POM，无实体、服务、Mapper 或契约实现 | 不能宣称业务能力已经存在 | 继续按 Phase 0D 契约/Flyway，再按 Phase 1 分阶段实现 | 否 |
| P1-003 | P1 | `ruoyi-admin` 一次接入六个 application-facing 模块 | `ruoyi-admin/pom.xml` 新增六个 QuantPF 依赖 | 后续启动扫描和模块边界需持续审查 | 在健康入口和契约阶段保持最小接入，不扩展依赖 | 否 |
| P1-004 | P1 | compile 产生 35 个 `target/` 目录 | `mvn compile` 成功后发现 35 个 target；未执行清理 | 工作区未 clean，可能影响后续差异审查 | 记录产物并由人工批准后使用安全、非破坏性方式清理 | 否 |
| P2-001 | P2 | 前端 packageManager 与当前版本已对齐但 Node 未冻结 | `package.json` 声明 pnpm@10.34.5，实测 pnpm 10.34.5；Node 仍为 25.2.1 | CI 与本机策略仍可能漂移 | 在 Phase 0D 文档冻结 CI 运行时矩阵 | 否 |
| P2-002 | P2 | 历史 SQL/原型仍存在多套命名体系 | 原型目录有历史 QuantPF SQL 和 XXL-JOB migration SQL；本轮未移动/执行 | 误用历史脚本会造成 schema drift 或重复表名 | 保持 reference-only，后续以静态 Flyway 计划替代 | 否 |

| P2-003 | P2 | 已批准 POM 修改存在换行/尾部空白差异 | `git diff --check` 报告 `ruoyi-admin/pom.xml` 的 CRLF/尾部空白及 root POM EOF 空行 | 差异审查噪声增大，不能简单判断为业务变更 | 保持当前内容不扩散修改；后续由人工批准进行格式归一化或接受现状 | 否 |

## 处置结论

在 P0-001 和 P0-002 未解决前，不允许把 Phase 0C 标记为完全通过，也不允许进入 Phase 0D 实施。P0-003 至 P0-005 仍是后续 Phase 0 的独立门禁，不得因本轮骨架编译成功而视为已关闭。