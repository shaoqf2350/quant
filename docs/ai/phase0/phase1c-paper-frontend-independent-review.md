# QuantPF Phase 1C 前端独立审查

- 审查日期：2026-10-07
- 阶段：`PHASE1C-PAPER-FRONTEND`（只读审查，不修改业务代码）
- 审查角色：Phase 1C 前端独立审查负责人
- 审查对象：DeepSeek inventory、OpenAPI、menu-facts、前端路由/权限/API、原型映射
- 审查方法：独立复核 7 项必读文件 + 补读 `error-codes.yaml`、`main.ts`、grep 路由注册入口
- 本审查：未修改任何业务代码；仅生成本文件

## 0. 审查结论

```text
BLOCKED
```

理由：存在 1 项 P0——OpenAPI 对 Paper 分页/查询端点的 response `data` 仅定义为 `{}`，前端 VO 类型无契约依据，贸然实施将凭空假设字段导致运行时渲染失败。另有 6 项 P1（菜单事实源未落 DB、原型 API 大量不存在于 OpenAPI、幂等键处理缺失、错误码处理缺失、无正式实施白名单、LIVE 模式混入风险）。在 P0 关闭与 P1 落实前，不允许进入前端实施。

---

## P0（阻断，必须修复才能进入实施）

### P0-1 OpenAPI Paper 分页/查询端点 response DTO 缺失

- 证据：`contract/openapi/quantpf-v29.yaml` 中，以下端点的成功响应均为 `$ref: '#/components/responses/ApiResponse'`，而 `ApiResponse.data` 定义为 `{}`（空对象，无具体字段）：
  - `/api/quant/dashboard/summary`（GET）
  - `/api/quant/instrument/page`（GET）
  - `/api/quant/market/kline`（GET）
  - `/api/quant/news/page`、`/api/quant/finance/statements`、`/api/quant/pool/page`（GET）
  - `/api/quant/strategy/page`、`/api/quant/signal/page`（GET）
  - `/api/quant/backtest/{id}`（GET）
  - `/api/quant/account/page`、`/api/quant/account/{id}/ledger`（GET）
  - `/api/quant/order/page`（GET）
  - `/api/quant/risk/status`（GET）
  - `/api/quant/job/execution/page`、`/api/quant/audit/page`（GET）
- 唯一明确定义 response schema 的是 Follow：`PaperFollowSessionResponse`（sessionId/accountId/strategyId/mode/status/startedAt/stoppedAt/idempotent）。请求体 `PaperOrderRequest`/`RiskHaltRequest`/`RiskResumeRequest`/`BacktestRequest` 已定义。
- 后果：前端 `types.ts` 中的 `PaperAccountVO`、`PaperOrderVO`、`AuditRecordVO`、`JobExecutionVO`、`StrategyVO`、`SignalVO`、`InstrumentVO`、`KlineVO`、`NewsVO`、`PoolItemVO`、`FinanceStatementVO`、`DashboardSummaryVO`、`RiskStatusVO`、`PageResult<...>` 的行字段**全部无契约依据**。实施时只能靠后端源码或运行时抓包反推，违背"契约先行"。
- 关闭条件：补全上述端点的 response `data` schema（至少 Paper 账户/订单/审计/风控状态/仪表盘摘要），或在实施白名单中逐端点标注"前端 VO 待后端接口事实确认"并冻结字段清单。

---

## P1（必须落实才能宣称实施就绪）

### P1-1 菜单事实源未落 DB，权限码两套不一致

- 证据：
  - `contract/menu/quantpf-menu-facts.yaml` 是契约文件（`stage: STAGE_1`），非 DB 事实。9 菜单权限码均为 `quant:<code>:view`（如 `quant:paper-account:view`、`quant:paper-order:view`）。
  - `PROTOTYPE_PAGE_MAP.md` 权限码粒度更细且命名不同：`quant:paper:list`、`quant:account:list`、`quant:trade:order`、`quant:risk:halt`、`quant:audit:list` 等（40+ 个）。
  - 前端权限校验链：`permission.ts` 守卫 → `getInfo()` 拉权限码 → `store/modules/permission.ts` 的 `filterDynamicRoutes` 用 `auth.hasPermiOr(route.permissions)`；页面内 `v-hasPermi` 指令同样比对权限码。权限码来源于后端 `sys_menu.perms`。
