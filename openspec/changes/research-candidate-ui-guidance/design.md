# Design

## Context

本设计承接 `proposal.md` 与三个 UI 行为规格，并承接 `data-foundation-ui-guidance` 已规划的 plus-ui 研究入口（研究任务/材料/AI 草稿/研究事件/研究助手）。前端基线为 plus-ui（Vue 3.5、Element Plus 2.14、vxe-table 4.x、ECharts 6.x、UnoCSS），经 RuoYi 动态菜单与 Sa-Token 权限工作；一期只展示日线业务能力，不提供分钟/Tick/盘中交易界面；Spring Boot 是业务状态主控，Dify 只经后端受控调用。

当前 `plus-ui/src/views` 尚无 `quant` 目录，`contract/menu/quantpf-menu-facts.yaml` 已有 Research 菜单与 Stage 1 禁止能力。本设计在既有 Research 菜单下扩展候选/研究卡/事实核验/资产研究/交接子页面，不另造菜单事实源。

## Goals / Non-Goals

**Goals:**

- 形成 plus-ui 可施工的模块二页面信息架构、路由、权限、API 映射、状态、交互与验收基线。
- 首屏面向引导型普通用户：先"结论/机会状态/下一步"，再"证据/风险"，最后专业详情。
- 为审核用户提供事实核验、研究卡审核/覆盖、候选交接的渐进式工作台。
- 让研究型 Dify 入口自然嵌入研究任务、研究卡、材料详情与全局助手，浏览器不接触 Dify。
- 把"无合格候选、数据未就绪、来源冲突、AI 草稿待核验、候选交接被退回、Paper 前置不满足"设计成明确可操作状态而非空表格。
- 支持 GuidedTask/状态时间线恢复，复用模块一公共组件。

**Non-Goals:**

- 不实现真实交易、LIVE/QMT/CTP、分钟/Tick、盘口实时图、订单流或复杂多图联动。
- 不在前端计算评分、因子、概率、硬门禁、策略或风险；页面只呈现服务端结果。
- 不在浏览器内嵌 Dify 页面或保存 Dify/API/数据源凭据。
- 不用 UI 简化绕过人工审核、模块三策略、模块四 RiskCheck、Paper 二次确认或审计。

## Decisions

### 1. 页面信息架构

在模块一 `views/quant/research/` 之下扩展模块二页面，复用模块一 `dashboard`、`market`、`research/task`、`research/draft`、`research/event`、`research/assistant` 与公共 `components/`：

```text
plus-ui/src/
├── api/quant/
│   ├── researchCandidate.ts   # 候选池/候选评估/筛选运行
│   ├── researchCard.ts        # 研究卡版本/审核动作
│   ├── researchClaim.ts       # 事实主张/证据链核验
│   ├── researchFactor.ts      # 基本面因子/事件暴露/商品研究结论
│   ├── researchReview.ts      # 人工审核记录
│   ├── handoff.ts             # 候选交接与回执
│   ├── researchQa.ts          # 研究助手问答
│   └── types.ts
├── views/quant/research/
│   ├── candidate/index.vue    # 候选池（四类入口、筛选、硬门禁/评分、机会状态）
│   ├── candidate/detail.vue   # 候选详情（分项评分、淘汰原因、证据）
│   ├── card/index.vue         # 研究卡列表
│   ├── card/detail.vue        # 研究卡详情（结论/证据/风险/多周期/版本）
│   ├── claim/index.vue        # 事实主张与证据链核验
│   ├── thesis/detail.vue      # 研究假设/预测/反方证据
│   ├── factor/index.vue       # 资产研究结果页
│   └── handoff/index.vue      # 候选交接记录/回执
└── components/
    ├── ResearchCardStatus.vue # 研究卡状态卡（结论/机会/风险/下一步）
    ├── CandidateScorePanel.vue# 70 分制分项评分面板（只读，权限展开）
    ├── ClaimEvidenceDrawer.vue# 主张↔证据回源抽屉
    └── MultiHorizonView.vue   # 短期/中期/长期预期
```

菜单细分（在既有 Research 菜单下）：

