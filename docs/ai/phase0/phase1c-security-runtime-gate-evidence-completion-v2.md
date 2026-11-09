# QuantPF Phase 1C Security Runtime Gate Evidence Completion V2

- 日期：2026-10-09
- 阶段：`PHASE1C-SECURITY-RUNTIME-GATE-EVIDENCE-COMPLETION-V2`
- 结果：`PARTIAL`
- 前端实现：`NOT ALLOWED`
- Production build/release：`BLOCKED`

## 1. 本轮执行范围

已读取本阶段指定 handoff、报告、风险、manifest、commands、whitelist、OpenAPI、错误码、菜单事实源、前端 package/request/jsencrypt/errorCode、QuantPF Controller/DTO/Application Service/Mapper/Repository/配置/测试及 V29 指定章节。

用户批准对本地 MySQL schema `quantpf` 执行查询。本轮仅执行 SELECT/只读事务；没有执行 INSERT、UPDATE、DELETE、DDL、Flyway、生产连接或前端构建。

## 2. quantpf 数据库只读事实

只读查询目标：本地 `quantpf`，MySQL 8.0.20。

结果：

- `sys_*` 表数量：20；
- `sys_menu`：存在；
- `sys_menu` 行数：109；
- `sys_menu` 字段包含 `component`、`perms`、`parent_id`、`path`；
- QuantPF 菜单筛选结果：0 行；
- Position、Execution、Follow、LIVE、QMT、CTP、Broker 相关 QuantPF 菜单筛选结果：0 行；
- QuantPF 业务表仍存在，包括 account/order/execution/audit/job/follow 等 `quantpf_*` 表。

因此本轮修正此前结论：

```text
quantpf.sys_menu table: PRESENT
quantpf.sys_menu schema columns: VERIFIED
QuantPF menu rows in sys_menu: ABSENT
QuantPF component/perms facts: NOT_CLOSED
```

该 schema 事实不等于 QuantPF 菜单已经落库。`quantpf-menu-facts.yaml` 与数据库菜单内容尚未完成对账闭合。

## 3. RSA 私钥和安全证据

以下证据仍缺失：

- 旧 RSA 密钥轮换/撤销签核；
- 新密钥受控管理证明；
- 旧密钥不再用于服务端解密；
- 前端长期私钥路径清理；
- 后端 CryptoFilter/@ApiEncrypt 兼容迁移；
- 回滚窗口和安全负责人签核。

前端 marker scan 仍命中 `.env.development`、`.env.production`、`src/types/env.d.ts`、`src/utils/jsencrypt.ts` 四个路径类别。没有读取或记录任何密钥值。

```text
RSA_SECURITY_GATE: NOT_CLOSED
```

## 4. Secrets scan

- gitleaks：不可用；
- trufflehog：不可用；
- Git 历史 marker scan：扫描 43 个触及敏感路径的提交，41 个提交命中变量/加密路径 marker，未发现 PEM 私钥头 marker；
- dist/build/node_modules：不存在；
- source map/CI artifact：未提供、未扫描；
- 正式 secrets scan：`PARTIAL`，不能标记 PASS。

## 5. 网关事实

仓库仅有示例 Nginx `/prod-api/` 代理配置，不是生产网关证据。生产 `/api/quant` 的外部路径、rewrite、追加/剥离规则仍为：

```text
GATEWAY_ROUTE: UNKNOWN
```

## 6. 12 个 Paper API 运行时回归

本轮没有执行 12 个 endpoint 的 Spring/API 回归。原因：当前既有 MySQL 集成测试会 INSERT/DELETE 测试数据，而本轮授权范围仍是数据库查询；同时 RSA 安全门尚未关闭。

12 个 endpoint 状态仍为：

```text
NOT_RUN
```

尚未由本轮端到端证明：

- HTTP status/body.code/msg/data；
- DTO 序列化；
- Repository/Mapper 正式路径；
- 无内存 fallback；
- Idempotency-Key；
- 重复扣款、重复冻结、重复持仓；
- rollback。

既有 standalone MockMvc 测试使用内存路径，不能替代持久化运行时证明。

## 7. 静态契约与前端工具链

- OpenAPI paths：23；
- DEFERRED paths：10；
- schema refs 缺失：0；
- response refs 缺失：0；
- error codes：19；重复：0；
- package.json 无 `test:unit`；
- 未发现前端测试 runner/config/spec；
- 未执行 pnpm lint/build；
- 未执行 `pnpm test:unit`。

## 8. 阶段判定

```text
PHASE1C-SECURITY-RUNTIME-GATE-EVIDENCE-COMPLETION-V2: PARTIAL
PHASE1C-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```

`quantpf.sys_menu` 表本身已确认存在，但 QuantPF 菜单行尚未落库，因此菜单契约仍未闭合。RSA、secrets scan、网关、12 API runtime、Repository/fallback/资金与持仓幂等及独立审查仍阻塞。
