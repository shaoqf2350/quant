# QuantPF Phase 1C Frontend Contract Closure 只读盘点

- 盘点日期：2026-10-07
- 阶段：`PHASE1C-PAPER-FRONTEND`（Contract Closure 只读盘点，不修改任何代码/契约/数据库/环境）
- 盘点模型：Phase 1C Frontend Contract Closure 只读盘点模型
- 只读范围：plan、independent-review、`plus-ui`、后端 QuantPF Controller/DTO/Application Service、OpenAPI、错误码、菜单事实源
- 结论前置：**契约与后端实现存在多处实质差异**（字段命名、HTTP 状态、分页 envelope、缺失端点），前端实施若只按 OpenAPI 会产生运行时错误。

---

## 1. OpenAPI path → 后端 Controller 逐条映射

基于真实后端源码（`ruoyi-admin/src/main/java/com/quantpf/web/controller/*.java`）与 `application.yml`（`server.servlet.context-path: /`）实测导出。

| # | OpenAPI path (method) | 后端 Controller | 类级 `@RequestMapping` | 方法级映射 | 是否存在 |
|---|---|---|---|---|---|
| 1 | `/api/quant/health` GET | `QuantPfHealthController` | `/api/quant/health` | `@GetMapping`（空） | ✅ 匹配 |
| 2 | `/api/quant/dashboard/summary` GET | — | — | — | ❌ **无 Controller** |
| 3 | `/api/quant/instrument/page` GET | — | — | — | ❌ **无 Controller** |
| 4 | `/api/quant/market/kline` GET | — | — | — | ❌ **无 Controller** |
| 5 | `/api/quant/news/page` GET | — | — | — | ❌ **无 Controller** |
| 6 | `/api/quant/finance/statements` GET | — | — | — | ❌ **无 Controller** |
| 7 | `/api/quant/pool/page` GET | — | — | — | ❌ **无 Controller** |
| 8 | `/api/quant/strategy/page` GET | — | — | — | ❌ **无 Controller** |
| 9 | `/api/quant/signal/page` GET | — | — | — | ❌ **无 Controller** |
| 10 | `/api/quant/backtest` POST | — | — | — | ❌ **无 Controller** |
| 11 | `/api/quant/backtest/{id}` GET | — | — | — | ❌ **无 Controller** |
| 12 | `/api/quant/account/page` GET | `QuantPaperAccountController` | `/api/quant/account` | `@GetMapping("/page")` | ✅ 匹配 |
| 13 | `/api/quant/account/{id}/ledger` GET | `QuantPaperAccountController` | `/api/quant/account` | `@GetMapping("/{id}/ledger")` | ✅ 匹配 |
| 14 | `/api/quant/order/page` GET | `QuantPaperOrderController` | `/api/quant/order` | `@GetMapping("/page")` | ✅ 匹配（参数差异见 §2） |
| 15 | `/api/quant/order/paper` POST | `QuantPaperOrderController` | `/api/quant/order` | `@PostMapping("/paper")` | ✅ 匹配（字段/状态差异见 §2） |
| 16 | `/api/quant/paper/follow-start` POST | `QuantPaperFollowController` | `/api/quant/paper` | `@PostMapping("/follow-start")` | ✅ 匹配 |
| 17 | `/api/quant/paper/follow-stop` POST | `QuantPaperFollowController` | `/api/quant/paper` | `@PostMapping("/follow-stop")` | ✅ 匹配 |
| 18 | `/api/quant/risk/status` GET | `QuantPaperRiskController` | `/api/quant/risk` | `@GetMapping("/status")` | ✅ 匹配 |
| 19 | `/api/quant/risk/halt` POST | `QuantPaperRiskController` | `/api/quant/risk` | `@PostMapping("/halt")` | ✅ 匹配 |
| 20 | `/api/quant/risk/resume` POST | `QuantPaperRiskController` | `/api/quant/risk` | `@PostMapping("/resume")` | ✅ 匹配 |
| 21 | `/api/quant/job/execution/page` GET | `QuantPaperJobExecutionController` | `/api/quant/job/execution` | `@GetMapping("/page")` | ✅ 匹配 |
| 22 | `/api/quant/audit/page` GET | `QuantPaperAuditController` | `/api/quant/audit` | `@GetMapping("/page")` | ✅ 匹配 |

