# Design

## Context

本设计承接 `proposal.md` 与三个 UI 行为规格，并以模块五详设和已确认前端基线为约束：前端使用 plus-ui（Vue 3.5、Element Plus、vxe-table、ECharts 6、UnoCSS），通过 RuoYi 动态菜单和权限体系工作；一期只展示日线业务能力，不提供分钟/Tick/盘中交易界面；后端 Spring Boot 是业务状态主控，Dify 只能通过后端受控调用。

当前 `contract/menu/quantpf-menu-facts.yaml` 已有 Dashboard、Research、Market、Strategy、Backtest、Paper Account、Paper Order、Risk、Audit 菜单和 Stage 1 禁止能力（LIVE/QMT/CTP/真实 Broker 订单）。本设计在这些既有菜单下细分页面，不另造一套前端菜单事实源。

## Goals / Non-Goals

**Goals:**

- 形成 plus-ui 可施工的页面信息架构、路由、权限、API 映射、状态、交互和验收基线。
- 首屏面向非技术、非金融专业的引导型普通用户：先结论/状态，再原因/影响，再专业详情。
- 为管理员、审核用户、专业用户提供渐进展开的数据源、质量、标的、日历、证据和任务能力。
- 让 Dify 入口自然嵌入研究任务、材料详情、标的详情和全局助手，但不让浏览器接触 Dify。
- 把“数据未就绪、授权不明、来源冲突、身份未确认、AI 草稿待核验、Paper 前置不满足”设计成明确可操作的状态，而不是空表格。
- 支持 GuidedTask/状态时间线恢复，减少普通用户重复输入。

**Non-Goals:**

- 不实现真实交易、LIVE/QMT/CTP、分钟/Tick、盘口实时图、订单流工作台或复杂多图联动。
- 不在前端计算指标、策略、风险、日历资格、仓位或成交；页面只呈现服务端结果。
- 不在浏览器内嵌 Dify 页面或保存 Dify/API/数据源凭据；不实现外部微信公众号/WxPusher。
- 不用 UI 简化绕过人工审核、RiskCheck、Paper 二次确认或审计。

## Decisions

### 1. 页面信息架构

沿用现有菜单事实源，在 plus-ui 下建议新增页面目录：

```text
plus-ui/src/
├── api/quant/
│   ├── dashboard.ts
│   ├── dataSource.ts
│   ├── dataQuality.ts
│   ├── snapshot.ts
│   ├── instrument.ts
│   ├── calendar.ts
│   ├── market.ts
│   ├── researchTask.ts
│   ├── dify.ts
│   ├── researchEvent.ts
│   └── types.ts
├── views/quant/
│   ├── dashboard/index.vue
│   ├── data/
│   │   ├── source/index.vue
│   │   ├── source/detail.vue
│   │   ├── quality/index.vue
│   │   ├── snapshots/index.vue
│   │   └── readiness/index.vue
│   ├── instrument/
│   │   ├── index.vue
│   │   └── detail.vue
│   ├── calendar/
│   │   ├── index.vue
│   │   ├── session.vue
│   │   └── conflict.vue
│   ├── market/
│   │   ├── index.vue
│   │   └── detail.vue
│   ├── research/
│   │   ├── task/index.vue
│   │   ├── task/detail.vue
│   │   ├── material/detail.vue
│   │   ├── draft/detail.vue
│   │   ├── event/detail.vue
│   │   └── assistant/index.vue
│   └── components/
│       ├── StatusSummaryCard.vue
│       ├── DataFreshnessBadge.vue
│       ├── SourceQualityBanner.vue
│       ├── EvidenceDrawer.vue
│       ├── VersionTimeline.vue
│       ├── GuidedTaskStepper.vue
│       ├── DifyRunStatus.vue
│       └── PermissionAction.vue
└── router/permission.ts           # 复用动态路由/权限，不新增旁路鉴权
```

菜单细分建议：

| 既有菜单 | 子页面 | 主要角色 |
|---|---|---|
| Dashboard | 工作台、数据健康、待办、Dify/任务状态 | 全部 |
| Market | 行情与数据日期、数据源健康、数据质量、交易日历、标的中心 | 普通/审核/专业 |
| Research | 快捷研究、研究任务、AI草稿、研究事件、证据链、研究助手 | 普通/审核 |
| Audit | 数据采集/研究/Dify/审核操作时间线 | 审核/专业 |

