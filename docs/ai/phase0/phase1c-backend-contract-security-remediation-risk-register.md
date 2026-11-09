# QuantPF Phase 1C Backend Contract and Security Remediation Implementation

- 日期：2026-10-07
- 阶段：`PHASE1C-BACKEND-CONTRACT-AND-SECURITY-REMEDIATION-IMPLEMENTATION`
- 结果：`PARTIAL`
- 前端实现：`NOT ALLOWED`
- Production build/release：`BLOCKED`

## 1. 执行范围

已读取 Phase 1C handoff、closure 报告、最终复核附录、风险登记、whitelist v3、DeepSeek 盘点、GLM 独立审查、V29、OpenAPI、错误码、菜单事实源、前端加密/请求/错误码文件以及 QuantPF Controller、DTO、Application Service 和相关测试。

本轮允许的契约文件已实施最小修正；未修改 Java、XML、Vue、TS、POM、package.json、pnpm-lock.yaml、vite.config.ts、router、permission store、.env、数据库或调度配置。未执行数据库迁移、生产连接、LIVE/QMT/CTP/Broker 接入或前端页面实现。

## 2. 已实施修改

### 2.1 OpenAPI

修改：`contract/openapi/quantpf-v29.yaml`

已完成：

- Account、Ledger、Order、Risk、Audit、Job Execution 响应增加具体 VO/list response schema；
- 明确当前 Controller 返回的是 `R.data` 裸数组，没有 total/rows 分页 envelope；
- Ledger 契约移除实际不存在的 page/pageSize 参数；
- Order page 移除 Controller 未实现的 accountId/status 过滤参数；
- 登记实际存在的 `POST /api/quant/order/{id}/cancel`；
- Order submit 改为实际 `accountId/instrumentId/side/orderType/quantity/limitPrice/estimatedNotional/fillPrice/clientOrderId/requestId`；
- Order submit 成功传输语义改为 HTTP 200 + body.code/data；
- Risk halt/resume 改为 `RiskReasonRequest`，明确 GLOBAL 语义；
- Follow start/stop、Risk、Order、Audit、Job execution 标记 body-code-over-HTTP-200 传输约定；
- PageSize 上限与实际服务限制对齐为 100；
- OpenAPI 中 10 个没有后端 Controller 的 path 增加 `x-quantpf-implementation-status: DEFERRED`，不得由当前前端生成 wrapper。

### 2.2 错误码

修改：`contract/error-codes/quantpf-error-codes.yaml`

已补齐：

- `QPF-ORDER-002`：成交数量超过剩余数量；
- `QPF-ORDER-003`：终态订单不可撤销；
- `QPF-RISK-003`：市场已关闭；
- 顶层增加 `transport: BODY_CODE_OVER_HTTP_200`，表达当前 R.fail 的实际传输模型。

## 3. 安全审查结果

### 3.1 客户端 RSA 私钥

当前仍可确认：

- `.env.development` 和 `.env.production` 仍声明 Vite RSA private-key 变量；
- `src/utils/jsencrypt.ts` 仍存在浏览器 `setPrivateKey` 路径；
- `request.ts` 仍在加密响应路径调用 `decrypt`；
- 后端 `AuthController.login` 仍使用 `@ApiEncrypt`，因此不能单独删除前端私钥而不设计后端兼容方案。

本轮没有修改这些文件，因为尚未取得密钥轮换/撤销证据，也没有获批的 TLS-only 或临时会话密钥协议。安全 P0 保持未关闭。

### 3.2 当前脱敏扫描

- 源码和 `.env` 私钥变量/私钥标记扫描：发现暴露路径，结果为风险确认，不是 PASS；
- `dist`、`build`、`node_modules`：当前不存在；
- gitleaks/trufflehog：当前环境未安装；
- Git 历史扫描：本轮未形成可复现完成证据，不能宣称历史扫描通过；
- 扫描输出未记录任何敏感值。

## 4. 未取得的外部事实

- 正确 RuoYi 主 schema 的 `sys_menu.component/perms`：`UNKNOWN`。`quantpf` 业务 schema 缺少 `sys_menu` 不能推断全库事实；
- 生产网关 `/api/quant` 的追加/剥离规则：`UNKNOWN`。Controller 的应用内路径不能替代网关证据；
- 密钥轮换/撤销及安全负责人签核：未提供；
- 测试脚本：`package.json` 没有 `test:unit`，未修改 package.json/pnpm-lock.yaml。

## 5. 10 个延期路径

以下路径仍不能进入当前 Paper 前端客户端范围：

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

## 6. 静态验收

- OpenAPI 路径数量：23（原 22 + 实际撤单路径 1）；
- 延期路径标记：10；
- 新增响应/VO schema 引用均能在同一 OpenAPI 文件中找到定义；
- 错误码无重复 code；
- OpenAPI/error-code 文件无制表符和敏感值；
- 未执行正式 YAML parser、pnpm lint/build 或 Maven build。