**后端额外存在但 OpenAPI 未登记的端点：**

| 端点 | Controller | 说明 |
|---|---|---|
| `POST /api/quant/order/{id}/cancel` | `QuantPaperOrderController.cancel` | **后端已实现撤单，但 OpenAPI 无此 path**（契约落后于实现） |

**结论**：OpenAPI 22 个 path 中 **11 个（dashboard/instrument/market/news/finance/pool/strategy/signal/backtest×2）在 ruoyi-admin 无对应 Controller**（这些能力属后续阶段或尚未实现，当前仅有"Paper 核心 11 端点"可落地）。Paper 核心（account/order/risk/audit/job-execution/follow/health）共 11 端点已实现，但存在字段与状态差异。

---

## 2. 每个 API 的真实 HTTP method / RequestMapping / response 类型

以下为 Paper 核心端点的**真实实现**（非 OpenAPI 声明）：

| 端点 | 真实 method | 真实 RequestMapping | 真实返回 | 关键差异 |
|---|---|---|---|---|
| `/api/quant/account/page` | GET | `@GetMapping("/page")`，参数 `pageNum`(default 1), `pageSize`(default 20) | `R.ok(List<AccountView>)` | OpenAPI 声明 `Page`/`PageSize` 参数名（`pageNum`/`pageSize` 一致 ✅）；但返回**裸 List 而非 `{total,rows}` 分页 envelope** |
| `/api/quant/account/{id}/ledger` | GET | `@GetMapping("/{id}/ledger")`，参数 `id`(Long) | `R.ok(List<LedgerEntry>)`；`IllegalArgumentException`→`R.fail(404)` | 返回裸 List；**无分页参数**（与 OpenAPI 声明的 Page/PageSize 不一致） |
| `/api/quant/order/page` | GET | `@GetMapping("/page")`，参数仅 `pageNum`/`pageSize` | `R.ok(List<OrderView>)` | **OpenAPI 声明 `accountId`/`status` 可选过滤参数，Controller 未实现**；返回裸 List |
| `/api/quant/order/paper` | POST | `@PostMapping("/paper")`，header `Idempotency-Key`(required=false，手工校验)，body `PaperOrderRequest` | `R.ok(OrderView)`；`R.fail(400/409)` | OpenAPI 声明 **202**，实际 `R.ok` → **HTTP 200**；请求体字段命名差异（见 §4） |
| `/api/quant/order/{id}/cancel` | POST | `@PostMapping("/{id}/cancel")`，参数 `id`(Long), `reason`(default) | `R.ok(OrderView)`；`R.fail(409/400)` | **OpenAPI 未登记** |
| `/api/quant/paper/follow-start` | POST | `@PostMapping("/follow-start")`，header `Idempotency-Key`，body `PaperFollowStartRequest` | `R.ok(Result)`；`QuantPfDomainException`→`R.fail(400/404/409)`，其它 RuntimeException→`R.fail(500)` | OpenAPI 200 ✅；body required 校验（请求 null→QPF-REQ-001 400） |
| `/api/quant/paper/follow-stop` | POST | `@PostMapping("/follow-stop")` | `R.ok(Result)` | 同上 |
| `/api/quant/risk/status` | GET | `@GetMapping("/status")` | `R.ok(RiskStatusView)` | OpenAPI 200 ✅ |
| `/api/quant/risk/halt` | POST | `@PostMapping("/halt")`，body `RiskReasonRequest` | `R.ok(RiskStatusView)` | OpenAPI 200 ✅ |
| `/api/quant/risk/resume` | POST | `@PostMapping("/resume")` | `R.ok(RiskStatusView)` | OpenAPI 200 ✅ |
| `/api/quant/job/execution/page` | GET | `@GetMapping("/page")`，参数 pageNum/pageSize + `jobName`/`job_name`、`executionId`/`execution_id`、`status`（camel/snake 双别名） | `R.ok(List<JobExecutionView>)` | 返回裸 List；过滤参数 OpenAPI 未声明（OpenAPI 仅 Page/PageSize） |
| `/api/quant/audit/page` | GET | `@GetMapping("/page")`，参数 pageNum/pageSize + `requestId`/`request_id`、`aggregateType`/`aggregate_type`、`aggregateId`/`aggregate_id`（双别名） | `R.ok(List<AuditView>)` | 返回裸 List；过滤参数 OpenAPI 未声明 |
| `/api/quant/health` | GET | `@GetMapping` | `R.ok(Map{status,phase,liveEnabled})` | OpenAPI 200 ✅ |

