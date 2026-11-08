# Tasks

> 本清单是模块二的统一实施入口，按用户指定顺序排列。`[后端]` 指 `D:\workspace\quant\RuoYi-Vue-Plus`，`[前端]` 指 `D:\workspace\quant\plus-ui`。实际后端迁移路径按仓库事实使用 `db/migration/`；前端接口按仓库 TypeScript 约定使用 `.ts`。TradingAgents 不作为独立业务阶段插入，而是在第 4 阶段作为 Dify 的按需研究引擎接入，并在第 7 阶段联调验收。

## 1. 数据库

- [ ] **1.1 数据库**：在 `[后端]/db/migration/` 按当前 Flyway head 递增创建模块二表迁移，包含 `quantpf_research_claim`、`quantpf_research_evidence_link`、`quantpf_research_thesis`、`quantpf_research_prediction`、`quantpf_research_counter_evidence`；验证新库和已有库迁移成功，且不修改已发布迁移。
- [ ] **1.2 数据库**：在 `[后端]/db/migration/` 创建资产研究迁移，包含 `quantpf_fundamental_factor_snapshot`、`quantpf_event_exposure`、`quantpf_commodity_research_snapshot`；验证字段类型、点时引用、状态 CHECK、业务唯一键和查询索引与 design.md 逐项对拍。
- [ ] **1.3 数据库**：在 `[后端]/db/migration/` 创建候选筛选/研究卡迁移，包含 `quantpf_screening_run`、`quantpf_candidate_assessment`、`quantpf_research_card`；验证硬门禁、70 分研究分、版本链、当前版本唯一性及 `SCORE_INCOMPLETE` 相关字段不能被默认补满。
- [ ] **1.4 数据库**：在 `[后端]/db/migration/` 创建审核/交接迁移，包含 `quantpf_research_review`、`quantpf_candidate_handoff`，并在 `[后端]/script/sql/` 增加状态/原因码/权限菜单幂等种子；验证 `expectedVersion`、审核理由、交接幂等键、correlation/causation 和 `quant:*` 权限可查询。
- [ ] **1.5 数据库**：复用模块一的 `quantpf_research_material`、`quantpf_research_task`、`quantpf_research_task_run`、`quantpf_dify_workflow_registry`、`quantpf_dify_invocation`、`quantpf_trading_agents_run`、`quantpf_trading_agents_agent_view`、`quantpf_ai_research_draft`、`quantpf_ai_validation_result`、`quantpf_research_event`、`quantpf_event_evidence` 及标的/行情/点时表，不重复建表；验证 `SHOW CREATE TABLE`、表清单和引用字段无同义重复。
- [ ] **1.6 数据库**：为 13 张模块二表落实统一规范：`BIGINT` 应用生成主键且无 `AUTO_INCREMENT`、UTC `DATETIME(6)`、金额/比例/评分 `DECIMAL`、末尾五个审计字段、内部父子关系约束、跨模块逻辑外键、唯一幂等索引和可检索状态索引；验证 Flyway 集成测试与 DDL 静态检查通过。

## 2. 后端实体

- [ ] **2.1 后端实体**：在 `[后端]/quantpf-modules/quantpf-domain/src/main/java/com/quantpf/domain/research/` 创建 `Claim.java`、`EvidenceLink.java`、`Thesis.java`、`Prediction.java`、`CounterEvidence.java`、`ResearchCard.java`、`ResearchReview.java`、`CandidateHandoff.java`、`ScreeningRun.java`、`CandidateAssessment.java`；验证实体字段、枚举、版本链和状态与 13 张表一致。
- [ ] **2.2 后端实体**：在 `[后端]/quantpf-modules/quantpf-domain/src/main/java/com/quantpf/domain/asset/` 创建 `FundamentalFactorSnapshot.java`、`EventExposure.java`、`CommodityResearchSnapshot.java`；验证 `DISCLOSED/CALCULATED/MODELLED/INFERRED`、点时 ID、资产类型及数值精度被正确映射。
- [ ] **2.3 后端实体**：在 `[后端]/quantpf-modules/quantpf-domain/src/main/java/com/quantpf/domain/repository/` 创建模块二 Repository 端口，并在 `[后端]/quantpf-modules/quantpf-domain/src/main/java/com/quantpf/domain/persistence/` 创建 13 张表的 Record/Mapper/XML 映射；验证 MyBatis-Plus `ASSIGN_ID`、日志自动填充、版本查询和当前版本查询集成测试通过。
- [ ] **2.4 后端实体**：为模块一 `TradingAgentsRun`/`AgentView` 与模块二研究任务、草稿建立只读关联 DTO/值对象，不把 TradingAgents Python 类或 Dify SDK 引入 domain；验证 domain 编译依赖图不依赖 Web、Dify、TradingAgents 实现模块。

## 3. 后端接口

