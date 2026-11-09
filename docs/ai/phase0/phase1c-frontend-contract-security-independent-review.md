# QuantPF Phase 1C Frontend Contract Closure 独立审查

- 审查日期：2026-10-07
- 阶段：`PHASE1C-FRONTEND-CONTRACT-CLOSURE-AND-SECURITY-REMEDIATION`（只读独立审查）
- 审查角色：Phase 1C Frontend Contract Closure 独立审查负责人
- 审查对象：closure 主文档、whitelist-v2、manifest、risk-register、commands.log、handoff、deepseek contract-security inventory、OpenAPI、error-codes、menu-facts、`request.ts`/`jsencrypt.ts`/`errorCode.ts`、后端 Controller/DTO/配置、原型映射
- 审查方法：全部必读文件独立重读 + 以下主动核验（非复述前轮结论）：
  1. **manifest 哈希核验**：对 manifest 记录的 13 个 SHA-256 逐个重算——**13/13 全部匹配**，审计证据真实；
  2. **产物核验**：`plus-ui/dist`、`build`、`node_modules` 均不存在——无私钥已泄入构建产物，与"production build 未执行"声明一致；
  3. **后端加密链路核验**：grep 确认 `ruoyi-common-encrypt` 中 `CryptoFilter.java` 与 `@ApiEncrypt` 注解真实存在——closure 提出的"私钥清理须与后端加密链路协调"有实证依据；
  4. **Controller/DTO 事实复核**：重读全部 7 个 Controller、4 个 DTO、7 个 Application Service——closure 的差异清单（order cancel 未登记、submit 字段/202 不一致、risk GLOBAL-only、裸 List 返回、过滤参数不一致）**全部属实**；
  5. **自我纠正**：deepseek inventory 中"11 个 path 无 Controller / Paper 核心 11 端点"计数有误——正确为 **12 个已实现 + 10 个无后端 = 22**（枚举清单本身正确，仅聚合数字差一）。
- 本审查未修改任何业务代码、契约、环境或数据库；仅生成本文件。

---

## 0. 最终判定

```text
BLOCKED
```

closure 工作本身质量合格（证据真实、结论保守、门禁正确），**但其自我声明的两个 P0（客户端私钥、DTO 契约）均未关闭，白名单状态为 `PROPOSED_NOT_APPROVED`，`sys_menu` 事实 UNKNOWN**——不允许进入前端代码实现。closure 文档自身的 `NOT ALLOWED` 判定经独立复核**成立**。

---

## 12 项重点审查结果

