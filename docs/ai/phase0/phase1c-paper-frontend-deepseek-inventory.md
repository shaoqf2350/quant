# QuantPF Phase 1C 前端只读 Inventory 与映射分析

- 分析日期：2026-10-07
- 阶段：`PHASE1C-PAPER-FRONTEND`（只读 inventory，不实施）
- 分析模型：Phase 1C 前端只读 inventory 和映射分析模型
- 只读范围：
  - `D:\workspace\quant\plus-ui`（前端仓库）
  - `D:\workspace\quant\RuoYi-Vue-Plus\contract`（契约）
  - `D:\我的文档\私人\量化\前端功能原型设计`（产品原型）
- 约束：未修改任何 Vue、TS、JSON、package.json、pnpm-lock.yaml、vite.config.ts、env 或后端文件；仅生成本文档。

---

## 1. package.json 技术版本矩阵

来源：`plus-ui/package.json`（name `ruoyi-vue-plus`，version `6.0.0`，`packageManager: pnpm@10.34.5`，`type: module`）。

| 分类 | 包 | 版本 | 说明 |
|---|---|---|---|
| 框架 | vue | 3.5.42 | Composition API |
| 路由 | vue-router | 5.3.1 | `createWebHistory` |
| 状态 | pinia | 4.0.3 | `defineStore` (setup store) |
| HTTP | axios | 1.20.0 | 封装于 `src/utils/request.ts` |
| UI | element-plus | 2.14.5 | 组件库 |
| 图标 | @element-plus/icons-vue | 2.3.2 | |
| 图标 | @iconify/vue | ^5.0.1 | |
| 表格 | vxe-table / vxe-pc-ui | 4.21.9 / 4.18.9 | 高级表格 |
| 图表 | echarts | 6.1.0 | |
| i18n | vue-i18n | 11.4.10 | |
| 工具 | @vueuse/core | 14.4.0 | |
| 工具 | await-to-js | 3.0.0 | `import { to }` |
| 富文本 | @wangeditor-next/editor / editor-for-vue | 6.4.1 | |
| 代码高亮 | highlight.js / @highlightjs/vue-plugin | 11.12.0 / 2.1.2 | |
| 加密 | jsencrypt | 3.5.4 | RSA，配合 `@noble/ciphers` ^2.4.0 (AES) |
| 进度条 | nprogress | 0.2.0 | |
| 构建 | vite | ^8.2.2 | rolldownOptions 可见（Vite8/Rolldown 内核） |
| 构建插件 | @vitejs/plugin-vue | ^6.0.8 | |
| CSS | unocss | 66.10.1 | + `@unocss/preset-wind3` / `preset-attributify` |
| CSS | sass | 1.104.0 | |
| 类型 | typescript | ^6.0.3 | + `vue-tsc` ^3.3.11 |
| 类型 | vue-types | 7.0.0 | |
| Lint | oxlint / oxfmt | ^1.82.0 / ^0.67.0 | 替代 eslint/prettier |
| 测试 | vitest | 4.1.11 | |
| 自动导入 | unplugin-auto-import / unplugin-vue-components | 21.1.0 / 32.1.0 | |

运行时约束：`engines.node >= 20.19.0`、`pnpm >= 10.0.0`；`browserslist` Chrome>=87 / Edge>=88 / Safari>=14 / Firefox>=78。

`pnpm-workspace.yaml`：packages 仅 `'.'`（单包），`overrides.esbuild = 0.28.1`。

---

## 2. 实际路由树

来源：`plus-ui/src/router/index.ts`。**关键事实：前端路由几乎全部为动态注入，静态路由极少。**

### 2.1 静态路由 `constantRoutes`（代码内写死）

| path | component | hidden | 说明 |
|---|---|---|---|
| `/redirect/:path(.*)` | Layout + `views/redirect/index.vue` | true | 重定向钩子 |
| `/social-callback` | `layout/components/SocialCallback/index.vue` | true | 第三方登录回调 |
| `/login` | `views/login.vue` | true | 登录 |
| `/register` | `views/register.vue` | true | 注册 |
| `/:pathMatch(.*)*` | `views/error/404.vue` | true | 404 |
| `/401` | `views/error/401.vue` | true | 401 |
| `/index` | Layout + `views/index.vue` | false | 首页（affix） |
| `/user/profile` | Layout + `views/system/user/profile/index.vue` | true | 个人中心 |

