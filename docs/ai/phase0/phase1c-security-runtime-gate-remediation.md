# QuantPF Phase 1C Security Runtime Gate Remediation

- 日期：2026-10-08
- 阶段：`PHASE1C-SECURITY-EVIDENCE-AND-RUNTIME-GATE-REMEDIATION`
- 结果：`PARTIAL`
- 前端实现：`NOT ALLOWED`
- Production build/release：`BLOCKED`

## 1. 本轮范围

已首先读取本轮指定的 handoff、closure、风险、manifest、commands、whitelist、DeepSeek 盘点、GLM 独立审查、V29、OpenAPI、错误码、菜单事实源、前端请求/加密文件以及 QuantPF Controller、DTO、Application Service、Mapper、Repository 和测试。

本轮没有修改 `.env`、`jsencrypt.ts`、`request.ts`、package.json、pnpm-lock.yaml、vite.config.ts、router、permission store、Java、POM、数据库或业务代码。仅写入 `docs/ai/phase0/` 和 `docs/ai/handoff/`。

## 2. 已确认事实

### 安全

- 前端 `.env.development`、`.env.production`、`src/types/env.d.ts` 和 `src/utils/jsencrypt.ts` 仍存在 RSA private-key 变量/代码路径；没有读取或记录值。
- 后端 `AuthController` 仍存在 `@ApiEncrypt`，不能单独删掉前端私钥而不先完成兼容迁移。
- `dist`、`build`、`node_modules` 当前不存在。
- gitleaks/trufflehog 当前未安装。
- 有限 Git 历史扫描覆盖 20 个触及敏感路径的提交：敏感变量路径反复存在；未发现 PEM private-key marker。该结果不是完整 secrets scan PASS。
- 完整 Git 历史、source map、CI artifact 扫描和安全负责人轮换/撤销签核仍缺失。

### 菜单数据库

- 没有取得用户明确批准的 RuoYi 主 schema 只读连接证据。
- `quantpf-menu-facts.yaml` 仅有 `permission` 字段，没有 `sys_menu.component/perms/parentId` 数据库事实。
- 因此 `sys_menu.component/perms`：`UNKNOWN`。
- 本轮没有对任何数据库执行写入、DDL、迁移或全库推断。

### 网关

- 本地仓库仅发现示例 Nginx 配置：`script/docker/nginx/conf/nginx.conf` 代理 `/prod-api/`，未发现 `/api/quant` 的生产网关路由。
- 该示例配置不能证明实际生产网关行为。
- 因此生产 gateway `/api/quant` rewrite：`UNKNOWN`。

### 前端测试/构建

- `package.json` scripts 没有 `test:unit`；没有发现前端测试 runner/config/spec 文件。
- 在私钥安全门未关闭前，不运行 `pnpm lint`、`pnpm build:dev` 或 `pnpm build:prod`，避免生成潜在敏感构建产物。

## 3. 静态契约验收

- OpenAPI 路径：23；
- `DEFERRED` 路径：10；
- schema 引用缺失：0；
- response 引用缺失：0；
- QPF error code：19；重复：0；
- OpenAPI tabs：0；
- 12 个 Paper runtime endpoint 的完整 Spring/API 回归：`NOT_RUN`。

## 4. Runtime gate 状态

以下 12 个 API 尚未完成本阶段要求的完整运行时回归：

1. Account page
2. Account ledger
3. Order page
4. Order submit
5. Order cancel
6. Follow start
7. Follow stop
8. Risk status
9. Risk halt
10. Risk resume
11. Audit page
12. Job execution page

未能核验：HTTP status、body.code/msg/data、DTO 字段、空列表、幂等、数据库事实、Repository 路径、内存 fallback、重复扣款/重复冻结/重复持仓。

## 5. 阶段结论

```text
PHASE1C-SECURITY-EVIDENCE-AND-RUNTIME-GATE-REMEDIATION: PARTIAL
PHASE1C-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```

主要阻塞：安全证据、主库菜单事实、生产网关事实、运行时回归尚未闭合。用户批准方案不能替代这些证据。

## 6. 下一步关闭条件

1. 提供不含敏感值的密钥轮换/撤销、后端兼容、回滚和安全签核证据；
2. 使用组织批准工具完成源码、env、dist/build、source map、CI artifact、Git 历史 secrets scan；
3. 在正确 RuoYi 主 schema 只读核验 `component/perms/parentId/path`；
4. 获取生产网关 `/api/quant` rewrite 证据；
5. 安全门关闭后完成 12 个 endpoint runtime regression；
6. 验证正式 Repository、无内存 fallback、无重复资金/持仓写入；
7. 由 GLM-5.3 独立复核；
8. 最终由 GPT-5.6 Sol 决定 whitelist 是否放行。
