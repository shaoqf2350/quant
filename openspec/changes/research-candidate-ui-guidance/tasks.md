# Tasks

## 1. 菜单契约与工程骨架

- [ ] 1.1 在 `contract/menu/quantpf-menu-facts.yaml` 增加候选池/研究卡/事实核验/资产研究/候选交接子页面与 `quant:research:*` 权限码，不增加禁止能力；验证菜单事实源与 `sys_menu` 种子一致
- [ ] 1.2 在 plus-ui 建立 `src/api/quant/` 与 `src/views/quant/research/{candidate,card,claim,thesis,factor,handoff}` 目录骨架与路由占位；验证 `pnpm dev` 可启动、路由可访问且无编译错误

## 2. API 模块与类型

- [ ] 2.1 实现 `api/quant/researchCandidate.ts` / `researchCard.ts` / `researchClaim.ts` / `researchFactor.ts` / `researchReview.ts` / `handoff.ts` / `researchQa.ts` 与 `types.ts`（DTO 不外泄 Record/主键作权限源）；验证类型与 `contract/openapi` 对齐
- [ ] 2.2 请求封装统一带 `requestId`/`Idempotency-Key`（写）/`expectedVersion`/`reason`，超时以同一幂等键查询；验证重复提交不触发重复副作用

## 3. 公共组件

- [ ] 3.1 实现 `ResearchCardStatus`（结论/机会状态/风险/下一步）、`CandidateScorePanel`（70 分制分项，只读）、`ClaimEvidenceDrawer`（主张回源）、`MultiHorizonView`（短/中/长期）；验证组件在空/缺字段/样本不足时给出普通语言提示而非空白
- [ ] 3.2 复用模块一 `EvidenceDrawer`/`VersionTimeline`/`GuidedTaskStepper`/`DifyRunStatus`/`PermissionAction`，确认候选/研究卡状态与草稿/事实/信号文案不混用

## 4. 候选池页面

- [ ] 4.1 实现 `/quant/research/candidate/index.vue`：资产类型/入口/方向/周期/阶段/机会状态/证据完整性筛选与列表；验证空状态显示"当前没有满足门禁的机会"及原因而非空表
- [ ] 4.2 实现 `candidate/detail.vue`：硬门禁逐项结果、分项评分面板、淘汰原因码翻译为用户可读文案+受影响门禁+是否可重试；验证原因码与字典一致、评分不以按钮方式修改

## 5. 研究卡页面

- [ ] 5.1 实现 `/quant/research/card/index.vue` 与 `card/detail.vue`：结论/机会状态/消息有效性与价格反应分开展示、支持/反方证据、多周期预期、统计区（样本不足显示"样本不足，无法验证"）、版本时间线与对比；验证版本切换可回溯历史
- [ ] 5.2 研究卡审核区（确认/否决/延期/请求补充/受限覆盖）带理由与 expectedVersion；验证版本过期拒绝提交、覆盖范围不包含身份/授权/风险

## 6. 事实核验页面

- [ ] 6.1 实现 `/quant/research/claim/index.vue` 三栏：主张列表+状态、主张陈述+影响变量、证据链（证据类型/来源/原文片段/时间/独立性分组）；验证顶部标签区分 AI 草稿/待核验/已核验事实/用户备注
- [ ] 6.2 实现 `thesis/detail.vue`（研究假设/预测/反方）与修订前驱链展示；验证事实修订后依赖假设/预测标记需重算且旧版本可查看

## 7. 资产研究结果页

- [ ] 7.1 实现 `/quant/research/factor/index.vue`：股票基本面因子、事件暴露/敏感度、商品研究结论分区展示；验证 DISCLOSED/CALCULATED/MODELLED/INFERRED 与报告期/点时版本明确，无敞口显示 NONE 不派生现货空单
- [ ] 7.2 资产研究页"用当前证据解释"触发研究型 Dify（后端调用），结果为解释草稿、不改计算结果；验证草稿可回源、不可编辑因子值

## 8. 交接页面

- [ ] 8.1 实现 `/quant/research/handoff/index.vue`：交接状态（待验证/已交接/被拒绝/过期/失效）、目标模块、回执、`correlationId`、拒绝原因与补充项；验证交接成功文案明确"已交策略评估，后续由模块三/四执行"，不显示下单语义

## 9. Dify 研究型入口与助手

- [ ] 9.1 实现研究任务/材料/资产研究/研究卡页的研究型 Dify 触发按钮（开始 AI 研究、分析影响、用当前证据解释），全部经 Spring Boot、浏览器不持 Key；验证前端无 Dify/API Key 常量或直连地址
- [ ] 9.2 实现 `research/assistant` 研究助手（复用模块一入口，扩展 `researchQa.ts`）：Spring Boot 先查询→Dify 组织回答→展示引用与状态；验证无候选时回答"当前没有满足门禁的机会"及原因、降级时显示"稍后重试/查看结构化数据"

## 10. 用户旅程验收与安全

- [ ] 10.1 运行普通用户旅程：低输入（≤3 项）研究到研究卡、GuidedTask 中断恢复、候选/研究卡不显示为交易信号、专业字段默认折叠；验证验收基线通过
- [ ] 10.2 运行审核/交接旅程：事实核验、研究卡审核/受限覆盖、交接被模块三拒绝回执、Paper 前置阻断；验证每次审核有理由与版本、不越硬门禁
- [ ] 10.3 安全扫描：前端无 Dify/数据库凭据、无真实下单路由、菜单权限与按钮一致、一期无分钟/Tick/盘中能力；更新技术分册 33/34 落地状态并跑 `pnpm build` 通过

## Workflow follow-up

- 实现完成后与 `research-candidate-code-guidance` 的 API/事件契约、`research-candidate-db-design` 的 13 表状态枚举做三方一致性校验（页面状态↔后端状态↔DB CHECK）。
- 模块三策略/模块四验证页面上线后，再补交接详情与统计字段的正式展示契约。