页面显示名称使用用户语言；技术字段、接口字段、原始快照和供应商配置只在审核/专业权限下展开。

### 2. 全局布局和状态条

所有量化页面顶部固定显示：

```text
当前用户与体验模式 | 当前 Paper 账户（如适用）
数据基准日期/市场本地日期与时区 | 最新日线日期/新鲜度
当前系统/数据/Dify/任务状态 | 模拟资金/不连接真实交易提示
```

全局状态条不使用单一绿色“系统正常”替代子状态。状态颜色必须同时配文字、图标和原因；颜色含义遵循可访问性，不以红绿作为唯一信息。

公共状态卡结构：

```text
状态标题
普通语言结论
数据日期/版本
为什么
影响哪些功能
系统已经完成什么
下一步按钮
专业详情/证据入口
```

### 3. 核心页面设计

#### 3.1 量化工作台 `/quant/dashboard`

布局从上到下：

1. **今日结论**：可研究市场、数据是否到齐、是否暂不生成模拟建议；
2. **数据健康**：最新日线、来源健康、质量问题、标的映射异常；
3. **研究待办**：待审核草稿、冲突、失败任务、过期任务；
4. **Dify 状态**：可用/降级/排队/限额，提供“查看研究任务”，不显示 Key；
5. **Paper 前置**：仅显示数据前置是否满足，不显示模块一直接下单按钮；
6. **时间线/通知**：按权限显示最近关键变化。

普通用户只看到摘要和下一步；管理员可以进入数据源/质量；审核用户可以进入冲突/草稿审核。

#### 3.2 数据源 `/quant/market/data-source`

- 列表字段：来源名称、资料类型、授权状态、主/备角色、健康状态、最新成功时间、影响范围；
- 管理员操作：新增/编辑目录、测试连接、启用申请、切换主备、暂停、查看历史；
- 普通用户：只读状态摘要；
- 专业详情：接口/字段/额度/历史范围/保存权，但 Token 只显示“已配置/未配置”；
- 连接测试成功不等于可用，页面必须同时显示授权和字段确认状态。

#### 3.3 数据质量 `/quant/market/data-quality`

采用“问题列表 + 影响抽屉 + 处理面板”：

- 列表：严重度、问题类型、对象、状态、影响功能、发现时间、责任角色；
- 影响抽屉：受影响的标的/日历/研究任务/Paper 前置；
- 审核动作：分析、等待来源、等待人工、解决、验证、关闭；
- 所有变更需要理由、expectedVersion、审计结果；
- 普通用户看到“为什么不能继续、系统做了什么、下一步”。

#### 3.4 标的中心 `/quant/market/instrument`

搜索支持代码、名称、交易所、资产类型、状态。详情页分为：

```text
身份摘要 → 交易属性版本 → 具体/连续关系 → 日历/时段绑定
→ 公司行为/合约事件 → 数据就绪 → 研究/Paper资格
```

连续/主力合约用明显标签“仅分析/筛选”，不提供执行按钮。股票、期货、现货信息分别展示，不能用相同字段文案混淆。

#### 3.5 交易日历 `/quant/market/calendar`

- 日历视图：市场本地日期、开闭市、特殊原因、上一/下一交易日；
- 时段视图：集合竞价、连续交易、午休、夜盘、跨日归属；
- 就绪视图：应到/已到/已校验/策略可用/Paper 可用；
- 冲突视图：来源对比、版本、审核状态；
- 普通用户只看状态卡；审核用户进入冲突处理；管理员处理来源和人工维护，尤其 GFEX 日历。

#### 3.6 行情与数据日期 `/quant/market`

一期只提供日线：

```text
标的搜索/筛选
→ 数据日期/来源/复权口径/质量
→ OHLCV 和市场状态
→ 版本/快照/证据
→ 研究前置状态
```

分钟、Tick、盘中交易菜单不在一期启用。字段缺失显示 `PARTIAL/DELAYED` 和影响范围，不把空值渲染为 0 或实时值。

#### 3.7 研究任务 `/quant/research/task`

页面采用向导：