**通用 HTTP 语义（关键）**：所有 Controller 返回 `org.dromara.common.core.domain.R`。`R.ok`/`R.fail(code,msg)` **均产生 HTTP 200**，错误码置于响应体 `code` 字段。因此 OpenAPI 为 follow/order 声明的 `400`/`404`/`409` HTTP 状态**不会在 HTTP 层出现**——它们在 body `code` 中表达（RuoYi 全局 `R` 模式）。这与前端 `request.ts` 响应拦截器（读 `res.data.code` 而非 HTTP status）吻合，但**OpenAPI 的响应状态建模与实际不符**。

---

## 3. API `/api` 前缀与 context-path 结论

- `ruoyi-admin/src/main/resources/application.yml` 第 5-7 行：`server.servlet.context-path: /`（根路径，无额外 context 前缀）。
- 后端 Controller 类级 `@RequestMapping("/api/quant/...")` **自带 `/api` 前缀**。
- 因此网关直连时，QuantPF 端点的**完整服务端路径**即 `/api/quant/...`（`/api` 前缀是后端 Controller 声明的，非网关/context 添加）。
- 前端 `request.ts` `baseURL = VITE_APP_BASE_API`（开发 `/dev-api`，生产 `/prod-api`），`vite.config.ts` proxy 将 `/dev-api` 前缀重写去除后转发 `http://localhost:8080`。
- **结论**：前端对 QuantPF 的 api url 必须写 `/api/quant/...`（含 `/api`），与 RuoYi 既有模块（写 `/system/...` 等无 `/api`）**前缀规则不同**。这是因为 RuoYi 网关会为通用模块补 `/api`，而 QuantPF Controller 已内联 `/api`（需以生产网关实际路由为准，本地 proxy 直连 8080 时 `/api` 前缀必须保留）。
- **风险**：若网关对 QuantPF 也统一剥离/补 `/api`，则该前缀会在生产环境与本地不一致。此为网关路由配置事实，本盘点无网关源码访问，**标记为需部署侧确认的 UNKNOWN**。

---

## 4. Paper Account / Order / Risk / Audit / Follow 的 response DTO 清单

以下 DTO 字段来自**真实 Application Service record 返回类型**（`quantpf-modules/quantpf-finance/.../Persistent*.java`），是前端 `types.ts` 的**唯一可信事实源**（OpenAPI 的 `data:{}` 不可用）。

| 能力 | DTO record | 字段 |
|---|---|---|
| Account page | `PersistentPaperAccountApplicationService.AccountView` | `id:Long, accountCode:String, status:String, availableCash:BigDecimal, frozenCash:BigDecimal` |
| Account ledger | `...LedgerEntry` | `type:String, amount:BigDecimal, balanceAfter:BigDecimal, referenceId:String, occurredAt:Instant` |
| Order page/submit/cancel | `PersistentPaperOrderApplicationService.OrderView` | `id:Long, accountId:Long, instrumentId:Long, clientOrderId:String, status:String, side:String, orderType:String, quantity:BigDecimal, rejectReason:String` |
| Risk status/halt/resume | `PersistentPaperRiskApplicationService.RiskStatusView` | `status:String("CLEAR"\|"HALTED"), reason:String(nullable)` |
| Follow start/stop | `PaperFollowPersistenceTransactionService.Result` | `sessionId:String, accountId:Long, strategyId:Long, mode:String("PAPER"), status:String("RUNNING"\|"STOPPED"), startedAt:Instant, stoppedAt:Instant(nullable), idempotent:boolean` |
| Audit page | `PersistentAuditApplicationService.AuditView` | `id:Long, requestId:String, actorId:String, action:String, aggregateType:String, aggregateId:String, result:String, detail:String, occurredAt:Instant` |
| Job execution page | `PersistentJobExecutionApplicationService.JobExecutionView` | `id:Long, jobName:String, executionId:String, schedulerType:String, status:String, requestId:String, startedAt:Instant, finishedAt:Instant, errorCode:String, errorMessage:String, createdAt:Instant` |
| Health | `Map` | `status, phase, liveEnabled` |