### 2.2 动态路由 `dynamicRoutes`

```ts
export const dynamicRoutes: RouteRecordRaw[] = [];  // 当前为空数组
```

**当前前端代码内没有任何 QuantPF 路由。** 所有业务路由（含 system/monitor/workflow/demo/ai 等）均通过后端 `getRouters` 接口下发，经 `permission.store.generateRoutes()` 组件字符串映射后 `router.addRoute()` 注入。

### 2.3 页面文件（不含 QuantPF）

`src/views/**` 已有：login、register、index、error(401/404)、redirect、system(用户/角色/菜单/部门/字典/配置/公告/OSS)、monitor(admin/cache/logininfo/online/operlog/snailai/snailjob)、tool/gen、workflow(全套)、demo、ai/chat。**无 `views/quant/**` 目录。**

---

## 3. 动态权限加载流程

来源：`src/permission.ts` + `src/store/modules/permission.ts` + `src/api/menu.ts`。

1. **路由守卫** `router.beforeEach`（`src/permission.ts`）：
   - 无 token → 非白名单路由重定向 `/login?redirect=...`；白名单 `['/login','/register','/social-callback','/register/*']`。
   - 有 token 且 `userStore.roles.length === 0` → `await userStore.getInfo()`（拉取用户信息+角色+权限码），成功后 `await permissionStore.generateRoutes()`。
   - `generateRoutes()` 返回的路由逐个 `router.addRoute(route)`（跳过 `isHttp(route.path)` 的 iframe 路由）。

2. **`generateRoutes()`**（`src/store/modules/permission.ts` 55-75 行）：
   - 调 `getRouters()` = `GET /system/menu/getRouters`（`src/api/menu.ts`）。
   - 对返回的组件字符串做映射：`'Layout'`→Layout、`'ParentView'`→ParentView、`'InnerLink'`→InnerLink，否则 `loadView(component, name)`。
   - `loadView`：`import.meta.glob('./../../views/**/*.vue')` 预建 `viewModuleMap`，用组件字符串（相对 `views/` 的路径，不含 `.vue`）查表，命中则 `createCustomNameComponent` 包装（注入路由 name）。**未命中返回 `undefined`，不会报错（空白页风险）。**
   - 另处理本地 `dynamicRoutes`（当前为空）+ `filterDynamicRoutes` 按 `permissions`/`roles` 过滤。
   - 最终 `setRoutes(rewriteRoutes)`、`setSidebarRouters(...)`、`setTopbarRoutes(...)`。

3. **权限码来源**：角色/权限码由 `getInfo()` → `userStore` 维护，菜单可见性由后端菜单表 + 前端 `auth.hasPermiOr` 决定。

**对 QuantPF 的推论**：QuantPF 菜单/路由若要出现在侧边栏，必须在后端 `sys_menu` 表配置对应记录（component 字符串形如 `quant/dashboard/index`），前端再创建 `src/views/quant/...` 目录下的 `.vue` 文件供 `import.meta.glob` 匹配。前端本地静态路由不是 Phase 1C 的改动点（除非改用 `dynamicRoutes` 本地声明）。

---

## 4. 菜单 API 和 permission.ts 关系

- `src/api/menu.ts` 仅一个函数 `getRouters()` → `GET /system/menu/getRouters`，返回 `RouteRecordRaw[]`（后端已组装好带 `component`/`path`/`name`/`meta`/`children` 的路由树）。
- `permission.ts`（`generateRoutes`）消费该返回，将 `component` 字符串解析为真实 `.vue` 组件并动态注册。
- 菜单（`sys_menu`）在**后端**管理；前端 `views/system/menu/index.vue` 只管菜单 CRUD，不直接生成前端 `views/quant` 页面文件。

