# QuantPF V25 产品原型页面映射

> 生成日期：2026-10-01。所有页面均可在 Microsoft Edge 中直接打开。

| HTML | 页面 | 建议路由 | 权限 | 主要接口 |
|:---|:---|:---|:---|:---|
| `pages/dashboard.html` | 交易驾驶舱 | `/quant/dashboard` | `quant:dashboard:view` | `GET /api/quant/dashboard/summary` |
| `pages/instrument-list.html` | 标的中心 | `/quant/instrument` | `quant:instrument:list` | `GET /api/quant/instrument/list` |
| `pages/instrument-detail.html` | 标的详情 | `/quant/instrument/:symbol` | `quant:instrument:view` | `GET /api/quant/instrument/{productType}/{symbol}/summary` |
| `pages/market-kline.html` | K线与指标 | `/quant/market/kline` | `quant:kline:list` | `GET /api/quant/kline/query` |
| `pages/market-orderbook.html` | 盘口快照 | `/quant/market/orderbook` | `quant:orderbook:list` | `GET /api/quant/orderbook/latest/{symbol}` |
| `pages/market-tick.html` | 逐笔成交 | `/quant/market/tick` | `quant:tick:list` | `GET /api/quant/tick/query` |
| `pages/market-import.html` | 行情导入 | `/quant/data/kline-import` | `quant:kline:import` | `POST /api/quant/kline/import` |
| `pages/news-latest.html` | 最新新闻 | `/quant/news/latest` | `quant:news:list` | `GET /api/quant/news/latest` |
| `pages/news-sentiment.html` | 舆情分析 | `/quant/news/sentiment` | `quant:news:list` | `GET /api/quant/news/sentiment/trend` |
| `pages/news-detail.html` | 新闻详情 | `/quant/news/:id` | `quant:news:view` | `GET /api/quant/news/{id}` |
| `pages/news-risk.html` | 新闻风险 | `/quant/news/risk` | `quant:risk:list` | `GET /api/quant/news/risk-events` |
| `pages/finance-calendar.html` | 财报日历 | `/quant/finance/calendar` | `quant:finance:list` | `GET /api/quant/finance/earnings-calendar` |
| `pages/finance-statements.html` | 三大报表 | `/quant/finance/statements/:symbol` | `quant:finance:view` | `GET /api/quant/finance/statements/{symbol}` |
| `pages/finance-factors.html` | 基本面因子 | `/quant/finance/factors/:symbol` | `quant:finance:view` | `GET /api/quant/finance/factors/{symbol}` |
| `pages/finance-analysis.html` | AI财报分析 | `/quant/finance/analysis/:symbol` | `quant:finance:analyze` | `GET/POST /api/quant/finance/analysis` |
| `pages/finance-peers.html` | 同业对比 | `/quant/finance/peers/:symbol` | `quant:finance:view` | `GET /api/quant/finance/peers/{symbol}` |
| `pages/strategy-config.html` | 策略配置 | `/quant/strategy/config` | `quant:strategy:list` | `GET/PUT /api/quant/strategy` |
| `pages/strategy-params.html` | 参数版本 | `/quant/strategy/params` | `quant:strategy:edit` | `GET/POST /api/quant/strategy-params` |
| `pages/strategy-runtime.html` | 运行监控 | `/quant/strategy/runtime` | `quant:strategy:runtime` | `GET /api/quant/strategy/{id}/runtime` |
| `pages/strategy-signals.html` | 交易信号 | `/quant/strategy/signals` | `quant:signal:list` | `GET /api/quant/signal/list` |
| `pages/strategy-performance.html` | 策略表现 | `/quant/strategy/performance` | `quant:performance:list` | `GET /api/quant/performance/strategy/list` |
| `pages/pool-today.html` | 今日标的池 | `/quant/pool/today` | `quant:pool:list` | `GET /api/quant/stock-pool/active` |
| `pages/pool-history.html` | 历史标的池 | `/quant/pool/history` | `quant:pool:list` | `GET /api/quant/stock-pool/history` |
| `pages/pool-hitrate.html` | 命中率分析 | `/quant/pool/hit-rate` | `quant:pool:list` | `GET /api/quant/stock-pool/statistics` |
| `pages/pool-screening.html` | 筛选任务 | `/quant/pool/screening` | `quant:pool:screening` | `POST /api/quant/stock-pool/trigger-screening` |
| `pages/backtest-create.html` | 新建回测 | `/quant/backtest/create` | `quant:backtest:submit` | `POST /api/quant/backtest/submit` |
| `pages/backtest-list.html` | 回测任务 | `/quant/backtest/list` | `quant:backtest:list` | `GET /api/quant/backtest/list` |
| `pages/backtest-detail.html` | 回测详情 | `/quant/backtest/:id` | `quant:backtest:list` | `GET /api/quant/backtest/result/{id}` |
| `pages/backtest-compare.html` | 参数对比 | `/quant/backtest/compare` | `quant:backtest:list` | `POST /api/quant/strategy-params/compare` |
| `pages/trade-accounts.html` | 账户总览 | `/quant/trade/accounts` | `quant:account:list` | `GET /api/quant/account/summary` |
| `pages/trade-positions.html` | 持仓管理 | `/quant/trade/positions` | `quant:position:list` | `GET /api/quant/position/list` |
| `pages/trade-orders.html` | 委托订单 | `/quant/trade/orders` | `quant:trade:list` | `GET /api/quant/trade/order/list` |
| `pages/trade-records.html` | 成交记录 | `/quant/trade/records` | `quant:trade:list` | `GET /api/quant/trade/record/list` |
| `pages/trade-manual.html` | 手动下单 | `/quant/trade/manual-order` | `quant:trade:order` | `POST /api/quant/trade/order` |
| `pages/trade-paper.html` | 模拟盘管理 | `/quant/trade/paper` | `quant:paper:list` | `GET /api/quant/paper/account/{accountId}` |
| `pages/risk-status.html` | 风控总览 | `/quant/risk/status` | `quant:risk:list` | `GET /api/quant/risk/status` |
| `pages/risk-rules.html` | 风控规则 | `/quant/risk/rules` | `quant:risk:list` | `GET /api/quant/risk/rules` |
| `pages/risk-logs.html` | 风控日志 | `/quant/risk/logs` | `quant:risk:log` | `GET /api/quant/risk/log/list` |
| `pages/risk-halt.html` | 紧急熔断 | `/quant/risk/halt` | `quant:risk:halt` | `POST /api/quant/risk/halt` |
| `pages/broker-qmt.html` | QMT配置 | `/quant/broker/qmt` | `quant:broker:edit` | `GET/PUT /api/quant/broker-config` |
| `pages/broker-ctp.html` | CTP配置 | `/quant/broker/ctp` | `quant:broker:edit` | `GET/PUT /api/quant/broker-config` |
| `pages/broker-status.html` | 连接状态 | `/quant/broker/status` | `quant:broker:list` | `GET /api/quant/broker-config/{id}/status` |
| `pages/broker-logs.html` | 连接日志 | `/quant/broker/logs` | `quant:broker:list` | `GET /api/quant/broker-config/{id}/connect-logs` |
| `pages/data-sources.html` | 数据源健康 | `/quant/data/source-health` | `quant:data-source:list` | `GET /api/quant/data-source/health` |
| `pages/data-quality.html` | 数据质量 | `/quant/data/quality` | `quant:data-source:manage` | `GET /api/quant/data-quality/check` |
| `pages/data-import-logs.html` | 导入日志 | `/quant/data/import-logs` | `quant:kline:list` | `GET /api/quant/kline/import-log` |
| `pages/ops-jobs.html` | XXL-JOB任务 | `/quant/ops/jobs` | `quant:job:list` | `GET /api/quant/job/list` |
| `pages/ops-monitor.html` | 系统监控 | `/quant/ops/monitor` | `quant:monitor:list` | `GET /actuator/prometheus + 聚合API` |
| `pages/ops-audit.html` | 操作审计 | `/quant/ops/audit` | `quant:audit:list` | `GET /api/quant/audit/list` |
| `pages/ops-permissions.html` | 量化权限 | `/quant/ops/permissions` | `system:role:list` | `RuoYi角色/菜单接口` |

## 产品原型验收重点

- 虚拟账户与真实账户有明显视觉标签。
- PAPER、SHADOW、READ_ONLY、LIVE执行模式明确。
- 真实下单、熔断、恢复、接入配置提供二次确认入口。
- 财报分析显示历史口径、行业样本、模型和输入快照。
- 回测显示数据快照、标的宇宙、复权、费率、滑点和日历版本。
- 运维任务页面仅展示XXL-JOB，不提供SnailJob运行入口。
- 所有原型数据均为模拟数据，不会发送真实订单。
