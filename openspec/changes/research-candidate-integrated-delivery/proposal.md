# Proposal

## Why

模块二【研究与候选发现】已分别产出 `research-candidate-db-design`（13 张表）、`research-candidate-code-guidance`（Spring Boot 分层与 Dify 集成）、`research-candidate-ui-guidance`（plus-ui 页面与 Dify 触发入口）三份 change，但三份尚未合并成一个可按源码路径直接施工的权威基线，且各自 tasks 的分组顺序与用户指定的"数据库→后端实体→后端接口→Dify 集成→前端页面→前端接口→联调验证"顺序不一致，路径还存在 `[后端]/sql/`、`[后端]/domain/`、`[后端]/controller/`、`[后端]/service/`、`.js` 等与实际仓库（Flyway `db/migration/`、`quantpf-modules/`/`ruoyi-admin`、`quantpf-research`、前端 `.ts`）不符的表述。需要对照总纲 V4.5、详设 0/2 与实际仓库全面审查并统一，避免表/实体/接口/页面/API 相互矛盾。

## What Changes

- 审查三个既有 module-2 change，将数据库、代码仓、用户界面内容整合为一个权威模块二实施提案，并记录"审查结果与统一修正"（复用模块一 9 张研究入口表 + 新增 13 张模块二表、研究 70 分制边界、研究型 Dify Workflow、路径/file 扩展名修正）。
- 在 `design.md` 提供三大完整章节：
  1. **数据库详细设计**：13 张模块二表清单/字段/类型/约束/索引与 Dify 交互方式，及复用模块一 9 张研究入口表、若依 `sys_*` 底座表；沿用 BIGINT 雪花主键 + 五末尾日志字段规范。
  2. **用户操作界面说明**：plus-ui 候选池/研究卡/事实核验/资产研究/审核/交接页面结构、用户操作流程与研究型 Dify 触发入口。
  3. **代码仓库落地指导**：后端 `quantpf-domain`/`quantpf-research`/`ruoyi-admin` 与前端 plus-ui 目录结构、关键类/接口职责、研究型 Dify API 调用约定、配置新增项。
- `specs/` 以可验证 WHEN/THEN 行为场景覆盖跨仓统一边界、研究候选用户路径可恢复、表/实体/页面契约一致、一期边界与联调门禁。
- `tasks.md` 严格按用户指定的 `1 数据库 → 2 后端实体 → 3 后端接口 → 4 Dify 集成 → 5 前端页面 → 6 前端接口 → 7 联调验证` 顺序编排，每个任务标出真实源码库路径（后端 `D:\workspace\quant\RuoYi-Vue-Plus`、前端 `D:\workspace\quant\plus-ui`）与验证方式。
- 本变更只输出审查整合设计与任务，不实现代码、SQL、Dify Workflow 或 Vue 页面。

## Capabilities

### New Capabilities

- `research-candidate-delivery-baseline`: 模块二数据库、后端、前端与 Dify 边界的一体化落地约束与跨仓实施基线。

### Modified Capabilities

（无——根 `openspec/specs/` 当前为空；本 change 是对三个已有 module-2 change 的整合审查，不直接改动它们的能力文件。）

## Impact

- **规划文档**：新增整合 change 的 proposal/design/specs/tasks；不覆盖或删除前三个 module-2 change，明确其为来源文档并记录统一后的权威基线。
- **后端源码库**：`D:\workspace\quant\RuoYi-Vue-Plus\quantpf-modules\quantpf-domain`、`quantpf-research`、`quantpf-strategy`（交接端口）、`quantpf-job`、`ruoyi-admin`、`contract/`、`db/migration/`、`script/sql/`。
- **前端源码库**：`D:\workspace\quant\plus-ui\src\views\quant\research\`、`src\api\quant\`、路由/菜单/权限契约。
- **既有决策**：复用模块一 9 张研究入口表、BIGINT 雪花 ID + 五日志字段、Dify v1.16.1（复用 `DifyResearchClient`）、只有 6 个阶段 + 联调验证、一期日线/A股与国内期货、无 LIVE/QMT/CTP 均保持不变。
- **联调**：按要求记录 JDK21 + Maven `DromaraApplication` 与 `pnpm dev` 的启动验证；真实 Dify Key/供应商权限不作为默认前置输入。