- 后果：若后端 `sys_menu` 实际 `perms` 用 menu-facts 的 `:view`，而前端页面 `v-hasPermi` 用原型的 `:list/:edit`，按钮不显示或菜单泄露；反之亦然。两套权限码不可混用。
- 关闭条件：确认后端 `sys_menu` 中 QuantPF 菜单的实际 `perms` 值（是 menu-facts 的 `:view` 还是更细粒度），前端统一采用该套；在实施白名单中冻结权限码清单。

### P1-2 原型 API 路径大量不存在于 OpenAPI

- 证据：`PROTOTYPE_PAGE_MAP.md` 引用 40+ 个 API（如 `/api/quant/instrument/list`、`/api/quant/kline/query`、`/api/quant/orderbook/latest/{symbol}`、`/api/quant/news/latest`、`/api/quant/finance/earnings-calendar`、`/api/quant/strategy-params`、`/api/quant/stock-pool/active`、`/api/quant/backtest/submit`、`/api/quant/account/summary`、`/api/quant/position/list`、`/api/quant/trade/order`、`/api/quant/paper/account/{accountId}`、`/api/quant/risk/rules`、`/api/quant/broker-config`、`/api/quant/job/list`、`/api/quant/audit/list` 等）。OpenAPI 实际仅 22 个端点。
- inventory 已声明"以契约为准"，但**未逐条列出原型中不存在于 OpenAPI 的 API 清单**。实施者若照搬原型 API 路径，前端会调用大量不存在的 API（404/路由失败）。
- 关闭条件：在实施白名单中显式列出"原型引用但 OpenAPI 未定义"的 API 红线清单，标注"禁止前端调用"；前端 api 封装仅允许 22 个 OpenAPI 端点。

### P1-3 幂等键处理缺失

- 证据：OpenAPI `IdempotencyKey` header（`name: Idempotency-Key`, `required: true`, `minLength: 8`, `maxLength: 128`）用于 `/api/quant/order/paper`、`/api/quant/paper/follow-start`、`/api/quant/paper/follow-stop`。`request.ts` 的 `repeatSubmit` 防抖（500ms 内同 url+data 拦截）是前端防重复提交，**不是幂等键**，语义不同。
- 后果：若前端不生成/注入唯一 `Idempotency-Key`，后端返回 `QPF-FOLLOW-004`/`QPF-REQ-001`（400）；若复用固定值，同 key 不同参数会触发 `QPF-IDEMP-001`（409）。
- 关闭条件：实施白名单明确 Idempotency-Key 生成方案（UUID v4 / nanoid，每次请求唯一，8..128 字符），注入到上述 3 个端点的请求 header；不得用 repeatSubmit 替代。

### P1-4 错误码处理缺失

- 证据：`contract/error-codes/quantpf-error-codes.yaml` 定义 16 个 QuantPF 错误码（QPF-REQ-001/QPF-IDEMP-001/QPF-ORDER-001/QPF-ACCOUNT-001/QPF-CASH-001/QPF-POSITION-001/QPF-RISK-001/QPF-RISK-002/QPF-MARKET-001/QPF-INSTRUMENT-001/QPF-AUDIT-001/QPF-INTERNAL-001/QPF-FOLLOW-001..004）。前端 `utils/errorCode.ts` 是 RuoYi 通用码表，未确认是否含 QuantPF 码。
- `request.ts` 响应拦截器对 `code !== 200` 统一 `ElNotification.error`，无差异化提示。但 QuantPF 的 409（冲突）/404（不存在）/400（缺参）需不同用户提示（如 QPF-FOLLOW-001 应提示"已有运行中的跟随会话"，QPF-RISK-002 应提示"模拟盘已熔断"）。
- 后果：用户看到通用错误提示，无法理解具体原因；QPF-IDEMP-001（重复提交）可能被误认为系统故障。
- 关闭条件：实施白名单包含前端错误码映射方案（码→中文提示），至少覆盖 Follow（001..004）、幂等（IDEMP-001）、风控（RISK-001/002）、订单（ORDER-001）、账户（ACCOUNT-001/CASH-001/POSITION-001）。