**关键结论**：Phase 1C 的前端页面必须被后端菜单表的 `component` 字段指向，否则即使前端存在 `.vue`，也不会出现在路由/侧边栏。后端菜单配置是否已在 `contract/menu/quantpf-menu-facts.yaml` 之外落地到 DB，属**后端/DB 侧事实，本 inventory 无法确认（UNKNOWN）**。

---

## 5. 当前已有 QuantPF/Paper/SnailJob 页面

| 主题 | 现状 | 证据 |
|---|---|---|
| QuantPF 页面 | **无** | `src/views` 无 `quant` 目录；grep `quant|QuantPF|QuantPf|paper` 在 `src/**/*.ts` 与 `*.vue` 中无前端业务命中（仅 `api/ai/agent/index.ts` 的 `/snail-ai/user/register` 与 `views/ai/chat/index.vue` 的 `/snail-chat` 命中，属 Snail 生态） |
| Paper 页面 | **无** | 同上 |
| SnailJob 页面 | **有** | `views/monitor/snailjob/index.vue`（iframe，`src=VITE_APP_SNAILJOB_ADMIN`） |
| SnailAI 页面 | **有** | `views/monitor/snailai/index.vue`（iframe，`src=VITE_APP_SNAILAI_ADMIN`）；`views/ai/chat/index.vue`（iframe `snail-chat`）；`api/ai/agent/index.ts`（`/snail-ai/user/register`） |

SnailJob/SnailAI 均为 **iframe 壳页面**（外部治理后台嵌入），非业务功能实现，与 QuantPF Paper 交易功能无关。

---

## 6. 当前 API 文件结构

`src/api/**` 组织模式：**每个业务域一个目录，目录内含 `index.ts`（请求函数）+ `types.ts`（类型）**，例 `api/system/menu/{index.ts,types.ts}`。顶层还有 `login.ts`、`menu.ts`、`types.ts`（登录/注册/验证码/PageResult 通用类型）。

现有业务域：`login`、`menu`、`ai`、`demo`、`monitor/{cache,logininfo,online,operlog}`、`system/{client,config,dept,dict,menu,message,notice,oss,ossConfig,post,role,social,user}`、`tool/gen`、`workflow/{category,definition,instance,leave,spel,task,workflowCommon}`。

**无 `api/quant/**`。** 请求封装统一：`import request from '@/utils/request'`，返回 `AxiosPromise<T>`（`src/utils/api-types.ts` 提供）；`request` 实例 `baseURL = import.meta.env.VITE_APP_BASE_API`（开发 `/dev-api`，生产 `/prod-api`），由 vite proxy 重写前缀后转发 `http://localhost:8080`（`vite.config.ts` proxy，`rewrite: path.replace('^'+BASE_API, '')`）。

**路径前缀坑（UNKNOWN，需后端确认）**：现有 RuoYi 前端 api url 形如 `/system/menu/list`（**无 `/api` 前缀**），而 QuantPF OpenAPI paths 形如 `/api/quant/account/page`（**含 `/api` 前缀**）。因此 QuantPF 前端封装 url 与既有 RuoYi 模块存在前缀差异；QuantPF Controller 实际 `@RequestMapping` 前缀是否会乘上后端 context-path 或已含 `/api`，本 inventory 仅据 OpenAPI 契约记录，**具体落地 url 需在实施前与后端 Controller 实际映射对照确认**。

---

## 7. OpenAPI Paper API → 前端 API 映射

来源：`contract/openapi/quantpf-v29.yaml`（version 29.0.0，servers url `/`）。下表列出 OpenAPI 全部 path（含 Paper 与相关域），并标注前端现状与候选 api 模块。**当前前端对所有这些端点均未实现确定映射（前端 `api/quant` 不存在）**，候选路径为规划建议，非既有事实。