```text
Step 1 选择对象/材料
Step 2 输入问题与周期（核心输入最多 3 项）
Step 3 系统准备材料、身份、授权、点时和数据资格
Step 4 用户确认后提交研究任务
Step 5 查看 Dify 状态/草稿/核验/事件
```

用户离开页面后通过 `GuidedTask` 恢复，已确认字段不重复询问。任务列表必须区分：准备中、排队、分析中、待核验、待审核、已完成、可重试、降级。

#### 3.8 AI 草稿/研究事件/证据

AI 草稿页三栏：

```text
左：任务与运行状态/Workflow 版本
中：草稿结论、事实候选、影响变量、反方问题
右：引用、原文/字段片段、发布时间/可见时间、质量/授权
```

顶部固定标签：

```text
AI草稿 / 待核验 / 已核验事实 / 已发布研究事件 / 未知
```

审核用户操作：确认、拒绝、要求补证据、标记冲突、延期；普通用户只能查看。事件发布后明确“可交给策略评估”，不能显示为买卖指令。

#### 3.9 全局研究助手 `/quant/research/assistant`

建议采用右侧抽屉或独立页面，不嵌入 Dify UI：

```text
用户问题
→ Spring Boot 权限/scope/意图门禁
→ 结构化事实查询
→ Spring Boot 服务端调用 Dify
→ 返回回答草稿+引用+状态
→ 前端展示并提供“创建研究任务/查看证据”按钮
```

对“开始分析”“加入关注”等操作意图，Dify 只返回意图草稿；前端必须转为明确确认表单，最终由 Spring Boot 执行。

### 4. Dify 用户触发位置

Dify 不以 iframe、前端直连 API 或独立外部页面嵌入。用户触发入口固定为：

| 入口 | 按钮 | Workflow 类型 | 结果 |
|---|---|---|---|
| 研究任务向导 | 开始 AI 研究 | 事件事实/影响/股票/商品研究 | 研究任务 + AI 草稿 |
| 新闻/材料详情 | 提取事实、分析影响 | `NEWS_FACT_EXTRACTION`/`NEWS_IMPACT_ANALYSIS` | 草稿，待核验 |
| 标的/行情详情 | 解释当前状态 | 研究卡说明/量价解释 | 解释草稿，不改状态 |
| 全局研究助手 | 提交问题 | `RESEARCH_QA` | 证据化回答 |
| 已发布研究事件 | 补充反方问题 | 研究解释/复核 | 新研究任务，不覆盖旧事件 |

所有按钮都显示当前数据日期、授权/质量状态和预计下一步。Dify 不可用时按钮变为“稍后重试/查看结构化数据”，不隐藏失败。

### 5. API 与页面状态映射

前端 API 只调用 Spring Boot：

```text
GET/POST /api/quant/data/source/*
GET/POST /api/quant/data/quality/*
GET /api/quant/instrument/*
GET /api/quant/calendar/*
GET /api/quant/market/*
GET/POST /api/quant/research/tasks/*
GET /api/quant/research/dify/*
POST /api/quant/research/event/*/review
GET /api/quant/research/assistant/context
POST /api/quant/research/assistant/query
```

每个写 API 请求带：

```text
requestId
Idempotency-Key
expectedVersion（状态变更时）
reason（审核/专业操作）
```

前端状态映射：

| 后端状态 | 普通用户文案 | 可执行动作 |
|---|---|---|
| `AVAILABLE` | 可以研究 | 查看/创建研究 |
| `PENDING_CONFIRMATION` | 数据源待确认 | 联系管理员/查看原因 |
| `DELAYED`/`STALE` | 数据较旧，暂不能生成新建议 | 查看历史/稍后重试 |
| `CONFLICTED` | 来源存在冲突，等待确认 | 查看原因 |
| `UNAUTHORIZED` | 当前数据未获授权 | 联系管理员 |
| `AI_ANALYSIS_PENDING` | AI 分析排队中 | 稍后查看 |
| `SCHEMA_INVALID`/`CLAIM_UNVERIFIED` | 分析结果待核验 | 查看/等待审核 |
| `PAPER_BLOCKED` | 暂不能生成模拟建议 | 查看阻断原因 |

### 6. 权限和渐进披露

建议权限：