### P1-5 Phase 1C 缺少正式实施白名单文件

- 证据：DeepSeek inventory 是分析文档，**不是实施白名单**。Phase 1B 有 `phase1b-follow-persistence-file-whitelist.json`（含 nextStageAllowedFiles/forbiddenFiles），Phase 1C 尚无对应文件。inventory 第 9 节仅列"候选文件清单"，无 forbiddenFiles、无 action（create/modify）、无 readOnly 标注、无人工批准点。
- 后果：无白名单则实施边界模糊，可能越权修改 `router/index.ts`、`package.json`、`vite.config.ts`、`.env`、`permission.ts` 等核心文件。
- 关闭条件：进入实施前生成 `phase1c-paper-frontend-file-whitelist.json`，明确 nextStageAllowedFiles（仅 `src/api/quant/**` + `src/views/quant/**` + 必要的 lang/菜单数据）、forbiddenFiles（router/index.ts、package.json、pnpm-lock.yaml、vite.config.ts、.env.*、permission.ts、store/modules/permission.ts、后端文件、契约文件）、action、readOnly。

### P1-6 LIVE/QMT/CTP 模式入口混入风险

- 证据：`menu-facts.yaml` `forbiddenCapabilities: [LIVE, QMT, CTP, REAL_BROKER_ORDER, REAL_BROKER_CANCEL]`。原型 `trade-manual.html` 执行模式选项含 `PAPER/SHADOW/READ_ONLY/LIVE`；`broker-qmt.html`、`broker-ctp.html` 直接是 QMT/CTP 接入页。
- STAGE_1 菜单无"手动下单/trade-manual"独立菜单，但 `quant.paper-order` 菜单对应 OpenAPI `/api/quant/order/paper`（POST 下单），若该页面含执行模式选择器，必须只允许 PAPER。
- 后果：若前端下单页暴露 LIVE/SHADOW/READ_ONLY 选项或 QMT/CTP 入口，违反 forbiddenCapabilities，且后端虽拦截但前端 UI 已泄露能力存在。
- 关闭条件：实施白名单明确 Paper 下单页只渲染 PAPER 账户/模式；剔除所有 LIVE/QMT/CTP/broker 页面与选项；原型 broker-*/trade-manual(LIVE 部分) 不纳入。

---

## P2（不阻断，建议处理）

### P2-1 不应修改 router/index.ts 的 dynamicRoutes
- `router/index.ts` 第 95 行 `dynamicRoutes = []`。QuantPF 路由应走后端 `getRouters` 注入（`permission.ts` 第 49 行 `router.addRoute(route)`），不得在 `dynamicRoutes` 硬编码业务路由。硬编码会绕过后端菜单管理，且 `filterDynamicRoutes` 按 `permissions`/`roles` 过滤若配错会泄露菜单。inventory 第 9.3 提"若走本地 dynamicRoutes 可声明"——建议删除该表述，明确只走后端注入。

### P2-2 job/execution 与 audit 归并需双函数
- inventory 将 `/api/quant/job/execution/page`（operationId `pageJobExecutions`）与 `/api/quant/audit/page`（operationId `pageAuditRecords`）归到同一 `api/quant/audit/index.ts`。可接受，但须确保两个函数 + 两个 VO 类型都定义，不得只写一个。

### P2-3 分页参数复用
- OpenAPI `Page(pageNum, default 1)` / `PageSize(1..200, default 20)`。前端应复用 RuoYi `PageQuery` 或建立 `quant` 通用分页参数，确保 pageSize 上限 200（与契约一致），不得放开。

### P2-4 RSA 私钥泄露（既有框架问题）
- `.env.development` 与 `.env.production` 均含 `VITE_APP_RSA_PRIVATE_KEY`（Base64 RSA 私钥）。Vite env 会打进客户端 bundle，浏览器 devtools 可提取，"接口加密"形同虚设。这是 RuoYi-Vue-Plus 既有设计，非 QuantPF 引入。Phase 1C **不得修改 .env**，但须注意：QuantPF Paper 交易的敏感数据（账户、持仓、订单）**不得依赖前端加密保护**；幂等键、token 等不得硬编码；Follow 的 sessionId/stopReason 不在前端日志明文输出。