| OpenAPI path | method | tag | 前端现状 | 候选 api 模块（未实施） |
|---|---|---|---|---|
| `/api/quant/health` | GET | health | 无 | `api/quant/health/index.ts` |
| `/api/quant/dashboard/summary` | GET | dashboard | 无 | `api/quant/dashboard/index.ts` |
| `/api/quant/instrument/page` | GET | instrument | 无 | `api/quant/instrument/index.ts` |
| `/api/quant/market/kline` | GET | market | 无 | `api/quant/market/index.ts` |
| `/api/quant/news/page` | GET | research | 无 | `api/quant/research/index.ts` |
| `/api/quant/finance/statements` | GET | research | 无 | `api/quant/research/index.ts` |
| `/api/quant/pool/page` | GET | research | 无 | `api/quant/research/index.ts` |
| `/api/quant/strategy/page` | GET | strategy | 无 | `api/quant/strategy/index.ts` |
| `/api/quant/signal/page` | GET | strategy | 无 | `api/quant/strategy/index.ts` |
| `/api/quant/backtest` | POST | backtest | 无 | `api/quant/backtest/index.ts` |
| `/api/quant/backtest/{id}` | GET | backtest | 无 | `api/quant/backtest/index.ts` |
| `/api/quant/account/page` | GET | paper-account | 无 | `api/quant/paper/account/index.ts` |
| `/api/quant/account/{id}/ledger` | GET | paper-account | 无 | `api/quant/paper/account/index.ts` |
| `/api/quant/order/page` | GET | paper-order | 无 | `api/quant/paper/order/index.ts` |
| `/api/quant/order/paper` | POST | paper-order | 无 | `api/quant/paper/order/index.ts`（header `Idempotency-Key` 必填，202） |
| `/api/quant/paper/follow-start` | POST | paper-order | 无 | `api/quant/paper/follow/index.ts`（header `Idempotency-Key`，200） |
| `/api/quant/paper/follow-stop` | POST | paper-order | 无 | `api/quant/paper/follow/index.ts`（header `Idempotency-Key`，200） |
| `/api/quant/risk/status` | GET | risk | 无 | `api/quant/risk/index.ts` |
| `/api/quant/risk/halt` | POST | risk | 无 | `api/quant/risk/index.ts` |
| `/api/quant/risk/resume` | POST | risk | 无 | `api/quant/risk/index.ts` |
| `/api/quant/job/execution/page` | GET | audit | 无 | `api/quant/audit/index.ts` |
| `/api/quant/audit/page` | GET | audit | 无 | `api/quant/audit/index.ts` |

**关键 schema（OpenAPI `components/schemas`）**，可作为前端 `types.ts` 依据：
- `PaperFollowStartRequest`：`accountId:int64, strategyId:int64, mode:enum[PAPER]`（required）。
- `PaperFollowStopRequest`：`sessionId:string`(required), `reason:string≤512`。
- `PaperFollowSessionResponse`：`sessionId, accountId, strategyId, mode:enum[PAPER], status:enum[RUNNING,STOPPED], startedAt, stoppedAt(nullable), idempotent:boolean`。
- `PaperOrderRequest`：`accountId, instrumentCode, side:enum[BUY,SELL], quantity>0, price>0, orderType:enum[MARKET,LIMIT], clientOrderId≤128, requestId≤128`。
- `RiskHaltRequest`/`RiskResumeRequest`：`reason:string≤512`, `scope`/`targetId`(halt only)。
- `BacktestRequest`：`strategyId, startDate, endDate, initialCapital`。
- 通用 `ApiResponse{code,msg,data}`、`ErrorResponse{code,msg,data}`、参数 `Id`/`Page(pageNum)`/`PageSize`(1..200)/`IdempotencyKey`(header,8..128)。
- 错误码：`contract/error-codes/quantpf-error-codes.yaml`（含 QPF-FOLLOW-001..004 等，未逐条展开）。

---

## 8. 原型页面 → 目标路由映射

来源：`PROTOTYPE_PAGE_MAP.md`（原型为 V25 全量愿景，55+ 页面）与 `contract/menu/quantpf-menu-facts.yaml`（STAGE_1 仅 9 菜单）。**两者权限粒度不一致，需区分对待。**

### 8.1 契约侧权威：`quantpf-menu-facts.yaml`（STAGE_1，enabled:true 的 9 菜单）

