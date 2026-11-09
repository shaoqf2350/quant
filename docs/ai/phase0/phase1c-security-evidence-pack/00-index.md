# QuantPF Phase 1C 脱敏安全证据包

日期：2026-10-09
状态：`PARTIAL / REQUIRES RECONCILIATION`

## 用户提供声明

- 旧 RSA 密钥已撤销：`SEC-20261009-001`；
- 新密钥由 Vault 管理，版本 `v3`：`SEC-20261009-001`；
- 前端长期私钥已移除：`SCAN-20261009-001`；
- 登录仍使用 `@ApiEncrypt`，Paper API 使用 TLS-only：`SEC-APPROVAL-001`；
- 安全签核：`SEC-APPROVAL-001`；
- 正式扫描报告：`SCAN-001`；
- 本地 disposable schema INSERT/DELETE 已批准：`SEC-APPROVAL-001`；
- 生产网关证据：尚无。

## 本机核验冲突

当前工作区仍命中 `.env.development`、`.env.production`、`src/types/env.d.ts`、`src/utils/jsencrypt.ts` 的私钥 marker/读取路径。因此“前端长期私钥已移除”尚不能作为本机 PASS，需要先对账或完成实际清理。

## 当前判定

```text
RSA_SECURITY_GATE: PARTIAL
SECRETS_SCAN: PARTIAL
GATEWAY_ROUTE: UNKNOWN
FRONTEND_IMPLEMENTATION: NOT ALLOWED
```

本包不含密码、Token、API Key、RSA 私钥、公钥全文或 Vault Secret Value。
