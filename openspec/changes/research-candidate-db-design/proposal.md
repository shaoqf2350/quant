# Proposal

## Why

模块二【研究与候选发现】是 QuantPF 把"已治理数据"转化为"有证据、有反方、可复核的研究结论与候选对象"的研究层，其总体详设（详设 2 V1.3）已完成、上下游接口也已冻结；但代码仓 `quantpf-research` 与 `quantpf-common` 仍是空骨架，且当前 Flyway 基线只覆盖 Paper 交易域（模块四）与模块一的治理/标的/日历/研究入口表。模块二独有的"事实主张、证据链、研究假设/预测/反方、资产研究因子、候选筛选/评分、研究卡、人工审核、候选交接"没有一张落库的表。没有这份数据库详细设计，模块二施工无法通过"先契约后代码"门禁，下游模块三也拿不到可引用的 `research_card` / `candidate_handoff` 主表。本 change 承接模块一 `data-foundation-db-design` 已冻结的 9 张研究入口表，只补齐模块二专属持久化边界，不重复建表。

## What Changes

- 产出模块二**数据库详细设计**（落地设计文档，承载于本 change 的 `design.md`）：
  - 表清单 = 复用模块一/若依底座表（不重建）+ 新增 13 张模块二专属 MySQL 表；
  - 每张新增表给出字段名、类型、约束（PK/UK/CHECK/FK/NOT NULL/默认值）、索引，以及与该表的 **Dify 数据交互方式**（禁止访问 / 受控输入上下文 / 输出草稿承载 / 只读结构化结果）；
  - 覆盖详设 2 的六个模块二子域：事实核验与证据链、研究链（解释/预测/反方）、资产专属研究（股票基本面因子、商品研究结论、事件暴露/敏感度）、候选筛选与评分、研究决策卡、人工审核与候选交接；
  - 明确与模块一 9 张研究入口表（`research_material`、`research_material_relation`、`research_task`、`research_task_run`、`ai_research_draft`、`dify_workflow_registry`、`dify_invocation`、`research_event`、`event_evidence`）的 FK 复用关系，以及向模块三 `quantpf_strategy_signal` / 模块四验证域引用对象的**逻辑外键**边界。
- 落地物（apply 阶段执行）：
  - 新增 Flyway 迁移（`V1_17_0` 起，按子域分批）建 13 张表、索引、CHECK/UK 约束；
  - 新表统一遵守承接自模块一的全库规范：`id BIGINT NOT NULL PRIMARY KEY`（禁 `AUTO_INCREMENT`），MyBatis-Plus `IdentifierGenerator` 与 Hutool `Snowflake` 共用同一套 `workerId/datacenterId`；列尾固定追加 `create_by/create_time/update_by/update_time/remark` 五个日志字段；
  - 种子数据：候选原因码/研究状态/证据类型/审核动作/交接状态等 `sys_dict_*` 字典 + `quant:*` 研究域菜单权限码；不引入业务种子数据；
  - 同步 `contract/`（错误码、事件 Schema 中涉及研究/候选域的部分）与文档台账（技术分册 27/28/33 章、详设 2 状态）。
- 不改变任何业务规则、评分口径、概率定义（总纲/详设已定）；不实现 Java 领域实体、Mapper、Dify 适配器、筛选/评分引擎、API 或前端页面（属模块二后续施工 change）；不触碰模块一已冻结表的历史迁移。

## Capabilities

### New Capabilities

- `fact-verification`: 研究主张（Claim）及其与来源/证据的关联（Evidence Link）的持久化行为——主张状态机（待核验→核验→冲突→修订→失效）、证据类型与来源独立性、AI 草稿不得直接升级为核验事实。
- `research-chain`: 研究假设（解释层）、条件式预测（方向/周期/成立/失效/统计结果引用）与反方证据/证伪条件的持久化行为，及其版本化与失效传播。
- `asset-research`: 股票基本面因子快照、事件暴露与敏感度、商品供需/基差/期限结构研究结论三类"确定性计算研究结果"的持久化行为，区分 `DISCLOSED/CALCULATED/MODELLED/INFERRED` 与点时版本。
- `candidate-screening`: 候选筛选运行记录、候选评估（硬门禁逐项结果 + 分项评分 + 淘汰原因码）与研究决策卡（版本化聚合结果）的持久化行为。
- `review-handoff`: 研究域人工审核记录（确认/否决/延期/请求补充/受限覆盖）与候选向模块三交接记录（交接/回执/拒绝/失效）的持久化行为。

### Modified Capabilities

（无——主 `openspec/specs/` 当前为空；本 change 全部为新增能力。模块一的 `data-foundation-db-design` 已通过其 `research-entry-dify` capability 冻结研究入口表，本 change 只引用、不修改其定义。）

## Impact

- **数据库**：MySQL 新增 13 张 `quantpf_research_*` / `quantpf_candidate_*` / `quantpf_screening_*` 表（事实核验 2、研究链 3、资产研究 3、候选筛选/评分/研究卡 3、审核/交接 2），全部遵守全库雪花主键 + 五个末尾日志字段规范，通过新 Flyway 迁移落地，不改历史迁移。
- **代码仓**：`RuoYi-Vue-Plus/db/migration/`（新迁移脚本）、`script/sql` 字典/菜单种子、`contract/error-codes` 与 `contract/events`（研究/候选/交接事件 Schema 与错误码）、`contract/menu` 权限码扩展。
- **底座/上游复用**：复用模块一 9 张研究入口表与 `sys_user/sys_role/sys_menu/sys_dict_*` 底座表（只保存稳定 ID、不复制）；引用 `quantpf_instrument`、`quantpf_market_quote_snapshot`、`quantpf_quant_feature_snapshot`、`quantpf_supply_demand_observation`、`quantpf_basis_term_structure`、`quantpf_data_point_in_time` 等标识身份/点时/行情键。
- **下游**：模块三详设引用的 `research_card`（研究卡版本）、`candidate_handoff`（交接回执）由本 change 定形；模块四下阶段才需的验证结果引用（`sample_result_ref`）先以逻辑外键预留，不建物理 FK。
- **既有决策**：MySQL + DuckDB 边界、Dify v1.16.1、XXL-JOB 唯一调度、雪花 ID + 五日志字段、一期仅日线/仅 A 股与国内期货、无 LIVE/QMT/CTP 均保持不变。
- **门禁**：受 Phase 0 约束（JDK21、禁止双栈调度、禁止真实交易通道）；本 change 不实现服务、不触碰既有 Paper 交易链路与模块四 RiskCheck。