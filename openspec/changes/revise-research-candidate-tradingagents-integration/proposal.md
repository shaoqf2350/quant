# Proposal: 复核研究与候选发现模块的 TradingAgents 落地设计

## Why

总纲 V5 将 TradingAgents 明确为 Dify 按需编排的独立多智能体研究引擎，而模块二现有数据库、代码、界面及综合交付提案仍有版本/边界不一致风险。需要以总纲 V5、详设 0、详设 2 和当前四份模块二提案为基准，形成单一、可实施、可验收的模块二交付基线，防止 AI 观点越权成为事实、评分或候选状态。

## What Changes

- 综合核查并补齐模块二既有数据库、代码指导、UI 指导和 integrated-delivery 四份在途提案；本 change 是统一审查与实施入口，不删除或重写来源提案。
- 固化研究调用链：Vue → Spring Boot → Dify Workflow →（按策略路由，可选）受控 TradingAgents Adapter → TradingAgents；结果回 Dify 汇总，再回 Spring Boot 校验、保存为 AI_DRAFT。
- 明确 TradingAgents 是模块二可选深度研究能力，适用于多空辩论、复杂影响链、反方研究等，不是每个研究任务的必经节点；简单摘要、结构化抽取、解释可由 Dify 完成。
- 明确 Spring Boot 独占事实核验、确定性因子/门禁/评分/候选状态/研究卡/审核/交接写入；Dify 与 TradingAgents 仅生成研究草稿和解释，不直写业务主表。
- 补全数据库表清单、字段类型/约束/索引、Dify/TradingAgents 数据边界、若依 UI 关键路径、后端与 Vue 目录/接口/配置、可验证 WHEN/THEN 场景及按规定顺序拆分的落地任务。
- 修正与仓库实际约定不符的路径：Flyway 在后端 `db/migration/`，实体位于 `quantpf-domain`，Controller 位于 `ruoyi-admin`，前端 API 使用 TypeScript `.ts`。

## Capabilities

### New Capabilities
- `research-candidate-delivery`: 定义研究与候选发现的端到端持久化、可选多智能体研究、用户操作、服务端治理和跨仓交付行为。

### Modified Capabilities
- 无。当前 `openspec/specs/` 尚无已归档主规格；本 change 建立新 capability，不声称修改既有规格。

## Impact

- 规划文档：`openspec/changes/revise-research-candidate-tradingagents-integration/`。
- 依据文档：总纲 V5、详设 0、详设 2，以及 `research-candidate-db-design`、`research-candidate-code-guidance`、`research-candidate-ui-guidance`、`research-candidate-integrated-delivery` 四份在途提案。
- 预期实施仓：`D:\workspace\quant\RuoYi-Vue-Plus` 与 `D:\workspace\quant\plus-ui`；本提案阶段不改源码、数据库迁移或正式详设。
- 对接能力：Spring Boot、MySQL/Flyway、Dify Workflow REST/SSE、Dify 内网调用 TradingAgents Adapter REST/JSON、若依权限与 Vue 页面。