| 父菜单 | 子页面 | 主要角色 |
|---|---|---|
| Research | 快捷研究/研究任务/AI 草稿/研究事件（模块一已有） | 普通/审核 |
| Research | 候选池 / 研究决策卡 / 事实核验 / 资产研究 / 候选交接 | 普通（候选/研究卡查看）、审核（核验/交接）、专业（因子/评分分项） |

页面显示名称用用户语言；字段名、Workflow 版本、快照、原始引用只在审核/专业权限展开。

### 2. 全局布局和状态条

复用模块一全局状态条，模块二在研究相关页面叠加：

```text
当前用户/体验模式 | 当前研究任务/候选/研究卡及版本
数据基准日期/市场本地日期与时区 | 最新日线新鲜度
候选状态（RESEARCH_READY 等） | Paper 前置/不连接真实交易提示
Dify/任务降级标记（相关页面）
```

公共"研究卡状态卡"结构：

```text
结论（普通语言） → 机会状态 → 关键风险 → 数据日期/版本 → 下一步 → 证据/专业详情
```

状态颜色只作辅助，必须同文字、图标、原因并列；候选、研究卡、交接、策略、订单状态用不同色/文案不混用。

### 3. 核心页面设计

#### 3.1 候选池 `/quant/research/candidate`

- 筛选：资产类型、行业/品种、来源入口（事件/股票/商品/量价）、方向、研究周期、候选阶段、机会状态、证据完整性、更新时间；
- 列表列：标的/对象、资产类型、方向、研究周期、机会状态、研究分（只显示汇总 + 展开分项）、淘汰原因、数据日期；
- 详情：硬门禁逐项结果、`CandidateScorePanel`（事件可信度与新颖性 15/行业传导链 20/标的质量·供需质量 20/估值·预期差 15，可展开原始证据）、原因码与下一步；
- 候选阶段与 `WATCHLIST`/`TRADE_READY` 明确区分：后两者属模块三，页面不冒称模块二可写；
- 淘汰原因展示为用户可读文案 + 受影响门禁 + 是否可重试，不只显示原因码。

#### 3.2 研究决策卡 `/quant/research/card`

- 结论区：普通语言结论、机会状态（可研究/等待回调/等待突破/机会已过/仅观察/数据不足等）、消息有效性与价格反应分开展示；
- 证据区：支持/反方证据、关键未知项、证伪条件；`ClaimEvidenceDrawer` 回源到来源/材料版本/原文片段/时间；
- 多周期区：`MultiHorizonView` 展示短期/中期/长期条件式预期及各自成立/失效条件，期货提示具体合约与换月影响；
- 统计区：样本数、条件概率/置信区间、成本后预期、最大不利波动、样本外状态，样本不足显示"样本不足，无法验证"；
- 版本区：`VersionTimeline` 显示研究卡版本与审核历史；当前版本与历史版本可对比；
- 审核区（审核用户）：确认/否决/延期/请求补充/受限覆盖，均需理由与期望版本。

#### 3.3 事实主张与核验 `/quant/research/claim`

三栏布局（复用模块一草稿页风格）：

```text
左：主张列表 + 状态（待核验/已核验/冲突/修订/失效）
中：主张陈述 + 影响变量 + 归属标的/行业
右：证据链（证据类型、来源、原文片段、发布时间/可见时间、来源独立性分组）
```

顶部固定标签：`AI 草稿 / 待核验 / 已核验事实 / 用户备注`。审核用户可确认、标记冲突、要求补证据；普通用户只读。修订显示 `previous_claim_id` 前驱链，不覆盖历史结论。

#### 3.4 资产研究结果 `/quant/research/factor`

按资产类型分区展示服务端确定性结果：

```text
股票：业务真实性/盈利质量/估值/事件敏感度，标注 DISCLOSED/CALCULATED/MODELLED/INFERRED 与报告期
商品：产业判断/价格判断/工具表达 + 现货敞口类型 + 具体合约 + 交割/移仓风险
事件暴露：行业/商品/标的暴露、方向、敏感度、估算方法与证据
```

页面不提供字段编辑；修正走来源补采/人工核验/新版本流程。 "用当前证据解释"按钮触发 Dify 生成解释草稿。

