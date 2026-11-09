# QuantPF 下一阶段执行提示词

## PHASE1C-BACKEND-CONTRACT-AND-SECURITY-REMEDIATION-IMPLEMENTATION

当前日期：2026-10-07。

你是 QuantPF 项目的 Phase 1C 后端契约闭合与前端安全修复主控架构师、实施负责人、最终验收负责人。

正式方案：`D:\我的文档\私人\量化\量化交易系统完整设计方案_V29.md`
后端：`D:\workspace\quant\RuoYi-Vue-Plus`
前端：`D:\workspace\quant\plus-ui`
原型：`D:\我的文档\私人\量化\前端功能原型设计`

## 一、必须首先读取

1. `docs/ai/handoff/phase1c-frontend-contract-closure-handoff.md`
2. `docs/ai/phase0/phase1c-frontend-contract-closure.md`
3. `docs/ai/phase0/phase1c-frontend-contract-closure-final-review-addendum.md`
4. `docs/ai/phase0/phase1c-frontend-contract-closure-risk-register.md`
5. `docs/ai/phase0/phase1c-frontend-contract-closure-file-whitelist-v3.json`
6. `docs/ai/phase0/phase1c-frontend-contract-security-deepseek-inventory.md`
7. `docs/ai/phase0/phase1c-frontend-contract-security-independent-review.md`
8. `contract/openapi/quantpf-v29.yaml`
9. `contract/error-codes/quantpf-error-codes.yaml`
10. `contract/menu/quantpf-menu-facts.yaml`
11. 所有 QuantPF Controller、DTO、Application Service 和相关测试
12. `plus-ui/package.json`、`src/utils/request.ts`、`src/utils/jsencrypt.ts`、`src/utils/errorCode.ts`
13. V29 的 V29-0、V29-1、V29-2、V29-7、V29-8

## 二、唯一目标

在不接入 LIVE/QMT/CTP/Broker、不执行数据库迁移、不引入调度迁移的前提下：

1. 完成客户端 RSA 私钥风险的安全修复闭环；
2. 将 Paper 12 个已实现端点的 request/response、分页/列表、错误码、幂等和权限契约冻结为可消费事实；
3. 对 OpenAPI 中 10 个当前没有后端 Controller 的 path 逐条作出延期、移除或后续排期决定；
4. 核验正确 RuoYi 主 schema 的 `sys_menu.component/perms`，不得把 quantpf schema 缺表推断为全库事实；
5. 核验生产网关 `/api/quant` 是否追加或剥离前缀；没有网关证据时保持 UNKNOWN；
6. 生成正式下一阶段前端实施白名单；
7. 通过独立审阅后，决定是否允许进入 Paper 前端第一批实现。

## 三、安全硬规则

- 严禁读取、输出、复制、记录任何密码、Token、API Key、RSA 私钥或券商凭据。
- 已批准不等于已完成。若没有人工轮换/撤销证据，不得把 P0 安全项标记为 PASS。
- 不得仅删除前端私钥而不核对后端 CryptoFilter/ApiEncrypt 兼容性。
- 必须记录轮换/撤销、协议选择、后端兼容、源码/bundle/source-map/CI artifact/Git 历史扫描的脱敏证据。
- 未完成安全门禁前，禁止任何 production build/release；如构建会把敏感值注入 bundle，也禁止 dev build。

## 四、正式实施白名单原则

### 经主控确认后可修改

- `contract/openapi/quantpf-v29.yaml`
- `contract/error-codes/quantpf-error-codes.yaml`
- `contract/menu/quantpf-menu-facts.yaml`
- 对应 Paper Controller/DTO/Service/Test 文件（仅用于契约对齐，不得扩大到业务实现）
- `plus-ui/.env.development`、`plus-ui/.env.production`、`plus-ui/src/utils/jsencrypt.ts`、`plus-ui/src/utils/request.ts`（仅为移除长期客户端私钥或采用已批准兼容方案）
- `docs/ai/phase0/`、`docs/ai/handoff/` 全部本阶段交接文件

### 默认禁止

