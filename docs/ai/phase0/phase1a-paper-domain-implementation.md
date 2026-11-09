# PHASE1A-PAPER-DOMAIN-IMPLEMENTATION

- 日期：2026-10-06
- 项目：QuantPF
- 阶段：`PHASE1A-PAPER-DOMAIN-IMPLEMENTATION`
- 结果：`PASS`
- 范围：Paper 领域和模拟交易核心逻辑第一批实现
- 未实现：HTTP API、前端、数据库迁移、Redis、Broker、SnailJob、XXL-JOB、QMT、CTP、LIVE

## 1. 实际创建模块代码

### quantpf-domain

创建：

- Paper 领域枚举：ProductType、InstrumentStatus、AccountType、AccountStatus、OrderSide、OrderType、OrderStatus、ExecutionStatus、RiskStatus、TradingHaltScope；
- 值对象：Money、Quantity、ClientOrderId、RequestId、OrderIdempotencyKey；
- 领域异常：QuantPfDomainException；
- Entity：Instrument、TradingAccount、Position、PaperOrder、PaperExecution；
- Repository 接口：TradingAccountRepository、PositionRepository、InstrumentRepository、PaperOrderRepository；
- 单元测试：DomainValueTest。

### quantpf-finance

创建：

- `PaperAccountLedgerService`；
- 资金冻结、资金释放、冻结资金结算；
- 持仓冻结、持仓释放、卖出结算、买入结算；
- 金额和数量使用 BigDecimal 封装值对象；
- 单元测试：PaperAccountLedgerServiceTest。

### quantpf-execution

创建：

- `OrderStateMachine`；
- `PaperBrokerAdapter`；
- `InMemoryPaperBrokerAdapter`；
- `PaperOrderService`；
- 订单状态迁移错误码；
- Paper 成交对象生成；
- 单元测试：OrderStateMachineTest。

### quantpf-risk

创建：

- RiskDecision；
- RiskContext；
- RiskGate；
- PaperRiskGate；
- 全局 halt、账户 halt；
- 市场关闭、行情过期、重复/非法请求、资金/持仓门禁基础接口；
- 单元测试：PaperRiskGateTest。

### quantpf-backtest

创建：

- `BacktestExecutionBoundary`；
- 仅定义最小回测边界，不实现完整回测引擎。

### quantpf-job

创建：

- `PaperApplicationServicePort`；
- 不直接访问 Mapper、SQL、DataSource、Redis、Broker、Shell 或调度器。

## 2. OrderStateMachine 结果

允许迁移：

```text
NEW -> ACCEPTED
NEW -> REJECTED
ACCEPTED -> PARTIALLY_FILLED
ACCEPTED -> FILLED
ACCEPTED -> CANCEL_REQUESTED
ACCEPTED -> REJECTED
PARTIALLY_FILLED -> FILLED
PARTIALLY_FILLED -> CANCEL_REQUESTED
CANCEL_REQUESTED -> CANCELED
```

终态：

```text
FILLED
CANCELED
REJECTED
```

非法迁移抛出 `QuantPfDomainException`，错误码为：

```text
QPF-ORDER-001
```

## 3. Finance/Execution/Risk 边界

- `quantpf-domain` 是领域模型和 persistence interface owner；
- `quantpf-finance` 只处理账户、资金、持仓和账务规则；
- `quantpf-execution` 只提供 PaperBrokerAdapter 和订单状态机；
- `quantpf-risk` 只提供 Paper 风控门禁；
- `quantpf-job` 只保留 application service 调用端口；
- 没有任何模块直接连接外部 Broker；
- 没有任何模块注册 LIVE/QMT/CTP Bean。

## 4. PaperBrokerAdapter

实现：

```text
InMemoryPaperBrokerAdapter
```

它只在内存中：

