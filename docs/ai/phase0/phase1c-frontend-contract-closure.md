# QuantPF Phase 1C Frontend Contract Closure and Security Remediation

- 审计日期：2026-10-07
- 阶段：`PHASE1C-FRONTEND-CONTRACT-CLOSURE-AND-SECURITY-REMEDIATION`
- 阶段结论：`PARTIAL`
- 前端源码实施：`NOT ALLOWED`
- Production build/release：`BLOCKED`
- 本轮仅向 `RuoYi-Vue-Plus/docs/ai/phase0/` 和 `docs/ai/handoff/` 写入文档。

## 1. 审计范围

已审计上一阶段 handoff、计划、独立审查、风险登记、文件白名单、manifest；V29 方案；前端 package/request/encryption/error-code/router/permission/menu 文件；后端 QuantPF Controller、DTO、OpenAPI、错误码、菜单事实源；原型页面映射；以及已配置本地 `quantpf` schema 的只读 `sys_menu` 查询结果。

本轮未修改 Java、XML、YAML、SQL、Vue、TS、JSON、POM、`.env`、数据库或生产配置；未执行迁移、生产连接、构建、lint、test 或 Git 写操作。任何敏感值均未写入报告、日志、manifest 或 handoff。

## 2. 当前状态摘要

- QuantPF Paper Controller 的主要 `/api/quant` 路径与 OpenAPI 路径前缀基本一致。
- OpenAPI v29 多个 Paper 成功响应仍为泛型 `ApiResponse` 或 `data: {}`，不足以冻结前端 VO、分页和空态语义。
- Order submit 的实际 request 字段、同步返回语义与 OpenAPI 的 `instrumentCode`/`price`/202 描述不一致；源码另有 OpenAPI 未登记的 order cancel。
- Risk halt/resume 当前 DTO 为 `reason`，业务语义为 GLOBAL；OpenAPI 额外声明的 scope/targetId 尚未由 Controller 实现。
- 前端 `.env.development`、`.env.production` 存在 Vite client-visible RSA private-key 变量，`jsencrypt.ts` 通过 `import.meta.env` 读取；这是 P0 安全问题。
- 在已配置的本地 `quantpf` schema 中未发现 `sys_menu`；未查询其它 schema，实际 DB `component/perms` 仍为 `UNKNOWN`。
- 原型包含大量当前没有正式 OpenAPI/Controller 的页面和 API；不能因原型存在而封装假 API。

## 3. P0 安全门禁：客户端 RSA 私钥

### 证据

前端环境文件声明 Vite client-visible RSA public/private key 变量；`src/utils/jsencrypt.ts` 通过 `import.meta.env` 设置浏览器私钥；`request.ts` 在加密开关开启时参与加密/解密链路。报告只记录变量名和代码路径，不记录值。

### 判定

相关私钥及其对应密钥对必须按已暴露处理，执行轮换/撤销。不能只删除前端变量而不评估后端 `CryptoFilter`/`@ApiEncrypt` 兼容性，否则可能破坏认证或加密响应。

### 必须完成的安全处置

1. 安全负责人在受控密钥系统生成新密钥对并撤销旧密钥对；私钥不得进入前端仓库、构建产物、日志或模型上下文。
2. 书面确定普通 Paper API 使用 TLS-only，或设计不向浏览器下发长期私钥的临时会话密钥协议。
3. 协调后端加密链路，完成 client-side private-key 配置和读取路径清理。
4. 扫描源码、`.env`、dist/bundle、source map 和 CI artifact；扫描结果只保留脱敏摘要。
5. 在轮换/清理/扫描有人工证据前，禁止 production build/release。

状态：`P0 BLOCKED`。

## 4. Paper API/DTO/分页/错误码/幂等闭合矩阵