### P2-5 Follow 页面产品定义缺失
- OpenAPI 有 `/api/quant/paper/follow-start|stop`，但 `PROTOTYPE_PAGE_MAP.md` 与 `app.js` 无对应"跟随模式"页面。Follow 前端展示入口/形态未定义。Phase 1C 若涉及 Follow 页面，须先补产品定义（启动/停止按钮、RUNNING/STOPPED 状态展示、幂等重放提示）。

### P2-6 loadView 未命中静默空白
- `store/modules/permission.ts` 第 157-163 行 `loadView` 未命中返回 `undefined`，不报错。若后端菜单 `component` 字符串与前端 `views/quant/**` 路径不匹配，页面空白无提示。建议实施时建立 component 字符串↔.vue 路径对照表，并在开发环境加未命中告警。

---

## 建议修正（针对 DeepSeek inventory）

1. **第 7 节**：补充标注 OpenAPI 分页端点 response data 为 `{}`（DTO 未定义），明确前端 VO 待契约补全（对应 P0-1）。
2. **第 8 节**：补充逐条列出"原型引用但 OpenAPI 未定义"的 API 红线清单（对应 P1-2）。
3. **第 9 节**：删除"若走本地 dynamicRoutes 可在此声明"表述，明确 QuantPF 路由只走后端菜单注入（对应 P2-1）。
4. **第 9 节**：候选清单补充 Idempotency-Key 生成方案、错误码映射方案、分页参数上限 200（对应 P1-3/P1-4/P2-3）。
5. **第 10 节 UNKNOWN**：将"菜单是否落 DB"从 UNKNOWN 升级为 P1-1 关闭条件（必须确认，不能停留未知）。
6. **新增**：明确 Phase 1C 须先产出 `phase1c-paper-frontend-file-whitelist.json`（对应 P1-5）。

---

## 允许进入前端实施的必要条件

1. **关闭 P0-1**：补全 OpenAPI Paper 分页/查询端点的 response `data` schema（至少账户/订单/审计/风控状态/仪表盘摘要），或在白名单冻结"前端 VO 待后端确认"字段清单。
2. **关闭 P1-1**：确认后端 `sys_menu` 中 QuantPF 菜单实际 `perms` 值，前端权限码统一采用该套，与 menu-facts/原型冲突处以 DB 事实为准。
3. **关闭 P1-2**：在白名单列出"原型引用但 OpenAPI 未定义"的 API 红线清单，前端 api 封装仅限 22 个 OpenAPI 端点。
4. **关闭 P1-3**：白名单明确 Idempotency-Key 生成方案（UUID/nanoid，每次唯一，8..128），注入 order/paper、follow-start、follow-stop 三端点 header。
5. **关闭 P1-4**：白名单包含前端错误码映射方案，覆盖 Follow/幂等/风控/订单/账户共 16 个 QuantPF 码。
6. **关闭 P1-5**：生成 `phase1c-paper-frontend-file-whitelist.json`，明确 allowed/forbidden/action/readOnly，禁止修改 router/index.ts、package.json、vite.config.ts、.env、permission.ts、store/modules/permission.ts、后端、契约。
7. **关闭 P1-6**：Paper 下单页只渲染 PAPER 账户/模式，剔除 LIVE/QMT/CTP/broker 入口。
8. **人工批准**：前端页面范围、API 消费范围、菜单/权限变更、是否允许修改前端 lang/路由 store，须经主控批准。

在以上 8 项落实前，Phase 1C 维持 `BLOCKED`，不得进入前端代码实施。

---

## 审查边界

- 本审查为只读；未修改任何业务代码。
- 补读 `error-codes.yaml`、`main.ts`，grep 路由注册入口（确认仅 router/index.ts、permission.ts、store/modules/permission.ts 三处涉及 addRoute/constantRoutes/dynamicRoutes）。
- 确认 `main.ts` 仅 `app.use(router)` + `import './permission'`，无其他路由注册入口；DeepSeek inventory 未遗漏真实路由（重点 1 通过）。
- 未连接生产/外部系统；未执行 Git；仅生成 `docs/ai/phase0/phase1c-paper-frontend-independent-review.md`。