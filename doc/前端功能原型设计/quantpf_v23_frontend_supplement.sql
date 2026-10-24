-- ============================================================================
-- QuantPF V23 前端菜单与前端能力补充脚本（MySQL 8.0+）
-- 生成日期：2026-09-30
-- 用途：前端菜单种子、页面权限、策略表现、账户权益快照、新闻关联、财报同业明细、
--       经纪商连接日志、策略运行日志、标的池结果评估，以及首页聚合查询所需索引。
-- 安全说明：本脚本不执行 DROP TABLE，不覆盖业务数据；菜单种子使用 code 幂等。
-- 落地位置建议：代码仓库 db/migration/V23__frontend_supplement.sql 或 sql/quantpf_v23_frontend_supplement.sql
-- 执行前：请确认 RuoYi-Vue-Plus 的 sys_menu 字段版本，并按文档 §V23.12 做菜单同步。
-- ============================================================================
SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS quantpf DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE quantpf;

-- 1. QuantPF 前端菜单事实源（适配不同若依版本；随后可同步至 sys_menu）
CREATE TABLE IF NOT EXISTS quantpf_ui_menu (
  pk_id BIGINT NOT NULL COMMENT '主键',
  menu_code VARCHAR(100) NOT NULL COMMENT '稳定菜单编码/权限编码',
  parent_code VARCHAR(100) DEFAULT NULL COMMENT '父菜单编码',
  menu_name VARCHAR(100) NOT NULL COMMENT '菜单名称',
  menu_type VARCHAR(20) NOT NULL DEFAULT 'MENU' COMMENT 'DIR/MENU/BUTTON',
  route_path VARCHAR(200) DEFAULT NULL,
  component VARCHAR(300) DEFAULT NULL,
  icon VARCHAR(100) DEFAULT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  permission_code VARCHAR(150) DEFAULT NULL,
  visible TINYINT NOT NULL DEFAULT 1,
  enabled TINYINT NOT NULL DEFAULT 1,
  external_link VARCHAR(500) DEFAULT NULL,
  meta_json JSON DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  create_by BIGINT DEFAULT NULL,
  update_by BIGINT DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY(pk_id), UNIQUE KEY uk_menu_code(menu_code), KEY idx_parent_code(parent_code), KEY idx_permission_code(permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='QuantPF V23 前端菜单事实源';

-- 2. 策略每日表现：支持模拟/真实策略收益、权益曲线和历史回撤
CREATE TABLE IF NOT EXISTS qs_strategy_performance_daily (
  pk_id BIGINT NOT NULL,
  strategy_id BIGINT DEFAULT NULL,
  strategy_code VARCHAR(64) NOT NULL,
  account_id VARCHAR(64) NOT NULL,
  account_type VARCHAR(20) NOT NULL DEFAULT 'VIRTUAL' COMMENT 'VIRTUAL/REAL',
  trade_date DATE NOT NULL,
  initial_equity DECIMAL(18,4) NOT NULL DEFAULT 0,
  ending_equity DECIMAL(18,4) NOT NULL DEFAULT 0,
  daily_profit DECIMAL(18,4) NOT NULL DEFAULT 0,
  daily_return DECIMAL(12,8) NOT NULL DEFAULT 0,
  cumulative_return DECIMAL(12,8) NOT NULL DEFAULT 0,
  peak_equity DECIMAL(18,4) NOT NULL DEFAULT 0,
  drawdown DECIMAL(12,8) NOT NULL DEFAULT 0,
  max_drawdown DECIMAL(12,8) NOT NULL DEFAULT 0,
  drawdown_days INT NOT NULL DEFAULT 0,
  recovery_days INT NOT NULL DEFAULT 0,
  trade_count INT NOT NULL DEFAULT 0,
  win_count INT NOT NULL DEFAULT 0,
  loss_count INT NOT NULL DEFAULT 0,
  win_rate DECIMAL(12,8) DEFAULT NULL,
  profit_loss_ratio DECIMAL(12,8) DEFAULT NULL,
  sharpe_ratio DECIMAL(12,8) DEFAULT NULL,
  volatility DECIMAL(12,8) DEFAULT NULL,
  position_ratio DECIMAL(12,8) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(pk_id), UNIQUE KEY uk_strategy_account_date(strategy_code,account_id,account_type,trade_date),
  KEY idx_performance_date(trade_date), KEY idx_performance_account(account_id,account_type), KEY idx_performance_drawdown(drawdown)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='策略每日表现与历史回撤';

-- 3. 账户权益快照：支持虚拟/真实账户资产曲线、对账与账户级回撤
CREATE TABLE IF NOT EXISTS qt_account_equity_snapshot (
  pk_id BIGINT NOT NULL,
  account_id VARCHAR(64) NOT NULL,
  account_type VARCHAR(20) NOT NULL DEFAULT 'VIRTUAL',
  snapshot_time DATETIME NOT NULL,
  trade_date DATE NOT NULL,
  cash DECIMAL(18,4) NOT NULL DEFAULT 0,
  frozen_cash DECIMAL(18,4) NOT NULL DEFAULT 0,
  margin_used DECIMAL(18,4) NOT NULL DEFAULT 0,
  market_value DECIMAL(18,4) NOT NULL DEFAULT 0,
  total_asset DECIMAL(18,4) NOT NULL DEFAULT 0,
  realized_pnl DECIMAL(18,4) NOT NULL DEFAULT 0,
  unrealized_pnl DECIMAL(18,4) NOT NULL DEFAULT 0,
  daily_pnl DECIMAL(18,4) NOT NULL DEFAULT 0,
  daily_return DECIMAL(12,8) NOT NULL DEFAULT 0,
  peak_asset DECIMAL(18,4) NOT NULL DEFAULT 0,
  drawdown DECIMAL(12,8) NOT NULL DEFAULT 0,
  position_count INT NOT NULL DEFAULT 0,
  sync_status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(pk_id), UNIQUE KEY uk_account_snapshot(account_id,snapshot_time),
  KEY idx_account_trade_date(account_id,account_type,trade_date), KEY idx_snapshot_time(snapshot_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户权益快照';

-- 4. 策略运行日志：支持策略运行监控、输入/信号/订单/拦截统计
CREATE TABLE IF NOT EXISTS qs_strategy_run_log (
  pk_id BIGINT NOT NULL,
  strategy_id BIGINT DEFAULT NULL,
  strategy_code VARCHAR(64) NOT NULL,
  run_type VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
  run_start_time DATETIME NOT NULL,
  run_end_time DATETIME DEFAULT NULL,
  run_status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
  input_count INT NOT NULL DEFAULT 0,
  signal_count INT NOT NULL DEFAULT 0,
  order_count INT NOT NULL DEFAULT 0,
  blocked_count INT NOT NULL DEFAULT 0,
  error_message VARCHAR(1000) DEFAULT NULL,
  run_detail_json JSON DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(pk_id), KEY idx_strategy_run(strategy_code,run_start_time), KEY idx_run_status(run_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='策略运行日志';

-- 5. 新闻-标的关系：避免在 qn_news_raw.stock_symbols 字符串中检索
CREATE TABLE IF NOT EXISTS qn_news_symbol_relation (
  pk_id BIGINT NOT NULL,
  news_id VARCHAR(128) NOT NULL,
  symbol VARCHAR(32) NOT NULL,
  product_type VARCHAR(20) NOT NULL DEFAULT 'STOCK',
  relation_type VARCHAR(30) NOT NULL DEFAULT 'AFFECTED',
  relevance_score DECIMAL(8,5) DEFAULT NULL,
  sentiment_score DECIMAL(8,5) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(pk_id), UNIQUE KEY uk_news_symbol(news_id,symbol,product_type), KEY idx_symbol_time(symbol,create_time), KEY idx_news_id(news_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新闻与标的关联';

-- 6. 新闻-行业关系
CREATE TABLE IF NOT EXISTS qn_news_sector_relation (
  pk_id BIGINT NOT NULL,
  news_id VARCHAR(128) NOT NULL,
  sector VARCHAR(100) NOT NULL,
  relation_type VARCHAR(30) NOT NULL DEFAULT 'AFFECTED',
  relevance_score DECIMAL(8,5) DEFAULT NULL,
  sentiment_score DECIMAL(8,5) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(pk_id), UNIQUE KEY uk_news_sector(news_id,sector), KEY idx_sector_time(sector,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新闻与行业关联';

-- 7. 同业实际指标明细：支持历史财报 + 行业数据综合分析
CREATE TABLE IF NOT EXISTS qf_peer_comparison_detail (
  pk_id BIGINT NOT NULL,
  symbol VARCHAR(32) NOT NULL,
  peer_symbol VARCHAR(32) NOT NULL,
  report_period VARCHAR(20) NOT NULL,
  sector VARCHAR(100) DEFAULT NULL,
  metric_code VARCHAR(50) NOT NULL,
  metric_value DECIMAL(18,8) DEFAULT NULL,
  peer_avg_value DECIMAL(18,8) DEFAULT NULL,
  peer_median_value DECIMAL(18,8) DEFAULT NULL,
  rank_no INT DEFAULT NULL,
  percentile DECIMAL(8,5) DEFAULT NULL,
  data_source VARCHAR(30) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(pk_id), UNIQUE KEY uk_peer_metric(symbol,peer_symbol,report_period,metric_code), KEY idx_peer_symbol_period(symbol,report_period), KEY idx_peer_sector(sector)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='同业对比实际指标明细';

-- 8. 经纪商连接日志：支持 QMT/CTP 断线、恢复、延迟诊断
CREATE TABLE IF NOT EXISTS qs_broker_connect_log (
  pk_id BIGINT NOT NULL,
  broker_config_id BIGINT NOT NULL,
  broker_type VARCHAR(20) NOT NULL COMMENT 'QMT/CTP',
  account_id VARCHAR(64) DEFAULT NULL,
  connect_type VARCHAR(20) NOT NULL COMMENT 'TD/MD/BOTH',
  connect_status VARCHAR(20) NOT NULL COMMENT 'CONNECTED/DISCONNECTED/FAILED',
  error_code VARCHAR(64) DEFAULT NULL,
  error_message VARCHAR(1000) DEFAULT NULL,
  connect_time DATETIME NOT NULL,
  disconnect_time DATETIME DEFAULT NULL,
  duration_ms INT DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(pk_id), KEY idx_broker_connect(broker_config_id,connect_time), KEY idx_connect_status(connect_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='QMT/CTP 连接日志';

-- 9. 数据源健康快照：支持数据源健康度前端页面
CREATE TABLE IF NOT EXISTS qm_data_source_health (
  pk_id BIGINT NOT NULL,
  source_code VARCHAR(64) NOT NULL,
  source_name VARCHAR(100) NOT NULL,
  data_type VARCHAR(30) NOT NULL,
  health_status VARCHAR(20) NOT NULL DEFAULT 'UP',
  last_success_time DATETIME DEFAULT NULL,
  last_failure_time DATETIME DEFAULT NULL,
  avg_latency_ms INT DEFAULT NULL,
  latest_latency_ms INT DEFAULT NULL,
  consecutive_failures INT NOT NULL DEFAULT 0,
  call_count_today INT NOT NULL DEFAULT 0,
  fallback_enabled TINYINT NOT NULL DEFAULT 0,
  is_primary TINYINT NOT NULL DEFAULT 0,
  error_message VARCHAR(1000) DEFAULT NULL,
  snapshot_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(pk_id), UNIQUE KEY uk_source_type(source_code,data_type), KEY idx_health_status(health_status), KEY idx_health_snapshot(snapshot_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源健康度';

-- 10. 标的池结果评估字段（兼容已有表：仅新增缺失字段）
SET @db := DATABASE();
SET @sql := (SELECT IF(COUNT(*)=0,'ALTER TABLE qs_stock_pool_detail ADD COLUMN next_day_return DECIMAL(12,8) DEFAULT NULL COMMENT ''T+1收益''','SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='qs_stock_pool_detail' AND column_name='next_day_return'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql := (SELECT IF(COUNT(*)=0,'ALTER TABLE qs_stock_pool_detail ADD COLUMN three_day_return DECIMAL(12,8) DEFAULT NULL COMMENT ''T+3收益''','SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='qs_stock_pool_detail' AND column_name='three_day_return'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql := (SELECT IF(COUNT(*)=0,'ALTER TABLE qs_stock_pool_detail ADD COLUMN five_day_return DECIMAL(12,8) DEFAULT NULL COMMENT ''T+5收益''','SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='qs_stock_pool_detail' AND column_name='five_day_return'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql := (SELECT IF(COUNT(*)=0,'ALTER TABLE qs_stock_pool_detail ADD COLUMN ten_day_return DECIMAL(12,8) DEFAULT NULL COMMENT ''T+10收益''','SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='qs_stock_pool_detail' AND column_name='ten_day_return'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql := (SELECT IF(COUNT(*)=0,'ALTER TABLE qs_stock_pool_detail ADD COLUMN max_favorable_excursion DECIMAL(12,8) DEFAULT NULL COMMENT ''最大有利偏移''','SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='qs_stock_pool_detail' AND column_name='max_favorable_excursion'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql := (SELECT IF(COUNT(*)=0,'ALTER TABLE qs_stock_pool_detail ADD COLUMN max_adverse_excursion DECIMAL(12,8) DEFAULT NULL COMMENT ''最大不利偏移''','SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='qs_stock_pool_detail' AND column_name='max_adverse_excursion'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql := (SELECT IF(COUNT(*)=0,'ALTER TABLE qs_stock_pool_detail ADD COLUMN hit_take_profit TINYINT NOT NULL DEFAULT 0 COMMENT ''是否命中止盈''','SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='qs_stock_pool_detail' AND column_name='hit_take_profit'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql := (SELECT IF(COUNT(*)=0,'ALTER TABLE qs_stock_pool_detail ADD COLUMN hit_stop_loss TINYINT NOT NULL DEFAULT 0 COMMENT ''是否命中止损''','SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='qs_stock_pool_detail' AND column_name='hit_stop_loss'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql := (SELECT IF(COUNT(*)=0,'ALTER TABLE qs_stock_pool_detail ADD COLUMN evaluation_status VARCHAR(20) NOT NULL DEFAULT ''PENDING'' COMMENT ''PENDING/COMPLETED''','SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='qs_stock_pool_detail' AND column_name='evaluation_status'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql := (SELECT IF(COUNT(*)=0,'ALTER TABLE qs_stock_pool_detail ADD COLUMN evaluation_time DATETIME DEFAULT NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='qs_stock_pool_detail' AND column_name='evaluation_time'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 11. 菜单种子：使用 INSERT ... ON DUPLICATE KEY UPDATE，重复执行安全
INSERT INTO quantpf_ui_menu(pk_id,menu_code,parent_code,menu_name,menu_type,route_path,component,icon,sort_no,permission_code,meta_json,remark) VALUES
(230000,'quant','', '量化交易系统','DIR','/quant','Layout','ri-stock-line',10,'quant:dashboard:view','{"title":"量化交易系统","noCache":true}','V23 根菜单'),
(230001,'quant:dashboard','quant','交易驾驶舱','MENU','dashboard','quant/dashboard/index','ri-dashboard-line',10,'quant:dashboard:view','{"title":"交易驾驶舱"}','首页总览'),
(230010,'quant:research','quant','研究与数据','DIR','research','Layout','ri-line-chart-line',20,'quant:instrument:list','{"title":"研究与数据"}','研究域目录'),
(230011,'quant:instrument','quant:research','标的中心','MENU','instrument','quant/instrument/index','ri-list-check-2',10,'quant:instrument:list','{"title":"标的中心"}','股票/期货/现货基础信息'),
(230012,'quant:market','quant:research','行情与数据管理','MENU','market','quant/market/index','ri-bar-chart-box-line',20,'quant:kline:list','{"title":"行情与数据管理"}','K线/盘口/逐笔/导入'),
(230013,'quant:news','quant:research','新闻与舆情','MENU','news','quant/news/index','ri-newspaper-line',30,'quant:news:list','{"title":"新闻与舆情"}','最新新闻与情绪分析'),
(230014,'quant:finance','quant:research','财报与基本面','MENU','finance','quant/finance/index','ri-file-chart-line',40,'quant:finance:list','{"title":"财报与基本面"}','财报/因子/同业/AI解读'),
(230015,'quant:pool','quant:research','标的池','MENU','pool','quant/pool/index','ri-filter-3-line',50,'quant:pool:list','{"title":"标的池"}','今日和历史标的池'),
(230020,'quant:strategy','quant','策略与交易','DIR','strategy','Layout','ri-flashlight-line',30,'quant:strategy:list','{"title":"策略与交易"}','策略和交易域目录'),
(230021,'quant:strategies','quant:strategy','策略管理','MENU','strategies','quant/strategy/index','ri-settings-3-line',10,'quant:strategy:list','{"title":"策略管理"}','策略配置/参数版本'),
(230022,'quant:backtest','quant:strategy','回测与绩效','MENU','backtest','quant/backtest/index','ri-pie-chart-2-line',20,'quant:backtest:list','{"title":"回测与绩效"}','回测/收益/回撤'),
(230023,'quant:positions','quant:strategy','持仓与账户','MENU','positions','quant/position/index','ri-safe-2-line',30,'quant:position:list','{"title":"持仓与账户"}','虚拟/真实账户和持仓'),
(230024,'quant:execution','quant:strategy','订单与成交','MENU','execution','quant/execution/index','ri-exchange-line',40,'quant:trade:list','{"title":"订单与成交"}','订单/成交/手动下单'),
(230030,'quant:ops','quant','运维配置','DIR','ops','Layout','ri-tools-line',40,'quant:monitor:list','{"title":"运维配置"}','接入/日历/监控/风控'),
(230031,'quant:broker','quant:ops','QMT / CTP 接入','MENU','broker','quant/broker/index','ri-links-line',10,'quant:broker:list','{"title":"QMT / CTP 接入"}','券商/期货连接配置'),
(230032,'quant:calendar','quant:ops','交易日历与任务','MENU','calendar','quant/calendar/index','ri-calendar-schedule-line',20,'quant:calendar:list','{"title":"交易日历与任务"}','交易日历/XXL-JOB'),
(230033,'quant:risk','quant:ops','风控中心','MENU','risk','quant/risk/index','ri-alarm-warning-line',30,'quant:risk:list','{"title":"风控中心"}','风控状态/日志/熔断'),
(230034,'quant:monitor','quant:ops','系统监控','MENU','monitor','quant/monitor/index','ri-pulse-line',40,'quant:monitor:list','{"title":"系统监控"}','服务/数据源/AI健康度'),
(230100,'quant:dashboard:refresh','quant:dashboard','刷新驾驶舱','BUTTON',NULL,NULL,NULL,1,'quant:dashboard:view','{}','按钮权限'),
(230101,'quant:trade:order','quant:execution','手动下单','BUTTON',NULL,NULL,NULL,1,'quant:trade:order','{}','虚拟和真实下单，真实账户需二次确认'),
(230102,'quant:real-trade:order','quant:execution','真实账户下单','BUTTON',NULL,NULL,NULL,2,'quant:real-trade:order','{"confirm":true,"audit":true}','高风险权限'),
(230103,'quant:risk:halt','quant:risk','紧急熔断','BUTTON',NULL,NULL,NULL,1,'quant:risk:halt','{"confirm":true,"audit":true}','高风险权限')
ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name),parent_code=VALUES(parent_code),route_path=VALUES(route_path),component=VALUES(component),permission_code=VALUES(permission_code),meta_json=VALUES(meta_json),update_time=CURRENT_TIMESTAMP,deleted=0;

-- 12. 建议 API 聚合视图（仅在基础表已经存在时执行；部署脚本可按项目实际字段调整）
-- 首页建议接口：GET /api/quant/dashboard/summary
-- 策略回撤：GET /api/quant/performance/strategy/{strategyCode}/drawdown
-- 账户曲线：GET /api/quant/account/{accountId}/equity
-- 新闻详情：GET /api/quant/news/{newsId}
-- 财报综合分析：GET /api/quant/finance/analysis/{symbol}?periods=12&withPeers=true
-- QMT/CTP 状态：GET /api/quant/broker-config/{id}/status

-- 13. RuoYi 菜单同步说明（不要盲目执行）
-- quantpf_ui_menu 是跨版本事实源。落地代码仓应由 MenuSyncService 按 menu_code 做 upsert 到 sys_menu，
-- 并将 parent_code 映射为 parent_id；不同 RuoYi 版本的 sys_menu 字段（is_frame/is_cache/visible/status 等）
-- 可能不同，禁止在未核对官方 DDL 前直接执行固定列 INSERT。
-- ============================================================================