| code | title | path | permission |
|---|---|---|---|
| quant.dashboard | Dashboard | `/quant/dashboard` | `quant:dashboard:view` |
| quant.research | Research | `/quant/research` | `quant:research:view` |
| quant.market | Market | `/quant/market` | `quant:market:view` |
| quant.strategy | Strategy | `/quant/strategy` | `quant:strategy:view` |
| quant.backtest | Backtest | `/quant/backtest` | `quant:backtest:view` |
| quant.paper-account | Paper Account | `/quant/paper-account` | `quant:paper-account:view` |
| quant.paper-order | Paper Order | `/quant/paper-order` | `quant:paper-order:view` |
| quant.risk | Risk | `/quant/risk` | `quant:risk:view` |
| quant.audit | Audit | `/quant/audit` | `quant:audit:view` |

`forbiddenCapabilities`：`LIVE`, `QMT`, `CTP`, `REAL_BROKER_ORDER`, `REAL_BROKER_CANCEL`。

### 8.2 原型侧：`PROTOTYPE_PAGE_MAP.md`（55+ 页面，粒度更细，含大量 STAGE_1 之外内容）

原型映射表给出更细权限码（如 `quant:instrument:list`、`quant:strategy:edit`、`quant:broker:edit` 等），与 STAGE_1 菜单的 `:view` 粒度不同。原型中 **`broker-qmt`、`broker-ctp`（QMT/CTP 接入）与 `ops-jobs`（XXL-JOB）** 直接命中 `forbiddenCapabilities` 或与 LIVE/QMT/CTP 相关，**Phase 1C 不应纳入**。

### 8.3 Phase 1C Paper 相关原型页面（与 STAGE_1 paper 域对齐的候选，非全部原型）

| 原型页 | 原型建议路由 | 原型建议 API | 命中的 STAGE_1 菜单 | Phase 1C 相关性 |
|---|---|---|---|---|
| `trade-paper.html` 模拟盘管理 | `/quant/trade/paper` | `GET /api/quant/paper/account/{accountId}` | `quant.paper-account` | 相关（但原型 API 路径与 OpenAPI `/api/quant/account/...` 不一致） |
| `trade-accounts.html` 账户总览 | `/quant/trade/accounts` | `GET /api/quant/account/summary` | `quant.paper-account` | 相关 |
| `trade-manual.html` 手动下单 | `/quant/trade/manual-order` | `POST /api/quant/trade/order` | `quant.paper-order` | 相关（原型含 PAPER/SHADOW/READ_ONLY/LIVE，LIVE 需剔除） |
| `trade-orders.html` 委托订单 | `/quant/trade/orders` | `GET /api/quant/trade/order/list` | `quant.paper-order` | 相关 |
| `trade-positions.html` 持仓管理 | `/quant/trade/positions` | `GET /api/quant/position/list` | `quant.paper-order` | 相关 |
| `trade-records.html` 成交记录 | `/quant/trade/records` | `GET /api/quant/trade/record/list` | `quant.paper-order` | 相关 |
| `risk-status.html` 风控总览 | `/quant/risk/status` | `GET /api/quant/risk/status` | `quant.risk` | 相关（与 OpenAPI 一致） |
| `risk-halt.html` 紧急熔断 | `/quant/risk/halt` | `POST /api/quant/risk/halt` | `quant.risk` | 相关（与 OpenAPI 一致） |
| `ops-audit.html` 操作审计 | `/quant/ops/audit` | `GET /api/quant/audit/list` | `quant.audit` | 相关（OpenAPI 为 `/api/quant/audit/page`） |
| `dashboard.html` 交易驾驶舱 | `/quant/dashboard` | `GET /api/quant/dashboard/summary` | `quant.dashboard` | 相关（与 OpenAPI 一致） |

**注意（重要，UNKNOWN 级）**：PHASE1B 已实现的 **Follow 端点 `/api/quant/paper/follow-start|stop`** 在 OpenAPI 中存在，但**原型 `PROTOTYPE_PAGE_MAP.md` 与 `app.js` 均无对应"跟随模式/Follow"页面**。Follow 前端展示需求来源未确认，Phase 1C 若涉及 Follow 页面，需先补产品定义。