#### 3.5 人工审核工作台

审核入口并入研究卡/候选详情，并汇总待办：

```text
待核验事实 / 待审核研究卡 / 冲突 / 交接待处理 / 过期项
```

每项审核动作记录操作者、理由、对象版本、前后状态；版本过期拒绝写入并刷新；覆盖只允许研究状态/排序/备注/有效期，页面明确提示"不能改变标的身份、授权或风险门禁"。

#### 3.6 候选交接 `/quant/research/handoff`

- 列表：候选、目标模块（策略）、交接状态（待验证/已交接/被拒绝/过期/失效）、交接时间、回执；
- 详情：交接 payload 摘要、`correlationId/causationId`、被拒绝时原因与补充要求；
- 交接成功后文案明确"已交模块三策略评估，后续由模块三/四执行"，不显示为已下单/已授 Paper。

### 4. Dify 用户触发位置（模块二）

Dify 不以 iframe、前端直连 API 或独立外部页嵌入。模块二研究型触发入口固定为：

| 入口 | 按钮 | Workflow 类型 | 结果与边界 |
|---|---|---|---|
| 研究任务向导 | 开始 AI 研究 | 事件影响分析/股票/商品/量价研究 | 研究任务 + AI 草稿（待核验） |
| 新闻/材料详情 | 分析影响、提取事实 | `NEWS_IMPACT_ANALYSIS` | 影响变量/传导链草稿 |
| 资产研究页 | 用当前证据解释 | 股票/商品/量价/现货解释 | 解释草稿，不改计算结果 |
| 研究卡页 | 用当前证据解释 | `RESEARCH_CARD_EXPLANATION` | 普通语言说明，只读已审卡 |
| 全局研究助手 | 提交问题 | `RESEARCH_QA` | 证据化回答，先由 Spring Boot 查询 |
| 复盘（模块四入口） | 生成复盘摘要 | `REVIEW_SUMMARY` | 文字草稿，交易事实后端维护 |

所有按钮显示当前数据日期、授权/质量状态与预计下一步。Dify 不可用时按钮变为"稍后重试/查看结构化数据"。确定性候选/评分/筛选不依赖 Dify，降级不影响其展示。

### 5. API 与页面状态映射

前端 API 只调 Spring Boot（研究域）：

```text
GET/POST /api/quant/research/claims/*
GET/POST /api/quant/research/theses/*
GET /api/quant/research/factors/*
GET/POST /api/quant/research/screening/*
GET/POST /api/quant/research/candidates/*
GET /api/quant/research/cards/*
POST /api/quant/research/cards/*/review
GET /api/quant/research/handoffs/*
POST /api/quant/research/qa/query
GET /api/quant/research/dify/*          # 继承模块一
```

写请求带 `requestId / Idempotency-Key / expectedVersion / reason`（审核与覆盖）。状态映射：

| 后端状态 | 普通用户文案 | 可执行动作 |
|---|---|---|
| `RESEARCH_READY` / PASS | 可进入策略评估 | 查看/交接 |
| `REJECT` | 已淘汰 | 查看淘汰原因 |
| `DEFER` | 等待数据/事件 | 稍后处理/查看原因 |
| `OPPORTUNITY_PASSED` | 机会已过 | 仅观察 |
| `DATA_INSUFFICIENT` | 数据不足 | 查看缺什么 |
| `AI_ANALYSIS_PENDING`/`CLAIM_UNVERIFIED` | AI 草稿待核验 | 查看/等待审核 |
| `HANDOFF_REJECTED` | 交接被退回 | 查看原因/补充 |
| `PAPER_BLOCKED` | 暂不能生成模拟建议 | 查看阻断原因 |

### 6. 权限和渐进披露

模块二建议权限（继承模块一）：

```text
quant:research:claim:view / review
quant:research:candidate:view / review
quant:research:card:view / confirm / reject / defer / override
quant:research:factor:view / raw:view
quant:research:handoff:view / manage
quant:research:qa:use
quant:research:raw:view
```

