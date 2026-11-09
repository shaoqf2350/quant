# QuantPF Phase 1C Paper Frontend Plan and Contract Consumption Audit

- 审计日期：2026-10-07（Asia/Shanghai）
- 阶段：`PHASE1C-PAPER-FRONTEND-PLAN-AND-CONTRACT-CONSUMPTION`
- 范围：只读审计、API/路由/权限/原型映射与实施计划
- 结果：`PARTIAL`
- `PHASE1C-PAPER-FRONTEND-IMPLEMENTATION`：`NOT STARTED / NOT ALLOWED`
- 独立模型审查：本轮未调用 DeepSeek 或 GLM；未生成或伪称存在独立审查签字

## 1. 执行边界

只审计 `D:\workspace\quant\plus-ui`、后端正式 OpenAPI/错误码/菜单事实源和前端原型。未修改 Vue、TS、JSON、package manifest、lockfile、Vite、env 或后端代码/契约；未执行 `pnpm install/lint/build/test`、Git 命令、生产 API 请求或外部系统连接。

敏感配置只记录“存在/风险”结论，未在本报告、命令日志、manifest、风险登记和 handoff 中记录其值。前端 `.env` 中的客户端 RSA 私钥配置需按 P0 处理，见风险 `P1C-FE-P0-001`。

## 2. 前端技术基线

| 项目 | package.json 声明 | lockfile/实测 | 结论 |
|---|---|---|---|
| Vue | 3.5.42 | 3.5.42 | 已安装依赖未执行；lockfile 锁定值 |
| Element Plus | 2.14.5 | 2.14.5 | 同上 |
| ECharts | 6.1.0 | 6.1.0 | 同上 |
| Vite | ^8.2.2 | 8.2.2 | 声明范围与锁定值 |
| TypeScript | ^6.0.3 | 6.0.3 | 声明范围与锁定值 |
| Vue Router | 5.3.1 | 5.3.1 | 路由实现版本 |
| Pinia | 4.0.3 | 4.0.3 | 状态管理 |
| pnpm | `packageManager=pnpm@10.34.5` | lockfileVersion `9.0` | 本机 `pnpm --version` 为 10.34.5 |
| Node | engines `>=20.19.0` | 本机 `node --version` 为 v25.2.1 | 满足声明范围；本轮未执行 build/lint |
| 测试脚本 | 无 `test` script | Vitest 4.1.11 为 devDependency | 存在测试运行器依赖，但 package scripts 未提供 test 命令 |

依赖版本基于 package.json 和 pnpm-lock.yaml 静态读取；没有执行 `pnpm install`，不声称 node_modules 与 lockfile 已完全一致。

## 3. 工程结构与路由事实

- 当前 `src/views` 下有 55 个 Vue 文件；`src/api` 下有 61 个 TypeScript 文件。
- `src/api/quant/` 不存在；`src/views/quant/` 不存在；源码内未发现 Paper/QuantPF API 消费实现。
- `src/router/index.ts` 只声明公共静态路由：登录/注册/错误页/首页/个人中心等；`dynamicRoutes` 是空数组。
- 登录后 `permission.ts` 调用后端 `/system/menu/getRouters`，将后端组件路径通过 `import.meta.glob('./../../views/**/*.vue')` 生成的 view map 解析；`Layout`、`ParentView`、`InnerLink` 特殊处理。动态路由不是由 QuantPF 菜单 YAML 自动生成。
- 新增页面如获批，组件路径、后端菜单 component 值、路由 path/name 和实际文件必须逐项一致，并做重复 route name、未知 component 和权限回归测试。
- SnailJob iframe 页面存在：`src/views/monitor/snailjob/index.vue`，地址来自 `VITE_APP_SNAILJOB_ADMIN`。这是已有运维控制台入口，不属于 Paper MVP；本阶段不修改/删除。

## 4. 菜单、权限与安全边界