| 能力 | Controller 实际事实 | 当前契约风险 | 结论 |
|---|---|---|---|
| Account page | `GET /api/quant/account/page`，返回列表型 `R.data`，未形成明确 total/page envelope | OpenAPI 只有泛型响应 | 补 `PaperAccountVO` 与分页/列表语义后再实施 |
| Ledger | `GET /api/quant/account/{id}/ledger`，当前未形成明确 page/pageSize 语义 | 前端无法可靠翻页/显示 total | 明确列表或 `PageResult<LedgerVO>` |
| Order page | `GET /api/quant/order/page` 仅体现 pageNum/pageSize | OpenAPI 的 accountId/status 过滤未被 Controller 体现 | 改实现或裁剪契约，不能让前端猜测 |
| Order submit | `POST /api/quant/order/paper`；必需 `Idempotency-Key`；实际 DTO 含 accountId/instrumentId/side/orderType/quantity/limitPrice/estimatedNotional/fillPrice/clientOrderId/requestId | OpenAPI 字段和 202 与实际同步 `R.ok` 不一致 | 需产品/后端审批后冻结 |
| Order cancel | 源码存在 `POST /api/quant/order/{id}/cancel?reason=` | OpenAPI 未登记 | 补 OpenAPI、权限、响应和错误码后才允许前端调用 |
| Risk status | `GET /api/quant/risk/status` | `data` 未具体建模 | 补 `RiskStatusVO` |
| Risk halt/resume | `/api/quant/risk/halt`、`/resume`；请求当前为 reason，语义为 GLOBAL | OpenAPI scope/targetId 与实际不一致 | 冻结 global-only 语义、权限和二次确认 |
| Audit | `GET /api/quant/audit/page`；支持 requestId/aggregateType/aggregateId 及 snake_case 兼容过滤 | DTO、分页、脱敏未冻结 | 可作为只读候选，先补 schema |
| Job execution | `GET /api/quant/job/execution/page`；只读执行审计 | DTO、分页未冻结 | 只能是审计查看，不是调度管理 |
| Follow start/stop | `POST /api/quant/paper/follow-start`、`/follow-stop`；需要幂等键 | 没有 GET/page session API；menu fact 未闭合 | 完整 Follow 页面延期 |
| Position/Execution | 当前无正式查询 Controller/OpenAPI | 原型页面不可消费 | 明确延期，禁止造 API |

### 错误码、HTTP 状态与幂等

- `repeatSubmit` 的 500ms 防抖不是业务幂等，不能替代 `Idempotency-Key`。
- Order submit、Follow start、Follow stop 均需唯一且长度 8–128 的 `Idempotency-Key`；同一业务重试必须按审批后的规则处理，不能无规则重放。
- 前端需要正式的 `QPF-*` code→message/action 映射。
- 当前 `R.fail(code,msg)` 主要把业务码写入 body；HTTP transport status 与 OpenAPI 400/409/404 声明未闭合，必须先冻结。

## 5. Controller 与 OpenAPI 对照

基本一致的主要路径：`/api/quant/health`、`/account/page`、`/account/{id}/ledger`、`/order/page`、`/order/paper`、`/paper/follow-start`、`/paper/follow-stop`、`/risk/status`、`/risk/halt`、`/risk/resume`、`/job/execution/page`、`/audit/page`。

必须修正或批准的差异：

1. Order cancel 只有源码路由，OpenAPI 缺失。
2. Order submit 字段名、同步/异步响应语义与 OpenAPI 不一致。
3. 多个 Paper query response 的 `data` schema 为空或过泛。
4. Order page 查询过滤项与 Controller 实际接收参数不一致。
5. Risk scope/targetId 在 OpenAPI 中存在，但当前请求和业务语义是 GLOBAL。
6. OpenAPI HTTP error response 与实际 body-code 传输模型未闭合。

## 6. 菜单/权限事实

