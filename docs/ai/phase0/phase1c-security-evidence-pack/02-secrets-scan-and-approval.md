# Secrets Scan 与安全审批证据

## 用户提供声明

- 扫描报告：`SCAN-001`；
- 扫描编号：`SCAN-20261009-001`；
- 安全审批：`SEC-APPROVAL-001`。

## 必须补录的脱敏字段

- 工具名称/版本；
- 源码、env、dist/build、source map、CI artifact、Git history 覆盖范围；
- 文件数、命中数、排除规则；
- 报告时间和签核岗位。

## 本机冲突

当前工作区仍有 4 个私钥 marker 路径命中，因此报告必须解释扫描基线与当前工作区差异，或先完成清理后重新扫描。

## 审批边界

允许：本地 disposable/test schema 的 INSERT/DELETE 集成测试。
禁止：生产数据库、生产网关写入、LIVE、QMT、CTP、Broker、生产调度、Flyway 生产迁移。

## 当前判定

```text
FORMAL_SCAN: USER_DECLARED_PENDING_RECONCILIATION
WORKSPACE_SCAN: RECONCILIATION_REQUIRED
PRODUCTION_BUILD: BLOCKED
```