- [ ] **3.1 后端接口**：在 `[后端]/quantpf-modules/quantpf-research/src/main/java/com/quantpf/research/claim/` 创建 `ClaimVerificationService.java`、`EvidenceLinkService.java` 及校验端口；验证草稿只能在 Schema/引用/时间/授权/身份/冲突检查后升级为主张，引用无法回源时被阻断。
- [ ] **3.2 后端接口**：在 `.../research/thesis/`、`.../asset/` 创建 `ResearchChainService.java`、`FundamentalFactorCalculator.java`、`EventExposureMapper.java`、`CommodityResearchService.java`；验证事实修订能传播 `NEEDS_RECALCULATION`，且 AI 不可修改确定性因子、暴露、供需、概率和统计值。
- [ ] **3.3 后端接口**：在 `.../research/screening/`、`.../card/` 创建 `ScreeningRunner.java`、`CandidateAssessor.java`、`ResearchScorer.java`、`ResearchCardService.java`；验证硬门禁先于评分，研究分严格为四维 70 分贡献，不被当成总 100 分或策略放行条件。
- [ ] **3.4 后端接口**：在 `.../research/review/`、`.../handoff/`、`.../qa/` 创建 `ResearchReviewService.java`、`CandidateHandoffService.java`、`ResearchQaService.java`；验证审核必须带 `expectedVersion`/理由，交接复用幂等键并只调用模块三端口，问答不接受订单/账户写意图。
- [ ] **3.5 后端接口**：在 `[后端]/ruoyi-admin/src/main/java/com/quantpf/web/controller/` 创建或扩展 `ResearchClaimController.java`、`ResearchCandidateController.java`、`ResearchCardController.java`、`ResearchReviewController.java`、`CandidateHandoffController.java`、`ResearchFactorController.java`、`ResearchQaController.java` 及 DTO；验证 Sa-Token/数据范围、幂等、版本冲突、错误码和 Controller 不直写 Mapper。
- [ ] **3.6 后端接口**：在 `[后端]/contract/` 更新 OpenAPI、事件、错误码和菜单权限契约；验证研究任务→候选→研究卡→审核→模块三交接 API 能按统一对象 ID、状态、版本和权限语义生成契约测试。

## 4. Dify 集成（含按需 TradingAgents）

- [ ] **4.1 Dify 集成**：在 `[后端]/quantpf-modules/quantpf-research/src/main/java/com/quantpf/research/dify/` 复用模块一 `DifyResearchClient`、`DifyStreamConsumer`、`DifyCallbackVerifier`，登记 `NEWS_IMPACT_ANALYSIS`、`STOCK_FUNDAMENTAL_RESEARCH`、`COMMODITY_SUPPLY_DEMAND`、`SPOT_BASIS_TERM_STRUCTURE`、`PRICE_VOLUME_RESEARCH`、`RESEARCH_CARD_EXPLANATION`、`RESEARCH_QA`、`REVIEW_SUMMARY`；验证只调用已发布、环境匹配的 Workflow，Key 不出现在前端、日志或代码仓。
- [ ] **4.2 Dify 集成**：在 `.../research/tradingagents/` 创建 `TradingAgentsAdapterClient.java`、`TradingAgentsRunService.java`、`TradingAgentsResultMapper.java`，对接内网 Adapter 的 `POST /internal/v1/research-runs`、状态查询、取消和健康接口；验证 REST/JSON、OpenAPI Schema、服务认证、`X-Request-Id`、`Idempotency-Key`、超时、限流、熔断和 `maxRounds=1..2`。
- [ ] **4.3 Dify 集成**：实现 Dify Workflow 的按需路由 Profile：简单摘要/抽取/卡片解释不调用 TradingAgents；复杂影响链、多空辩论、反方研究和重要研究卡深研才调用 `trading_agents_research`；验证一次任务最多创建一个有界 TA run，保留 `taRunId`、角色观点、引用、轮次、模型/Schema 版本和分歧。
- [ ] **4.4 Dify 集成**：实现 Dify/TradingAgents 输出 Envelope、原始响应对象引用与 `quantpf_ai_research_draft`/`quantpf_ai_validation_result` 持久化；验证模型自报评分、概率、硬门禁、机会状态和交易命令被忽略/标为草稿，引用无法回源不能升级事实或研究卡。
- [ ] **4.5 Dify 集成**：在 `application*.yml` 和环境变量模板补充 `quantpf.research.dify.*`、`quantpf.research.trading-agents.*`、Workflow Profile、并发/超时/重试/评分/交接配置；验证 Dify/TA 无 MySQL、Redis、DuckDB、MinIO 权威写权限，`trading-agents.enabled=false` 可安全降级。
- [ ] **4.6 Dify 集成**：编写 Dify SSE 完成/中断/超时、TA 部分角色失败/超时/取消、重复幂等、回调签名与重放防护的集成测试；验证 Dify/TA 不可用时确定性因子、硬门禁、筛选、既有研究卡和交接仍可运行，新 AI 结果进入待补充而非伪造结论。

## 5. 前端页面

