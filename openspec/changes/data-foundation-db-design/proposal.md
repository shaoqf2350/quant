# Proposal

## Why

模块一【数据与标的基础】是 QuantPF 全部下游能力（研究、策略、风险、Paper、回测）的确定性数据底座，其总体详设（详设 1 V1.5）已完成，2026-10-24 的配置决策（总纲 24.10 / 技术分册 ADR-9~12 / 详设 1 §25.2.1）已明确存储选型、数据源、品种范围与调度边界；但代码仓中 `quantpf-marketdata`、`quantpf-research`、`quantpf-common` 仍是空骨架，现有 Flyway 基线（V1_0_0~V1_9_0）只覆盖 Paper MVP 交易域，缺少数据治理、标的身份、交易日历、行情存储、研究入口与 Dify 协作的全部持久化结构。没有这份数据库详细设计，模块一施工无法通过"先契约后代码"的门禁，下游模块也无表可引用。

## What Changes

- 产出模块一**数据库详细设计**（落地设计文档，承载于本 change 的 design.md）：
  - 表清单 = 若依底座复用表（sys_*，不新建）+ 已落地 quantpf 表演进（ALTER，不改历史迁移）+ 新增 MySQL 表 + 新增 DuckDB 表（K 线/Tick，按 ADR-9）；
  - 每张表给出字段名、类型、约束（PK/UK/CHECK/FK/NOT NULL/默认值）、索引，以及与 Dify 的数据交互方式（禁止访问 / 受控输入上下文 / 输出草稿承载 / 集成配置）；
  - 覆盖详设 1 的八个子域：数据源与供应商、来源组与核验、采集与快照、标准化与质量（含点时版本、用途资格）、标的身份（含连续合约、期货品种、行业归属）、交易日历与时段（含就绪、冲突、GFEX 人工维护、FX 预留）、公司行为与合约事件、行情/市场状态/容量/量价特征预留，以及新闻→研究事件入口与 Dify 协作支撑表。
- 落地物（apply 阶段执行）：
  - 新增 Flyway 迁移（V1_10_0 起，按子域分批）建表、加索引、加 CHECK/UK 约束，并对现有 QuantPF MySQL 业务表执行兼容演进：统一为应用生成的 `BIGINT` 雪花主键（移除 `AUTO_INCREMENT`），并在列末尾追加 `create_by/create_time/update_by/update_time/remark` 五个日志字段；
  - `quantpf_trading_calendar`（旧单表）与 `quantpf_market_bar`（MySQL K 线）按 ADR-9 冻结为兼容只读，数据职责分别移交新日历模型与 DuckDB K 线表；
  - DuckDB 侧 K 线/Tick 表 DDL 与 schema 版本化管理方案（进程内 JDBC，数据文件挂 Docker volume）；
  - 种子数据：一期 25 个期货品种目录、日历编码（含 `CN_FUTURE_GFEX` 预留）、数据源/来源组初始记录（`PENDING_CONFIRMATION` 起步）、sys_dict 字典与 `quant:*` 菜单权限码；
  - 同步 `contract/`（错误码、事件 Schema 中涉及数据域的部分）与文档台账（技术分册 27/28/33 章、详设 1 状态）。
- 不改变任何业务规则、评分口径、概率定义（总纲/详设已定）；不实现 Java 领域实体、Mapper、采集适配器与 API（属模块一后续施工 change）；不接入真实数据源（施工期全 Mock，联调前购买 Tushare 2000 积分套餐）。

## Capabilities

### New Capabilities

- `data-governance`: 数据源/供应商/接口/字段目录、来源组与交叉核验、采集任务、原始快照与哈希去重、数据质量状态、点时版本与用途资格的持久化行为。
- `instrument-calendar`: 标的身份与版本化交易属性、连续合约与具体合约映射、期货品种目录、行业归属、交易日历定义/交易日/时段模板/标的绑定/日线就绪/来源快照/冲突处理、公司行为与合约事件、外汇时段规则预留的持久化行为。
- `market-data-storage`: K 线与 Tick 的 DuckDB 存储、MySQL 盘口摘要、市场状态、容量评估、量价特征快照、商品供需、基差/期限结构与现货报价的持久化行为及与既有 MySQL 表的边界。
- `research-entry-dify`: 研究材料状态机、研究任务/运行、Dify Workflow 注册与调用记录、AI 草稿保存与校验状态、研究事件与证据链的持久化行为，以及 Spring Boot 与 Dify 的数据交换边界。

### Modified Capabilities

（无——`openspec/specs/` 当前为空，本 change 全部为新增能力。）

## Impact

- **数据库**：MySQL 新增约 40 张 `quantpf_*` 表；现有 QuantPF MySQL 业务表通过兼容迁移统一主键和五个末尾日志字段；2 张旧表冻结只读；DuckDB 新增 K 线/Tick 2 张表及 schema 版本管理。全部通过新 Flyway 迁移与 DuckDB 初始化器落地，不改历史迁移。
- **代码仓**：`RuoYi-Vue-Plus/db/migration/`（新迁移脚本）、DuckDB schema 资源与初始化工具、`script/sql` 种子数据、`contract/error-codes` 与 `contract/events`（数据域事件 Schema）、`contract/menu` 权限码扩展。
- **底座复用**：sys_user/sys_role/sys_menu（权限）、sys_dict_type/sys_dict_data（枚举展示翻译）、sys_config（参数）、sys_oper_log（通用操作日志）、sys_notice/sys_message（通知联动）、sys_oss（大对象快照引用）——只复用不改造。
- **文档**：技术分册 27.2/28.1/33.3 与落地进度总览、详设 1 §19.3/§25 状态同步。
- **下游**：模块二/三/四详设引用的 `quantpf_research_task`、`quantpf_ai_research_draft`、`quantpf_research_event`、`quantpf_event_evidence`、`quantpf_quant_feature_snapshot` 等表由本 change 一次性定形，后续模块只扩展不重建。
- **门禁**：受 Phase 0 约束——JDK21 工具链、禁止双栈调度、禁止真实交易通道；本 change 不触碰 LIVE/QMT/CTP。