| # | 审查项 | 结论 | 依据 |
|---|---|---|---|
| 1 | 客户端 RSA 私钥风险是否被正确阻断 | **通过（门禁正确，风险未关闭）** | closure §3 定为 P0 BLOCKED；whitelist-v2 将 `.env.*`/`jsencrypt.ts`/`request.ts` 列入 excludedUntilClosure；manifest 只记录变量名不记录值；实测 dist/node_modules 不存在，无私钥已泄入产物；后端 `CryptoFilter`/`@ApiEncrypt` 存在性已核实——closure 要求"先协调后端加密链路再清理"而非裸删，判断正确。但**风险本身未处置**：私钥仍在前端仓库 `.env` 中，轮换/撤销/扫描均无人工证据 |
| 2 | OpenAPI response DTO 是否足以支持前端 VO | **正确识别为不足** | closure §4 明确"不足以冻结前端 VO、分页和空态语义"；独立复核确认所有 page 接口返回**裸 `List`**（无 total/rows envelope），OpenAPI `data:{}` 空泛。deepseek inventory 已导出真实 record 字段（AccountView/OrderView 等 7 个），可作为 DTO 冻结的输入，但**源码事实 ≠ 冻结契约**，closure 的门禁正确 |
| 3 | 是否把菜单 YAML 当成 sys_menu DB 事实 | **通过** | closure §6 明确区分："quantpf schema 查询不到 sys_menu…… DB component/perms 必须标为 UNKNOWN"，未把 YAML 冒充 DB 事实。注：该 DB 查询证据本会话不可复现（无凭据），但 UNKNOWN 的保守结论两个方向都成立（见 P2-5） |
| 4 | API `/api` 前缀是否有后端证据 | **通过（留一个缺口）** | closure"主要路径基本一致"有 Controller 源码支撑（`context-path: /` + `@RequestMapping("/api/quant/...")` 已独立核实）。但**生产网关**对 `/api/quant` 的路由规则（是否再补/剥前缀）两份文档均未纳入风险登记（见 P1-4） |
| 5 | Idempotency-Key 是否与 repeatSubmit 混淆 | **通过** | closure §4 明确"repeatSubmit 的 500ms 防抖不是业务幂等，不能替代 Idempotency-Key"；whitelist-v2 专列 `idempotency.ts` 候选文件并挂"Retry semantics approved"门禁 |
| 6 | 错误码映射是否覆盖所有 Paper 写操作 | **门禁到位，内容待冻结** | closure 要求"正式的 QPF-* code→message/action 映射"作为前置。16 个 QPF 码已在契约中。补充事实（独立复核）：risk halt/resume 当前实现**无失败路径**（恒 R.ok），order cancel 错误消息是否携带 QPF 码未验证——冻结时须补 |
| 7 | 是否存在假 Position/Execution/Follow query/Order cancel API | **通过** | closure §4/§7 明确 Position/Execution"禁止造 API"、Follow 完整页延期、order cancel"补契约后才允许前端调用"；whitelist-v2 将三者 UI 全部排除；全库无任何假 API 封装（`src/api/quant` 不存在） |
| 8 | 是否把 LIVE/QMT/CTP/Broker/XXL-JOB 页面带入 | **通过** | closure §7 明确延期清单；whitelist-v2 excludedUntilCoverage 明确列出 SnailJob/XXL-JOB/QMT/CTP/LIVE/Broker |
| 9 | router/index.ts 或 permission.ts 是否进入不必要修改白名单 | **通过** | 二者均在 excludedUntilClosure，**不在** nextImplementationCandidates |
| 10 | 是否缺少前端构建产物秘密扫描 | **不缺少（工具未指定）** | closure §3 处置项 4 覆盖"源码、.env、dist/bundle、source map、CI artifact"扫描；handoff §9 要求组织批准的 secrets scanner。缺口：未指定具体扫描工具与判定标准（见 P2-2） |
| 11 | 是否缺少人工批准点 | **通过** | whitelist-v2 `status: PROPOSED_NOT_APPROVED` + 4 项 requiredApprovals（密钥轮换、DTO 契约、sys_menu 核验、页面范围）；closure §3 要求"轮换/清理/扫描有人工证据前禁止 production build" |
| 12 | 是否可以进入前端代码实现 | **否——BLOCKED** | 两个 P0 未关闭 + 白名单未批准 + sys_menu UNKNOWN + DTO 未冻结。closure 自判 `NOT ALLOWED`，独立复核成立 |

---

## P0（未关闭，阻断实施）

### P0-1 客户端 RSA 私钥仍未处置
- 现状：`VITE_APP_RSA_PRIVATE_KEY` 仍存在于 `.env.development`/`.env.production`；`jsencrypt.ts` 仍从 `import.meta.env` 读取并在浏览器 `setPrivateKey`。
- closure 已正确将其定为 P0 BLOCKED 并给出处置清单（轮换/撤销→协议替代→后端协调→产物扫描→人工证据），但**五步均无完成证据**。
- 独立复核补充：当前无 dist 产物，泄露面限于仓库与 git 历史；一旦执行任何 build（含 dev build），私钥即进入可分发产物。**在轮换完成前不仅禁止 production build，也应避免任何新增构建产物的交付**。

