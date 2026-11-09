# QuantPF Phase 0C Web Access Gate Risk Register

- 日期：2026-10-04
- 阶段：`PHASE0C-WEB-ACCESS-GATE-IMPLEMENTATION-AND-RUNTIME-VERIFICATION`
- 实施方案：方案 A
- 结论：`BLOCKED`

| ID | 等级 | 风险 | 证据 | 影响 | 建议 | 是否阻塞 |
| -- | -- | -- | -- | -- | -- | ---- |
| P0-002 | P0 | 健康接口扫描边界已修改，但尚未完成安全运行时验收 | `DromaraApplication` 已显式扫描 `org.dromara` 和 `com.quantpf.web.controller`；未启动应用、未发送 HTTP | 不能宣称 `/api/quant/health` 已返回 HTTP 200 | 提供经过批准的隔离 profile、无外部依赖启动方式和临时端口后，再进行单次 HTTP 验证 | 是 |
| P0-003 | P0 | SnailJob 尚未迁移 | SnailJob client/config/server/task 仍存在，XXL-JOB 未添加 | 后续调度改造可能形成双栈或重复触发 | 单独执行任务盘点、单调度器切换和回滚验收 | 是 |
| P0-004 | P0 | 正式契约和 Flyway 尚未创建 | OpenAPI、事件 Schema、错误码、菜单事实源和 `db/migration` 均未建立 | 不能进入契约优先和数据库施工 | 先完成 Phase 0D plan-only 设计与审批 | 是 |
| P0-005 | P0 | 敏感配置轮换/撤销尚无执行记录 | 本轮只确认非敏感键存在性，未读取或输出任何值 | 凭据仍可能存在前端构建/配置传播风险 | 单独完成轮换/撤销和安全注入证明 | 是 |
| P1-001 | P1 | 默认 shell 仍非 JDK21 | 默认 Java8/Maven实际Java17；显式 JDK21 下 validate/compile 通过 | 直接运行命令可能基线漂移 | CI 和人工手册固定 JDK21；本轮不改系统环境 | 否 |
| P1-002 | P1 | Node v25.2.1 非 LTS | `node --version`=v25.2.1 | CI 长期维护性和复现性较弱 | 后续冻结 CI LTS 版本 | 否 |
| P1-003 | P1 | 35 个 target 目录仍存在 | Maven compile 后检测到 35 个 target | 增加工作区审查噪声 | 另行批准安全清理方案 | 否 |
| P1-004 | P1 | QuantPF 模块仍为空骨架 | 十个模块只有 POM，无业务实体/服务/Mapper | 不得宣称业务能力已实现 | 继续 Phase 0D 计划后再分阶段实现 | 否 |
| P2-001 | P2 | 前端未执行 lint/build/test | 本轮不修改前端且未运行命令 | 前端门禁尚未建立 | 前端阶段固定 pnpm 10.34.5 后执行 | 否 |
| P2-002 | P2 | 缺少无外部依赖的批准启动配置 | 当前 application 配置存在 datasource、Redis、SnailJob 入口 | 无法安全完成 HTTP 验收 | 单独设计隔离启动策略，不得把生产配置用于验收 | 是 |

## 处置结论

方案 A 的代码修改和 JDK21 编译已通过，但 P0-002 的运行时验证以及其他 Phase 0 P0 门禁尚未关闭：

```text
PHASE0C-WEB-ACCESS-GATE-IMPLEMENTATION-AND-RUNTIME-VERIFICATION: BLOCKED
PHASE0D-CONTRACT-AND-FLYWAY-PLAN: NOT ALLOWED
```
## 2026-10-06 修订

| ID | 等级 | 风险 | 证据 | 影响 | 建议 | 是否阻塞 |
| -- | -- | -- | -- | -- | -- | ---- |
| P0-006 | P0 | 完整 `ruoyi-admin` 无法在当前不改配置的条件下形成纯 Web 隔离上下文 | 依次遇到 Redisson 连接失败、Lock4j 无 executor、LiteFlow `noop` 规则初始化失败；三次本机临时启动均 exit 1 | 无法完成单次健康 HTTP 200 验收 | 单独设计受控测试启动上下文或隔离 profile；限定只加载健康 Web 层，不继续堆叠排除参数 | 是 |

修订后的最终判定仍为：

```text
PHASE0C-WEB-ACCESS-GATE-IMPLEMENTATION-AND-RUNTIME-VERIFICATION: BLOCKED
PHASE0D-CONTRACT-AND-FLYWAY-PLAN: NOT ALLOWED
```