- 菜单/路由当前运行事实来自 RuoYi 后端 `/system/menu/getRouters`；前端权限指令/`auth.hasPermiOr` 主要控制 UI 展示，不能代替后端 Sa-Token/API 授权。
- `contract/menu/quantpf-menu-facts.yaml` 当前仅有 9 个菜单事实：dashboard、research、market、strategy、backtest、paper-account、paper-order、risk、audit。
- 菜单 YAML 未被前端代码直接导入；它是契约/事实文档，不等同于数据库菜单已部署。
- Paper Follow、Paper Position 菜单事实缺失；职位/持仓页面也没有正式 API。原型权限码（如 `quant:account:list`、`quant:trade:list`、`quant:paper:list`）与 menu facts 的 `quant:paper-account:view`、`quant:paper-order:view` 不一致，不能直接照抄原型权限。
- Paper 页面权限建议先沿用正式 menu facts 已有 view permission；写操作权限需单独经后端菜单/权限事实审批，禁止只在前端按钮做保护。

### P0 安全发现

`.env.development` 和 `.env.production` 均包含 Vite 前缀的 RSA private-key 配置；`src/utils/jsencrypt.ts` 从 `import.meta.env` 读取并在浏览器端调用 `setPrivateKey`。Vite 前缀变量会面向客户端构建，因此不能把私钥保密。必须由维护者立即轮换/撤销该密钥对，并将私钥移出前端构建配置；后续采用不依赖客户端私钥的安全协议。不得把密钥值写入工单、日志或新报告。在完成轮换、配置清理和产物检查前，阻止生产打包/发布及新的前端构建交付。

## 5. Paper API 消费矩阵

前端 API 文件现状：以下所有 QuantPF API 均“未消费”，没有 `src/api/quant`。所有通用 response 除 Health/Follow response 外多为 `ApiResponse.data: {}`，具体 DTO/分页 rows schema 未被 OpenAPI 定义，前端类型生成不能据此安全完成。

| 能力 | OpenAPI 方法与路径 | 当前消费 | request/response 与差异 | 幂等/分页/计划 |
|---|---|---|---|---|
| Paper Account | `GET /api/quant/account/page` | 否 | 页列表；响应为泛型 ApiResponse，缺少 Account DTO | pageNum/pageSize；计划支持列表 |
| Account Ledger | `GET /api/quant/account/{id}/ledger` | 否 | id + page 参数；Ledger DTO 未定义 | pageNum/pageSize；计划作为 Account 详情/tab |
| Paper Order 查询 | `GET /api/quant/order/page` | 否 | accountId/status 可选；Order DTO 未定义 | pageNum/pageSize；计划支持 |
| Paper Order 提交 | `POST /api/quant/order/paper` | 否 | PaperOrderRequest 有结构；OpenAPI 声明 202，后端实现实际同步状态需实施前核准；通用 response 无 Order response DTO | Idempotency-Key；不得依赖通用 repeat-submit 拦截代替业务幂等 |
| Paper Order 撤单 | OpenAPI 未登记 | 否 | 不能在前端按原型生成调用；必须先核实后端正式路由并更新契约 | 暂缓，契约冻结前不得消费 |
| Paper Execution 查询 | OpenAPI 没有 execution 路径 | 否 | 无 endpoint/DTO | 暂缓；不得拼造 API |
| Paper Position | OpenAPI 没有 position 路径 | 否 | 无 endpoint/DTO | 阻塞 Position 页面；先做契约/API 补齐计划 |
| Paper Risk Status | `GET /api/quant/risk/status` | 否 | ApiResponse data 无 RiskStatus DTO | 无分页；计划支持只读状态 |
| Paper Risk Halt/Resume | `POST /api/quant/risk/halt`, `/resume` | 否 | request schema 有 reason/scope/targetId；response 泛型 | 高风险二次确认、权限、成功/失败状态；resume 不应默认暴露给普通角色 |
| Follow Start/Stop | `POST /api/quant/paper/follow-start`, `/follow-stop` | 否 | start request accountId/strategyId/mode=PAPER；stop sessionId/reason；定义了 session response schema，但 operation response 使用通用 ApiResponse | 必须传 Idempotency-Key；需要控制双击、重放和错误码；当前缺少 GET 当前/历史 Follow session API，无法做可靠状态列表/恢复显示 |
| Audit | `GET /api/quant/audit/page` | 否 | 分页 response data 具体 Audit DTO 未定义 | pageNum/pageSize；计划只读列表，敏感字段脱敏 |
| Job Execution Audit | `GET /api/quant/job/execution/page` | 否 | 分页 response DTO 未定义；这只是执行审计查询，不是 XXL-JOB 管理 API | pageNum/pageSize；如进 MVP，仅作为只读记录，不展示调度控制 |

