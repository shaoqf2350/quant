# PHASE0D Paper Domain Model

## 1. Core aggregates

- `Instrument`：证券/期货合约基础信息；
- `TradingAccount`：Paper 账户；
- `CashLedger`：资金流水和冻结；
- `Position`：持仓、可用数量、冻结数量；
- `Order`：委托意图、状态、幂等键；
- `Execution`：成交/撮合结果；
- `StrategySignal`：策略信号；
- `RiskSnapshot`：风控快照；
- `BacktestRun`：回测运行；
- `JobExecutionAudit`：任务执行审计。

## 2. Ownership

- `quantpf-domain`：实体、枚举、Mapper、XML、Repository 的唯一持久化 owner；
- `quantpf-execution`：PaperBrokerAdapter、订单状态机和执行编排；
- `quantpf-finance`：账户、现金、持仓和账务；
- `quantpf-risk`：风控门禁和 halt；
- `quantpf-backtest`：隔离回测执行；
- `quantpf-job`：只触发 application service，不直接 SQL、Mapper、Shell 或 Broker；
- `ruoyi-admin`：Controller 和 Web 接入。

## 3. Invariants

- 订单状态迁移必须单向、可审计、可幂等；
- clientOrderId/requestId/idempotencyKey 必须唯一或可重放；
- 成交必须产生 execution event 和 ledger event；
- 余额冻结必须与订单生命周期一致；
- 持仓冻结必须与卖出订单生命周期一致；
- Risk halt 时禁止新 Paper 订单；
- LIVE/QMT/CTP 永远不是 Stage 1 Spring Bean。

## 4. Implementation priority

```text
Domain enums/value objects
→ Paper account/ledger
→ Order state machine
→ PaperBrokerAdapter
→ Risk gates
→ Backtest boundary
→ Application services
→ HTTP controllers
```