**关键差异（映射冲突，需裁决）**：
1. 原型建议路由用 `/quant/trade/*`、`/quant/ops/*`、`/quant/data/*`，而 STAGE_1 契约菜单用 `/quant/paper-account`、`/quant/paper-order`、`/quant/audit`。二者路由命名不一致。
2. 原型权限码（`quant:account:list`、`quant:trade:order` 等）与 STAGE_1 契约权限码（`quant:paper-account:view` 等）粒度/命名不一致。
3. 原型 API 路径（`/api/quant/paper/account/{id}`、`/api/quant/trade/order`、`/api/quant/audit/list`）与 OpenAPI 路径（`/api/quant/account/page`、`/api/quant/order/paper`、`/api/quant/audit/page`）不一致。

**结论**：Phase 1C 应以 `contract/openapi/quantpf-v29.yaml`（API 事实）+ `contract/menu/quantpf-menu-facts.yaml`（STAGE_1 菜单事实）为权威；原型仅作交互/视觉参考，其路由、权限码、API 路径不得原样照搬。

---

## 9. 前端实施候选文件清单（规划建议，非既有事实）

以下为若获准实施 Phase 1C 时可能新增的文件（严格以 STAGE_1 9 菜单 + OpenAPI 为依据）。**当前均不存在，不得视为已实施。**

### 9.1 API 层（`src/api/quant/**`，遵循现有 `index.ts + types.ts` 模式）

| 候选文件 | 对应 OpenAPI |
|---|---|
| `src/api/quant/health/index.ts` | `/api/quant/health` |
| `src/api/quant/dashboard/index.ts` (+types.ts) | `/api/quant/dashboard/summary` |
| `src/api/quant/instrument/index.ts` (+types.ts) | `/api/quant/instrument/page` |
| `src/api/quant/market/index.ts` (+types.ts) | `/api/quant/market/kline` |
| `src/api/quant/research/index.ts` (+types.ts) | news/finance/pool |
| `src/api/quant/strategy/index.ts` (+types.ts) | strategy/signal |
| `src/api/quant/backtest/index.ts` (+types.ts) | backtest |
| `src/api/quant/paper/account/index.ts` (+types.ts) | account/page, ledger |
| `src/api/quant/paper/order/index.ts` (+types.ts) | order/page, order/paper |
| `src/api/quant/paper/follow/index.ts` (+types.ts) | follow-start/stop |
| `src/api/quant/risk/index.ts` (+types.ts) | risk status/halt/resume |
| `src/api/quant/audit/index.ts` (+types.ts) | audit/page, job/execution/page |

### 9.2 视图层（`src/views/quant/**`，供 `import.meta.glob('./../../views/**/*.vue')` 匹配）

以 STAGE_1 菜单为骨架的最小页面集（每菜单一入口 `.vue`，组件字符串形如 `quant/<menucode>/index`）：

| 候选文件 | 菜单 code |
|---|---|
| `src/views/quant/dashboard/index.vue` | quant.dashboard |
| `src/views/quant/research/index.vue` | quant.research |
| `src/views/quant/market/index.vue` | quant.market |
| `src/views/quant/strategy/index.vue` | quant.strategy |
| `src/views/quant/backtest/index.vue` | quant.backtest |
| `src/views/quant/paperAccount/index.vue` | quant.paper-account |
| `src/views/quant/paperOrder/index.vue` | quant.paper-order |
| `src/views/quant/risk/index.vue` | quant.risk |
| `src/views/quant/audit/index.vue` | quant.audit |

> 子页面（如 instrument 详情、订单/持仓/成交、风险规则/日志/熔断、回测创建/列表/详情）需在确定菜单"父/子"结构后按需扩展。**目录/文件命名须与后端菜单 `component` 字符串严格一致**，此为实施时的硬约束。

### 9.3 其它可能的触点（仅列出观察，不主张改动）

- `src/router/index.ts`：若走本地 `dynamicRoutes`，可在此声明 QuantPF 路由（当前为空数组）；否则不动静态路由。
- `src/lang/*`：页面标题/菜单的 i18n 词条（若需国际化）。
- `contract` 前端相关：无前端可选契约（OpenAPI 为后端契约，前端不改）。

