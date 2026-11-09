# PHASE0D API Mapping Matrix

- 日期：2026-10-06
- 正式 OpenAPI：`contract/openapi/quantpf-v29.yaml`
- 原型仅为 reference，不是正式 API 事实源。

| 领域 | 正式资源/operation 方向 | 原型处理 | Stage 1 |
|---|---|---|---|
| health | `/api/quant/health` GET | formal | allowed |
| dashboard | `/api/quant/dashboard/summary` GET | formal | allowed |
| instrument | `/api/quant/instrument/page` GET | formal | allowed |
| market | `/api/quant/market/kline` GET | formal | allowed |
| news | `/api/quant/news/page` GET | formal/deferred by provider | allowed with mock/provider boundary |
| finance | `/api/quant/finance/statements` GET | formal/deferred | allowed with provider boundary |
| pool | `/api/quant/pool/page` GET | formal | allowed |
| strategy | `/api/quant/strategy/page` GET | formal | allowed |
| signal | `/api/quant/signal/page` GET | formal | allowed |
| backtest | `/api/quant/backtest` POST, `/api/quant/backtest/{id}` GET | formal | allowed |
| paper account | `/api/quant/account/page` GET, `/api/quant/account/{id}/ledger` GET | formal | allowed |
| paper order | `/api/quant/order/page` GET, `/api/quant/order/paper` POST | formal | allowed |
| paper follow | `/api/quant/paper/follow-start` POST, `/api/quant/paper/follow-stop` POST | formal with Paper only | allowed with halt gate |
| risk | `/api/quant/risk/status` GET, `/api/quant/risk/halt` POST, `/api/quant/risk/resume` POST | formal | allowed with permission/audit |
| job execution | `/api/quant/job/execution/page` GET | formal | allowed |
| audit | `/api/quant/audit/page` GET | formal | allowed |
| broker/live | QMT/CTP/LIVE submit/cancel | deferred/reference-only | forbidden |

每个原型 API 必须最终标记为 `formal`、`compatibility`、`deprecated`、`deferred` 或 `reference-only`，不得直接复制旧路径。