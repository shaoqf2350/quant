# Phase 1C Frontend Contract Closure Risk Register

- 日期：2026-10-07
- 状态：`PARTIAL`
- 本轮仅记录审计证据；没有修改源码、契约、环境变量或数据库。

| ID | 等级 | 风险 | 证据 | 影响 | 建议 | 是否阻塞 |
| -- | -- | -- | -- | -- | -- | -- |
| P1C-SEC-001 | P0 | 浏览器可见 RSA 私钥配置/读取路径 | 前端 `.env.*` 有 Vite private-key 变量；`jsencrypt.ts` 通过 `import.meta.env` 设置浏览器私钥 | 密钥泄露、历史构建产物风险、不能安全发布 | 轮换/撤销；清理 client-side private key；采用 TLS-only 或不下发长期私钥的协议；执行脱敏 secrets scan | 是 |
| P1C-CONTRACT-001 | P0 | Paper 查询/分页 response DTO 未冻结 | OpenAPI 多处 `ApiResponse.data: {}` 或过泛，Controller 返回列表型 `R` | 前端 VO、total、分页和空态无法可靠实现 | 补具体 VO、PageResult/列表语义、示例和脱敏字段 | 是 |
| P1C-CONTRACT-002 | P1 | Controller 与 OpenAPI 请求/响应语义不一致 | Order submit 字段/202 与实际同步返回不一致；cancel 仅源码存在；risk scope/targetId 未实现 | 前端错误请求或误判成功 | 后端契约审批后选择改实现或改契约 | 是 |
| P1C-CONTRACT-003 | P1 | 菜单数据库事实未知 | 配置的 `quantpf` schema 未发现 `sys_menu`；其他 schema 未查询 | component/perms 错配、动态路由空白或按钮隐藏 | 提供正确获批 schema，执行只读核验 | 是 |
| P1C-SEC-002 | P1 | 私钥清理需与后端加密链路协调 | 后端 CryptoFilter/ApiEncrypt 仍存在 | 单删前端 key 可能破坏认证/加密响应 | 书面确认迁移窗口、兼容策略和回滚边界 | 是 |
| P1C-API-001 | P1 | 写接口未形成前端业务幂等 helper | repeatSubmit 仅 500ms 防抖，不等于 Idempotency-Key | 重试可能重复下单/Follow | 三个写接口统一唯一 key 与重试语义 | 是 |
| P1C-API-002 | P1 | QPF 错误码映射不足 | 前端 errorCode.ts 未覆盖正式 QuantPF code 集 | 用户无法按风控/冲突/参数失败分流 | 冻结 code→message/action 映射并测试 | 否 |
| P1C-SCOPE-001 | P1 | 原型引用大量未实现 API | 原型映射超出 OpenAPI/Controller | 页面会调用不存在端点 | 仅实现正式 API，建立延期清单 | 是 |
| P1C-SCOPE-002 | P1 | Follow 无 session 查询 API/menu fact | 仅 start/stop，无 GET/page | 无法实现可恢复状态页 | 补查询契约，或明确排除 UI | 是 |
| P1C-SCOPE-003 | P1 | Position/Execution 无正式查询 API/DTO | OpenAPI/Controller 无路径 | 页面必须造数据或调用假 API | 延期，进入后端契约补齐 | 是 |
| P1C-SEC-003 | P1 | LIVE/QMT/CTP/Broker 页面混入 | 原型含相关页面 | 误导进入真实交易能力 | 白名单和菜单明确排除 | 是 |
| P1C-TECH-001 | P2 | component miss 可静默空白 | permission.ts 的 loadView 未命中可返回 undefined | 菜单出现但页面空白 | 建立 component→Vue 映射并加开发告警 | 否 |
| P1C-TECH-002 | P2 | HTTP status/body.code 未统一 | R.fail 主要写 body code | 拦截器与 OpenAPI 行为不一致 | 冻结 transport 规范并测试 | 否 |
| P1C-TECH-003 | P2 | Job execution 与 audit 边界不清 | 都是只读记录，不是调度控制 | UI 误解为 XXL-JOB 管理 | 页面命名和权限明确为执行审计 | 否 |

结论：P0 安全和 P0 DTO/分页契约未关闭，前端实施和 production build/release 不放行。

[2026-10-07 FINAL REVIEW ADDENDUM]
Independent review corrections:
- P1C-FE-001: package.json has no test:unit script; do not execute it. Any test runner/package change needs separate approval.
- P1C-FE-002: whitelist v3 added conditional test-file candidates and an explicit no-test-runner gate.
- P1C-FE-003: ten OpenAPI paths without backend controllers were explicitly enumerated for defer/remove/schedule disposition.
- P1C-FE-004: production gateway /api/quant routing evidence is now a required P1 gate.
- P1C-FE-005: secrets scanner tool/scope/threshold must be fixed before security pass.
- P1C-FE-006: sys_menu remains UNKNOWN until the approved RuoYi schema is checked; quantpf schema absence is not a whole-database conclusion.
Current status remains PARTIAL / frontend implementation NOT ALLOWED / production build BLOCKED.

