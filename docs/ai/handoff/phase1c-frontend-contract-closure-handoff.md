# QuantPF Phase 1C Frontend Contract Closure Handoff

- 日期：2026-10-07
- 唯一交接入口：`docs/ai/phase0/phase1c-frontend-contract-closure.md`
- 阶段结果：`PARTIAL`
- 前端实现：`NOT ALLOWED`

## 1. 当前目标
关闭客户端私钥风险，闭合 Paper 前端 API/DTO/分页/错误码/幂等/菜单契约，并冻结下一阶段前端实施白名单。

## 2. 当前仓库事实
前端：`D:\workspace\quant\plus-ui`；后端：`D:\workspace\quant\RuoYi-Vue-Plus`；原型：`D:\我的文档\私人\量化\前端功能原型设计`。本轮没有源码、环境文件、契约或数据库修改。

## 3. 当前 P0/P1 阻塞
- 浏览器可见 RSA private-key 配置/读取路径，必须轮换/撤销、清理并扫描。
- OpenAPI Paper query response DTO、分页、错误传输语义不完整。
- Order submit/cancel、Risk global halt/resume、Follow session read contract 尚未冻结。
- `quantpf` schema 中没有 `sys_menu`，DB component/perms 事实未知。
- 前端尚无正式业务 Idempotency-Key helper 和 QPF 错误码映射。

## 4. 允许的下一批页面（门禁关闭后）
Paper Account + Ledger、Paper Order list/submit、Risk status（写操作需高风险权限与二次确认）、Audit read-only。Job execution 若纳入只能是执行审计查看。

## 5. 必须延期
Position、Execution、完整 Follow management、SnailJob/XXL-JOB 管理、QMT、CTP、Broker、LIVE，以及未进入正式 OpenAPI 的原型页面。

## 6. 下一目标
`PHASE1C-BACKEND-CONTRACT-AND-SECURITY-REMEDIATION-APPROVAL`

下一位模型必须先完成：
1. 取得不含密钥值的轮换/撤销和 bundle/source-map/CI 扫描证据；
2. 明确 TLS-only 或不向浏览器下发长期私钥的临时会话密钥方案，并与后端 CryptoFilter/ApiEncrypt 协调；
3. 补齐准入 API 的具体 response VO、分页/列表、错误码、幂等和示例；
4. 决定 order submit/cancel、risk global halt/resume、Follow session read API；
5. 在正确、获批的 RuoYi schema 上只读核验 `sys_menu.component/perms`；
6. 由人工批准 `phase1c-frontend-contract-closure-file-whitelist-v2.json`。

## 7. 禁止事项
安全/契约未关闭前不得修改 Vue/API wrapper、`.env`、`jsencrypt.ts`、`request.ts`、router 或 permission store；不得 production build/release；不得创建 Position/Execution/Follow 假 API；不得接入 LIVE/QMT/CTP/Broker；不得执行数据库迁移。

## 8. 交接文件
- `docs/ai/phase0/phase1c-frontend-contract-closure.md`
- `docs/ai/phase0/phase1c-frontend-contract-closure-risk-register.md`
- `docs/ai/phase0/phase1c-frontend-contract-closure-file-whitelist-v2.json`
- `docs/ai/phase0/phase1c-frontend-contract-closure-manifest.json`
- `docs/ai/phase0/phase1c-frontend-contract-closure-commands.log`

## 9. 后续验收命令（仅门禁关闭后）
```powershell
pnpm lint
pnpm build:prod
pnpm test:unit
```
另需执行组织批准的 secrets scanner，扫描源码、`.env`、dist/bundle、source map 和 CI artifact；日志只保留脱敏摘要。

## 10. 最终判定
```text
PHASE1C-FRONTEND-CONTRACT-CLOSURE-AND-SECURITY-REMEDIATION: PARTIAL
PHASE1C-FRONTEND-IMPLEMENTATION: NOT ALLOWED
PHASE1C-PRODUCTION-BUILD-AND-RELEASE: BLOCKED
```

## Final review correction

请以 `phase1c-frontend-contract-closure-final-review-addendum.md` 和 `phase1c-frontend-contract-closure-file-whitelist-v3.json` 为本阶段最新约束。此前 handoff 中的 `pnpm test:unit` 不可执行，因 package.json 无该 script；不得自行修改 package.json/pnpm-lock.yaml。十个没有后端 Controller 的 OpenAPI path 必须显式延期/移除/排期。生产网关 `/api/quant`、正确 RuoYi schema 的 `sys_menu`、安全扫描工具和测试框架决定均为下一阶段门禁。

