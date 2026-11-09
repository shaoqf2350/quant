# Disposable Schema 测试批准与回滚边界

审批编号：`SEC-APPROVAL-001`
状态：`USER_APPROVED_PENDING_EXECUTION`

## 允许

- 仅本地 disposable/test schema；
- 测试数据 INSERT/DELETE；
- Paper 持久化、幂等、rollback、无 fallback 集成测试；
- 随机前缀和 AfterEach 清理。

## 禁止

- 直接对共享/生产 `quantpf` 做写入型测试；
- 恢复旧 RSA 私钥；
- 恢复浏览器长期私钥；
- 生产数据库、LIVE、QMT、CTP、Broker、生产调度、Flyway 生产迁移。

## 待填写

- disposable schema 名称；
- JDBC host/schema 脱敏摘要；
- Maven 命令和结果；
- 清理结果；
- 测试报告编号。
