# PHASE1A-PAPER-APPLICATION-API 实施报告

- 日期：2026-10-06
- 项目：QuantPF
- 阶段：`PHASE1A-PAPER-APPLICATION-API`
- 结果：`PASS`
- API类型：Paper-only，内存隔离实现
- 数据库/Redis：未连接
- 生产应用启动：未执行

## 1. 实际实施内容

### Application Service

新增：

- `PaperAccountApplicationService`；
- `PaperOrderApplicationService`；
- `PaperRiskApplicationService`。

这些服务使用内存存储和已有 Paper 领域逻辑，不连接数据库、Redis 或 Broker。

### DTO

新增：

- `PaperOrderRequest`；
- `RiskReasonRequest`。

### Controller

新增：

- `QuantPaperAccountController`；
- `QuantPaperOrderController`；
- `QuantPaperRiskController`；
- `QuantPaperFollowController`；
- `QuantPaperAuditController`；
- `QuantPaperJobExecutionController`。

Controller 路径均位于：

```text
com.quantpf.web.controller
```

## 2. API 映射

实现的 API：

```text
GET  /api/quant/account/page
GET  /api/quant/account/{id}/ledger
GET  /api/quant/order/page
POST /api/quant/order/paper
GET  /api/quant/risk/status
POST /api/quant/risk/halt
POST /api/quant/risk/resume
POST /api/quant/paper/follow-start
POST /api/quant/paper/follow-stop
GET  /api/quant/audit/page
GET  /api/quant/job/execution/page
```

Paper order 要求：

- `Idempotency-Key` 请求头；
- accountId；
- instrumentId；
- side；
- orderType；
- quantity；
- limitPrice；
- estimatedNotional；
- fillPrice；
- clientOrderId；
- requestId。

缺少 `Idempotency-Key` 返回错误码 400；重复幂等键返回错误码 409。

## 3. Paper-only 边界

- Follow API 只返回 `mode=PAPER`；
- 没有 LIVE/QMT/CTP 路由；
- 没有真实 Broker adapter；
- 没有 submit/cancel 真实交易能力；
- 没有调度器连接；
- 没有数据库访问；
- 没有 Redis 访问。

## 4. 测试结果

新增 API 测试：

```text
QuantPaperApiStandaloneTest
```

测试覆盖：

1. Paper account page；
2. 缺少 Idempotency-Key；
3. Paper order 创建；
4. 重复 Idempotency-Key；
5. Risk halt；
6. Risk resume；
7. Paper follow start；
8. Paper follow stop。

完整 Maven 测试结果：

```text
BUILD SUCCESS
```

ruoyi-admin 测试报告：

```text
Tests run: 7
Failures: 0
Errors: 0
Skipped: 0
```

其中：

- API 测试：6 个；
- Phase 0C 健康测试：1 个。

测试使用 standalone MockMvc，不启动完整 `DromaraApplication`。

## 5. JDK21/Maven

- JDK：Temurin 21.0.12.1；
- Maven：3.9.13；
- JDK21 Maven test：PASS；
- 相关模块和 ruoyi-admin reactor：PASS。

## 6. POM 状态

本轮没有修改：

- 根 `pom.xml`；
- `ruoyi-admin/pom.xml`；
- QuantPF 模块 POM。

现有 POM 差异来自前序 Phase 0C，未由本轮新增。

## 7. 数据库、Redis、调度和交易状态

本轮没有：

- 连接 MySQL；
- 连接 Redis；
- 执行 SQL；
- 执行 Flyway；
- 启动 SnailJob；
- 启动 XXL-JOB；
- 连接 Broker；
- 创建 QMT/CTP/LIVE；
- 启动完整应用端口。

## 8. 静态安全扫描

在 QuantPF API 代码和 QuantPF 模块中未发现：

- QMT；
- CTP；
- LIVE；
- LiveOrderGateway；
- DataSource；
- JdbcTemplate；
- RedisTemplate；
- RedissonClient；
- SnailJob；
- XXL-JOB；
- ProcessBuilder；
- Runtime.getRuntime。

既有健康 Controller 中的 `liveEnabled=false` 是静态健康字段，不属于交易能力。

## 9. 当前风险

### P0

- API 当前使用内存 service，尚未持久化；
- Paper API 尚未接入 MyBatis Mapper/XML；
- Flyway 尚未在隔离数据库执行；
- 完整 ruoyi-admin 生产启动未验证；
- SnailJob 尚未迁移；
- 敏感配置轮换/撤销尚未闭环。

### P1

- Controller 依赖内存 runtime，后续需要替换为正式 application service Bean；
- API 权限注解尚未接入；
- 审计 API 当前返回空内存列表；
- Paper follow 当前为内存状态标志；
- 并发幂等测试尚未完成。

### P2

- OpenAPI 尚未通过正式 lint 插件验证；
- 前端尚未接入；
- 错误响应还需要统一异常处理器；
- 需要补充分页、权限和 DTO 校验边界。

## 10. 当前阶段判定

```text
PHASE1A-PAPER-APPLICATION-API: PASS
PHASE1B-PAPER-INTEGRATION-TEST: NOT STARTED
```

本轮 PASS 仅表示 Paper API 的内存隔离版本和 standalone MockMvc 测试通过，不表示生产 API 已完成。