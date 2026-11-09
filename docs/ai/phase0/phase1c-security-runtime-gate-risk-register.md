# Phase 1C Security Runtime Gate Risk Register

- 日期：2026-10-08
- 阶段结果：`PARTIAL`

| ID | 等级 | 风险 | 当前证据 | 影响 | 关闭条件 | 是否阻塞 |
| -- | -- | -- | -- | -- | -- | -- |
| P1C-GATE-001 | P0 | 客户端 RSA private key 仍在前端边界 | `.env.*`、`jsencrypt.ts`、`request.ts` 标记路径仍存在；无轮换/撤销签核 | 私钥可进入浏览器/构建产物 | 轮换撤销、协议迁移、清理、兼容回归、人工签核 | 是 |
| P1C-GATE-002 | P0 | secrets scan 证据不完整 | gitleaks/trufflehog 不可用；Git 全历史、CI artifact、source map 未完成 | 无法证明泄露面已关闭 | 组织批准 scanner 完成脱敏扫描 | 是 |
| P1C-GATE-003 | P0 | RuoYi 主库 sys_menu 未核验 | 正确主 schema 未提供；quantpf 业务库缺表不能外推 | component/perms/path 错配 | 主 schema 只读证据 | 是 |
| P1C-GATE-004 | P1 | 生产网关 rewrite 未核验 | 只有 Spring Controller `/api/quant` | 外部路径可能 404/重复前缀 | 网关配置或受控 HTTP 证据 | 是 |
| P1C-GATE-005 | P1 | 12 端点运行时回归未执行 | 安全门关闭前禁止构建/完整回归 | DTO、body-code、持久化路径可能偏差 | 受控 Spring/API 回归全通过 | 是 |
| P1C-GATE-006 | P1 | OpenAPI 保留 10 个 DEFERRED path | 已有标记但生成器仍可能生成客户端 | 前端调用不存在 Controller | current-client contract 拆分或生成器过滤 | 是 |
| P1C-GATE-007 | P1 | 无前端 test:unit | package.json 无脚本、无 runner/config/spec | 自动化前端测试缺失 | 单独审批测试框架，或明确静态/lint/build 验收 | 否 |
| P1C-GATE-008 | P1 | 权限事实源只有 YAML permission | YAML 无 component/perms/parentId/path DB 事实 | 动态路由/按钮可能不一致 | 主库只读对账 | 是 |
| P1C-GATE-009 | P2 | QPF 错误 body 文案动态拼接 | Controller 将 code 与 message 拼接 | 前端错误映射不稳定 | 冻结解析或统一错误 DTO | 否 |

当前前端实现结论：`NOT ALLOWED`。
