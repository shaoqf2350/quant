# Phase 1C Security Runtime Gate Evidence Completion Risk Register

- 日期：2026-10-08
- 结果：`PARTIAL`

| ID | 等级 | 风险 | 当前证据 | 影响 | 建议 | 是否阻塞 |
| -- | -- | -- | -- | -- | -- | -- |
| P1C-EVC-001 | P0 | RSA 私钥轮换/撤销无证据且前端路径仍存在 | marker scan 命中 env/type/jsencrypt 路径；无安全负责人签核 | 浏览器/构建产物密钥泄露风险 | 取得轮换/撤销、受控密钥管理、后端兼容和清理证据后再改代码 | 是 |
| P1C-EVC-002 | P0 | 完整 secrets scan 未完成 | gitleaks/trufflehog 不可用；全历史/source-map/CI 未扫描 | 无法证明历史/产物清洁 | 组织批准 scanner，全范围脱敏扫描 | 是 |
| P1C-EVC-003 | P0 | RuoYi 主库 sys_menu 未核验 | 无获批 schema 连接证据 | component/perms/path 无法确认 | 主 schema 只读核验 | 是 |
| P1C-EVC-004 | P1 | 生产网关 `/api/quant` rewrite 未核验 | 仓库只有 `/prod-api/` 示例 Nginx | 生产 API 可能路径错误 | 获取生产配置或受控 HTTP 证据 | 是 |
| P1C-EVC-005 | P1 | 12 Paper API runtime regression 未执行 | 安全门未关闭，运行时测试 NOT_RUN | DTO/错误/幂等/持久化行为未知 | 安全门关闭后执行隔离 Spring/MySQL 测试 | 是 |
| P1C-EVC-006 | P1 | 无 fallback/重复资金与持仓写入证据 | 本轮未做端到端 runtime/DB regression | 可能空数据、重复扣款/冻结/持仓 | 测试正式 Repository、并发幂等和 rollback | 是 |
| P1C-EVC-007 | P1 | 10 个 DEFERRED path 仍在 OpenAPI 文档 | 已标记 DEFERRED 但 generator 可能仍生成 | 前端误调未实现端点 | 生成客户端必须过滤或拆分当前契约 | 是 |
| P1C-EVC-008 | P2 | 无 `test:unit` runner | package scripts 无 test:unit；未发现 runner | 前端单测门禁不可用 | 另行审批测试体系或定义替代验收 | 否 |

当前前端实现结论：`NOT ALLOWED`。