1. 接收 PaperOrder；
2. 将订单从 NEW 转为 ACCEPTED；
3. 生成 PaperExecution；
4. 将订单转为 FILLED；
5. 不访问数据库；
6. 不访问 Redis；
7. 不访问 Broker；
8. 不访问 QMT、CTP、LIVE。

## 5. 测试结果

使用显式 JDK21 和 Maven 3.9.13 执行：

```powershell
mvn -f "D:\workspace\quant\RuoYi-Vue-Plus\pom.xml" `
  -pl "quantpf-modules/quantpf-domain,quantpf-modules/quantpf-finance,quantpf-modules/quantpf-execution,quantpf-modules/quantpf-risk,quantpf-modules/quantpf-backtest,quantpf-modules/quantpf-job" `
  -am `
  -DskipTests=false `
  test
```

结果：

```text
BUILD SUCCESS
exit code 0
```

本轮新增测试共 11 个：

- domain：2 个；
- finance：2 个；
- execution：3 个；
- risk：4 个。

新增测试全部通过：

```text
Failures: 0
Errors: 0
Skipped: 0
```

第一次执行发现 finance 测试期望值错误：买入成交后可用现金应为 950，而不是 990。已修正测试断言，未修改生产逻辑，重新执行后全部通过。

## 6. JDK21/Maven 结果

- JDK：Temurin 21.0.12.1；
- Maven：3.9.13；
- Maven compile/test：PASS；
- QuantPF job dependency tree：PASS；
- 未修改任何 POM。

## 7. POM 状态

根 `pom.xml`：未修改。

`ruoyi-admin/pom.xml`：未修改。

QuantPF 模块 POM：未修改。

现有依赖 DAG 可支持本轮代码，未发现需要新增依赖的情况。

## 8. 数据库/Redis/调度状态

本轮没有：

- 连接 MySQL；
- 连接 Redis；
- 执行 Flyway；
- 执行 SQL；
- 读取或输出数据库密码；
- 读取或输出 Redis 密码；
- 启动 SnailJob；
- 启动 XXL-JOB；
- 发送调度心跳；
- 连接 Broker。

## 9. QMT/CTP/LIVE 扫描

QuantPF 模块中未发现 QMT、CTP、LIVE、LiveOrderGateway 或真实 submit/cancel 实现。

扫描结果中 `liveEnabled=false` 仅来自既有健康接口静态响应，不是交易能力。

`quantpf-job` 中未发现 Mapper、SQL、DataSource、JdbcTemplate、ProcessBuilder、Runtime 或 Shell 调用。

## 10. 当前风险

### P0

- 完整 ruoyi-admin 生产启动仍未验证；
- SnailJob 尚未迁移，XXL-JOB 尚未接入；
- Flyway 尚未在隔离数据库执行；
- 敏感配置轮换/撤销尚未闭环；
- Paper API 和前端尚未实现。

### P1

- 当前 Entity/Repository 仍是第一批最小模型，尚未接入真实 Mapper/XML；
- 资金和持仓服务尚未接入持久化事务；
- PaperBrokerAdapter 目前为内存实现；
- 订单状态机尚未接入事件发布和审计持久化；
- Node v25.2.1 非 LTS；
- target 构建产物仍存在。

### P2

- OpenAPI 尚未生成后端 DTO/Controller；
- JSON Schema 尚未接入 CI lint；
- 前端尚未接入正式契约；
- 仍需增加更完整的边界测试和并发幂等测试。

## 11. 是否允许进入下一阶段

```text
PHASE1A-PAPER-DOMAIN-IMPLEMENTATION: PASS
PHASE1A-PAPER-APPLICATION-API: NOT STARTED
```

允许在人工审查本轮代码和 manifest 后进入 Paper API 阶段。

## 12. 回滚说明

本轮仅新增 QuantPF 模块 Java/test 文件和 Phase 1A 文档；未修改 POM、YAML、SQL、contract、前端或调度文件。回滚应采用文件级审阅，不执行破坏性 Git 命令。