**关键差异（P0/P1 风险）**：

1. **分页 envelope 缺失**：所有 `page` 接口返回**裸 `List<...>`**（无 `total`、无 `rows` 包裹）。前端 RuoYi 通用 `PageResult<T>{total,rows}` 不能直接套用；且前端无法获得总数做分页。这是**契约与实现的实质冲突**——OpenAPI 声明 Pag`E` 参数但未承诺 envelope，实现返回 List。
2. **字段命名差异**：Order 请求体用 `instrumentId:Long`（非 OpenAPI 的 `instrumentCode:String`）、`limitPrice/estimatedNotional/fillPrice:BigDecimal`（OpenAPI 用 `price` 单字段）。OrderView 用 `instrumentId`（非 `instrumentCode`）。**前端若按 OpenAPI `PaperOrderRequest`（instrumentCode+price）提交，后端 `PaperOrderRequest` record 反序列化会因字段名不匹配而丢值或报错。**

---

## 5. 缺失 Position / Execution / Follow session read / Order cancel 的事实

| 能力 | 后端实现 | OpenAPI | 结论 |
|---|---|---|---|
| Paper Position（持仓查询） | ❌ 无 Controller / 无 Application Service | ❌ 无 path | **两侧均缺失**，Position 页面无条件实施，须先补契约+API |
| Paper Execution（成交流水查询） | ❌ 无 Controller（成交仅为 Order.submit 内部的 `executionId` 派生，无独立查询端点） | ❌ 无 path | **两侧均缺失**，Execution 页面无条件实施 |
| Follow session read（查询当前/历史 RUNNING session） | ❌ 仅有 `selectRunning`/`selectBySessionIdForUpdate`（Mapper 内部，未暴露为 GET 端点）；Controller 仅有 start/stop 两个 POST | ❌ 无 GET path | **两侧均缺失**，Follow 页面无法以服务端事实恢复状态（前端不能用 localStorage 伪造 RUNNING） |
| Order cancel（撤单） | ✅ `POST /api/quant/order/{id}/cancel` 已实现（`PersistentPaperOrderApplicationService.cancel` + `QuantPaperOrderController.cancel`） | ❌ **OpenAPI 未登记** | **后端有、契约无**。前端如需撤单，必须先补 OpenAPI 契约；不得按原型 `/api/quant/trade/order/cancel` 猜造 |

---

## 6. sys_menu component/perms 只读核对结果

- 本盘点无数据库访问能力（未连接后端 DB），`sys_menu` 表实际 `component`/`perms` 值**无法读取**。
- 唯一契约事实源 `contract/menu/quantpf-menu-facts.yaml`：
  - 9 个菜单 code：`quant.dashboard/research/market/strategy/backtest/paper-account/paper-order/risk/audit`。
  - 权限码（9 个）：`quant:dashboard:view`、`quant:research:view`、`quant:market:view`、`quant:strategy:view`、`quant:backtest:view`、`quant:paper-account:view`、`quant:paper-order:view`、`quant:risk:view`、`quant:audit:view`。
  - `forbiddenCapabilities: [LIVE, QMT, CTP, REAL_BROKER_ORDER, REAL_BROKER_CANCEL]`。
- **sys_menu.component 与 sys_menu.perms 的真实 DB 值 = UNKNOWN**（无 DB 访问）。
- 前端 `loadView` 要求后端 `component` 字段（如 `quant/paper-account/index`）与 `views/**/*.vue` 相对路径严格一致，否则静默空白页。**该 component 命名约定 = UNKNOWN**。

---

## 7. Idempotency-Key 三个写接口的前端注入方案

以下 3 个 POST 端点由后端要求 `Idempotency-Key` header（`required=false` 但 Controller 内部校验空值返回 400）：

