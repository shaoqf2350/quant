# QuantPF Phase 0C Baseline Repair Risk Register

- 日期：2026-10-04
- 阶段：`PHASE0C-BASELINE-REPAIR-AND-RUNTIME-GATE-CLOSURE`
- 结论：`BLOCKED`

| ID | 等级 | 风险 | 证据 | 影响 | 建议 | 是否阻塞 |
| -- | -- | -- | -- | -- | -- | ---- |
| P0-001 | P0（已部分关闭） | 默认 shell 仍非 JDK21，但显式 JDK21 进程已通过构建 | 默认 `java -version`=8.0.482，默认 Maven Java=17.0.1；显式 Temurin 21.0.12.1 下 validate/compile 均 exit 0 | 直接运行命令仍可能落到错误 JDK，导致基线漂移 | 在后续 CI/人工运行手册中固定 JDK21；本轮不修改系统环境 | 否（显式门禁已通过；默认环境仍需治理） |
| P0-002 | P0 | 健康 Controller 不在默认 Spring 扫描树，且未完成运行时验证 | 启动类包为 `org.dromara`，仅 `@SpringBootApplication`；Controller 包为 `com.quantpf.web.controller`；无显式扫描/自动配置 | `/api/quant/health` 可能运行时 404，不能宣称 Phase 0C 完整通过 | 人工批准一种扫描边界方案，或批准将健康接口延后到 Web 接入阶段；之后再做隔离 HTTP 验证 | 是 |
| P0-003 | P0 | SnailJob 尚未迁移 | SnailJob client/config/server/task/frontend 仍存在；XXL-JOB 未接入 | 直接进入调度实现可能形成双栈重复触发 | 单独执行任务盘点、单调度器切换和回滚验收 | 是 |
| P0-004 | P0 | 正式 OpenAPI、事件 Schema、错误码和 Flyway 尚未建立 | 对应正式目录/文件不存在 | 不能安全进入契约优先或数据库施工 | 先做 Phase 0D 计划和审批，不执行 migration | 是 |
| P0-005 | P0 | 敏感配置轮换/撤销尚无执行记录 | Phase 0A 风险仍存在；本轮只确认键存在性，未读取值 | 前端构建或配置传播仍可能暴露私钥/凭据 | 单独留存轮换/撤销结果和安全注入证明，不把秘密写入仓库 | 是 |
| P1-001 | P1 | Node v25.2.1 非 LTS | 实测 v25.2.1 | CI 长期可维护性和复现性较弱 | 后续确定 CI LTS 版本，未经批准不改本机 Node | 否 |
| P1-002 | P1 | 35 个 target 目录仍存在 | JDK21 compile 后仍检测到 35 个 target；本轮未清理 | 增加差异审查噪声和磁盘产物 | 另行批准安全的非破坏性清理方案 | 否 |
| P1-003 | P1 | QuantPF 模块仍为空骨架 | 十个模块只有 POM，无业务实体/服务/Mapper | 不能声称业务闭环存在 | 先完成 Phase 0D 契约/Flyway 计划，再按 Phase 1 实施 | 否 |
| P2-001 | P2 | 前端未执行 lint/build/test | 本轮明确未运行 | 前端质量门禁尚未建立 | 在前端变更阶段、固定 pnpm 10.34.5 后执行 | 否 |
| P2-002 | P2 | approved POM 存在换行/尾部空白差异 | 既有 `git diff --check` 非零；本轮未格式化 | 差异审查噪声增加 | 单独批准后统一格式，不在本轮扩散修改 | 否 |

## 处置结论

P0-002 尚未关闭，因此：

```text
PHASE0C-BASELINE-REPAIR-AND-RUNTIME-GATE-CLOSURE: BLOCKED
PHASE0D-CONTRACT-AND-FLYWAY-PLAN: NOT ALLOWED
```