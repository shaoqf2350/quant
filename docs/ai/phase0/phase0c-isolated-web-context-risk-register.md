# QuantPF Phase 0C Isolated Web Context Risk Register

- 日期：2026-10-06
- 阶段：`PHASE0C-ISOLATED-WEB-CONTEXT-PLAN`
- `planOnly`: `true`

| ID | 等级 | 风险 | 证据 | 影响 | 建议 | 是否阻塞 |
| -- | -- | -- | -- | -- | -- | ---- |
| P0-006 | P0 | 完整 ruoyi-admin 不能在不改底座配置的条件下形成纯 Web 隔离上下文 | Redisson、Lock4j、LiteFlow 依次阻塞临时启动 | 健康接口没有 HTTP 200 运行时证据 | 采用 `@WebMvcTest` slice，仅加载健康 Controller；实施前重新审批 | 是 |
| P0-007 | P0 | 测试 slice 通过可能被误认为生产启动通过 | `@WebMvcTest` 不加载完整生产自动配置 | 错误放行 Phase 0D 或 Phase 1 | 报告和测试命名必须明确是 Web slice contract evidence，不是生产启动验收 | 是 |
| P0-008 | P0 | `spring-boot-starter-test` 继承关系尚未在 test 执行中验证 | 根 POM 有 test scope 依赖，尚无本轮测试文件 | 后续测试可能因 classpath 缺项失败 | 实施轮先运行依赖树/测试；缺依赖时单独审批 POM 变更 | 否 |
| P0-009 | P0 | 数据库、Redis、Broker、调度和交易配置由后续人工提供 | 本轮不修改 YAML，不连接真实基础设施 | 不能进行完整生产启动验收 | 将这些配置明确延后，保留本地 Web slice 验收边界 | 是 |
| P1-001 | P1 | Node v25.2.1 非 LTS | 已有 Phase 0 基线记录 | CI 长期维护性较弱 | 后续 CI 冻结 LTS，不在本轮修改 | 否 |
| P1-002 | P1 | 35 个 target/及 jar 产物存在 | 前一轮 Maven compile/package 产生 | 工作区审查噪声增加 | 后续另行批准安全清理 | 否 |
| P1-003 | P1 | 历史 SQL 不是生产迁移入口 | 原型目录有 V23/V24/V25/XXL-JOB SQL | 误执行会导致 schema drift | 仅 reference/legacy；Phase 0D 另做 Flyway 计划 | 是 |
| P2-001 | P2 | 前端尚未执行 lint/build/test | 本轮不修改前端 | 前端质量门禁未建立 | 前端阶段再执行 | 否 |

## 处置结论

本轮只完成 plan-only 交接：

```text
PHASE0C-ISOLATED-WEB-CONTEXT-PLAN: COMPLETE
PHASE0C-WEB-ACCESS-GATE-IMPLEMENTATION-AND-RUNTIME-VERIFICATION: BLOCKED
PHASE0D-CONTRACT-AND-FLYWAY-PLAN: NOT ALLOWED
```