## 7. 阶段判定

```text
PHASE1C-BACKEND-CONTRACT-AND-SECURITY-REMEDIATION-IMPLEMENTATION: PARTIAL
PHASE1C-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```

原因：契约修正已完成，但客户端私钥风险、密钥轮换/撤销、后端加密兼容、正确 sys_menu 事实、生产网关证据和完整 secrets scan 仍未闭合。

## 8. 下一步放行条件

只有以下事项全部有证据，才允许进入前端第一批实现：

1. 客户端密钥已轮换/撤销；
2. 前端长期私钥路径已移除或替换为获批协议；
3. 后端 `@ApiEncrypt`/CryptoFilter 兼容验收通过；
4. secrets scanner 及 Git 历史扫描完成并脱敏；
5. 正确 RuoYi 主 schema 的 `sys_menu.component/perms` 核验通过；
6. 生产网关 `/api/quant` 路由规则确认；
7. whitelist v3 获人工批准；
8. GLM-5.3 独立复核无未关闭 P0/P1。

'''@ | Set-Content -LiteralPath "$phase\phase1c-backend-contract-security-remediation.md" -Encoding UTF8
@'
# Phase 1C Backend Contract and Security Remediation Risk Register

- 日期：2026-10-07
- 结果：`PARTIAL`

| ID | 等级 | 风险 | 证据 | 影响 | 建议 | 是否阻塞 |
| -- | -- | -- | -- | -- | -- | -- |
| P1C-BSR-001 | P0 | 客户端 RSA 私钥仍在浏览器边界 | `.env.*` Vite private-key 变量；`jsencrypt.ts` setPrivateKey；request.ts decrypt | 密钥暴露，不能安全发布 | 轮换/撤销；TLS-only 或临时会话密钥；协调 AuthController/CryptoFilter；完成脱敏扫描 | 是 |
| P1C-BSR-002 | P0 | 安全协议兼容性未验收 | AuthController.login 仍有 `@ApiEncrypt` | 裸删前端 key 可能破坏登录/响应解密 | 先完成后端兼容方案和隔离登录验收 | 是 |
| P1C-BSR-003 | P1 | Git 历史/构建产物扫描证据不完整 | gitleaks/trufflehog 不存在；历史扫描未形成可复现 PASS；dist/build/node_modules 不存在 | 无法证明历史或未来 artifact 无敏感信息 | 安装组织批准 scanner 或提供等效工具；记录脱敏摘要和阈值 | 是 |
| P1C-BSR-004 | P0 | 正确 RuoYi 主库 sys_menu 事实未知 | quantpf schema 缺少 sys_menu；未取得主 schema 证据 | component/perms 错配，动态路由空白/按钮错误 | 只读核验正确主 schema，禁止以 YAML 冒充 DB 事实 | 是 |
| P1C-BSR-005 | P1 | 生产网关 /api/quant 路由未知 | 仅有应用内 context-path `/` 和 Controller 映射 | 部署后路径 404 或重复前缀 | 获取网关配置/路由测试证据 | 是 |
| P1C-BSR-006 | P0 | Paper response/transport 契约未经过运行时回归 | OpenAPI 已更新；Java Controller 未修改；未运行 Maven/pnpm | 契约和实际序列化仍需集成验证 | 安全门关闭后执行受控 Spring/MySQL API 回归 | 是 |
| P1C-BSR-007 | P1 | 10 个未来 OpenAPI path 没有后端实现 | 已加 DEFERRED extension，但 path 仍保留在 V29 文件 | 代码生成器可能仍生成不可用客户端 | 当前前端 whitelist 禁止这些 path；后续决定移除或拆分 current-client contract | 是 |
| P1C-BSR-008 | P1 | 无前端测试脚本 | package.json 仅有 lint/build/dev/fmt/preview，无 test:unit | 无法执行自动化 UI/API 单测 | 单独审批测试框架，不得隐式改 package.json | 否 |
| P1C-BSR-009 | P1 | QPF error code 与 Controller 文案仍有动态拼接 | Controller 返回 code:message 拼接；错误码文件为静态事实源 | 前端 mapping 可能不稳定 | 后续统一错误响应 DTO 或冻结解析规则 | 否 |
| P1C-BSR-010 | P2 | audit/job 双写参数别名未统一 | Controller 支持 camelCase 和 snake_case；OpenAPI 当前只冻结 camelCase | 客户端兼容性和审计查询差异 | 后续决定保留 camelCase，snake_case 仅兼容并记录 deprecated | 否 |

## 当前结论

契约文件已完成最小闭合修正，但 P0 安全、菜单 DB、网关、运行时回归和完整扫描证据未关闭。因此前端实现继续 `NOT ALLOWED`。
