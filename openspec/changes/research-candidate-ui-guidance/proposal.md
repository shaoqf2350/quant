# Proposal

## Why

模块二【研究与候选发现】的后端分层与 Dify 集成指导（`research-candidate-code-guidance`）和数据库设计（`research-candidate-db-design`）已经冻结，但若依前端 `plus-ui/src/views` 目前还没有任何 `quant` 页面。普通用户会卡在"研究任务之后如何看候选/研究卡、如何理解事实/解释/预测、AI 草稿和核验事实怎么区分、候选怎么交接给策略模块"这些操作缺口上。没有页面信息架构与用户操作契约，前端施工要么只套 48 页旧原型、要么让用户直面专业字段，违反"低输入引导式体验"的强制验收基线。本 change 补上模块二候选/研究卡/事实核验/审核/交接页面与 Dify 研究型触发入口的落地设计，并与模块一 `data-foundation-ui-guidance` 已规划的研究任务/草稿/事件页自然衔接。

## What Changes

- 生成模块二用户操作界面落地设计文档（`design.md`），覆盖 plus-ui 页面结构、路由/菜单、页面信息层次、三模式（引导/审核/专业）、状态卡、加载/空/错误/过期/授权/降级状态和 API 映射。
- 设计以下核心页面（承接模块一 `research/task`、`draft`、`event`、`assistant` 之后）：
  - 候选池与候选详情（四类入口、筛选运行、硬门禁/分项评分、机会状态、淘汰原因）；
  - 研究决策卡（结论—证据—风险—下一步、版本历史、短中长多周期预期、统计字段引用）；
  - 事实主张与证据链核验页（AI 草稿/待核验/已核验事实分层、引用回源）；
  - 资产研究结果页（基本面因子、事件暴露/敏感度、商品研究结论）；
  - 人工审核工作台（确认/否决/延期/请求补充/受限覆盖、版本校验）；
  - 候选交接与回执页（交接状态、模块三回执、拒绝原因）。
- 固化三种体验模式：普通用户默认"结论—原因—影响—下一步"，评分分项/Workflow 版本/原始引用/快照按权限渐进展开；研究候选不得被表述为可买入/卖出，不提供绕过策略、风险、Paper 门禁的直接下单路径。
- 说明研究型 Dify 的用户触发方式（均经 Spring Boot、不 iframe 嵌入、浏览器不持 Key）：
  - 研究任务页/材料详情"开始 AI 研究/分析影响/研究解释"；
  - 研究卡页"用当前证据解释"（`RESEARCH_CARD_EXPLANATION`）；
  - 全局研究助手（`RESEARCH_QA`）；复盘摘要（`REVIEW_SUMMARY`，交易事实后端维护）；
  - 所有结果显示为草稿，必须经过校验/审核，不可在页面上伪装成事实或信号。
- specs 以可验证 WHEN/THEN 场景覆盖管理员、审核用户、引导型普通用户从研究任务到候选/研究卡/审核/交接的关键路径，及 Dify 触发/降级/边界。
- 本 change 只生成 UI/交互设计、行为契约和实施任务，不实现 Vue 页面、API、Dify 或后端代码。

## Capabilities

### New Capabilities

- `research-candidate-ui`: 研究与候选发现在 plus-ui/RuoYi 前端的页面信息架构、路由/导航、状态展示、权限和操作契约——候选池、研究卡、事实核验、资产研究、审核、交接页面。
- `research-candidate-guided-journeys`: 面向引导/审核/专业用户的研究任务、候选发现、研究卡查看、事实/研究卡审核、候选交接的可恢复用户旅程与状态反馈。
- `research-dify-ui-entry`: 研究型 Dify（事件影响分析、资产研究解释、研究卡说明、研究问答、复盘摘要）在前端的受控触发、草稿展示、引用回源、状态轮询/降级与安全边界。

### Modified Capabilities

（无——主 `openspec/specs/` 当前为空，本 change 全部为新增能力。模块一 `data-foundation-ui`/`dify-ui-entry` 是研究任务/草稿入口的基础 UI 契约，本 change 只在其上扩展候选/研究卡/审核/交接与研究型 Dify 触发，不修改其定义。）

## Impact

- **前端**：`plus-ui/src/views/quant/research/*`（候选、研究卡、事实核验、资产研究、审核、交接、助手）、`plus-ui/src/api/quant/*`、路由/菜单/权限事实源、公共状态卡/证据抽屉/引导步骤组件。
- **后端契约**：research/claims/theses/factors/screening/candidates/cards/reviews/handoffs/qa/dify 的 API；前端不直连 Dify、不持 Dify/API Key。
- **权限**：复用 RuoYi Sa-Token 菜单/按钮权限，新增研究审核、候选复核、研究助手使用与专业查看权限映射。
- **用户体验**：低输入（研究任务核心输入 ≤3 项）、普通语言阻断提示、渐进披露、数据日期/时区明确、Paper/真实交易隔离提示、候选≠交易信号文案。
- **安全**：Dify Key/数据库凭据/供应商 Token 不进入浏览器；AI 草稿不得被 UI 伪装成事实、信号或订单；写命令带 requestId/Idempotency-Key/expectedVersion。