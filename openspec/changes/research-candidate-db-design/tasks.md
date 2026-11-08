# Tasks

## 1. 基线盘点与防重

- [ ] 1.1 盘点模块一 `data-foundation-db-design` 已冻结的 9 张研究入口表（research_material/material_relation/task/task_run/dify_workflow_registry/dify_invocation/ai_research_draft/research_event/event_evidence）及本次 13 张新表名，用 `SHOW TABLES LIKE 'quantpf_%'` 与 `openspec list` 核验无同名重复；确认结果录入迁移检查单
- [ ] 1.2 确认雪花主键与五个末尾日志字段约定已就绪：MyBatis-Plus `IdentifierGenerator` 与 Hutool `Snowflake` 共用 `workerId/datacenterId`，验证两种生成器同配置不碰撞（跑一次生成器单测）
- [ ] 1.3 在 `db/migration` 确认模块一迁移版本号上界（`V1_16_0`），冻结模块二起始版本 `V1_17_0` 并记录于本 change 迁移计划，验证后续脚本按 Flyway 版本扫描不冲突

## 2. 事实核验与研究链迁移（V1_17_0）

- [ ] 2.1 编写 `V1_17_0__research_fact_and_chain.sql`：建 `quantpf_research_claim`（含 `claim_hash` 唯一、`verification_status` CHECK、`previous_claim_id`/`current_flag` 版本列、五日志字段），并在全新库执行 `mvn flyway` 或等价命令验证建表成功、`SHOW CREATE TABLE` 主键为 `BIGINT` 且无 `AUTO_INCREMENT`
- [ ] 2.2 同迁移建 `quantpf_research_evidence_link`（`UK(claim_id, material_id, quote_location, evidence_type)`、`evidence_type` CHECK、`independence_group`），验证外键 `claim_id` 引用 `research_claim.id` 生效
- [ ] 2.3 同迁移建 `quantpf_research_thesis` / `quantpf_research_prediction` / `quantpf_research_counter_evidence`（版本唯一键、方向/周期 CHECK、`sample_result_ref` 逻辑引用不建物理 FK），验证五表存在于同一迁移版本
- [ ] 2.4 编写约束测试：插入非法 `verification_status`、重复 `claim_hash`、缺少来源引用的已核验主张应被拒绝，验证 CHECK/UK 与"缺来源不得 VERIFIED"规则可执行

## 3. 资产研究因子迁移（V1_18_0）

- [ ] 3.1 编写 `V1_18_0__asset_research_factors.sql` 建 `quantpf_fundamental_factor_snapshot`（`UK(instrument_id, report_period, factor_version, point_in_time_id)`、`disclosure_method` CHECK），验证点时唯一键与因子版本并存可回放
- [ ] 3.2 同迁移建 `quantpf_event_exposure`（`UK(event_id, exposure_type, exposure_code)`、`sensitivity_method` CHECK），验证事件↔行业/商品/标的暴露映射唯一且 `event_id` 引用 `research_event.id`
- [ ] 3.3 同迁移建 `quantpf_commodity_research_snapshot`（产业/价格/工具判断字段、`spot_exposure_type` 含 NONE、`tool_expression` CHECK），验证 `future_product_id` 引用 `quantpf_future_product.id` 且无敞口可停 NONE

## 4. 候选筛选与研究卡迁移（V1_19_0）

- [ ] 4.1 编写 `V1_19_0__candidate_screening_and_card.sql` 建 `quantpf_screening_run`（`UK(screening_key)`、`entry_type` CHECK），验证重复 `screening_key` 被唯一键拒绝
- [ ] 4.2 同迁移建 `quantpf_candidate_assessment`（`UK(screening_run_id, subject_ref, direction, research_period)`、`hard_gate_results`/`dimension_scores` JSON + `research_score` DECIMAL、`status`/`candidate_stage` CHECK），验证分项评分与硬门禁分离可查询
- [ ] 4.3 同迁移建 `quantpf_research_card`（`UK(card_key, card_version_no)`、`current_key` 生成列 + `UK(card_key, current_key)` 保证单一当前版本、`message_validity` 七态 CHECK），验证同 `card_key` 只允许一个 `current_flag=1` 版本而历史版本保留

