# QuantPF Phase 1C Frontend Implementation Handoff

- 日期：2026-10-07
- 当前阶段结果：`PHASE1C-BACKEND-CONTRACT-AND-SECURITY-REMEDIATION-IMPLEMENTATION: PARTIAL`
- 前端放行：`PHASE1C-FRONTEND-IMPLEMENTATION: NOT ALLOWED`
- 唯一主报告：`docs/ai/phase0/phase1c-backend-contract-security-remediation.md`

## 1. 已完成

- Paper 12 个已实现端点的 OpenAPI response VO/list schema 已补齐；
- order cancel 已登记；
- Order submit 实际字段与 HTTP 200/body-code 语义已记录；
- Risk halt/resume 已冻结为 GLOBAL + reason；
- PageSize 已对齐实际服务上限 100；
- 10 个未实现 OpenAPI path 已标记 DEFERRED；
- QPF-ORDER-002、QPF-ORDER-003、QPF-RISK-003 已登记。

## 2. 未关闭阻塞

- 客户端 RSA 私钥仍在前端配置/读取路径；
- 尚无轮换/撤销、后端加密兼容和脱敏历史扫描证据；
- `sys_menu.component/perms` 正确 RuoYi 主 schema 未核验；
- 生产网关 `/api/quant` 路由未知；
- OpenAPI 运行时回归未执行；
- package.json 没有 `test:unit`，不能执行或自行创建该命令。

## 3. 下一步唯一动作

先完成安全证据、主库菜单证据、网关证据和 Spring/API 回归；然后执行 GLM-5.3 独立审阅。只有无未关闭 P0/P1 后，才可申请前端第一批实现白名单。

## 4. 第一批允许候选

仅限：Paper Account + Ledger、Paper Order list/submit、Risk status、Audit read-only。撤单 UI 需继续确认权限/幂等；Position、Execution、完整 Follow 管理、调度、QMT、CTP、LIVE 均延期。

## 5. 后续验收命令

安全门关闭后：

```powershell
pnpm lint
pnpm build:dev
pnpm build:prod
```

不执行：`pnpm test:unit`，因为脚本不存在。

## 6. 最终判定

```text
PHASE1C-BACKEND-CONTRACT-AND-SECURITY-REMEDIATION-IMPLEMENTATION: PARTIAL
PHASE1C-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```