OpenAPI 共 22 个 QuantPF path object。分页参数实际名为 `pageNum` / `pageSize`；lockfile/源码通用 `PageResult` 使用 `total` / `rows`，但 QuantPF OpenAPI 的 ApiResponse.data 未承诺该具体结构，须先冻结 DTO。

全局前端 `src/utils/errorCode.ts` 目前仅含常见 HTTP 401/403/404 和 default 文案，不含 `QPF-*` 错误码；request interceptor 对后端 `code/msg` 做通用处理，但具体 Follow/Order/Risk 错误可读性需通过独立错误码映射或后端 message 契约解决。实际错误码在 `contract/error-codes/quantpf-error-codes.yaml`。

## 6. OpenAPI 与前端/API 差异

1. 正式 OpenAPI 22 个路径均未被前端消费。
2. OpenAPI 有 account/order/risk/audit/job execution；没有 position、execution 查询、Follow session 查询、撤单操作。
3. 原型映射使用 `/api/quant/account/summary`、`/api/quant/position/list`、`/api/quant/trade/order/list`、`/api/quant/audit/list` 等非当前 OpenAPI 路径；不可直接照用。
4. OpenAPI Paper order submit 声明 202，但当前后端是否实际 202 需以 controller/runtime 实测确认；禁止前端自行假设异步轮询。
5. Follow OpenAPI 有 start/stop 及 Idempotency-Key，但没有读取当前/历史 session 的 API，页面无法以服务端事实恢复状态。
6. 绝大多数响应只定义 `{code,msg,data:{}}`，缺少具体 response DTO、分页 envelope 和字段语义。
7. OpenAPI `PaperFollowSessionResponse` 已定义，但 Follow operations 引用通用 ApiResponse，不直接引用该 schema。
8. `errorCode.ts` 不含 QuantPF 错误码映射。

## 7. 原型页面映射和范围决定

原型是 V25 静态模拟界面，共 50 个页面映射；README 明确使用模拟数据、不发真实网络请求，页面与 API 必须重新按 V29 OpenAPI 核验。

| 页面/原型映射 | 目标能力 | API/权限事实 | Phase 1C 建议 |
|---|---|---|---|
| `trade-accounts.html` 账户总览 | Paper Account + Ledger | 原型 endpoint 与 OpenAPI 不同；菜单事实 quant.paper-account | 经 DTO 冻结后纳入 |
| `trade-orders.html` 委托订单 | Paper Order list/submit | 原型路径不匹配；正式 API 有 page/submit，无契约撤单 | 经 DTO/状态/幂等核准后纳入 |
| `trade-positions.html` 持仓管理 | Paper Position | 正式 OpenAPI 无 Position API，菜单事实也无 | 延期，先补契约/API |
| `risk-status.html` 风控总览 | Risk status | 有 status API；原型 permission `quant:risk:list` 与 menu fact `quant:risk:view` 不同 | 纳入，只读优先 |
| `risk-halt.html` 紧急熔断 | Halt/Resume | 有 halt/resume API；需二次确认及写权限 | 可纳入受限操作，人工批准权限后 |
| `trade-paper.html` 模拟盘管理 | Follow 状态/控制 | 原型路由/API 与当前 OpenAPI 不同；Follow 只有 start/stop，无 query；无 menu fact | 延期完整页；补 GET session API 和 menu/permissions 后再做 |
| `ops-audit.html` 操作审计 | Audit page | 原型 `/audit/list` 与正式 `/audit/page` 不同；menu fact quant.audit | 纳入只读页，须冻结 DTO 和脱敏规则 |
| `ops-jobs.html` XXL-JOB任务 | Scheduler management | 原型是 XXL-JOB 管理；当前底座仍保留 SnailJob，Phase 1C 排除 | 排除；不是执行审计页，禁止提供双调度器控制 |
| `ops-permissions.html` 量化权限 | 权限管理 | 原型仅映射 RuoYi role/menu API，无 QuantPF 权限契约 | 延期到菜单/权限施工审批 |
| `broker-qmt.html` / `broker-ctp.html` / `broker-status.html` / `broker-logs.html` | 实盘/柜台配置 | 禁止能力 | 明确排除，不创建路由、API 或页面 |