## 5. 审核与交接迁移（V1_20_0）

- [ ] 5.1 编写 `V1_20_0__review_and_handoff.sql` 建 `quantpf_research_review`（`UK(review_key)`、`review_action` CHECK、`review_scope` JSON 记录覆盖原值/新值），验证重复 `review_key` 被拒且覆盖记录含原值/新值/有效期
- [ ] 5.2 同迁移建 `quantpf_candidate_handoff`（`UK(handoff_key)`、`handoff_status` CHECK、`target_module=STRATEGY`、`handoff_payload` JSON），验证重复 `handoff_key` 返回原回执语义（唯一键层面不重复插入）

## 6. 索引与约束补强（V1_21_0）

- [ ] 6.1 编写 `V1_21_0__research_indexes_and_constraints.sql`：补齐 `(instrument_id, market_local_date)`、`(status, create_time)`、`(verification_status, create_time)`、`(asset_type, opportunity_status)` 等复合索引，执行 `EXPLAIN` 验证候选/研究卡列表与筛选回放查询命中索引
- [ ] 6.2 校验 13 张表列尾五日志字段顺序完全一致（`create_by/create_time/update_by/update_time/remark`）、无 `created_at/updated_at` 新列、无 `AUTO_INCREMENT`，用脚本或 `SHOW CREATE TABLE` 全表断言通过

## 7. 种子数据与契约同步

- [ ] 7.1 维护 `sys_dict_type/sys_dict_data` 种子：候选原因码、研究/证据/审核/交接状态、机会状态、证据类型、披露口径等展示字典；幂等脚本重复执行不产生重复，验证字典可查询且主状态仍由 CHECK 约束
- [ ] 7.2 维护 `contract/menu` 权限码种子：`quant:research:*`、`quant:candidate:*`、`quant:research:review` 等；验证菜单事实源与 `sys_menu` 种子一致且不含凭据
- [ ] 7.3 同步 `contract/error-codes` 与 `contract/events`：研究主张/候选/交接数据域错误码与事件 Schema（含 `eventId/schemaVersion/aggregateId/idempotencyKey`），验证事件 Schema 与详设 2 §22 对齐

## 8. 文档台账同步

- [ ] 8.1 更新技术分册 27.2/28.1/33.3（新增 13 表、迁移版本、落地状态"数据库设计已冻结、代码未落地"）、落地进度总览与详设 2 §21/§26 状态；验证三处表清单与迁移实际一致、无"已落地"误标

## 9. Mock 全链验收

- [ ] 9.1 在全新库用 Mock 数据跑"研究任务→AI 草稿→claim/evidence_link→thesis/prediction/counter_evidence"链，验证每条结论可回放（点时快照 + 版本 + 前驱链）且 AI 草稿状态与核验事实分离
- [ ] 9.2 跑"因子/暴露→screening_run→candidate_assessment→research_card→research_review→candidate_handoff"链，验证硬门禁失败候选被淘汰、研究卡版本可回溯、交接幂等且回执可查
- [ ] 9.3 验证重复采集/重复任务/重复交接不产生重复副作用、点时读取不暴露未来版本、未授权/冲突数据不进入 `RESEARCH_READY`，并执行一次新库全量 Flyway（`V1_0_0`→`V1_21_0`）+ 回滚演练

## Workflow follow-up

- 归档本 change 前，复核模块一 `data-foundation-db-design` 与模块三/四未来 change 的 FK/逻辑引用一致（尤其 `candidate_handoff` → 模块三策略评估、`sample_result_ref` → 模块四验证结果）。
- 数据设计完成不等于代码已落地；模块二 Java 领域实体、Dify 适配器、筛选/评分引擎、API 与前端由后续施工 change 承接（参照 `data-foundation-code-guidance` / `data-foundation-ui-guidance` 模式）。