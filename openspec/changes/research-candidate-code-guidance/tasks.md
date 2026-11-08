# Tasks

## 1. 基础设施与错误码

- [ ] 1.1 在 `quantpf-common.error` 新增研究/候选域错误码（主张校验失败、硬门禁拒绝、评分缺项、研究卡版本冲突、交接失败等），并在 `contract/error-codes` 同步；验证错误码枚举与契约一致且可被 `GlobalExceptionHandler` 转译
- [ ] 1.2 复用模块一 `IdentifierGenerator`/`SnowflakeFacade`/`InjectionMetaObjectHandler` 配置，补 ArchUnit 基线测试：`quantpf-domain` 不依赖 Web/Dify/XXL-JOB/Paper execution，`quantpf-research` 不依赖 `quantpf-execution/finance/risk` 实现；验证架构测试通过

## 2. 领域对象与 Repository 端口

- [ ] 2.1 在 `quantpf-domain.research` 新增 Claim/EvidenceLink/Thesis/Prediction/CounterEvidence 及价值对象与 Repository 端口，对齐 `research-candidate-db-design` 的表字段与状态枚举；验证实体字段与迁移表一一对应
- [ ] 2.2 在 `quantpf-domain.research` 与 `quantpf-domain.asset` 新增 ScreeningRun/CandidateAssessment/ResearchCard/ResearchReview/CandidateHandoff 与 FundamentalFactorSnapshot/EventExposure/CommodityResearchSnapshot 及端口；验证领域对象不握有 MyBatis/Mapper 依赖
- [ ] 2.3 在 `quantpf-domain.persistence` 新增 13 张表的 Record/Mapper/XML，主键 `BIGINT`（`ASSIGN_ID`）、五日志字段、`current_flag`/版本列映射；用 Mapper 集成测试插入/查询/只读校验成功且无 `AUTO_INCREMENT`

## 3. 事实核验与研究链服务

- [ ] 3.1 实现 `ClaimVerificationService`：从 `ai_research_draft` 升级事实候选、绑定 `EvidenceLinkService`、执行主张状态机与 `previous_claim_id`/`current_flag` 版本；验证"缺来源不得 VERIFIED"、"修订保留前驱"单测通过
- [ ] 3.2 实现 `ResearchChainService`（thesis/prediction/counter_evidence）：构建传导链/条件预测/反方，实现事实修订 → 依赖对象 `NEEDS_RECALCULATION`/`INVALIDATED` 失效传播；验证失效传播集成测试通过
- [ ] 3.3 实现 `adapter/mysql` 研究域 Adapter，复用模块一显式事务/幂等键，写命令带 `requestId`/`Idempotency-Key`/`expectedVersion`；验证重复创建主张/假设幂等返回原结果

## 4. 资产研究与候选筛选服务

- [ ] 4.1 实现 `FundamentalFactorCalculator` / `EventExposureMapper` / `CommodityResearchService`：确定性计算并标记 `DISCLOSED/CALCULATED/MODELLED/INFERRED`，无敞口不派生现货空单；验证因子按时点/版本可回放、推导值不冒充披露值
- [ ] 4.2 实现 `ScreeningRunner`：固定范围/规则版本/数据快照跑批，先写运行态→分批写评估→原子置完成；验证重复运行新建 `screening_run` 且可回放
- [ ] 4.3 实现 `CandidateAssessor` + `ResearchScorer`：硬门禁优先于评分，产四维 70 分制子分（事件可信度与新颖性 15/行业传导链 20/标的质量·供需质量 20/估值·预期差 15）；验证"硬门禁失败不得评分补偿"、"70 分不当总分"单测通过

## 5. 研究卡与审核服务

- [ ] 5.1 实现 `ResearchCardService`：聚合研究结论/多周期预期/统计字段引用模块四，`card_key` + `card_version_no` 版本化 + `current_flag` 单一当前版本；验证修改生成新版本、旧版本保留
- [ ] 5.2 实现 `ResearchReviewService`：CONFIRM/REJECT/DEFER/REQUEST_MORE/OVERRIDE，期望版本校验、覆盖范围受限于研究状态/排序/备注/有效期；验证版本过期拒绝写入、覆盖不改硬门禁

## 6. 交接与模块三端口

- [ ] 6.1 在 `quantpf-strategy` 暴露 `CandidateHandoffSink` 端口（不写实现），在 `quantpf-research` 实现 `CandidateHandoffService`：幂等键 + `handoff_status` 回执 + `correlationId/causationId`；验证重复交接返回原回执、不创建订单副作用
- [ ] 6.2 校验交接后候选不进入策略触发/交易计划/Paper 订单，架构测试断言 `quantpf-research` 无 `quantpf-execution/finance/risk` 依赖

## 7. Dify 研究型集成

- [ ] 7.1 复用模块一 `DifyResearchClient`/`DifyStreamConsumer`/`DifyCallbackVerifier`，新增研究型 Workflow 请求/输出构建（NEWS_IMPACT_ANALYSIS/STOCK_FUNDAMENTAL_RESEARCH/COMMODITY_SUPPLY_DEMAND/SPOT_BASIS_TERM_STRUCTURE/PRICE_VOLUME_RESEARCH/RESEARCH_CARD_EXPLANATION/RESEARCH_QA/REVIEW_SUMMARY）；验证最小上下文请求不含凭据
- [ ] 7.2 实现研究型输出校验扩展（引用→时间来→授权→身份→冲突），Dify 自报评分/因子/方向只落草稿、不写确定性表；验证合同测试"自报评分被忽略"
- [ ] 7.3 实现 `ResearchQaService`：Spring Boot 先查询授权范围结构化结果 → Dify 组织带引用回答 → 无候选时明确说明；验证问答不直连库、限额与降级生效

## 8. REST 门面与契约

- [ ] 8.1 在 `ruoyi-admin` 暴露 research/claims/theses/factors/screening/candidates/cards/reviews/handoffs/qa/dify 控制器 + DTO；接入 Sa-Token scope/权限、幂等、审计，控制器不直取 Mapper；验证越权访问被拒且多用户数据隔离
- [ ] 8.2 同步 `contract/openapi`、`contract/events`、`contract/menu`：研究/候选/交接 API、事件 Schema（含 eventId/schemaVersion/idempotencyKey）、`quant:research:*`/`quant:candidate:*` 权限码；验证契约与 13 表及服务职责一致

## 9. 任务调度与联调

- [ ] 9.1 在 `quantpf-job` 实现 `ResearchScreeningJobHandler`、`RevalidationJobHandler`、`HandoffRetryJobHandler`，仅调用应用服务、带幂等/审计；验证 Handler 无领域规则、无双栈调度
- [ ] 9.2 用 Mock + Stub Workflow 跑「研究任务→草稿→主张→研究链→因子→筛选→评估→研究卡→审核→交接→模块三回执」全链，验证可回放、幂等、点时正确
- [ ] 9.3 跑现有 Paper 下单/撤单/账务/风险/审计回归，确认模块二未新增真实 Broker/交易路径、`PaperRiskGate` 语义不变；更新技术分册 33/34 落地状态

## Workflow follow-up

- 实现完成后，对照 `research-candidate-db-design` 的 Flyway 迁移与 `research-candidate-code-guidance` 的服务边界做交叉验收（表↔实体↔服务一一对应）。
- 模块三策略评估与模块四验证结果主表建成后，补 `candidate_handoff`→模块三、`sample_result_ref`→模块四的正式 FK/端口契约。