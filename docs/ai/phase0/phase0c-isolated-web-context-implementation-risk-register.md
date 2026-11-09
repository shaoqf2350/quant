# QuantPF Phase 0C Isolated Web Context Implementation Risk Register

- 日期：2026-10-06
- 阶段：`PHASE0C-ISOLATED-WEB-CONTEXT-IMPLEMENTATION-AND-TEST`
- 结论：隔离 MVC 测试 PASS；整体 Phase 0 仍被其他 P0 阻塞。

| ID | 等级 | 风险 | 证据 | 影响 | 建议 | 是否阻塞 |
| -- | -- | -- | -- | -- | -- | ---- |
| P0-003 | P0 | SnailJob 尚未迁移 | SnailJob 依赖、配置、服务端和任务仍存在；XXL-JOB 未接入 | 不能进入调度迁移完成态 | 另行执行单调度器迁移计划 | 是 |
| P0-004 | P0 | 正式契约和 Flyway 尚未建立 | OpenAPI、事件 Schema、错误码、菜单事实源、db/migration 均未建立 | 不能进入契约/数据库施工完成态 | 另行执行 Phase 0D plan-only | 是 |
| P0-005 | P0 | 敏感配置轮换/撤销记录尚未形成 | 本轮未读取或输出真实值 | 配置安全状态未闭环 | 后续由人工执行并记录结果，不写入秘密 | 是 |
| P0-006 | P0 | 完整 ruoyi-admin 仍无法在无外部依赖下启动 | 上一轮 Redisson/Lock4j/LiteFlow 依次阻塞 | 生产启动未验证 | 单独设计完整应用隔离启动方案 | 是 |
| P0-007 | P0 | MVC 测试通过不等于生产启动通过 | 测试使用 standalone MockMvc，不启动生产上下文 | 不能据此放行 Phase 0D/Phase 1 | 交接文件保留该边界 | 是 |
| P0-009 | P0 | 数据库/Redis/Broker/调度/交易配置延后人工提供 | 本轮未使用用户提供的连接信息 | 完整生产环境仍待配置 | 后续人工配置后单独验收，禁止写入敏感值 | 是 |
| P1-001 | P1 | 默认 shell JDK8/17 漂移 | 显式 JDK21 测试通过，默认环境仍可能漂移 | 直接命令可能不复现 | CI/手册固定 JDK21 | 否 |
| P1-002 | P1 | Node v25.2.1 非 LTS | Phase 0 基线记录 | 前端 CI 维护风险 | 后续冻结 CI LTS | 否 |
| P1-003 | P1 | target/jar 产物存在 | testCompile/test 产生构建产物 | 工作区审查噪声 | 另行批准安全清理 | 否 |
| P2-001 | P2 | Boot 4.1.1 预期 WebMvcTest 注解不在当前 test-autoconfigure jar | 初版注解测试编译失败 | 不能直接使用计划中的 Boot slice 注解 | 使用 standalone MockMvc；后续如需 Boot slice 另行解决依赖版本 | 否 |
| P2-002 | P2 | 前端未执行 lint/build/test | 本轮只做后端隔离测试 | 前端门禁未建立 | 前端阶段执行 | 否 |

## 处置结论

```text
PHASE0C-ISOLATED-WEB-CONTEXT-IMPLEMENTATION-AND-TEST: PASS
PHASE0D-CONTRACT-AND-FLYWAY-PLAN: STILL NOT ALLOWED
```