| 端点 | 幂等键语义（后端） | 前端注入要求 |
|---|---|---|
| `POST /api/quant/order/paper` | `submit` 用 key 防重复下单；`PersistentPaperOrderApplicationService` 将 key 写入 `PaperOrderRow.idempotencyKey` | 每次点击生成唯一 key；定时/重试场景同一逻辑操作复用同一 key |
| `POST /api/quant/paper/follow-start` | `PaperFollowPersistenceTransactionService.start` 用 `start_request_id` 做幂等重放/冲突（`QPF-IDEMP-001`） | 同上 |
| `POST /api/quant/paper/follow-stop` | `stop` 用 `stop_request_id` 幂等 | 同上 |

**建议方案**（须在实施白名单冻结）：
1. 生成：`crypto.randomUUID()`（浏览器原生；`plus-ui` 依赖已有 `@noble/ciphers` 可辅助，但 `randomUUID` 足够，长度 36 满足 8..128）。
2. 注入：在 `src/api/quant/.../index.ts` 的 submit/follow-start/follow-stop 请求 `headers: { 'Idempotency-Key': key }`。
3. 语义：**每个"用户意图"生成一次 key**——普通点击每次新 key；同一次表单提交重试（网络超时后重放）复用同一 key；不得用 `request.ts` 的 `repeatSubmit` 短窗口防抖替代（那是防连点，不是幂等）。
4. 校验：3 端点不得复用同一 key；后端返回 `QPF-IDEMP-001`(409)/`QPF-FOLLOW-001`(409) 时前端提示"请求已处理/存在活跃会话"，不重复提交。

---

## 8. QPF 错误码 → UI 行为映射建议

来源 `contract/error-codes/quantpf-error-codes.yaml`（16 个码）。前端 `src/utils/errorCode.ts` 当前无 `QPF-*` 映射，须获批后补充。

| 码 | httpStatus(契约) | 实际 HTTP | UI 行为建议 |
|---|---|---|---|
| QPF-REQ-001 | 400 | 200+body400 | 表单校验错误提示，就地标红 |
| QPF-IDEMP-001 | 409 | 200+body409 | "请求重复/幂等键冲突"，禁用提交按钮展示处理结果，不重发 |
| QPF-ORDER-001 | 409 | 200+body409 | "订单状态非法"，刷新订单列表 |
| QPF-ACCOUNT-001 | 404 | 200+body404 | "账户不存在"，返回账户列表 |
| QPF-CASH-001 | 409 | 200+body409 | "可用资金不足"，高亮账户余额 |
| QPF-POSITION-001 | 409 | 200+body409 | "持仓不足"，提示持仓页（该页未实现，仅提示） |
| QPF-RISK-001 | 409 | 200+body409 | "风控拒绝该请求"+reason，二次确认是否调整 |
| QPF-RISK-002 | 409 | 200+body409 | "模拟盘已熔断"，全局禁止下单按钮 |
| QPF-MARKET-001 | 409 | — | "行情数据过期"，禁用依赖实时行情的操作 |
| QPF-INSTRUMENT-001 | 404 | 200+body404 | "标的不存在" |
| QPF-AUDIT-001 | 500 | — | 审计写失败，提示联系管理员（不影响主流程） |
| QPF-INTERNAL-001 | 500 | 200+body500 | 通用内部错误，展示重试/联系管理员 |
| QPF-FOLLOW-001 | 409 | 200+body409 | "已存在运行中的跟随会话"，刷新 Follow 状态 |
| QPF-FOLLOW-002 | 404 | 200+body404 | "跟随会话不存在" |
| QPF-FOLLOW-003 | 409 | 200+body409 | "账户/策略未满足跟随准入（非 PAPER/ACTIVE）" |
| QPF-FOLLOW-004 | 400 | 200+body400 | "缺少幂等键"，前端拦截（不应发送无 key 请求） |

前端映射实现建议：在 `src/utils/errorCode.ts` 增加 `QPF-*` → 中文文案表；`request.ts` 拦截器对 `res.data.code` 命中 `QPF-*` 时用映射文案替代通用 `ElNotification`。