- [ ] **5.1 前端页面**：在 `[前端]/src/views/quant/research/candidate/` 创建 `index.vue`、`detail.vue`，展示四类入口、筛选运行、硬门禁、70 分分项、淘汰原因和机会状态；验证普通用户看到候选≠信号≠订单，空结果显示门禁原因和下一步。
- [ ] **5.2 前端页面**：在 `[前端]/src/views/quant/research/card/` 创建 `index.vue`、`detail.vue`，展示结论、证据、反方、未知、多周期预期、版本、研究卡状态和审核动作；验证概率/策略/风险字段按来源标识，AI 说明显示草稿/待核验标签。
- [ ] **5.3 前端页面**：在 `[前端]/src/views/quant/research/claim/`、`thesis/`、`factor/` 创建 `index.vue`/`detail.vue`，展示证据回源、研究假设/预测/反方核查和确定性资产因子；验证引用可定位到材料版本/原文位置，不能回源时不显示为证据。
- [ ] **5.4 前端页面**：在 `[前端]/src/views/quant/research/handoff/` 创建 `index.vue`，展示候选交接、模块三回执、拒绝原因和补充入口；验证交接拒绝可恢复且页面不提供下单、开仓、平仓、QMT/CTP/LIVE 按钮。
- [ ] **5.5 前端页面**：在 `[前端]/src/views/quant/components/` 创建/复用 `ResearchCardStatus.vue`、`CandidateScorePanel.vue`、`ClaimEvidenceDrawer.vue`、`MultiHorizonView.vue`、`DifyRunStatus.vue`；验证普通/审核/专业角色按权限渐进披露字段，任务离开后可从 Spring Boot 状态恢复。
- [ ] **5.6 前端页面**：更新 `contract/menu/quantpf-menu-facts.yaml` 对应 Research 子菜单、权限和路由注册；验证动态菜单是唯一事实源，前端隐藏按钮不替代后端鉴权。

## 6. 前端接口

- [ ] **6.1 前端接口**：在 `[前端]/src/api/quant/` 创建 `researchCandidate.ts`、`researchCard.ts`、`researchClaim.ts`、`researchFactor.ts`、`researchReview.ts`、`handoff.ts`、`researchQa.ts`、`types.ts`；验证 DTO 与后端 OpenAPI 的 ID、状态、版本、分页和错误码一致。
- [ ] **6.2 前端接口**：为创建任务、深度研究、审核/覆盖、交接和问答封装 Spring Boot REST 调用，统一传递 `requestId`、`Idempotency-Key`、`expectedVersion` 和理由；验证超时重试复用同一幂等键，重复提交不产生副作用。
- [ ] **6.3 前端接口**：实现任务/Workflow/TA 运行进度查询或 SSE 状态订阅，只消费 Spring Boot 归一化事件；验证浏览器网络记录不出现 Dify API、TradingAgents Adapter URL、Bearer Key 或内部服务 Token。
- [ ] **6.4 前端接口**：实现 Dify/TA 降级、无候选、引用缺失、版本冲突、交接退回和权限不足的统一错误展示；验证用户能看到确定性数据、原因和下一步，而不是空白页或模型伪造结论。

## 7. 跨阶段联调与验收

- [ ] **7.1 跨阶段联调**：使用 JDK21 + Maven 启动 `[后端]` 的 `DromaraApplication`，使用 `pnpm dev` 启动 `[前端]`，加载 Mock Dify 与 TradingAgents Adapter；验证“研究任务→AI 草稿→事实核验→研究链→资产因子→筛选→候选→研究卡→审核→交接回执”完整链路。
- [ ] **7.2 跨阶段联调**：执行复杂影响链/多空辩论/反方深研与简单摘要对照测试；验证只有复杂 Profile 产生 TA run，简单任务不调用 TA，Dify 汇总后由 Spring Boot 保存 AI_DRAFT，多角色分歧完整保留。
- [ ] **7.3 跨阶段联调**：执行点时回放、数据未授权、身份冲突、引用失效、Dify/TA 超时、重复任务、旧版本审核、重复交接和模块三拒绝测试；验证所有 WHEN/THEN 场景、审计、错误码和恢复动作符合 `specs/research-candidate-delivery/spec.md`。
- [ ] **7.4 跨阶段联调**：运行后端单元/集成测试、Flyway 校验、前端 typecheck/lint/build 和既有 Paper 回归；验证不存在分钟/Tick/盘中交易、LIVE/QMT/CTP、真实 Broker submit/cancel、AI 直连数据库或候选直达下单路径。
- [ ] **7.5 跨阶段联调**：将实际启动命令、环境变量清单、API/页面验收结果和已知降级限制回写模块二详设及总纲索引；验证文档引用路径、版本和四份来源提案没有互相矛盾。

## Workflow follow-up

- 实施完成后运行 `/openspec-verify-change`，逐项核对本 change 的规格、设计、任务和代码实现。
- 验证通过后运行 `/openspec-archive-change`，再同步 capability spec；本 propose 阶段不执行源码实现或归档。