- `quantpf-menu-facts.yaml` 当前包含 Dashboard、Research、Market、Strategy、Backtest、Paper Account、Paper Order、Risk、Audit 等事实源条目。
- Position、Execution、Follow 管理不是当前已冻结的菜单事实。
- 已配置的本地 `quantpf` schema 查询不到 `sys_menu`；没有查询其他 schema。因此数据库中的 `component`、`perms` 必须标为 `UNKNOWN`。
- 前端动态路由依赖后端菜单返回，`permission.ts` 根据 component 字符串解析 views glob。不要通过静态路由绕过权限链；实施时必须建立 component→Vue 文件对照表。

## 7. 下一阶段页面裁剪

### 仅在安全和契约门关闭后可候选

1. Paper Account + Ledger；
2. Paper Order list/submit；撤单必须待正式契约；
3. Risk status；halt/resume 仅在高风险写权限、二次确认和审计要求获批后；
4. Audit read-only；Job execution 仅可作为只读执行审计附加页。

### 明确延期

Position、Execution、完整 Follow management、SnailJob/XXL-JOB 管理、QMT、CTP、Broker、LIVE，以及原型中尚未形成正式 OpenAPI 的 research/market/strategy/backtest/data 页面。

## 8. 下一阶段实施白名单 v2

当前本轮只有 docs 写权限。候选但未授权的后续前端文件见 `phase1c-frontend-contract-closure-file-whitelist-v2.json`。`.env.*`、`jsencrypt.ts`、`request.ts`、router、permission store、Position/Execution/Follow、调度、QMT/CTP/LIVE 均不在默认白名单内。

## 9. 验收标准

- [ ] 旧客户端 RSA key 已轮换/撤销，且证据不含密钥值。
- [ ] 浏览器 bundle/source map/CI artifact 无私钥模式命中。
- [ ] 前端不再读取长期私钥；后端加密兼容策略有书面批准。
- [ ] 每个准入 API 有具体 request/response VO、分页/列表语义、枚举、时间字段、权限、幂等和示例。
- [ ] Controller 与 OpenAPI 逐条对照通过。
- [ ] `sys_menu.component/perms` 已从正确 schema 取得只读证据并与事实源一致。
- [ ] QPF 错误码映射、HTTP status/body.code 语义通过测试。
- [ ] 仅使用人工批准的白名单，不添加 LIVE/QMT/CTP/Broker 入口。
- [ ] 安全门关闭后才执行 lint/build/test。

## 10. 阶段判定

```text
PHASE1C-FRONTEND-CONTRACT-CLOSURE-AND-SECURITY-REMEDIATION: PARTIAL
PHASE1C-FRONTEND-IMPLEMENTATION: NOT ALLOWED
PHASE1C-PRODUCTION-BUILD-AND-RELEASE: BLOCKED
NEXT ALLOWED TARGET: PHASE1C-BACKEND-CONTRACT-AND-SECURITY-REMEDIATION-APPROVAL
```

## 11. 审计定位哈希

- `plus-ui/package.json`: `D1376599B7C4736C081EC9BEAA7354E236C1D4DE371C8B16ECB0E6F13E871BE1`
- `plus-ui/src/utils/request.ts`: `56B2C78E5B6B776C807C8D9EF76C97A75AE5426BA3E87E4712819D07CCF59436`
- `plus-ui/src/utils/jsencrypt.ts`: `F0B75259E57F887E3DE6E43F284280E59F465E67E5A0F83237D31DFCF41FED6B`
- `contract/openapi/quantpf-v29.yaml`: `E3941D8395916A8050F6714E8E6EC1C4BA9966169DC4A4610A88369DA8B93319`
- `contract/error-codes/quantpf-error-codes.yaml`: `9CF9E504226C01BBB580EF53FC8A38C3BC27FABA210FCC48EB489592B59EDAE8`
- `contract/menu/quantpf-menu-facts.yaml`: `C8FFDB712DC326B866D97F14C13A339386AA0803677BC772404393C0EAA8469E`