### 推荐实施切片（待人工批准）

- **切片 A：契约具备后** Account/Ledger、Order 查询/提交、Risk Status、Audit read-only。
- **切片 B：权限批准后** Risk halt/resume（二次确认、权限、审计验证）。
- **暂缓** Position/Execution 页面，直到正式查询 API + response DTO 加入契约并后端可验收。
- **暂缓** 完整 Follow 页面，直到 Follow session GET/page API、DTO、菜单/权限事实及 OpenAPI 完成；当前只能验证 start/stop 写操作客户端，不能提供可恢复、以服务端为准的完整管理页。
- Job execution 如纳入，仅做只读 execution audit，不是 XXL-JOB 管理。

## 8. 下一阶段候选文件白名单（不是当前授权）

本阶段唯一可写范围是后端仓库 `docs/ai/phase0/` 与 `docs/ai/handoff/`。附带的 `phase1c-paper-frontend-file-whitelist.json` 是**下一阶段审批候选表**，并非前端写授权。

候选前端实现文件类别：

- `plus-ui/src/api/quant/**`：Paper API modules 和 DTO types；
- `plus-ui/src/views/quant/paper-account/**`；
- `plus-ui/src/views/quant/paper-order/**`；
- `plus-ui/src/views/quant/risk/**`；
- `plus-ui/src/views/quant/audit/**`；
- 若获批并且契约先补齐，再新增 `paper-position/**`、`paper-follow/**`；
- `plus-ui/src/utils/errorCode.ts`：仅在确定错误码展示方案后；
- `plus-ui/src/router/index.ts`、`src/store/modules/permission.ts` 不建议修改，优先沿用后端动态菜单；如确需修改需单独批准；
- E2E/component tests 与必要 fixtures（具体路径由下一阶段计划冻结）。

不得自动修改 package.json、pnpm-lock.yaml、vite.config.ts、`.env.*`、RSA crypto 实现、调度页面、RuoYi 通用权限逻辑。任何依赖变更须另行批准。

## 9. 页面验收标准

所有正式页面至少通过：

1. 后端菜单动态路由加载；permission string 与 approved menu facts 一致；后端 API 仍执行授权；无 hard-coded 假菜单。
2. API 路径/方法/字段与冻结 OpenAPI 一致；不调用原型中未登记 endpoint。
3. Loading、empty、error、retry、分页 total/rows、筛选重置和 API 失败状态覆盖。
4. 写操作重复点击保护与 Idempotency-Key 生成/稳定复用语义正确；网络超时后可安全重放；UI 层重复提交不能取代后端幂等。
5. 风控 halt/resume 具备二次确认、明确 PAPER 标签、权限限制和结果审计提示。
6. Follow 页面状态必须从持久化服务端查询恢复，不能用浏览器 storage/内存伪造 RUNNING。
7. Position/Execution 页面只在正式 query API 及 DTO 可用后验收。
8. 任何前端包/产物中不含私钥、密码、Token 或券商凭据；密钥事故处置完成后再允许 production build。
9. 页面不得包含 LIVE 下单/撤单、QMT/CTP broker 操作或 XXL-JOB/SnailJob 调度控制。
10. pnpm lint/build 与新增组件/API 测试通过；当前阶段未执行这些命令。

## 10. 风险和阻塞

- P0：Vite 环境中存在客户端私钥配置并被源码读取；必须轮换/撤销、清理配置、替代协议和产物扫描后才可 build/release。
- P1：Position/Execution/Follow-session read API 缺失；阻塞对应完整页面。
- P1：OpenAPI 响应 DTO 不完整，order status 语义待对齐；阻塞可靠类型和 UI 联调。
- P1：Paper Follow/Position menu facts 缺失；原型权限与正式菜单不一致。
- P1：全局错误码映射不含 QPF codes。
- P1：OpenAPI 未列撤单 endpoint；不得依据原型自行消费。
- P2：无 Vitest test script；虽依赖 Vitest，当前没有统一 test 命令。
- P2：前端技术版本较新；必须在批准的工具链基线上实际 lint/build 验收。本轮遵照禁止，未执行。
- P2：SnailJob iframe 仍存在；不属于 Paper MVP，调度迁移另开阶段。

