# QuantPF Phase 1C Security Runtime Gate Evidence Completion

- 日期：2026-10-08
- 阶段：`PHASE1C-SECURITY-RUNTIME-GATE-EVIDENCE-COMPLETION`
- 结果：`PARTIAL`
- 前端实现：`NOT ALLOWED`
- 生产构建/发布：`BLOCKED`

## 1. 本轮执行范围

已读取本轮指定的 final-v2 handoff、security runtime remediation 报告/风险/manifest/commands/whitelist、OpenAPI、错误码、菜单事实源、前端 package/request/jsencrypt/errorCode、QuantPF Controller/DTO/Application Service/Mapper/Repository/测试及 V29 指定章节。

本轮只写 `docs/ai/phase0/` 和 `docs/ai/handoff/`。未修改 `.env`、前端加密/请求代码、package/lock、Java、POM、数据库、网关配置或业务代码；未执行数据库迁移、生产连接、前端 build 或 Git 写操作。

## 2. RSA 密钥安全证据

### 实际状态

- 当前安全负责人轮换/撤销签核：`MISSING`；
- 新密钥由受控密钥系统管理的证据：`MISSING`；
- 旧密钥不再用于服务端解密的证据：`MISSING`；
- 前端长期私钥路径已清理的证据：`MISSING`；
- 后端 CryptoFilter/@ApiEncrypt 兼容验收：`MISSING`；
- 回滚窗口/迁移边界证据：`MISSING`。

只读 marker scan 仍在 `.env.development`、`.env.production`、`src/types/env.d.ts`、`src/utils/jsencrypt.ts` 发现 private-key 变量/代码路径。没有读取、输出或记录任何密钥值。

因此：`RSA security gate: NOT CLOSED`。未删除/修改前端私钥配置或加密链路。

## 3. Secrets scan 状态

- 正式 scanner：`gitleaks`、`trufflehog` 均不可用；
- 前端源码及环境配置 marker scan：命中 4 个文件路径类别；只记录路径和数量；
- dist/build/node_modules：当前均不存在；
- Git 历史：此前有限扫描覆盖 20 个相关提交，发现变量路径反复存在；不构成完整 Git 历史 secrets scan；
- source map/CI artifact：未提供或未扫描；
- 总结：`PARTIAL`，不能标记通过。

## 4. RuoYi 主库菜单事实

没有提供用户批准的 RuoYi 主 schema 连接证据，本轮未连接数据库。`quantpf-menu-facts.yaml` 是静态契约，不是数据库事实。`sys_menu.component/perms/parentId/path`：`UNKNOWN`。

## 5. 网关事实

仓库内发现示例 Nginx `script/docker/nginx/conf/nginx.conf` 使用 `/prod-api/` location 并代理到后端根路径；这是仓库示例，不是生产环境配置，不能证明 `/api/quant` 的生产 rewrite。生产外部路径及追加/剥离行为：`UNKNOWN`。

## 6. 运行时回归

安全门未关闭，因此没有启动 Spring 服务、没有执行 12 个 endpoint runtime regression、没有执行 pnpm lint/build。当前 12 个 Paper API 的运行时 HTTP status/body/DTO/幂等/数据库事实均为 `NOT_RUN`。

本轮不能据静态代码或既有局部测试宣称：

- 12 个端点全部通过；
- 所有路径都走正式 Repository；
- 不存在内存 fallback；
- 不存在重复扣款、冻结或重复持仓。

这些项目必须由安全门关闭后的隔离 Spring/MySQL 运行时测试证明。

## 7. 前端测试/构建门禁

`package.json` 无 `test:unit`，也未发现测试 runner/config/spec 文件。不得运行该命令，不得修改 package.json/pnpm-lock.yaml。私钥路径未处置前不执行 `pnpm lint`、`pnpm build:dev`、`pnpm build:prod`。

## 8. 静态契约复核

- OpenAPI paths：23；
- `DEFERRED` paths：10；
- schema refs：15，缺失 0；
- response refs：11，缺失 0；
- error codes：19，重复 0。

该结果是静态引用检查，不等同于 YAML parser、Spring runtime 或外部网关验收。

## 9. 最终阶段判定

```text
PHASE1C-SECURITY-RUNTIME-GATE-EVIDENCE-COMPLETION: PARTIAL
PHASE1C-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```

当前阻塞是外部证据缺失，而非可由代码猜测或静态文件替代。第一批前端候选仍限于 Account/Ledger、Order list/submit、Risk status、Audit read-only；Position、Execution、完整 Follow 管理、调度、QMT/CTP/LIVE/Broker 和 10 个 DEFERRED path 继续延期。

## 10. 待提供的最小证据包

1. 安全负责人签核的密钥轮换/撤销和兼容迁移证明（不得含密钥值）；
2. 组织批准的完整 secrets scanner 结果摘要；
3. 获批 RuoYi 主 schema 的只读查询结果（component/perms/parentId/path）；
4. 生产网关路由配置或受控外部 HTTP 验证结果；
5. 安全门关闭后 12 API 的隔离运行时回归结果；
6. Repository/no-fallback/资金与持仓幂等的测试证据；
7. GLM-5.3 独立审查结果。
