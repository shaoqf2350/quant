# Phase 1C Security Runtime Gate Evidence Completion V2 Risk Register

- 日期：2026-10-09
- 结果：`PARTIAL`

| ID | 等级 | 风险 | 当前证据 | 影响 | 建议 | 是否阻塞 |
| -- | -- | -- | -- | -- | -- | -- |
| P1C-V2-001 | P0 | RSA 轮换/撤销证据缺失，前端私钥路径仍在 | env/type/jsencrypt marker 仍存在；无安全签核 | 浏览器/构建产物泄露风险 | 轮换/撤销、兼容迁移、清理和签核 | 是 |
| P1C-V2-002 | P0 | 正式 secrets scan 未完成 | gitleaks/trufflehog 不可用；source map/CI 未扫 | 无法证明历史/产物清洁 | 组织批准 scanner 全范围脱敏扫描 | 是 |
| P1C-V2-003 | P0 | QuantPF 菜单未落库 | `quantpf.sys_menu` 存在且有 109 条通用记录，但 QuantPF 筛选为 0 | 前端动态菜单、component/perms 未闭合 | 对账并受控写入/迁移 QuantPF 菜单，需另行授权 | 是 |
| P1C-V2-004 | P1 | 生产网关 `/api/quant` 未核验 | 只有 `/prod-api/` 示例 Nginx | 外部路径可能错误 | 提供生产配置或受控 HTTP 证据 | 是 |
| P1C-V2-005 | P1 | 12 Paper API runtime regression 未执行 | 现有测试会写删数据；本轮仅批准查询 | DTO/错误/持久化/幂等行为未知 | 单独批准 disposable/test schema 写入测试 | 是 |
| P1C-V2-006 | P1 | 无 fallback/重复资金持仓证据 | standalone 是内存路径，持久化 runtime 未跑 | 可能重复扣款/冻结/持仓 | 隔离 Spring/MySQL 并发/rollback 回归 | 是 |
| P1C-V2-007 | P1 | 菜单事实源与数据库未对账 | YAML 有 9 项，DB QuantPF 筛选 0 行 | 前端权限/路由无法可靠工作 | 先完成 menu facts→sys_menu 对账方案 | 是 |
| P1C-V2-008 | P2 | 无前端测试 runner | 无 test:unit、无 runner/config/spec | 自动化前端测试缺失 | 另行审批测试框架或替代验收 | 否 |

最终前端放行：`NOT ALLOWED`。