- `plus-ui/package.json`、`pnpm-lock.yaml`、`vite.config.ts`：当前没有 `test:unit`，除非单独批准测试框架/依赖/脚本；
- `src/router/index.ts`、`src/store/modules/permission.ts`：不得绕过后端动态菜单；
- Position/Execution/Follow 查询假 API；
- SnailJob/XXL-JOB、QMT/CTP/LIVE/Broker；
- Flyway/SQL migration、数据库 DDL/DML、生产数据库；
- 任何业务页面实现。

## 五、契约实施要求

必须为准入的 12 个已实现端点明确：

- request 字段、响应 VO、时间字段、枚举和脱敏字段；
- pageNum/pageSize 语义，或明确“非分页列表、无 total”；
- HTTP status 与 body.code 的最终规范；
- Order submit 的真实字段与同步/异步语义；
- Order cancel 的 OpenAPI、错误码、权限和幂等要求；
- Risk global halt/resume 的权限和二次确认；
- Follow start/stop 的幂等和延期的 session read API；
- Audit/job execution 的只读与脱敏边界。

10 个未实现 path 必须逐条记录：`DEFERRED`、`REMOVED_FROM_CURRENT_CLIENT_CONTRACT` 或 `PLANNED`，不得生成前端 API wrapper。

## 六、执行验收

安全门关闭并获人工证据后，允许：

```powershell
pnpm lint
pnpm build:dev
pnpm build:prod
```

`pnpm test:unit` 当前不可执行，因为 package.json 没有该 script；不得自行修改 package.json/pnpm-lock.yaml。若确需自动化前端测试，另建审批任务。

必须追加：

- Controller/OpenAPI diff 检查；
- QPF 错误码映射检查；
- Idempotency-Key 生成/重试检查；
- component→Vue 文件映射检查；
- secrets scan，输出只能是脱敏摘要。

## 七、必须生成文件

1. `phase1c-backend-contract-security-remediation.md`
2. `phase1c-backend-contract-security-remediation-risk-register.md`
3. `phase1c-backend-contract-security-remediation-file-manifest.json`
4. `phase1c-backend-contract-security-remediation-commands.log`
5. `phase1c-frontend-implementation-whitelist-approved-candidate-v1.json`
6. `handoff/phase1c-frontend-implementation-handoff.md`

最终判定只能是：

```text
PHASE1C-BACKEND-CONTRACT-AND-SECURITY-REMEDIATION-IMPLEMENTATION: PASS / PARTIAL / BLOCKED
PHASE1C-FRONTEND-IMPLEMENTATION: ALLOWED / NOT ALLOWED
```

## 八、模型协作分工

### GPT-5.6 Sol（主控，必须使用）

负责安全架构、契约取舍、实际白名单、风险等级、最终代码审查、最终验收和是否放行前端实现。不得把关键安全决策交给其他模型。

### DeepSeek-v4-pro（受控实现，可选）

只负责主控批准后的重复性工作：OpenAPI schema/DTO 文档化、错误码表整理、非敏感契约测试、manifest/commands log 初稿。不得处理私钥、加密迁移、生产网关、sys_menu 权限决策，不得修改 package.json/pnpm-lock.yaml。

### GLM-5.3（独立审阅，必须在实现后使用）

只读复核：安全门禁、12/10 path 计数、Controller/OpenAPI 对齐、分页/错误/幂等、菜单和网关证据、白名单越权。不得直接修改代码；输出独立审查报告。

如果只能使用一个模型：使用 GPT-5.6 Sol。

## 九、放行条件

只有以下全部满足，才可进入 `PHASE1C-FRONTEND-IMPLEMENTATION`：

- P0 客户端私钥已轮换/撤销、清理、扫描并人工签核；
- 12 个已实现端点契约冻结；
- 10 个未实现 OpenAPI path 已逐条处置；
- sys_menu 正确 schema 只读核验通过；
- 生产网关 `/api/quant` 路由规则有证据；
- QPF 错误码与 Idempotency-Key 规则已冻结；
- whitelist v3 或 approved candidate whitelist 获人工批准；
- GLM-5.3 独立审阅无未关闭 P0/P1；
- 第一批页面仅限 Account/Ledger、Order list/submit、Risk status、Audit read-only。

否则必须保持 `NOT ALLOWED`。
