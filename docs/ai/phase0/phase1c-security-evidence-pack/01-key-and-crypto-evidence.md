# RSA 轮换/撤销与后端加密兼容证据

## 用户提供声明

- 旧密钥：已撤销，编号 `SEC-20261009-001`；
- 新密钥：Vault v3；
- 登录：仍使用 `@ApiEncrypt`；
- Paper API：TLS-only；
- 安全审批：`SEC-APPROVAL-001`。

## 待补脱敏核验

- Vault 审计摘要：旧版本 REVOKED/DISABLED，新版本 ACTIVE；
- 服务端当前引用版本为 v3；
- 旧密钥不再用于服务端解密；
- 前端长期私钥从 env、类型声明、jsencrypt 读取路径移除；
- `/auth/login` 和 Paper API 兼容测试编号；
- 回滚不恢复旧密钥、不恢复浏览器长期私钥；
- 安全负责人签核摘要。

禁止填写任何密钥值、Secret Value、密码或 Token。

## 当前判定

```text
KEY_ROTATION: USER_DECLARED_PENDING_VERIFICATION
KEY_REVOCATION: USER_DECLARED_PENDING_VERIFICATION
BACKEND_CRYPTO_COMPATIBILITY: NOT_RUN
SECURITY_GATE: NOT_CLOSED
```
