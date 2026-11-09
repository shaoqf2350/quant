# QuantPF Phase 1C Backend Contract and Security Remediation Implementation

- 日期：2026-10-07
- 阶段：`PHASE1C-BACKEND-CONTRACT-AND-SECURITY-REMEDIATION-IMPLEMENTATION`
- 结果：`PARTIAL`
- 前端实现：`NOT ALLOWED`
- Production build/release：`BLOCKED`

## 1. 执行范围

已读取 Phase 1C handoff、closure 报告、最终复核附录、风险登记、whitelist v3、DeepSeek 盘点、GLM 独立审查、V29、OpenAPI、错误码、菜单事实源、前端加密/请求/错误码文件以及 QuantPF Controller、DTO、Application Service 和相关测试。

本轮仅修改批准范围内的 `contract/openapi/quantpf-v29.yaml`、`contract/error-codes/quantpf-error-codes.yaml`，并写入 `docs/ai/phase0/` 与 `docs/ai/handoff/` 交接文件。未修改 Java、XML、Vue、TS、POM、package.json、pnpm-lock.yaml、vite.config.ts、router、permission store、.env、数据库或调度配置。

## 2. 已实施契约修正

### OpenAPI

- Account、Ledger、Order、Risk、Audit、Job Execution 增加具体 VO/list response schema；
- 明确当前 Controller 返回 `R.data` 裸数组，没有 total/rows 分页 envelope；
- Ledger 移除实际不存在的 page/pageSize 参数；
- Order page 移除 Controller 未实现的 accountId/status 过滤参数；
- 登记实际存在的 `POST /api/quant/order/{id}/cancel`；
- Order submit 改为实际 `accountId/instrumentId/side/orderType/quantity/limitPrice/estimatedNotional/fillPrice/clientOrderId/requestId`；
- Order submit 成功传输语义改为 HTTP 200 + body.code/data；
- Risk halt/resume 改为 `RiskReasonRequest`，明确 GLOBAL 语义；
- Follow、Risk、Order、Audit、Job execution 标记 body-code-over-HTTP-200 约定；
- PageSize 上限与实际服务限制对齐为 100；
- 10 个没有后端 Controller 的路径增加 `x-quantpf-implementation-status: DEFERRED`。

### 错误码

已补齐：

- `QPF-ORDER-002`：成交数量超过剩余数量；
- `QPF-ORDER-003`：终态订单不可撤销；
- `QPF-RISK-003`：市场已关闭；
- 顶层增加 `transport: BODY_CODE_OVER_HTTP_200`。

## 3. 安全审查结果

当前仍可确认：

- `.env.development` 和 `.env.production` 仍声明 Vite RSA private-key 变量；
- `src/utils/jsencrypt.ts` 仍存在浏览器 `setPrivateKey` 路径；
- `request.ts` 仍在加密响应路径调用 `decrypt`；
- 后端 `AuthController.login` 使用 `@ApiEncrypt`，因此不能单独删除前端私钥而不设计后端兼容方案。

本轮没有修改这些文件，因为尚未取得密钥轮换/撤销证据，也没有获批的 TLS-only 或临时会话密钥协议。安全 P0 保持未关闭。

当前环境的脱敏扫描结果：

- 前端源码和 `.env` 私钥标记/变量路径：发现风险路径；
- `dist`、`build`、`node_modules`：当前不存在；
- `gitleaks`、`trufflehog`：当前未安装；
- Git 历史扫描：本轮未形成可复现完成证据，状态保持 `NOT_COMPLETED`；
- 没有输出任何敏感值。

## 4. 未取得的外部事实

- 正确 RuoYi 主 schema 的 `sys_menu.component/perms`：`UNKNOWN`。`quantpf` 业务 schema 缺少 `sys_menu` 不能推断全库事实；
- 生产网关 `/api/quant` 的追加/剥离规则：`UNKNOWN`；
- 密钥轮换/撤销及安全负责人签核：未提供；
- package.json 没有 `test:unit`，未修改 package.json/pnpm-lock.yaml；
- 未执行正式 YAML parser、pnpm lint/build 或 Maven build。

## 5. 10 个延期路径

1. `/api/quant/dashboard/summary`
2. `/api/quant/instrument/page`
3. `/api/quant/market/kline`
4. `/api/quant/news/page`
5. `/api/quant/finance/statements`
6. `/api/quant/pool/page`
7. `/api/quant/strategy/page`
8. `/api/quant/signal/page`
9. `/api/quant/backtest`
10. `/api/quant/backtest/{id}`

以上路径不得生成当前前端 API wrapper。

## 6. 静态验收

- OpenAPI 路径数量：23（原 22 + 实际撤单路径 1）；
- 延期路径标记：10；
- 新增 response/schema 引用均能在同一 OpenAPI 文件中找到定义；
- 错误码无重复 code；
- OpenAPI/error-code 文件无制表符、无 literal backtick 换行残留、无敏感值；
- 未执行正式 YAML parser、Maven、pnpm 或 Spring/MySQL 运行时回归。

## 7. 阶段判定

```text
PHASE1C-BACKEND-CONTRACT-AND-SECURITY-REMEDIATION-IMPLEMENTATION: PARTIAL
PHASE1C-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```

契约最小修正已完成，但客户端私钥风险、密钥轮换/撤销、后端加密兼容、正确 sys_menu 事实、生产网关证据、完整 secrets scan 和运行时回归仍未闭合。

## 8. 下一步放行条件

只有以下事项全部有证据，才允许进入前端第一批实现：

1. 客户端密钥已轮换/撤销；
2. 前端长期私钥路径已移除或替换为获批协议；
3. 后端 `@ApiEncrypt`/CryptoFilter 兼容验收通过；
4. secrets scanner 及 Git 历史扫描完成并脱敏；
5. 正确 RuoYi 主 schema 的 `sys_menu.component/perms` 核验通过；
6. 生产网关 `/api/quant` 路由规则确认；
7. whitelist candidate 获人工批准；
8. GLM-5.3 独立复核无未关闭 P0/P1。