### P0-2 Paper response DTO / 分页 / 错误传输契约未冻结
- 现状：OpenAPI 22 个 path 中 12 个已实现的响应 `data` 均为 `{}` 或泛型；实际返回裸 `List`（无 total/rows）；HTTP 传输层恒 200、业务码在 body（与 OpenAPI 400/404/409 建模不符）；order submit 字段名（`instrumentId/limitPrice/estimatedNotional/fillPrice` vs OpenAPI `instrumentCode/price`）与 202 语义不一致。
- 前端 `types.ts`、分页控件、空态、错误分支全部依赖此项冻结。closure 门禁正确。

---

## P1（必须修正项）

### P1-1 handoff 验收命令自相矛盾：`pnpm test:unit` 不存在且不可创建
- handoff §9 列出 `pnpm test:unit` 作为门禁关闭后的验收命令，但 `package.json` scripts 仅有 build/build:dev/build:prod/dev/fmt/lint/lint:fix/preview——**无任何 test script**（plan §2 自己也记录了这一点）。
- 同时 whitelist-v2 未把 `package.json` 列入可修改候选，添加 test script 即违反白名单。
- 结果：验收路径不可执行。修正：要么在白名单 v3 中显式批准 `package.json` 增加 test script（Vitest 4.1.11 已在 devDependencies），要么修正 handoff 的验收命令清单。

### P1-2 whitelist-v2 缺少测试文件与测试门禁
- nextImplementationCandidates 14 项全部是 api/views/utils 交付文件，**无任何测试文件条目**；plan §8 曾把"E2E/component tests 与 fixtures"列为候选，closure 白名单将其丢失。
- 结合 Phase 1B 曾发生"skipped 测试被报告为 PASS"的教训，无测试门禁的白名单会复现同类风险。
- 修正：白名单 v3 必须为每个页面/API 模块补充对应测试文件条目（如 `src/api/quant/__tests__/`、views 组件测试），并在 gate 中加入"测试通过且无 skipped-as-pass"。

### P1-3 OpenAPI 中 10 个无后端实现的 path 未列入显式修正清单
- 独立复核：22 个 path 中 **10 个无任何 Controller**（dashboard/summary、instrument/page、market/kline、news/page、finance/statements、pool/page、strategy/page、signal/page、backtest POST、backtest/{id} GET）。
- closure §7 延期了对应页面，但未要求对这 10 个 path 做契约处置（删除/标注 `not-implemented`/排期）。若前端按 OpenAPI 生成客户端会得到 10 个必然 404 的调用面。
- 修正：下一阶段 `BACKEND-CONTRACT-AND-SECURITY-REMEDIATION-APPROVAL` 的范围中显式列入这 10 个 path 的处置决定。
- （同步更正：deepseek inventory 曾写"11 个无 Controller/11 个已实现"，正确计数为 **10 无 + 12 有 = 22**；其枚举清单本身无误。）

### P1-4 生产网关 `/api` 路由规则未入风险登记
- 本地事实已闭合（context-path `/` + Controller 自带 `/api/quant`），但**生产网关**是否对 `/api/quant` 有统一改写（补/剥前缀）无任何证据；若生产网关行为与本地 proxy 不同，前端所有 api url 前缀会整体失效。
- closure 与 risk-register 均未收录此项。修正：列入 remediation 阶段必答项（网关路由表或部署侧书面确认）。

---

## P2（建议处理，不阻断）

