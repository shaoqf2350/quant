# QuantPF Phase 1A Paper API Risk Register

| ID | 等级 | 风险 | 证据 | 影响 | 建议 | 是否阻塞 |
|---|---|---|---|---|---|---|
| P1A-API-P0-001 | P0 | API 使用内存 application service，未接入持久化 | `PaperApiRuntime` 使用内存服务 | 重启后数据丢失，不能作为生产 API | 下一阶段接入 Mapper/Repository/事务 | 是 |
| P1A-API-P0-002 | P0 | API 未完成正式权限和异常统一 | 当前 Controller 为最小 Paper 验收实现 | 权限和错误响应未达到生产要求 | 增加统一异常映射和权限门禁 | 是 |
| P1A-API-P0-003 | P0 | 完整应用未启动验证 | 测试均为 standalone MockMvc | 不能宣称生产 Web API 可用 | 隔离环境完成后再做应用启动验证 | 是 |
| P1A-API-P1-001 | P1 | Paper follow/audit/job API 为内存占位 | follow 使用内存标志，audit/job 返回空列表 | 功能不完整 | 接入领域事件、审计和持久化 | 否 |
| P1A-API-P1-002 | P1 | POM 未修改，生产服务 Bean 扫描边界尚未接入 | 当前 Controller 可测试但完整应用扫描仍受限 | 部署后可能无法注入正式 service | 后续设计受控 Web/application 配置 | 否 |
| P1A-API-P1-003 | P1 | 并发幂等尚未验证 | 仅有单线程重复 key 测试 | 并发请求一致性未知 | 增加并发测试和数据库唯一约束验证 | 否 |
| P1A-API-P2-001 | P2 | OpenAPI 尚未运行正式 lint | 当前只做代码和 MockMvc 测试 | 契约引用错误可能遗漏 | 后续加入契约 lint | 否 |
| P1A-API-P2-002 | P2 | 前端尚未接入 API | 本轮明确不修改前端 | UI 闭环未完成 | 后续前端契约接入阶段实现 | 否 |

## 处置结论

```text
PHASE1A-PAPER-APPLICATION-API: PASS
PHASE1B-PAPER-INTEGRATION-TEST: NOT STARTED
```