## 11. 构建前置条件

1. 先处置 P0 私钥风险，不运行生产 build 作为“验证”手段，直到轮换/清理由安全负责人完成。
2. 解决 Position、Follow session read、Execution API 的契约缺口或正式延期相应页面。
3. 冻结所有 MVP response DTO、分页 envelope、业务错误码和 Order 202/同步响应语义。
4. 批准菜单事实及后端动态菜单实际施工路径。
5. 批准下一阶段文件白名单、页面范围、写权限和是否允许路由/API 文件变化。
6. 工具链确认后再执行 `pnpm lint`、`pnpm build`；本阶段未运行。

## 12. 阶段判定

审计与方案文档已完成，但存在生产构建安全 P0 和页面/契约阻塞，故本阶段为：

```text
PHASE1C-PAPER-FRONTEND-PLAN-AND-CONTRACT-CONSUMPTION: PARTIAL
PHASE1C-PAPER-FRONTEND-IMPLEMENTATION: NOT STARTED / NOT ALLOWED
```

不得因为 Phase 1B 后端通过而自动进入前端实现。需要先解决 P0、由用户批准具体白名单/页面范围，并完成契约补齐或范围缩减。

## 13. DeepSeek / GLM 最终复核收敛

已读取并复核：

- `phase1c-paper-frontend-deepseek-inventory.md`；
- `phase1c-paper-frontend-independent-review.md`。

GLM 独立审查结果为 `PASS_WITH_CHANGES`，其结论纳入本主报告：

1. **新增 P0：契约 response DTO 缺失**。Paper 账户、订单、审计、风控、仪表盘等查询接口的 `ApiResponse.data` 仍是 `{}`，不能据此安全编写前端 VO、表格字段和分页逻辑。
2. **升级 P0：客户端私钥风险**。Vite 前端环境变量包含 private-key 名称且源码读取，必须轮换/撤销、清理并完成产物扫描；在此之前不允许生产构建/发布。
3. **P1：菜单 DB 事实未确认**。YAML 不是 `sys_menu` 数据库事实；必须确认实际 component/perms，再冻结前端权限。
4. **P1：原型 API 红线**。原型中未出现在 V29 OpenAPI 的路径禁止进入前端 API 封装。典型包括 `/api/quant/position/list`、`/api/quant/trade/order`、`/api/quant/trade/order/list`、`/api/quant/trade/record/list`、`/api/quant/account/summary`、`/api/quant/audit/list`、`/api/quant/job/list`、`/api/quant/paper/account/{accountId}`、`/api/quant/broker-config` 等。
5. **P1：幂等键必须独立实现**。现有 `repeatSubmit` 只是短窗口防重复提交，不等价于 `Idempotency-Key`。订单提交、Follow start、Follow stop 必须由前端生成 8–128 字符唯一键并注入 header，且重试语义必须可审计。
6. **P1：QPF 错误码映射缺失**。`src/utils/errorCode.ts` 当前没有 QuantPF 业务错误码；前端需在获批后补充稳定的用户提示和恢复动作映射。
7. **P2：QuantPF 路由只允许后端动态菜单注入**。不得把业务路由写入 `router/index.ts` 的 `dynamicRoutes` 作为绕过菜单事实的替代方案。
8. **P2：组件路径需严格对照**。后端 `sys_menu.component` 必须与 `import.meta.glob` 生成的 `views/**` 相对路径一致；`loadView` 未命中会静默返回 `undefined`，实施前必须建立 component↔file 对照和开发期告警策略。
9. **P1：API 前缀需后端事实确认**。现有 RuoYi API 多为无 `/api` 前缀，而 V29 OpenAPI 使用 `/api/quant`；前端实施前必须以实际 Controller/网关映射确认，不得只按原型或猜测。

因此本阶段仍为：

```text
PHASE1C-PAPER-FRONTEND-PLAN-AND-CONTRACT-CONSUMPTION: PARTIAL
PHASE1C-PAPER-FRONTEND-IMPLEMENTATION: NOT STARTED / NOT ALLOWED
```