- 引导模式：结论、机会状态、风险、下一步；
- 审核模式：证据、版本、冲突、核验/审核/交接动作；
- 专业模式：分项评分、因子、快照、Workflow/Prompt 版本、原始引用摘要、审计。

敏感值（Dify Key、数据库凭据、供应商 Token、完整原文）永不因切换模式返回明文；研究候选/研究卡不得显示"买入/卖出/下单"，界面只提供"交接策略评估/查看后续"。

### 7. 组件与交互约定

在模块一公共组件基础上新增：`ResearchCardStatus`（统一结论/机会/风险/下一步）、`CandidateScorePanel`（70 分制分项，只读）、`ClaimEvidenceDrawer`（主张回源）、`MultiHorizonView`（短/中/长期）；复用 `EvidenceDrawer`、`VersionTimeline`、`GuidedTaskStepper`、`DifyRunStatus`、`PermissionAction`。

加载/空/错误/过期/无权限/降级不能只显示空表格，必须给人话解释和下一步；写命令提交后按钮锁定，超时以同一幂等键查询，不重复发送；审核/覆盖按钮在版本变化后禁用并要求刷新。

### 8. 可访问性与响应式

- 关键状态（机会状态、草稿/事实区分、交接回执）同时用文字、图标和颜色；
- 表格支持键盘焦点、筛选条件保留、分页恢复；窄屏优先展示状态卡与下一步，专业表格转抽屉；
- 日期同时显示市场本地日期、时区和数据来源时间；
- `requestId/任务ID` 可复制联系管理员，但数据库主键不作授权凭证。

## Risks / Trade-offs

- **[Risk] 页面过多导致普通用户迷失** → 工作台 + 研究卡状态卡为默认入口；候选池专业列默认折叠，审核/专业页面按权限展开。
- **[Risk] 过度简化掩盖研究风险** → 阻断/淘汰/机会已过/样本不足强制显示原因、日期、影响与证据入口；候选≠交易信号文案固定。
- **[Risk] AI 草稿被当买卖建议** → 固定标签区分草稿/事实/信号，无直达订单路径，研究卡页只提供"交接策略评估"。
- **[Risk] 前端菜单与权限事实源漂移** → 继续以 `contract/menu/quantpf-menu-facts.yaml` 为源，页面路由/按钮契约 CI 校验。
- **[Risk] 一期误展示实时或交易能力** → 菜单不提供分钟/Tick/盘中与真实下单；数据日期与一期日线边界固定显示。
- **[Risk] Dify 交互与刷新不同步** → 一期 REST 轮询 + 任务/草稿状态机，二期再加 SSE/WebSocket，不改变 Spring Boot 主控。

## Migration Plan

1. 在 `contract/menu/quantpf-menu-facts.yaml` 增加模块二候选/研究卡/事实核验/资产研究/交接子页面与权限，不增加禁止能力。
2. 在 plus-ui 建立 `api/quant` 与 `views/quant/research/{candidate,card,claim,thesis,factor,handoff}` 骨架，先接候选池与研究卡只读页。
3. 接入事实核验、研究卡审核、候选交接 API，完成审核/普通/专业权限态与空/错误/阻断状态。
4. 接入研究型 Dify 入口（研究卡解释、研究助手、资产研究解释），先用 Mock API 完成交互合同测试与引用回源。
5. 运行用户旅程验收：普通用户低输入到研究卡、审核覆盖、交接回执、Dify 降级、Paper 前置阻断、版本冲突与安全扫描。
6. 完成一期日线边界检查后评估二期实时/多资产；不因 UI 完成自动解锁真实交易或二期量价能力。

回滚：页面按菜单/权限和 Feature Flag 独立关闭；关闭 Dify 入口不影响确定性候选/研究卡展示；保留任务/草稿状态与用户输入，不删除后端事实。

## Open Questions

以下不改变页面边界或安全契约，可在产品设计/实施前确定：

- 研究卡状态卡最终字段顺序与候选池默认列；
- 分项评分面板是否默认展开还是仅在专业模式可展开；
- 研究助手抽屉 vs 独立页的最终形态；
- 轮询间隔与二期 SSE/WebSocket 是否启用；
- 审核用户批量操作上限与交接列表分页大小。