1. **P2-1 secrets scanner 未指定工具与标准**：处置清单要求"组织批准的 scanner"但未定工具（如 gitleaks/trufflehog）、扫描范围基线与判定阈值；建议在 remediation 阶段具体化。
2. **P2-2 git 历史中的私钥**：轮换完成后历史泄露即失效，但若任何人试图"只删除 .env 行"而不轮换，历史仍是泄露面；建议在处置清单明示"轮换是唯一合格路径，git 历史清理不替代轮换"。
3. **P2-3 order cancel 错误语义未定**：当前 `IllegalStateException → R.fail(409, msg)`，消息是否稳定携带 `QPF-ORDER-001` 未验证；契约补齐时须定义错误码与文案。
4. **P2-4 audit/job-execution 的 camel/snake 双别名过滤参数未入契约**：Controller 支持 `requestId/request_id` 等双写法，OpenAPI 未声明；冻结 DTO 时应决定保留哪些别名。
5. **P2-5 commands.log 的 DB 查询证据本会话不可复现**（无 DB 凭据，无法重跑"quantpf schema 无 sys_menu"查询）；结论 UNKNOWN 保守正确不受影响，但建议后续 DB 证据附可复现命令与只读连接说明，避免 Phase 1B 式"不可复现声明"先例。
6. **P2-6 deepseek inventory 聚合计数差一**（10/12 误写为 11/11）：枚举正确、数字有误，已在 P1-3 同步更正；后续文档引用时应以本审查计数为准。

---

## 必须修正项（进入实施前的硬性清单）

1. 完成 P0-1 五步安全处置并留存不含密钥值的人工证据（轮换/撤销、协议决定、后端协调、产物扫描、签核）。
2. 冻结 P0-2：12 个已实现端点的 request/response VO、分页/列表 envelope（明确 total 语义或声明"无总数分页"）、order submit 字段与 202/200 语义、错误传输规范（HTTP 200 + body code 或改造）、order cancel 契约登记。
3. 修正 P1-1：解决 `pnpm test:unit` 与白名单的矛盾（批准 package.json test script 或改验收命令）。
4. 修正 P1-2：白名单 v3 补测试文件条目与测试门禁。
5. 修正 P1-3：对 10 个无后端 path 做显式契约处置决定。
6. 修正 P1-4：取得生产网关 `/api/quant` 路由的书面证据。
7. 在获批 schema（RuoYi 主库，非 quantpf schema）只读核验 `sys_menu.component/perms`，与 menu-facts 对账。
8. 人工批准 whitelist-v3（页面范围、权限码、Follow/Position/Execution 延期确认）。

## 不允许修改文件（独立审查确认的红线）

- `plus-ui/.env.development`、`.env.production`（密钥处置未完成前）；
- `plus-ui/src/utils/jsencrypt.ts`、`src/utils/request.ts`（须与后端加密迁移协调后一并处理）;
- `plus-ui/src/router/index.ts`、`src/store/modules/permission.ts`（QuantPF 路由只走后端动态菜单注入）；
- `plus-ui/package.json`、`pnpm-lock.yaml`、`vite.config.ts`（除非 P1-1 修正中显式批准 test script）；
- 后端 Controller/DTO/Service、`contract/**`、数据库、任何 LIVE/QMT/CTP/Broker/调度相关文件；
- 不得创建 Position/Execution/Follow session read 假 API。

## 允许进入前端实现的条件

P0-1、P0-2 关闭 + P1-1..P1-4 修正 + sys_menu 只读核验完成 + whitelist-v3 获人工批准 + 试点页面范围限定为：Paper Account+Ledger、Paper Order list/submit（撤单待契约）、Risk status（halt/resume 需高风险写权限与二次确认获批）、Audit read-only。满足前维持 `NOT ALLOWED`。

---

## 审查边界与证据

- 只读审查；未修改任何业务代码、契约、环境或数据库；未连接生产/外部系统；未执行 Git；未输出任何密钥值。
- 独立核验动作：13 个 manifest SHA-256 重算（全匹配）；dist/build/node_modules 存在性检查（均不存在）；`CryptoFilter.java`/`ApiEncrypt.java` 存在性 grep；全部 7 Controller + 4 DTO + 7 Application Service 重读；`errorCode.ts`（仅 4 个通用码，无 QPF-*）与 `jsencrypt.ts`（浏览器 setPrivateKey）重读。
- 文件名勘误：任务指定的 `phase1c-frontend-contract-security-closure.md` 实际为 `phase1c-frontend-contract-closure.md`，已按实际文件审查。
- 本审查不替代安全负责人与主控的人工批准；最终放行以 P0/P1 关闭证据为准。