---

## 9. 原型 API 与正式 OpenAPI 的禁止调用清单

以下原型 `PROTOTYPE_PAGE_MAP.md` 引用的路径**未出现在 V29 OpenAPI**（且后端无对应 Controller），前端实施**禁止调用**：

- `/api/quant/account/summary`（原型账户总览；正式为 `/account/page`）
- `/api/quant/position/list`（无 Position API）
- `/api/quant/trade/order`（无此下单路径；正式为 `/order/paper`）
- `/api/quant/trade/order/list`（无；正式为 `/order/page`）
- `/api/quant/trade/record/list`（无 Execution API）
- `/api/quant/paper/account/{accountId}`（无；正式为 `/account/page` + `/account/{id}/ledger`）
- `/api/quant/audit/list`（无；正式为 `/audit/page`）
- `/api/quant/job/list`（无；这只是执行审计，非 XXL-JOB 管理）
- `/api/quant/broker-config`、`/broker-config/{id}/status`、`/broker-config/{id}/connect-logs`（QMT/CTP，forbiddenCapabilities）
- `/api/quant/instrument/list`、`/instrument/{productType}/{symbol}/summary`、`/kline/query`、`/orderbook/latest/{symbol}`、`/tick/query`、`/kline/import`、`/news/latest`、`/news/sentiment/trend`、`/news/{id}`、`/news/risk-events`、`/finance/earnings-calendar`、`/finance/statements/{symbol}`、`/finance/factors/{symbol}`、`/finance/analysis`、`/finance/peers/{symbol}`、`/strategy`/`strategy-params`/`strategy/{id}/runtime`/`signal/list`/`performance/strategy/list`、`/stock-pool/*`、`/backtest/submit`、`/backtest/list`、`/backtest/result/{id}`、`/strategy-params/compare`、`/risk/rules`、`/risk/log/list`、`/data-source/health`、`/data-quality/check`、`/kline/import-log` ——均无后端 Controller、无 OpenAPI path，**一律禁止前端封装调用**。

---

## 10. 客户端私钥风险整改验收清单

背景：`plus-ui/.env.development` 与 `.env.production` 含 `VITE_APP_RSA_PRIVATE_KEY`（Vite 前缀变量，构建进浏览器 bundle），`src/utils/jsencrypt.ts` 在浏览器调用 `setPrivateKey`。**私钥在客户端不保密**（风险 P0）。本盘点不记录密钥值。

整改验收清单（须在 production build 前全部通过）：

1. ✅ 待办：轮换/撤销该 RSA 密钥对（当前公钥对应私钥已暴露于可分发产物风险面）。
2. ✅ 待办：将私钥移出 `.env.*` 与前端构建配置；前端不得再 `setPrivateKey`。
3. ✅ 待办：替换为不依赖客户端私钥的安全协议（如后端单向签名/会话密钥，或彻底去除前端 RSA）。
4. ✅ 待办：全量扫描现有 `dist`/构建产物，确认无私钥串、无 `encrypt-key` 残留。
5. ✅ 待办：`pnpm build` 产物做关键词扫描（私钥特征、Base64 私钥 header），产出扫描报告。
6. ✅ 待办：完成整改前**禁止**执行生产 build/发布，禁止交付新前端构建。
7. ✅ 待办：新增前端代码不得再引入任何密钥/密码/Token/券商凭据；Follow 的 sessionId/stopReason 不在日志明文输出。

---

## 11. 下一阶段前端白名单 v2 的候选文件

基于本盘点收敛（仅 Paper 核心 11 端点 + 新增撤单契约补齐后），候选文件如下（**非当前授权，须经主控批准后再写**）：

### 11.1 API 层（`plus-ui/src/api/quant/**`，遵循 `index.ts + types.ts`）

