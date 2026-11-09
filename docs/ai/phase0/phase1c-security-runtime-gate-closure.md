# QuantPF Phase 1C Security Evidence and Runtime Contract Gate Closure

- 日期：2026-10-08
- 阶段：`PHASE1C-SECURITY-EVIDENCE-AND-RUNTIME-CONTRACT-GATE-CLOSURE`
- 结果：`PARTIAL`
- 前端实现：`NOT ALLOWED`
- Production build/release：`BLOCKED`

## 1. 必读范围与执行边界

已读取上一阶段 handoff、契约修正报告/风险/manifest/commands、whitelist、DeepSeek 盘点、GLM 独立审查、OpenAPI、错误码、菜单事实源、前端 package/request/jsencrypt/errorCode、QuantPF Controller/DTO/Application Service/测试和 V29 指定章节。

本轮只写 `docs/ai/phase0/` 与 `docs/ai/handoff/`，未修改前端 `.env`、`jsencrypt.ts`、`request.ts`、package.json、pnpm-lock.yaml、router、permission store、Java、POM、数据库或菜单数据。

## 2. 已完成核验

- OpenAPI 当前 23 个路径：原有 22 个 + 实际 Controller 的 Order cancel；
- 10 个没有后端 Controller 的路径均标记 `DEFERRED`；
- OpenAPI schema 引用：15 个，缺失 0；response 引用：11 个，缺失 0；
- 错误码 19 个，无重复；
- package.json scripts 无 `test:unit`；未发现前端 test runner/config/spec 文件；
- dist、build、node_modules 当前均不存在；
- 前端源码/环境文件仍存在 RSA private-key 标记路径；没有记录任何值；
- 后端 `AuthController` 仍有 `@ApiEncrypt`，不能单独移除浏览器私钥而不先完成兼容迁移；
- `quantpf-menu-facts.yaml` 只有 `permission` 字段，没有 DB 的 component/perms 事实；
- Controller 内应用路径可以证明 `/api/quant`，不能证明生产网关 rewrite。

## 3. 安全证据结论

### P0-SEC-001 客户端 RSA private key

未提供轮换/撤销凭证、新密钥受控管理凭证、后端兼容迁移凭证或安全负责人签核。当前仍存在浏览器 `setPrivateKey` 与环境变量路径，故安全门未关闭。

### Secrets scan

- 当前安全标记扫描：源码/环境文件 4 个命中文件类别，结果只记录数量和路径类别；
- dist/build/node_modules：不存在；
- gitleaks/trufflehog：未安装；
- 有限 Git 历史路径扫描：扫描 20 个相关历史提交；发现 private-key 变量路径在历史中反复存在；未发现 PEM private-key marker；该结果不是 secrets scan PASS；
- Git 全历史完整扫描、CI artifact/source map 扫描：未完成。

## 4. sys_menu 与网关结论

- `sys_menu.component/perms`：`UNKNOWN`。本轮没有用户明确批准的 RuoYi 主 schema 只读连接证据；不能用 `quantpf` schema 缺表推断全库。
- 生产网关 `/api/quant` rewrite：`UNKNOWN`。没有网关配置或受控外部 HTTP 证据；不能由 Spring Controller 路径推断。

## 5. 运行时契约回归结论

本轮未执行 `pnpm lint`、`pnpm build:dev`、`pnpm build:prod`，原因是安全门未关闭且构建可能把私钥注入产物；未执行 `pnpm test:unit`，因为脚本不存在。

以下 12 个端点的完整 Spring/API runtime regression 仍待安全门关闭后执行：

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

运行时回归必须验证 HTTP status、body.code/msg/data、DTO 字段、空列表、错误码、Idempotency-Key、持久化路径和无内存 fallback。

## 6. 前端范围

第一批候选仅限 Account/Ledger、Order list/submit、Risk status、Audit read-only。Position、Execution、完整 Follow management、SnailJob、XXL-JOB、QMT、CTP、LIVE、Broker 和 10 个 DEFERRED 路径继续延期。

## 7. 阶段判定

```text
PHASE1C-SECURITY-EVIDENCE-AND-RUNTIME-CONTRACT-GATE-CLOSURE: PARTIAL
PHASE1C-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```

P0 安全证据、主库菜单事实、网关路由、完整 secrets scan 和运行时回归尚未闭合，不能放行前端。

## 8. 下一步关闭条件

1. 提供不含密钥值的轮换/撤销、后端兼容、回滚和安全签核证据；
2. 完成组织批准的 secrets scanner 与 Git 历史、source map、CI artifact 扫描；
3. 在正确 RuoYi 主 schema 只读核验 component/perms/parentId/path；
4. 获取生产网关 `/api/quant` rewrite 证据；
5. 在安全门关闭后完成 12 个端点 Spring/API 回归；
6. 由 GLM-5.3 独立复核；
7. 最终更新 whitelist 为 `APPROVED` 或保持 `NOT ALLOWED`。
