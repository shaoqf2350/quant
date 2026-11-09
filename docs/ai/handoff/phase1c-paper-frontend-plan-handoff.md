# QuantPF Phase 1C Paper Frontend Plan Handoff

## 1. 当前目标

`PHASE1C-PAPER-FRONTEND-PLAN-AND-CONTRACT-CONSUMPTION`

## 2. 当前结果

```text
PHASE1C-PAPER-FRONTEND-PLAN-AND-CONTRACT-CONSUMPTION: PARTIAL
PHASE1C-PAPER-FRONTEND-IMPLEMENTATION: NOT STARTED / NOT ALLOWED
```

计划与审计文件已完成，但 P0 客户端私钥风险与正式 API/DTO/menu 缺口阻止直接进入页面实现。

## 3. 唯一主报告

`D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1c-paper-frontend-plan.md`

本 handoff 是下一阶段唯一交接入口。

## 4. 仓库事实

- 前端根：`D:\workspace\quant\plus-ui`
- 后端根：`D:\workspace\quant\RuoYi-Vue-Plus`
- 原型根：`D:\我的文档\私人\量化\前端功能原型设计`
- `src/api/quant` 和 `src/views/quant` 均不存在。
- 当前 `src/views` 有 55 个 Vue 文件，`src/api` 有 61 个 TS 文件。
- 静态路由含公共路由；动态菜单经后端 `/system/menu/getRouters` 加载；permission store 从 views glob map 解析组件。
- SnailJob iframe 页面存在，但不属于 Paper MVP。
- OpenAPI 有 22 QuantPF 路径对象；menu facts 有 9 项。

## 5. 重要阻塞

### P0

dev/prod `.env` 均有 Vite RSA private-key 变量，前端源码从 `import.meta.env` 读取并在浏览器设置私钥。不得复制/输出密钥值。维护者须轮换/撤销密钥、移除 client-side private key 配置、调整传输加密设计并扫描构建产物。在证据闭环前不得生产 build/release。

### P1 API/菜单缺口

- OpenAPI 没有 Position 查询 API；
- 没有 Execution 查询 API；
- Follow 只有 start/stop，没有读取当前/历史 session API；
- Paper endpoints 的 response DTO/page envelope 多未定义；
- order submit OpenAPI 为 202，需核对实际后端响应；
- OpenAPI 未定义撤单 endpoint；
- menu facts 没有 Follow/Position；原型 permission code 与正式 menu facts 不一致；
- 前端 errorCode map 不含 QPF codes。

## 6. 推荐 MVP 页面顺序

契约补齐/确认后优先：

1. Paper Account + Ledger；
2. Paper Order list/submit（撤单等契约确认后）；
3. Risk status；halt/resume 需额外写权限和二次确认；
4. Audit read-only。

延期：

- Position 和 Execution：等待正式查询 API/DTO；
- 完整 Follow management：等待 session read API/DTO 和 menu fact；
- XXL-JOB/SnailJob 管理、LIVE、QMT、CTP、Broker 页面：排除。

Job Execution API 如纳入，仅为只读执行审计，不是 scheduler 管理。

## 7. 文件白名单

`D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1c-paper-frontend-file-whitelist.json`

该文件是建议候选白名单，不是实施授权。当前阶段实际只写 docs。后续前端白名单须用户人工批准。

## 8. 必须先完成的步骤

1. 用户/安全维护者完成客户端密钥轮换/撤销、配置清理和 bundle scan；不得把密钥值发给模型或写入文档。
2. 另开后端契约补齐/确认任务：response DTO、pagination、Position/Execution/Follow session reads、Order cancel、submit response status、菜单权限。
3. 用户批准页面范围、候选文件白名单、菜单和权限、路由/API 文件是否可改。
4. 再进入受控前端实现；通过前置安全门后，执行 lint/build/组件/API测试。

## 9. 当前前端验收

- Node `v25.2.1`；pnpm `10.34.5`；lockfile v9。
- 当前审计未运行 pnpm lint/build/test（本阶段禁止）。
- 不得将未运行命令宣称为通过。

## 10. 推荐模型分工

- GPT-5.6 Sol：主控契约闭合、白名单、权限安全和最终验收。
- DeepSeek-v4-pro：仅在下个获批实现阶段处理被批准页面/API重复性实现；本轮没有调用它。
- GLM-5.3：独立审查路由/权限/API/泄露风险；本轮没有调用它。

## 11. 未解决问题

详见唯一主报告第 10–12 节和风险登记。当前不允许自动进入前端实现。

## 12. DeepSeek / GLM 最终复核后的新增门禁

- 独立审查文件：`D:\workspace\quant\RuoYi-Vue-Plus\docs\ai\phase0\phase1c-paper-frontend-independent-review.md`，结论为 `PASS_WITH_CHANGES`。
- P0 增补：OpenAPI 查询/分页 `data` schema 不完整，不能安全冻结前端 VO。
- P1 增补：必须核实 `sys_menu` DB 的实际 `component/perms`；必须核实 `/api` 前缀；必须冻结 `Idempotency-Key` 生成/重试；必须建立 QPF 错误码映射；原型未定义的 API 禁止封装。
- P2 固定：QuantPF 业务路由只走后端动态菜单注入，`dynamicRoutes` 保持空；实施时必须建立 `component` 字符串到 `.vue` 文件的对照表，防止 `loadView` 静默空白。

## 13. 下一目标

```text
PHASE1C-FRONTEND-CONTRACT-CLOSURE-AND-SECURITY-REMEDIATION
```

先处理客户端私钥轮换/撤销与清理证据，再补齐或正式裁剪 Paper 前端所需的 response DTO、分页 schema、Position/Execution/Follow session read、Order cancel、API 前缀、菜单 DB component/perms、QPF 错误码和幂等键方案。该阶段完成前，不进入前端页面实现。
