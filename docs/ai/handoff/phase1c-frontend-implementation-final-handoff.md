# QuantPF Phase 1C Frontend Implementation Final Handoff

- 日期：2026-10-08
- 当前门禁：`PHASE1C-SECURITY-EVIDENCE-AND-RUNTIME-CONTRACT-GATE-CLOSURE: PARTIAL`
- 前端实现：`NOT ALLOWED`
- 唯一主报告：`docs/ai/phase0/phase1c-security-runtime-gate-closure.md`

## 1. 已确认

OpenAPI 23 paths、10 deferred paths、Paper response schema、Order cancel、实际订单字段、GLOBAL risk 语义和 QPF error codes 已完成静态核验。

## 2. 未关闭

- 客户端 RSA private-key 轮换/撤销和兼容迁移证据缺失；
- gitleaks/trufflehog 不可用，完整 secrets/Git/source-map/CI 扫描未完成；
- 正确 RuoYi 主 schema 的 sys_menu.component/perms 未核验；
- 生产网关 /api/quant rewrite 未核验；
- 12 个 Paper endpoint Spring/API runtime regression 未完成；
- package.json 没有 test:unit，不能执行该命令。

## 3. 下一步唯一目标

`PHASE1C-SECURITY-EVIDENCE-AND-RUNTIME-GATE-REMEDIATION`

先补安全、主库菜单、网关和运行时证据；再由 GLM-5.3 独立审阅。全部通过后，才可申请第一批前端实现。

## 4. 第一批候选

Account/Ledger、Order list/submit、Risk status、Audit read-only。

## 5. 明确延期

Position、Execution、完整 Follow 管理、SnailJob、XXL-JOB、QMT、CTP、LIVE、Broker 和 10 个 DEFERRED OpenAPI 路径。

## 6. 放行命令

安全门关闭后，仅允许：

```powershell
pnpm lint
pnpm build:dev
pnpm build:prod
```

不得执行 `pnpm test:unit`，不得自行修改 package.json/pnpm-lock.yaml。

## 7. 最终状态

```text
PHASE1C-SECURITY-EVIDENCE-AND-RUNTIME-CONTRACT-GATE-CLOSURE: PARTIAL
PHASE1C-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```
