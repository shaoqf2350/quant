# Proposal

## Why

模块一已经分别形成数据库设计、Spring Boot/Dify 代码仓指导和 plus-ui 用户界面说明，但三份文档尚未合并成一个可按源码路径直接执行的总体施工基线，且已有任务清单与用户指定的严格 1–7 任务格式不一致。现在需要对照总纲 V4.5、详设 0/1 和实际仓库全面审查并统一，避免表结构、后端/前端目录、API、Dify 入口和验收顺序相互矛盾。

## What Changes

- 审查三个既有 OpenSpec change：`data-foundation-db-design`、`data-foundation-code-guidance`、`data-foundation-ui-guidance`，将数据库、代码仓、用户界面内容整合为一个权威模块一实施提案。
- 在 `design.md` 必须提供三大完整章节：
  1. 数据库详细设计：若依既有表复用、现有 QuantPF 表演进、新增 MySQL/DuckDB 表清单；字段、类型、约束、索引、Dify 边界；统一 BIGINT 雪花 ID、五个末尾日志字段；表与业务流程/事务/点时/授权关系。
  2. 用户操作界面说明：plus-ui 页面、路由、导航、管理员/审核/普通用户操作、Dify 触发入口、状态/异常/恢复、一期日线与 Paper 前置界限。
  3. 代码仓落地指导：Spring Boot 与 Vue 目录结构、关键类职责、依赖方向、API/事件、Dify Workflow API/配置/安全/降级。
- `specs/` 以可验证 WHEN/THEN 行为场景覆盖管理员数据源配置、数据采集与质量修复、日历/标的资格、普通用户研究任务、Dify 草稿/事件、前端触发和 Paper 前置门禁。
- `tasks.md` 严格按用户规定的顺序编排数据库→后端实体→后端接口→Dify集成→前端页面→前端接口→联调验证，并在每个任务条目中标出源码库路径和验证方式。
- 所有源码路径均以：
  - 后端：`D:\workspace\quant\RuoYi-Vue-Plus`
  - 前端：`D:\workspace\quant\plus-ui`
  为准；现有目录/约定与建议新增目录分开标识。
- 本变更只输出审查整合设计与任务，不实现代码、数据库迁移、Dify Workflow 或 Vue 页面。

## Capabilities

### New Capabilities

- `data-foundation-delivery-baseline`: 模块一数据库、后端、前端和 Dify 边界的一体化落地约束与跨仓实施基线。
- `data-foundation-implementation-journeys`: 管理员、审核用户和引导型普通用户从数据源配置/就绪到研究任务、Dify 解释和 Paper 前置的端到端行为。

### Modified Capabilities

（无——根 `openspec/specs/` 当前为空；这是对三个已有 change 的整合审查，不直接改动它们的能力文件。）

## Impact

- **规划文档**：新增整合 change 的 proposal/design/specs/tasks；不覆盖或删除前三个 change，明确其为来源文档并记录统一后的权威基线。
- **后端源码库**：`D:\workspace\quant\RuoYi-Vue-Plus\quantpf-modules\`、`ruoyi-admin`、`contract/`、`db/migration/`、DuckDB schema 资源。
- **前端源码库**：`D:\workspace\quant\plus-ui\src\views\quant\`、`src/api/quant/`、菜单/路由/权限契约。
- **既有决策**：MySQL + DuckDB 边界、Dify v1.16.1、XXL-JOB 唯一调度、施工期 Mock、一期仅日线/A股与国内期货、无 LIVE/QMT/CTP、雪花 ID 和五个日志字段均保持不变。
- **联调**：按要求记录 JDK21 + Maven `DromaraApplication` 与 `pnpm dev` 的启动验证步骤；真实密钥/供应商权限不得作为本提案的默认前置输入。