| 候选文件 | 对应真实端点 |
|---|---|
| `api/quant/health/index.ts` | `/api/quant/health` |
| `api/quant/paper/account/index.ts` (+types.ts) | `/account/page`、`/account/{id}/ledger`；types 用 `AccountView`/`LedgerEntry` |
| `api/quant/paper/order/index.ts` (+types.ts) | `/order/page`、`/order/paper`、`/order/{id}/cancel`（契约补齐后）；types 用 `OrderView` |
| `api/quant/paper/follow/index.ts` (+types.ts) | `/paper/follow-start`、`/paper/follow-stop`；types 用 `Result` |
| `api/quant/risk/index.ts` (+types.ts) | `/risk/status`、`/risk/halt`、`/risk/resume`；types 用 `RiskStatusView` |
| `api/quant/audit/index.ts` (+types.ts) | `/audit/page`、`/job/execution/page`；types 用 `AuditView`/`JobExecutionView` |

### 11.2 视图层（`plus-ui/src/views/quant/**`，须与后端菜单 component 精确一致）

| 候选文件 | 菜单 code |
|---|---|
| `views/quant/paper-account/index.vue` | quant.paper-account |
| `views/quant/paper-order/index.vue` | quant.paper-order |
| `views/quant/risk/index.vue` | quant.risk |
| `views/quant/audit/index.vue` | quant.audit |

> `paper-position`、`paper-follow` 完整页面**暂缓**（Position/Execution/Follow-session-read API 均缺失，见 §5）；仅当契约+后端 API 补齐后，再纳入 `views/quant/paper-follow/**`。

### 11.3 错误码映射（获批后）

- `src/utils/errorCode.ts`：增加 `QPF-*` 中文文案表（见 §8）。

### 11.4 明确禁止（forbidden，任何情况下不进入白名单）

- `src/router/index.ts`、`src/store/modules/permission.ts`：不改（QuantPF 路由只走后端 `/system/menu/getRouters` 动态注入）。
- `package.json`、`pnpm-lock.yaml`、`vite.config.ts`、`.env.*`、`src/utils/jsencrypt.ts`、`src/utils/request.ts`：不改。
- 后端 Controller/DTO/Service、`contract/**`：前端阶段不改。
- LIVE/QMT/CTP/broker/XXL-JOB/SnailJob 相关页面与 API：不创建、不调用。

---

## 附：本盘点的 UNKNOWN 清单

1. `sys_menu` 表实际 `component`/`perms` DB 值（无 DB 访问）——UNKNOWN。
2. QuantPF `/api` 前缀在**生产网关**的实际路由规则（网关是否再补/剥 `/api`）——UNKNOWN（本地 proxy 直连 8080 时 `/api` 须保留）。
3. OpenAPI 各分页端点的 **`data` schema**（`total`/`rows` 是否存在）——契约未定义，实现返回裸 List，需冻结确认。
4. Order submit 的 HTTP 语义（OpenAPI 202 vs 实现 200）以谁为准——需契约/实现核准。
5. `component` 字符串命名约定（`quant/paper-account/index` vs `quant/paperAccount/index`）——UNKNOWN，须与后端菜单对齐。
6. 网关对 `/api/quant` 的统一前缀处理与 Sa-Token 授权拦截范围——UNKNOWN。

---

## 附：只读证据索引

- `application.yml`（context-path `/`）→ §3。
- `QuantPaperAccountController`/`QuantPaperOrderController`/`QuantPaperRiskController`/`QuantPaperAuditController`/`QuantPaperJobExecutionController`/`QuantPaperFollowController`/`QuantPfHealthController` → §1/§2。
- `PaperOrderRequest`/`RiskReasonRequest`/`PaperFollowStartRequest`/`PaperFollowStopRequest` → §4。
- `PersistentPaperAccountApplicationService`/`PersistentPaperOrderApplicationService`/`PersistentPaperRiskApplicationService`/`PersistentPaperFollowApplicationService`/`PersistentAuditApplicationService`/`PersistentJobExecutionApplicationService`/`PaperFollowPersistenceTransactionService.Result` → §4/§5。
- `contract/openapi/quantpf-v29.yaml`、`contract/error-codes/quantpf-error-codes.yaml`、`contract/menu/quantpf-menu-facts.yaml` → §1/§6/§8/§9。
- `plus-ui`（`.env.*`、`vite.config.ts`、`request.ts`、`jsencrypt.ts`）→ §3/§10。

**未生成假 API，未修改任何代码/契约/数据库/环境文件，未输出任何真实密钥。**