---

## 10. UNKNOWN（无法确认，须后续确认）

1. **QuantPF 后端菜单是否已在 `sys_menu` 表落地**：`contract/menu/quantpf-menu-facts.yaml` 是契约文件，非 DB 事实；菜单是否已写入后端 DB、`component` 字段取值为何，前端侧无法确认。
2. **前端 API 前缀差异**：Ruoyi 既有 api 无 `/api` 前缀，QuantPF OpenAPI 含 `/api` 前缀；QuantPF Controller 实际挂载路径（是否经网关 context-path 或已含 `/api`）未在后端源码中核实（本 inventory 仅读前端+契约）。
3. **后端 `getRouters` 返回的 QuantPF 路由 `component` 字符串命名约定**：决定 `views/quant/**` 目录与 `loadView` 匹配的精确路径，尚未有后端事实。
4. **Follow 页面产品定义**：OpenAPI 有 follow-start/stop，但原型无对应页面，Follow 前端展示入口/形态未定义。
5. **Paper 下单成功后 HTTP 202 vs follow 的 200**：OpenAPI 中 `order/paper` 返回 202、follow 返回 200，前端 axios 处理是否需针对 202 特别处理，未确认。
6. **权限码粒度**：STAGE_1 菜单用 `:view` 权限码，原型用更细 `:list/:edit/:submit`；后端角色授权实际采用哪套权限码，未确认。
7. **`VITE_APP_SNAILJOB_ADMIN` / `VITE_APP_SNAILAI_ADMIN` 与 QuantPF 关系**：二者是 Snail 生态 iframe 地址，与 QuantPF 无关；Phase 1C 是否复用 iframe 模式接入 Omega/其他治理后台，未定义。
8. **原型 `index_v23_backup.html`、多个 SQL（`quantpf_v23/v24/v25*.sql`）**：均为原型/数据参考材料，其与正式 V29 契约的关系未确认，不作为实施依据。

---

## 11. 不得凭空创建（合规声明）

本 inventory **未创建**任何 API、权限码或页面；上文第 7/8/9 节的"候选/建议"均为基于既有契约（OpenAPI + menu-facts）的规划映射，**不是已实施事实**。Phase 1C 实施须以：
1. `contract/openapi/quantpf-v29.yaml`（API 事实源）
2. `contract/menu/quantpf-menu-facts.yaml`（STAGE_1 菜单/权限事实源，`forbiddenCapabilities` 强制排除 LIVE/QMT/CTP/真实下单撤单）

为唯一权威；产品原型仅作视觉参考，其路由/权限码/API 路径与契约冲突处一律以契约为准。**任何未在契约中出现的 API、权限码、页面，实施时均属越界，不得凭空创建。**

---

## 附：只读证据索引

- `plus-ui/package.json`、`pnpm-workspace.yaml` —— 技术矩阵。
- `plus-ui/src/router/index.ts` —— 静态路由 + 空 dynamicRoutes。
- `plus-ui/src/permission.ts`、`store/modules/permission.ts`、`api/menu.ts` —— 动态权限/路由加载。
- `plus-ui/src/utils/request.ts`、`utils/api-types.ts`、`api/system/menu/{index.ts,types.ts}` —— 请求封装与 api 目录模式。
- `plus-ui/vite.config.ts`、`.env.development`、`.env.production` —— proxy 与 base api 前缀。
- `plus-ui/src/views/**`（glob 名单）—— 现有页面与 Snail iframe 壳。
- `contract/openapi/quantpf-v29.yaml` —— 22 个 /api/quant 端点与 schema。
- `contract/menu/quantpf-menu-facts.yaml` —— STAGE_1 9 菜单 + forbiddenCapabilities。
- `contract/error-codes/quantpf-error-codes.yaml` —— 错误码。
- `前端功能原型设计/PROTOTYPE_PAGE_MAP.md`、`assets/app.js` —— 原型页面/路由/权限/API 映射。