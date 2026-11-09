# Phase 1C Security Runtime Gate Remediation Risk Register

- 日期：2026-10-08
- 结果：`PARTIAL`

| ID | 等级 | 风险 | 当前证据 | 影响 | 关闭条件 | 是否阻塞 |
| -- | -- | -- | -- | -- | -- | -- |
| P1C-RGR-001 | P0 | 客户端 RSA 私钥仍在前端边界 | `.env.*`、env 类型、`jsencrypt.ts` 仍有变量/读取路径 | 浏览器和未来 bundle 暴露风险 | 轮换/撤销、协议迁移、清理、兼容测试、签核 | 是 |
| P1C-RGR-002 | P0 | 完整 secrets scan 未闭合 | gitleaks/trufflehog 不可用；Git 全历史/source map/CI 未完成 | 无法证明泄露面关闭 | 组织批准工具完成脱敏扫描 | 是 |
| P1C-RGR-003 | P0 | RuoYi 主库 sys_menu 未核验 | 没有获批主 schema 连接；YAML 非 DB 事实 | 动态路由和权限错误 | 主 schema 只读对账 | 是 |
| P1C-RGR-004 | P1 | 生产网关路由未知 | 仅有示例 `/prod-api/` Nginx，非生产 `/api/quant` 证据 | 外部路径 404/重复前缀 | 网关配置或受控 HTTP 证据 | 是 |
| P1C-RGR-005 | P1 | 12 个 Paper endpoint runtime regression 未执行 | 安全门前禁止运行构建/完整回归 | 契约运行时不一致 | 受控 Spring/API 回归全通过 | 是 |
| P1C-RGR-006 | P1 | 持久化/fallback/资金幂等未完成运行时证明 | 尚未完成端到端检查 | 可能出现空列表、重复扣款/冻结/持仓 | 12 endpoint + DB事实+并发回归 | 是 |
| P1C-RGR-007 | P1 | 10 个 DEFERRED path 仍在同一 OpenAPI | 已标记但生成器仍可能产生客户端 | 前端调用不存在 Controller | current-client contract 拆分或过滤 | 是 |
| P1C-RGR-008 | P1 | 前端没有测试 runner | 无 `test:unit`、无 config/spec | 自动化前端测试缺失 | 单独审批测试框架或明确替代验收 | 否 |
| P1C-RGR-009 | P2 | 网关示例与前端 base API 可能不同 | 示例使用 `/prod-api/`，契约使用 `/api/quant` | 部署路径不一致 | 获取部署事实后统一 | 否 |

结论：当前保持 `PARTIAL / NOT ALLOWED`。
