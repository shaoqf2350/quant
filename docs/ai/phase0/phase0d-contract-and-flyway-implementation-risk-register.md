# PHASE0D Contract and Flyway Implementation Risk Register

| ID | 等级 | 风险 | 证据 | 影响 | 建议 | 是否阻塞 |
|---|---|---|---|---|---|---|
| P0-001 | P0 | Dev YAML 已配置本地数据库/Redis | 用户明确授权；本轮未连接 | 后续应用启动可能访问本地服务 | 连接前人工复核；密码不提交 Git | 是 |
| P0-002 | P0 | SnailJob 未迁移 | 底座仍含 SnailJob；XXL-JOB未接入 | 双栈/重复触发风险 | 独立调度迁移阶段 | 是 |
| P0-003 | P0 | Flyway 尚未执行 | SQL仅静态创建 | schema实际一致性未验收 | 先人工审查，再在隔离数据库执行 | 是 |
| P0-004 | P0 | 完整生产应用未验证 | Redisson/Lock4j/LiteFlow 曾阻塞启动 | 不能宣称生产启动通过 | 单独设计完整隔离启动验收 | 是 |
| P0-005 | P0 | 敏感配置轮换/撤销未闭环 | 本轮不记录凭据 | 安全状态未闭环 | 单独执行并记录非敏感结果 | 是 |
| P1-001 | P1 | 历史 SQL 与新 schema 命名体系存在差异 | V23/V24/V25历史SQL存在动态DDL/CREATE DATABASE/USE | 误执行会造成schema drift | 仅reference/legacy；禁止直接执行 | 否 |
| P1-002 | P1 | 契约尚未生成后端/前端代码 | 本轮只创建源契约 | API实现尚未存在 | 进入Paper域实现阶段后按契约生成 | 否 |
| P1-003 | P1 | Node v25.2.1非LTS | 基线已知事实 | CI长期维护风险 | 后续冻结LTS | 否 |
| P2-001 | P2 | OpenAPI/YAML未使用正式lint插件验证 | 本轮未新增插件 | 语法/引用错误可能遗漏 | 后续加隔离契约lint门禁，需另行审批POM | 否 |

## 处置结论

```text
PHASE0D-CONTRACT-AND-FLYWAY-IMPLEMENTATION: PASS
PHASE1A-PAPER-DOMAIN-IMPLEMENTATION: ALLOWED TO START AFTER HUMAN REVIEW
```

PASS不表示执行了数据库迁移或完成了生产启动。