# QuantPF Phase 1C Contract Closure Final Review Addendum

- 复核日期：2026-10-07
- 复核依据：DeepSeek-v4-pro 只读盘点、GLM-5.3 独立审查、closure 主报告及实际 `plus-ui/package.json`。
- 结论：原 `PARTIAL / NOT ALLOWED` 判定正确；本附录补齐独立审查发现，不能放行前端实现。

## 一、最终修正

1. `plus-ui/package.json` 当前没有 `test:unit` script，也没有已发现的 Jest/Vitest 配置或前端测试文件。此前 handoff 中的 `pnpm test:unit` 不可执行，已修正为“不得执行该命令”；下一阶段如需要前端自动化测试，必须单独批准测试框架、依赖、配置和 package script，不能隐式修改 package.json/pnpm-lock.yaml。
2. OpenAPI 共 22 个路径；其中 12 个已有后端 Controller（含 health），以下 10 个目前没有后端实现：dashboard/summary、instrument/page、market/kline、news/page、finance/statements、pool/page、strategy/page、signal/page、backtest POST、backtest/{id} GET。下一阶段必须对这 10 个路径逐条标记为“延期/未实现/从当前前端客户端契约中移除”，禁止生成调用封装。
3. whitelist v3 增加了前端契约测试/验收门禁，但不授权修改 package.json、pnpm-lock.yaml 或引入测试依赖；没有测试运行器时，以静态类型检查、lint、受控 dev build 和人工 API 契约审查替代，且 build 仍受私钥门禁约束。
4. 增加生产网关 `/api/quant` 路由核验为 P1 必需项。当前 Controller 只证明应用内路径；不能推断生产网关是否追加或剥离 `/api`。
5. secrets scan 需在安全施工计划中固定工具、范围和判定标准；工具名称可由安全负责人批准，但必须覆盖源码、`.env`、dist/bundle、source map、CI artifact 和 Git 历史摘要。扫描输出不得保存密钥内容。
6. `sys_menu` 证据仍保持 `UNKNOWN`。必须使用用户批准的 RuoYi 主 schema 只读查询；不能把 `quantpf` 业务 schema 的缺表结果扩展为“全库无 sys_menu”。

## 二、最终放行门禁

只有同时满足以下条件，才允许进入前端代码实现：

- P0-SEC-001 客户端私钥已轮换/撤销，前端长期私钥读取路径清理，后端加密兼容策略获批准，脱敏 secrets scan 通过；
- P0-CONTRACT-001 Paper 12 个已实现端点的 request/response、分页/列表、HTTP/body 错误语义冻结；
- P1-001 `test:unit` 命令矛盾已关闭：要么批准并实施测试脚本，要么不把它列为验收命令；
- P1-002 whitelist v3 已人工批准，且测试依赖/配置是否允许有明确结论；
- P1-003 10 个无 Controller OpenAPI path 已逐条延期/移除/排期；
- P1-004 生产网关 `/api/quant` 路由规则有书面证据；
- `sys_menu.component/perms` 在正确 schema 完成只读核验；
- 第一批页面限定为 Account/Ledger、Order list/submit、Risk status、Audit read-only；Position、Execution、Follow 管理、调度、QMT、CTP、LIVE 不放行。

## 三、阶段结论

```text
PHASE1C-FRONTEND-CONTRACT-CLOSURE-AND-SECURITY-REMEDIATION: PARTIAL
PHASE1C-FRONTEND-IMPLEMENTATION: NOT ALLOWED
PHASE1C-PRODUCTION-BUILD-AND-RELEASE: BLOCKED
```

