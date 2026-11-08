# Proposal

## Why

V5 总纲已经把 TradingAgents 提升为独立多智能体研究引擎，但模块一现有数据库、代码、界面和集成交付提案仍主要按“Spring Boot + Dify”设计，部分位置还保留“Dify 或 Spring Boot 均可直接调用 TradingAgents”的模糊表述。需要重新核查数据与标的基础模块的全部落地设计，统一为“Spring Boot 权威数据与任务 → Dify 固定 Workflow 编排 → 按需调用 TradingAgents Adapter → Dify 汇总 → Spring Boot 校验落库”的生产主链路，并把表结构、界面入口、代码路径、契约和验收场景落实到可实施粒度。

## What Changes

- 合并审查 `data-foundation-db-design`、`data-foundation-code-guidance`、`data-foundation-ui-guidance` 和 `data-foundation-integrated-delivery` 的既有结论，形成模块一重新核查后的统一实施基线。
- 完善模块一数据库详细设计，逐表列出若依底座复用表、现有 QuantPF 表和新增/演进表的字段、类型、约束、索引、数据归属，以及 Dify/TradingAgents 运行引用、原始草稿和校验结果的持久化方式。
- 明确 MySQL 保存权威元数据与业务状态、DuckDB 保存高容量行情分析数据、MinIO 保存原始材料/快照/AI 原始响应大对象；Dify Dataset 只保存获准检索的文本知识，TradingAgents 不拥有 QuantPF 权威业务库。
- 明确模块一中 TradingAgents 的加入范围：新闻重大事件、来源冲突、复杂影响链和重要材料反方研究可由 Dify 按需调用；数据采集、去重、质量、授权、点时、标的身份、交易日历、行情、容量和用途资格仍由 Spring Boot 确定性处理。
- 统一通信方式为 Spring Boot 调用 Dify Workflow REST API/SSE，Dify 通过内网 HTTPS REST/JSON 调用 TradingAgents Adapter；MCP 不作为一期主通信协议；Spring Boot 仅可在健康检查、状态查询、取消或显式降级场景直连同一 Adapter。
- 完善若依 Vue 页面结构和用户关键路径，明确数据源、质量、标的、日历、行情、研究任务、AI 草稿/辩论和证据页面中的 Dify/TradingAgents 触发位置、运行状态、人工复核和降级提示。
- 完善后端与前端代码仓落地指导，列出模块目录、关键实体/Controller/Service/Adapter、OpenAPI/事件/错误码、配置项、密钥与网络边界。
- 把关键用户操作、TradingAgents 按需编排、部分成功、引用越界、重复提交、Dify/TradingAgents 不可用和禁止绕过等行为改写为可自动验证的 WHEN/THEN 增量规范。
- 按用户指定的 1–6 顺序重写实施任务，并补充契约、迁移、测试、安全、发布和文档一致性子任务；本提案只生成规划制品，不修改两个源码库。

## Capabilities

### New Capabilities

- `data-foundation-tradingagents-delivery`: 数据与标的基础模块在引入 TradingAgents 后的数据治理、Dify 编排、多智能体研究、用户操作、持久化、代码落地和安全验收行为。

### Modified Capabilities

- 无。当前根 OpenSpec 尚无已归档主规格；既有同主题内容均位于在途 change 中，本变更通过新能力汇总并纠正其偏差，后续归档时应避免与旧 change 重复同步。

## Impact

- 规划文档：模块一现有四组 OpenSpec 提案的数据库、代码、UI 和综合交付结论，以及《详设_0》《详设_1》《详设_6》中的三方通信边界。
- 后端目标仓库：`D:\workspace\quant\RuoYi-Vue-Plus`，重点影响 `db/migration`、`script/sql`、`quantpf-domain`、`quantpf-marketdata`、`quantpf-research`、`quantpf-job`、`ruoyi-admin`、`contract/openapi`、`contract/events`、`contract/error-codes` 和 `contract/menu`。
- 前端目标仓库：`D:\workspace\quant\plus-ui`，重点影响 `src/views/quant`、`src/api/quant`、公共运行状态/证据组件、动态菜单和权限码。
- 外部系统：自托管 Dify v1.16.1、独立 TradingAgents Adapter/Research Engine、模型供应商、MySQL、Redis、DuckDB、MinIO、XXL-JOB。
- 接口：Dify `/v1/workflows/run` 流式调用、TradingAgents 内部 Run API、可选 Dify 签名回调，以及 QuantPF 研究任务/AI 草稿/验证结果查询接口。
- 安全边界：浏览器不直连 Dify/TradingAgents；二者不直连 QuantPF 权威数据库和交易写接口；所有输出为 `AI_DRAFT`，必须经 Spring Boot 校验与必要人工复核。
