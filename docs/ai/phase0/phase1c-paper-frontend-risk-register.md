# QuantPF Phase 1C Paper Frontend Plan Risk Register

- 日期：2026-10-07
- 阶段：`PHASE1C-PAPER-FRONTEND-PLAN-AND-CONTRACT-CONSUMPTION`
- 结果：`PARTIAL`

| ID | 等级 | 风险 | 证据 | 影响 | 建议 | 是否阻塞 |
|---|---|---|---|---|---|---|
| P1C-FE-P0-001 | P0 | Vite 客户端构建配置中存在 RSA private-key 变量且前端源码读取它 | `.env.development`/`.env.production` 的变量名；`src/utils/jsencrypt.ts` 使用 `import.meta.env` 并调用 setPrivateKey | 客户端 bundle 不具备私钥保密性；已有数据传输加密设计不能保护被打包的私钥 | 立即轮换/撤销密钥对；从 Vite client env 删除私钥；改为不依赖客户端私钥的协议；开展产物秘密扫描。报告未记录密钥值 | 是，阻塞 build/release 和前端实施交付 |
| P1C-FE-P1-001 | P1 | Paper Position API/DTO 缺失 | OpenAPI 22 paths 无 position query | Position 页面无法合法取数 | 后端先补正式 API/DTO 或正式延期页面 | 是，阻塞 Position 页面 |
| P1C-FE-P1-002 | P1 | Follow session query API/DTO 缺失 | OpenAPI 只有 start/stop | 页面刷新后无法从服务端恢复当前/历史 Follow 状态 | 增加 GET/page API、DTO、契约、授权、测试后再实施完整 Follow 页面 | 是，阻塞完整 Follow 管理页 |
| P1C-FE-P1-003 | P1 | Execution query API 缺失 | OpenAPI 无 execution endpoint | 成交/执行记录不能通过假 API 展示 | 增加正式 endpoint 或不做执行记录页 | 是，阻塞 Execution 页面 |
| P1C-FE-P1-004 | P1 | API response DTO/分页 schema 不完整 | 多个 endpoint 响应为 ApiResponse.data 空 schema | TypeScript 模型、表格字段和分页行为无法可靠冻结 | 先补 response DTO、page envelope、字段/时间/金额语义 | 是，阻塞联调和验收 |
| P1C-FE-P1-005 | P1 | Order submit 202 语义需与后端事实核对 | OpenAPI 声明 202；客户端需决定是否等待/轮询 | 错误 UI 生命周期和重复提交处理 | 在 API contract-consumption implementation 前复核 Controller/服务并统一状态码 | 是，阻塞 Order submit UI |
| P1C-FE-P1-006 | P1 | Follow/Position 菜单事实缺失，原型权限码与 menu facts 不同 | YAML 仅 9 项；原型使用不同 permission code | 页面菜单不显示或权限错误；可能绕过后端授权预期 | 单独审批菜单事实、数据库菜单施工和后端权限码；前端不自行造权限 | 是，阻塞对应页面 |
| P1C-FE-P1-007 | P1 | QPF error code 不在前端 errorCode map | `src/utils/errorCode.ts` 仅 HTTP 常规错误和 default | 用户只看到泛化错误，幂等/风控冲突不可操作性恢复 | 冻结错误码展示表并批准前端映射变更 | 否，但需在相关写页前解决 |
| P1C-FE-P1-008 | P1 | 原型引用不存在/不匹配的 endpoint | PROTOTYPE_PAGE_MAP 与 V29 OpenAPI 对照 | 错误调用、假数据和范围漂移 | 每个 API 以 OpenAPI 为唯一契约，原型只作视觉参考 | 是，实施验收门禁 |
| P1C-FE-P1-009 | P1 | OpenAPI 未登记撤单能力 | Paper order contract 只有 page/submit | 前端可能对存在的后端实现猜测路径 | 后端核实并更新 OpenAPI 后再启用 cancel UI | 是，阻塞撤单按钮 |
| P1C-FE-P2-001 | P2 | package scripts 没有 test | package.json 无 test script，Vitest 仅依赖 | 无统一 CI 测试入口 | 前端实施计划单独确定测试脚本/策略并审批 POM/package 变更 | 否 |
| P1C-FE-P2-002 | P2 | 动态菜单 YAML 并非前端运行时来源 | permission store 调 `/system/menu/getRouters` | 误认为改 YAML 即可出菜单 | 菜单源更新和实际 RuoYi menu seed/admin 配置必须另行设计 | 否 |
| P1C-FE-P2-003 | P2 | SnailJob iframe 存在 | monitor/snailjob/index.vue 使用环境 URL | 不应被误纳为 Paper MVP 或在双栈阶段误启 | 保留现状，不改页面；调度迁移另立阶段 | 否 |
| P1C-FE-P2-004 | P2 | 未执行 lint/build/test | 本阶段禁止执行 | 本阶段不能证明当前前端构建状态 | 取得人工批准并处置 P0 后按验收命令执行 | 是，阻塞前端实现验收完成 |

## 最终独立审查补充风险

| ID | 等级 | 风险 | 证据 | 影响 | 建议 | 是否阻塞 |
|---|---|---|---|---|---|---|
| P1C-FE-P0-002 | P0 | Paper 查询/分页响应 DTO 缺失 | OpenAPI 多数成功响应引用 `ApiResponse`，其 `data` 为 `{}` | 前端 VO、表格和分页字段只能凭空猜测 | 先补账户/订单/审计/风控/仪表盘等 response schema，或正式缩减页面范围 | 是 |
| P1C-FE-P1-010 | P1 | 菜单 YAML 不是数据库事实 | 动态路由实际来自 `/system/menu/getRouters`，`sys_menu` 尚未核验 | component/perms 可能不匹配，页面不显示或权限错误 | 先确认 DB component/perms，再冻结前端页面路径与权限码 | 是 |
| P1C-FE-P1-011 | P1 | API 前缀可能不一致 | 既有前端 API 常用无 `/api`；OpenAPI 使用 `/api/quant` | 请求可能 404 或重复前缀 | 对照后端 Controller、context-path、proxy 和实际本地路由后再封装 | 是 |
| P1C-FE-P1-012 | P1 | 幂等键缺少前端注入设计 | request.ts 的 repeatSubmit 不是 `Idempotency-Key` | 重试可能被后端拒绝或产生不可解释冲突 | 为 order/follow 三类写操作冻结唯一键生成与重试规则 | 是 |
| P1C-FE-P1-013 | P1 | QuantPF 错误码未进入前端映射 | `src/utils/errorCode.ts` 无 QPF codes | 用户无法区分幂等、风控、账户和 Follow 冲突 | 批准后增加稳定 QPF 错误码到用户动作的映射 | 否，实施前需完成 |
| P1C-FE-P1-014 | P1 | 原型 API 与正式契约不一致 | GLM 列出 40+ 原型路径不在 22 个 OpenAPI path 内 | 前端调用 404/假 API | API 封装只允许 V29 OpenAPI path；原型路径列入禁止清单 | 是 |
| P1C-FE-P2-005 | P2 | dynamicRoutes 可能被误用绕过菜单 | 当前 `dynamicRoutes=[]`，正常路径为后端菜单注入 | 可能绕过后端权限和菜单事实 | 保持 dynamicRoutes 为空，除非单独审批 | 否 |
| P1C-FE-P2-006 | P2 | loadView 未命中时静默空白 | `loadView` 未找到 component 时返回 undefined | 菜单 component 拼写错误时难排查 | 实施时建立 component↔file 对照并增加开发期告警 | 否 |