```text
quant:data:view
quant:data:source:view / manage / test
quant:data:quality:view / handle
quant:instrument:view / manage
quant:calendar:view / manage / conflict-review
quant:market:view
quant:research:task:view / create / retry / cancel
quant:research:draft:view / review
quant:research:event:view / review / publish
quant:research:assistant:use
quant:research:raw:view
```

- 引导模式：结论、原因、影响、下一步；
- 审核模式：证据、版本、冲突、审核动作；
- 专业模式：接口字段、快照、Schema、Workflow、原始响应摘要、审计。

敏感数据永不因切换专业模式而显示明文：Token、Dify Key、数据库连接和完整 Cookie 永不返回。

### 7. 组件与交互约定

公共组件职责：

- `StatusSummaryCard`：统一状态/原因/影响/下一步；
- `DataFreshnessBadge`：日期、时区、延迟、质量；
- `SourceQualityBanner`：授权/冲突/过期阻断；
- `EvidenceDrawer`：从结论回到来源/快照/版本；
- `VersionTimeline`：对象状态和审核时间线；
- `GuidedTaskStepper`：恢复任务和低输入引导；
- `DifyRunStatus`：任务运行、草稿、失败/降级；
- `PermissionAction`：按钮权限和服务端错误统一反馈。

加载/空/错误/过期/无权限/降级不能只显示表格空状态；必须提供人话解释和下一步。写命令提交后按钮锁定，超时以同一幂等键查询，不重复发送。

### 8. 可访问性与响应式

- 关键状态同时使用文字、图标和颜色；
- 表格支持键盘焦点、筛选条件保留、分页恢复；
- 移动/窄屏优先显示状态卡和下一步，专业表格转抽屉/横向滚动；
- 日期同时显示市场本地日期、时区和数据来源时间；
- 用户可复制 requestId/任务 ID 联系管理员，但内部数据库主键不是授权凭证。

## Risks / Trade-offs

- **[Risk] 页面过多导致普通用户迷失** → 工作台、状态卡和 GuidedTask 作为默认入口；专业页面按权限和渐进展开隐藏。
- **[Risk] 过度简化掩盖数据风险** → 所有阻断状态强制显示原因、日期、影响和证据入口；Paper/Risk 二次确认不减少。
- **[Risk] Dify 交互状态与页面刷新不同步** → 一期 REST 轮询 + 任务状态机；二期再加 SSE/WebSocket，不改变 Spring Boot 主控。
- **[Risk] 用户把 AI 草稿当买卖建议** → 固定标签、颜色/文案、禁用直达订单路径，并在研究事件页明确策略/风险后续步骤。
- **[Risk] 前端菜单与权限事实源漂移** → 菜单继续以 `contract/menu/quantpf-menu-facts.yaml` 为源，页面路由/按钮契约 CI 校验。
- **[Risk] 一期误展示实时能力** → 菜单和页面不提供分钟/Tick/盘中操作；数据日期和一期日线边界固定显示。

## Migration Plan

1. 在 `contract/menu/quantpf-menu-facts.yaml` 增加模块一页面细分、权限和状态，不增加禁止能力。
2. 在 plus-ui 建立 `api/quant`、`views/quant` 和公共状态/证据/引导组件骨架，先接工作台和数据健康只读页面。
3. 接入数据源、质量、标的、日历和市场 API，完成管理员/审核/普通用户权限态与空/错误/阻断状态。
4. 接入研究任务、Dify 运行状态、AI 草稿/研究事件和助手入口；先用 Mock API 完成交互合同测试。
5. 运行用户旅程验收：普通用户低输入、质量修复恢复、Dify降级、Paper前置阻断、审核版本冲突和安全扫描。
6. 完成一期日线边界检查后，再评估二期实时推送；不因 UI 完成自动解锁真实交易。

回滚：页面按菜单/权限和 Feature Flag 独立关闭；关闭 Dify 入口不影响确定性数据页面；保留任务/草稿状态和用户输入草稿，不删除后端事实。

## Open Questions

以下不改变页面边界或安全契约，可在产品设计/实施前确定：

- 工作台状态卡的最终数量和默认排序；
- 轮询间隔、空闲超时与二期 SSE/WebSocket 是否启用；
- ECharts 日线图的具体指标叠加范围；
- 审核用户的默认批量操作上限。
