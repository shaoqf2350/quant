-- ============================================================================
-- QuantPF 量化交易系统 MySQL 建库脚本（schema.sql）
-- 来源：量化交易系统完整设计方案 V21（§8 数据库表 DDL + 附录 A）
-- 目标库：quantpf（业务数据层主库）
-- 引擎：MySQL 8.0+
--
-- 使用方式：
--   mysql -uroot -p < schema.sql
--   或在 MySQL 客户端执行：source /path/to/schema.sql
--
-- ⚠️ 警告：本脚本包含 DROP TABLE IF EXISTS，重复执行会【删除并重建】同名表，
--          执行前请确认目标库中没有需要保留的数据！
--
-- 说明：
--   1. 共 29 张表，按数据分层分组：基础信息 / 交易 / 策略 / 标的池 / 回测 /
--      行情 / 配置 / 新闻风控 / 财报 / 智能体。
--   2. qs_kline_bar / qs_order_book / qs_tick_data 三张行情表在生产环境
--      建议迁移到时序库（TimescaleDB / DuckDB），此处 MySQL 版本用于热数据/降级。
--   3. 统一规范：主键 pk_id bigint；公共字段 create_by/update_by/create_time/
--      update_time/deleted；标的代码 symbol；数量 quantity；均价 avg_entry_price；
--      逻辑删除 deleted bigint default 0。
--   4. V21 新增：qm_trading_calendar / qm_trading_session（交易日历）与
--      qt_account（账户资金，虚拟/真实共用）。
-- ============================================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `quantpf` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `quantpf`;

-- ============================================================================
-- 1. 基础信息层
-- ============================================================================

-- 1.1 标的基础信息表
DROP TABLE IF EXISTS `qm_stock_basic`;
CREATE TABLE `qm_stock_basic` (
  `pk_id` bigint NOT NULL COMMENT '主键ID',
  `product_type` varchar(20) NOT NULL DEFAULT 'STOCK' COMMENT 'STOCK/FUTURE/SPOT',
  `exchange` varchar(20) NOT NULL COMMENT 'SSE/SZSE/CFFEX/SHFE/DCE/CZCE/INE',
  `symbol` varchar(32) NOT NULL COMMENT '交易代码（000001.SZ / IF2406）',
  `name` varchar(100) DEFAULT NULL COMMENT '名称',
  `industry` varchar(50) DEFAULT NULL COMMENT '行业',
  `sector` varchar(50) DEFAULT NULL COMMENT '板块/概念',
  `list_date` date DEFAULT NULL COMMENT '上市日期',
  `expire_date` date DEFAULT NULL COMMENT '到期日（期货）',
  `contract_multiplier` decimal(18,4) DEFAULT '1.0000' COMMENT '合约乘数',
  `tick_size` decimal(18,6) DEFAULT NULL COMMENT '最小变动价位',
  `lot_size` int DEFAULT NULL COMMENT '最小交易单位',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '0-停牌/退市，1-正常',
  `tags` varchar(500) DEFAULT NULL COMMENT '标签（ST,创业板等）',
  `business_scope` text COMMENT '经营范围摘要/品种说明',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL, `update_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `uk_product_symbol` (`product_type`, `symbol`),
  KEY `idx_exchange` (`exchange`), KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标的基础信息表';

-- 1.2 交易日历表（V21 新增）
DROP TABLE IF EXISTS `qm_trading_calendar`;
CREATE TABLE `qm_trading_calendar` (
  `pk_id` bigint NOT NULL,
  `exchange` varchar(20) NOT NULL COMMENT '交易所/市场：SSE/SZSE/CFFEX/SHFE/DCE/CZCE/INE',
  `trade_date` date NOT NULL COMMENT '交易日',
  `is_trading_day` tinyint NOT NULL DEFAULT '1' COMMENT '是否交易日：0-否，1-是',
  `is_weekend` tinyint NOT NULL DEFAULT '0' COMMENT '是否周末',
  `holiday_name` varchar(100) DEFAULT NULL COMMENT '休市原因（节假日名称）',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL, `update_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `uk_exchange_date` (`exchange`, `trade_date`),
  KEY `idx_trade_date` (`trade_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易日历表';

-- 1.3 交易时段表（V21 新增）
DROP TABLE IF EXISTS `qm_trading_session`;
CREATE TABLE `qm_trading_session` (
  `pk_id` bigint NOT NULL,
  `exchange` varchar(20) NOT NULL COMMENT '交易所/市场',
  `product_type` varchar(20) NOT NULL DEFAULT 'STOCK' COMMENT '适用品种',
  `session_no` int NOT NULL COMMENT '时段序号（1=早盘,2=午盘,3=夜盘）',
  `session_name` varchar(50) DEFAULT NULL COMMENT '时段名称',
  `start_time` time NOT NULL COMMENT '开始时间',
  `end_time` time NOT NULL COMMENT '结束时间',
  `is_night` tinyint NOT NULL DEFAULT '0' COMMENT '是否夜盘（跨日）',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL, `update_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `uk_exchange_product_session` (`exchange`, `product_type`, `session_no`),
  KEY `idx_product_type` (`product_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易时段表';

-- ============================================================================
-- 2. 交易层（订单 / 信号 / 持仓 / 成交 / 账户）
-- ============================================================================

-- 2.1 交易订单表
DROP TABLE IF EXISTS `qt_trade_order`;
CREATE TABLE `qt_trade_order` (
  `pk_id` bigint NOT NULL,
  `order_no` varchar(64) NOT NULL COMMENT '系统订单号',
  `outer_order_id` varchar(64) DEFAULT NULL COMMENT '券商返回订单号',
  `account_id` varchar(64) NOT NULL COMMENT '账户ID',
  `account_type` varchar(20) NOT NULL DEFAULT 'VIRTUAL' COMMENT 'VIRTUAL/REAL',
  `product_type` varchar(20) NOT NULL DEFAULT 'STOCK',
  `symbol` varchar(32) NOT NULL,
  `strategy_id` bigint DEFAULT NULL, `strategy_name` varchar(100) DEFAULT NULL,
  `signal_id` bigint DEFAULT NULL,
  `side` varchar(20) NOT NULL COMMENT 'BUY/SELL/BUY_OPEN/SELL_OPEN/BUY_CLOSE/SELL_CLOSE',
  `order_type` varchar(20) NOT NULL DEFAULT 'LIMIT' COMMENT 'LIMIT/MARKET/STOP/STOP_LIMIT',
  `price` decimal(18,6) DEFAULT NULL, `stop_price` decimal(18,6) DEFAULT NULL,
  `quantity` bigint NOT NULL DEFAULT '0' COMMENT '委托数量（股/手）',
  `filled_quantity` bigint NOT NULL DEFAULT '0', `filled_price` decimal(18,6) DEFAULT NULL,
  `commission` decimal(18,4) DEFAULT '0.0000',
  `order_source` varchar(20) DEFAULT 'SYSTEM' COMMENT 'SYSTEM/MANUAL',
  `broker_provider` varchar(32) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUBMITTED/PARTIAL_FILL/FILLED/CANCELLED/REJECTED/EXPIRED',
  `cancel_reason` varchar(200) DEFAULT NULL, `reject_reason` varchar(200) DEFAULT NULL,
  `submit_time` datetime DEFAULT NULL, `fill_time` datetime DEFAULT NULL,
  `cancel_time` datetime DEFAULT NULL, `expire_time` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL, `update_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_account` (`account_id`), KEY `idx_symbol` (`symbol`),
  KEY `idx_status` (`status`), KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易订单表';

-- 2.2 交易信号表
DROP TABLE IF EXISTS `qt_trade_signal`;
CREATE TABLE `qt_trade_signal` (
  `pk_id` bigint NOT NULL,
  `signal_no` varchar(64) NOT NULL,
  `product_type` varchar(20) NOT NULL DEFAULT 'STOCK',
  `symbol` varchar(32) NOT NULL,
  `strategy_id` bigint DEFAULT NULL, `strategy_name` varchar(100) DEFAULT NULL,
  `signal_type` varchar(32) NOT NULL COMMENT 'BUY/SELL/BUY_OPEN/SELL_OPEN/BUY_CLOSE/SELL_CLOSE/WATCH',
  `signal_strength` varchar(10) DEFAULT NULL COMMENT 'STRONG/MEDIUM/WEAK',
  `entry_price` decimal(18,6) DEFAULT NULL, `stop_loss` decimal(18,6) DEFAULT NULL,
  `take_profit` decimal(18,6) DEFAULT NULL, `risk_reward_ratio` decimal(5,2) DEFAULT NULL,
  `position_size` decimal(18,4) DEFAULT NULL COMMENT '建议仓位比例',
  `confidence` decimal(5,4) DEFAULT NULL,
  `reason` text, `metadata` json DEFAULT NULL,
  `risk_check_result` varchar(20) DEFAULT NULL COMMENT 'PASS/BLOCKED/REDUCE',
  `generate_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `trade_date` date DEFAULT NULL COMMENT '交易日（幂等去重）',
  `convert_to_order` tinyint NOT NULL DEFAULT '0', `order_no` varchar(64) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL, `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `uk_signal_no` (`signal_no`),
  UNIQUE KEY `uk_signal_dedup` (`symbol`, `strategy_name`, `signal_type`, `trade_date`),
  KEY `idx_symbol` (`symbol`), KEY `idx_generate_time` (`generate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易信号表';

-- 2.3 持仓表
DROP TABLE IF EXISTS `qt_position`;
CREATE TABLE `qt_position` (
  `pk_id` bigint NOT NULL,
  `account_id` varchar(64) NOT NULL,
  `account_type` varchar(20) NOT NULL DEFAULT 'VIRTUAL' COMMENT 'VIRTUAL/REAL',
  `broker_config_id` bigint DEFAULT NULL COMMENT '真实持仓关联 qs_broker_config.pk_id',
  `product_type` varchar(20) NOT NULL DEFAULT 'STOCK',
  `symbol` varchar(32) NOT NULL,
  `position_side` varchar(10) NOT NULL COMMENT 'LONG/SHORT',
  `quantity` bigint NOT NULL DEFAULT '0', `available_quantity` bigint NOT NULL DEFAULT '0',
  `frozen_quantity` bigint NOT NULL DEFAULT '0',
  `avg_entry_price` decimal(18,6) DEFAULT '0.000000',
  `total_cost` decimal(18,4) DEFAULT '0.0000', `market_price` decimal(18,6) DEFAULT NULL,
  `market_value` decimal(18,4) DEFAULT '0.0000', `unrealized_pnl` decimal(18,4) DEFAULT '0.0000',
  `realized_pnl` decimal(18,4) DEFAULT '0.0000',
  `stop_loss_price` decimal(18,6) DEFAULT NULL,
  `last_sync_time` datetime DEFAULT NULL, `last_update_time` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL, `update_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `uk_account_symbol_side` (`account_id`, `account_type`, `symbol`, `position_side`),
  KEY `idx_account` (`account_id`), KEY `idx_symbol` (`symbol`), KEY `idx_broker_config` (`broker_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='持仓表';

-- 2.4 成交记录表
DROP TABLE IF EXISTS `qt_trade_record`;
CREATE TABLE `qt_trade_record` (
  `pk_id` bigint NOT NULL,
  `trade_no` varchar(64) NOT NULL, `order_no` varchar(64) DEFAULT NULL,
  `product_type` varchar(20) NOT NULL DEFAULT 'STOCK', `symbol` varchar(32) NOT NULL,
  `side` varchar(20) NOT NULL,
  `price` decimal(18,6) NOT NULL, `quantity` bigint NOT NULL DEFAULT '0',
  `amount` decimal(18,4) DEFAULT NULL, `commission` decimal(18,4) DEFAULT '0.0000',
  `stamp_tax` decimal(18,4) DEFAULT '0.0000', `transfer_fee` decimal(18,4) DEFAULT '0.0000',
  `total_cost` decimal(18,4) DEFAULT '0.0000',
  `outer_trade_id` varchar(64) DEFAULT NULL, `outer_order_id` varchar(64) DEFAULT NULL,
  `fill_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `remark` varchar(500) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL, `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `uk_trade_no` (`trade_no`),
  KEY `idx_order_no` (`order_no`), KEY `idx_symbol` (`symbol`), KEY `idx_fill_time` (`fill_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成交记录表';

-- 2.5 账户资金表（虚拟/真实共用，V21 新增）
DROP TABLE IF EXISTS `qt_account`;
CREATE TABLE `qt_account` (
  `pk_id` bigint NOT NULL,
  `account_id` varchar(64) NOT NULL COMMENT '账户ID',
  `account_type` varchar(20) NOT NULL DEFAULT 'VIRTUAL' COMMENT 'VIRTUAL/REAL',
  `broker_config_id` bigint DEFAULT NULL COMMENT '真实账户关联 qs_broker_config.pk_id',
  `account_name` varchar(100) DEFAULT NULL COMMENT '账户名称',
  `product_type` varchar(20) DEFAULT NULL COMMENT '账户主要品种（空=多品种）',
  `initial_capital` decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '初始资金',
  `cash` decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '可用资金',
  `frozen_cash` decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '冻结资金（挂单占用）',
  `margin_used` decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '已用保证金（期货）',
  `market_value` decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '持仓市值',
  `total_asset` decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '总资产',
  `realized_pnl` decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '已实现盈亏',
  `unrealized_pnl` decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '浮动盈亏',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/FROZEN/CLOSED',
  `last_sync_time` datetime DEFAULT NULL COMMENT '最后同步时间',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL, `update_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `uk_account_id` (`account_id`),
  KEY `idx_account_type` (`account_type`), KEY `idx_broker_config` (`broker_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户资金表（虚拟/真实共用）';

-- ============================================================================
-- 3. 策略层（策略配置 / 参数版本）
-- ============================================================================

-- 3.1 策略配置表
DROP TABLE IF EXISTS `qs_strategy_config`;
CREATE TABLE `qs_strategy_config` (
  `pk_id` bigint NOT NULL,
  `strategy_code` varchar(50) NOT NULL, `strategy_name` varchar(100) NOT NULL,
  `strategy_type` varchar(32) NOT NULL COMMENT 'BREAKOUT/CHANLUN/TREND/MEAN_REVERT/CUSTOM',
  `product_types` varchar(200) DEFAULT 'STOCK' COMMENT '适用品种（逗号分隔）',
  `description` text, `params_json` json DEFAULT NULL, `risk_params_json` json DEFAULT NULL,
  `enabled` tinyint NOT NULL DEFAULT '0', `schedule_cron` varchar(50) DEFAULT NULL,
  `max_position_per_signal` decimal(5,2) DEFAULT '10.00' COMMENT '单信号最大仓位(%)',
  `max_daily_trades` int DEFAULT NULL, `version` int NOT NULL DEFAULT '1',
  `last_run_time` datetime DEFAULT NULL, `last_run_status` varchar(20) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL, `update_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `uk_strategy_code` (`strategy_code`),
  KEY `idx_enabled` (`enabled`), KEY `idx_strategy_type` (`strategy_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='策略配置表';

-- 3.2 策略参数配置表
DROP TABLE IF EXISTS `qs_strategy_param_config`;
CREATE TABLE `qs_strategy_param_config` (
  `pk_id` bigint NOT NULL,
  `strategy_code` varchar(50) NOT NULL, `param_version` int NOT NULL DEFAULT '1',
  `param_name` varchar(100) NOT NULL,
  `param_values` json DEFAULT NULL, `risk_param_values` json DEFAULT NULL,
  `is_active` tinyint NOT NULL DEFAULT '0',
  `backtest_result_id` varchar(64) DEFAULT NULL, `description` varchar(500) DEFAULT NULL,
  `operator` varchar(50) DEFAULT NULL, `operate_time` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL, `update_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `uk_strategy_version` (`strategy_code`, `param_version`),
  KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='策略参数配置表';

-- ============================================================================
-- 4. 标的池（汇总 / 明细）
-- ============================================================================

-- 4.1 标的池汇总表
DROP TABLE IF EXISTS `qs_stock_pool`;
CREATE TABLE `qs_stock_pool` (
  `pk_id` bigint NOT NULL,
  `trade_date` date NOT NULL,
  `total_count` int NOT NULL DEFAULT '0', `stock_count` int NOT NULL DEFAULT '0',
  `future_count` int NOT NULL DEFAULT '0', `spot_count` int NOT NULL DEFAULT '0',
  `breakout_count` int NOT NULL DEFAULT '0', `chanlun_count` int NOT NULL DEFAULT '0',
  `avg_confidence` decimal(5,4) DEFAULT NULL, `market_sentiment_score` decimal(5,2) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'COMPLETED' COMMENT 'COMPLETED/PROCESSING/FAILED',
  `screening_duration_ms` int DEFAULT NULL, `remark` varchar(500) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL, `update_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `uk_trade_date` (`trade_date`), KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标的池汇总表';

-- 4.2 标的池筛选明细表
DROP TABLE IF EXISTS `qs_stock_pool_detail`;
CREATE TABLE `qs_stock_pool_detail` (
  `pk_id` bigint NOT NULL,
  `pool_id` bigint NOT NULL, `product_type` varchar(20) NOT NULL DEFAULT 'STOCK',
  `symbol` varchar(32) NOT NULL, `stock_name` varchar(100) DEFAULT NULL,
  `pattern_type` varchar(50) NOT NULL COMMENT 'BREAKOUT/CHANLUN_*/MANUAL/AI_RECOMMEND',
  `pattern_types` varchar(200) DEFAULT NULL, `strategy_name` varchar(100) DEFAULT NULL,
  `confidence` decimal(5,4) DEFAULT NULL, `composite_score` decimal(5,2) DEFAULT NULL,
  `entry_price` decimal(18,6) DEFAULT NULL, `stop_loss` decimal(18,6) DEFAULT NULL,
  `target_price` decimal(18,6) DEFAULT NULL, `risk_reward_ratio` decimal(5,2) DEFAULT NULL,
  `screening_reason` text, `kline_analysis` json DEFAULT NULL,
  `ai_recommendation` varchar(20) DEFAULT NULL COMMENT 'STRONG_BUY/BUY/WATCH/AVOID',
  `ai_reasoning` text, `ai_risk_factors` json DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/TRADED/EXPIRED/REMOVED/STOP_LOSS',
  `screening_date` date NOT NULL, `expire_date` date DEFAULT NULL,
  `trade_order_no` varchar(64) DEFAULT NULL, `remove_reason` varchar(200) DEFAULT NULL,
  `next_day_change` decimal(10,4) DEFAULT NULL, `next_day_close` decimal(18,6) DEFAULT NULL,
  `manual_remark` varchar(500) DEFAULT NULL, `remark` varchar(500) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL, `update_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`pk_id`),
  KEY `idx_pool_id` (`pool_id`), KEY `idx_symbol` (`symbol`),
  KEY `idx_status` (`status`), KEY `idx_screening_date` (`screening_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标的池筛选明细表';

-- ============================================================================
-- 5. 回测（结果 / 交易明细 / 权益曲线）
-- ============================================================================

-- 5.1 回测结果表
DROP TABLE IF EXISTS `qs_backtest_result`;
CREATE TABLE `qs_backtest_result` (
  `pk_id` bigint NOT NULL,
  `backtest_id` varchar(64) NOT NULL, `strategy_name` varchar(100) NOT NULL,
  `strategy_params` json DEFAULT NULL, `period` varchar(20) NOT NULL COMMENT 'M15/H1/H2/DAILY/WEEKLY/MONTHLY',
  `product_type` varchar(20) NOT NULL DEFAULT 'STOCK', `symbols` json DEFAULT NULL,
  `start_date` date NOT NULL, `end_date` date NOT NULL,
  `initial_capital` decimal(18,4) NOT NULL,
  `commission_rate` decimal(8,6) NOT NULL DEFAULT '0.001000',
  `slippage_rate` decimal(8,6) NOT NULL DEFAULT '0.001000',
  `adjust_flag` varchar(20) DEFAULT 'FORWARD' COMMENT '复权口径 FORWARD/BACKWARD',
  `final_capital` decimal(18,4) DEFAULT NULL, `total_profit` decimal(18,4) DEFAULT NULL,
  `return_rate` decimal(10,6) DEFAULT NULL, `annualized_return` decimal(10,6) DEFAULT NULL,
  `peak_capital` decimal(18,4) DEFAULT NULL, `trough_capital` decimal(18,4) DEFAULT NULL,
  `max_drawdown` decimal(10,6) DEFAULT NULL, `sharpe_ratio` decimal(10,4) DEFAULT NULL,
  `annualized_volatility` decimal(10,6) DEFAULT NULL,
  `total_trades` int DEFAULT '0', `win_trades` int DEFAULT '0', `loss_trades` int DEFAULT '0',
  `win_rate` decimal(10,6) DEFAULT NULL, `avg_win` decimal(18,4) DEFAULT NULL,
  `avg_loss` decimal(18,4) DEFAULT NULL, `profit_loss_ratio` decimal(10,4) DEFAULT NULL,
  `days` int DEFAULT '0',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/COMPLETED/FAILED',
  `error_message` varchar(1000) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `complete_time` datetime DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `uk_backtest_id` (`backtest_id`),
  KEY `idx_strategy_name` (`strategy_name`), KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回测结果表';

-- 5.2 回测交易明细表
DROP TABLE IF EXISTS `qs_backtest_trade`;
CREATE TABLE `qs_backtest_trade` (
  `pk_id` bigint NOT NULL,
  `backtest_id` varchar(64) NOT NULL, `trade_no` int NOT NULL,
  `symbol` varchar(32) NOT NULL, `product_type` varchar(20) NOT NULL DEFAULT 'STOCK',
  `side` varchar(20) NOT NULL,
  `entry_price` decimal(18,6) NOT NULL, `entry_time` datetime NOT NULL,
  `quantity` decimal(18,4) NOT NULL, `entry_commission` decimal(18,4) DEFAULT NULL,
  `exit_price` decimal(18,6) DEFAULT NULL, `exit_time` datetime DEFAULT NULL,
  `exit_commission` decimal(18,4) DEFAULT NULL,
  `exit_reason` varchar(50) DEFAULT NULL COMMENT 'TAKE_PROFIT/STOP_LOSS/STRATEGY/FORCE_CLOSE',
  `profit` decimal(18,4) DEFAULT NULL, `return_rate` decimal(10,6) DEFAULT NULL,
  `holding_periods` int DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/CLOSED',
  `entry_reason` varchar(500) DEFAULT NULL, `remark` varchar(500) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`pk_id`),
  KEY `idx_backtest_id` (`backtest_id`), KEY `idx_symbol` (`symbol`), KEY `idx_entry_time` (`entry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回测交易明细表';

-- 5.3 回测权益曲线表
DROP TABLE IF EXISTS `qs_backtest_equity`;
CREATE TABLE `qs_backtest_equity` (
  `pk_id` bigint NOT NULL,
  `backtest_id` varchar(64) NOT NULL, `kline_time` datetime NOT NULL,
  `total_equity` decimal(18,4) NOT NULL, `cash` decimal(18,4) NOT NULL,
  `position_value` decimal(18,4) NOT NULL, `position_count` int NOT NULL DEFAULT '0',
  `drawdown` decimal(10,6) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `uk_backtest_kline` (`backtest_id`, `kline_time`),
  KEY `idx_kline_time` (`kline_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回测权益曲线表';

-- ============================================================================
-- 6. 行情数据层（K线 / 盘口 / 逐笔 / 导入日志）
-- 说明：K线/盘口/逐笔三表在生产环境建议迁移到时序库（TimescaleDB/DuckDB），
--       此处 MySQL 版本用于热数据/降级存储。
-- ============================================================================

-- 6.1 统一K线数据表
DROP TABLE IF EXISTS `qs_kline_bar`;
CREATE TABLE `qs_kline_bar` (
  `pk_id` bigint NOT NULL,
  `symbol` varchar(32) NOT NULL, `product_type` varchar(20) NOT NULL DEFAULT 'STOCK',
  `period` varchar(20) NOT NULL COMMENT 'M15/H1/H2/DAILY/WEEKLY/MONTHLY',
  `bar_time` datetime NOT NULL,
  `open_price` decimal(18,6) NOT NULL, `high_price` decimal(18,6) NOT NULL,
  `low_price` decimal(18,6) NOT NULL, `close_price` decimal(18,6) NOT NULL,
  `current_price` decimal(18,6) DEFAULT NULL,
  `volume` bigint NOT NULL DEFAULT '0', `amount` decimal(18,4) DEFAULT '0.0000',
  `open_interest` bigint DEFAULT NULL COMMENT '持仓量（期货）',
  `adjust_flag` varchar(20) DEFAULT 'FORWARD' COMMENT '复权口径',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `uk_symbol_period_time` (`symbol`, `period`, `bar_time`),
  KEY `idx_symbol_period` (`symbol`, `period`), KEY `idx_bar_time` (`bar_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一K线数据表';

-- 6.2 盘口数据快照表
DROP TABLE IF EXISTS `qs_order_book`;
CREATE TABLE `qs_order_book` (
  `pk_id` bigint NOT NULL,
  `symbol` varchar(32) NOT NULL, `product_type` varchar(20) NOT NULL DEFAULT 'STOCK',
  `data_time` datetime(3) NOT NULL,
  `last_price` decimal(18,6) DEFAULT NULL,
  `outer_volume` bigint DEFAULT '0', `inner_volume` bigint DEFAULT '0',
  `turnover_rate` decimal(10,4) DEFAULT NULL, `pe_ratio_dynamic` decimal(12,4) DEFAULT NULL,
  `total_bid_volume` bigint DEFAULT '0', `total_ask_volume` bigint DEFAULT '0',
  `bid_prices` json DEFAULT NULL, `bid_volumes` json DEFAULT NULL,
  `ask_prices` json DEFAULT NULL, `ask_volumes` json DEFAULT NULL,
  `order_delegate_info` json DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`pk_id`),
  KEY `idx_symbol_time` (`symbol`, `data_time`), KEY `idx_data_time` (`data_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘口数据快照表';

-- 6.3 逐笔成交数据表
DROP TABLE IF EXISTS `qs_tick_data`;
CREATE TABLE `qs_tick_data` (
  `pk_id` bigint NOT NULL,
  `symbol` varchar(32) NOT NULL, `product_type` varchar(20) NOT NULL DEFAULT 'STOCK',
  `tick_time` datetime(3) NOT NULL,
  `last_price` decimal(18,6) NOT NULL, `volume` bigint NOT NULL,
  `amount` decimal(18,4) DEFAULT NULL,
  `total_volume` bigint DEFAULT NULL, `total_amount` decimal(18,4) DEFAULT NULL,
  `open_interest` bigint DEFAULT NULL,
  `trade_direction` varchar(10) DEFAULT NULL COMMENT 'BUY/SELL/UNKNOWN',
  `exchange_time` datetime(3) DEFAULT NULL, `channel_no` int DEFAULT NULL,
  `biz_index` bigint DEFAULT NULL COMMENT '去重/排序索引',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`pk_id`),
  KEY `idx_symbol_tick_time` (`symbol`, `tick_time`), KEY `idx_tick_time` (`tick_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逐笔成交数据表';

-- 6.4 K线导入日志表
DROP TABLE IF EXISTS `qs_kline_import_log`;
CREATE TABLE `qs_kline_import_log` (
  `pk_id` bigint NOT NULL,
  `import_no` varchar(64) NOT NULL, `symbol` varchar(32) NOT NULL,
  `product_type` varchar(20) NOT NULL, `period` varchar(20) NOT NULL,
  `import_mode` varchar(20) NOT NULL COMMENT 'FULL/INCREMENTAL',
  `file_name` varchar(200) DEFAULT NULL,
  `total_rows` int DEFAULT '0', `success_rows` int DEFAULT '0',
  `skip_rows` int DEFAULT '0', `fail_rows` int DEFAULT '0',
  `error_detail` json DEFAULT NULL,
  `import_status` varchar(20) NOT NULL COMMENT 'SUCCESS/PARTIAL/FAILED',
  `operator` varchar(50) DEFAULT NULL, `import_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `remark` varchar(500) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `uk_import_no` (`import_no`),
  KEY `idx_symbol_period` (`symbol`, `period`), KEY `idx_import_time` (`import_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='K线导入日志表';

-- ============================================================================
-- 7. 配置层（经纪商接入）
-- ============================================================================

-- 7.1 经纪商接入配置表
DROP TABLE IF EXISTS `qs_broker_config`;
CREATE TABLE `qs_broker_config` (
  `pk_id` bigint NOT NULL,
  `config_code` varchar(64) NOT NULL, `broker_type` varchar(20) NOT NULL COMMENT 'QMT/CTP',
  `broker_name` varchar(100) NOT NULL, `account_id` varchar(64) NOT NULL,
  `account_name` varchar(100) DEFAULT NULL, `product_type` varchar(20) NOT NULL DEFAULT 'STOCK',
  `qmt_server_ip` varchar(64) DEFAULT NULL, `qmt_server_port` int DEFAULT NULL,
  `qmt_account_id` varchar(64) DEFAULT NULL, `qmt_connect_mode` varchar(20) DEFAULT 'DIRECT',
  `ctp_front_td_addr` varchar(200) DEFAULT NULL, `ctp_front_md_addr` varchar(200) DEFAULT NULL,
  `ctp_broker_id` varchar(32) DEFAULT NULL, `ctp_user_id` varchar(32) DEFAULT NULL,
  `ctp_app_id` varchar(32) DEFAULT NULL, `ctp_auth_code` varchar(32) DEFAULT NULL,
  `ctp_td_protocol` int DEFAULT NULL, `ctp_md_protocol` int DEFAULT NULL,
  `password_encrypted` varchar(256) DEFAULT NULL COMMENT 'AES-256 加密',
  `td_enabled` tinyint NOT NULL DEFAULT '0', `md_enabled` tinyint NOT NULL DEFAULT '1',
  `last_connect_time` datetime DEFAULT NULL, `last_connect_status` varchar(20) DEFAULT NULL,
  `last_error_msg` varchar(500) DEFAULT NULL, `config_version` int NOT NULL DEFAULT '1',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL, `update_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `uk_config_code` (`config_code`),
  KEY `idx_broker_type` (`broker_type`), KEY `idx_account_id` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='经纪商接入配置表';

-- ============================================================================
-- 8. 新闻 / 风控层
-- ============================================================================

-- 8.1 原始新闻表
DROP TABLE IF EXISTS `qn_news_raw`;
CREATE TABLE `qn_news_raw` (
  `pk_id` bigint NOT NULL,
  `news_id` varchar(128) NOT NULL, `source` varchar(50) NOT NULL COMMENT 'CLS/SINA/XUEQIU/EASTMONEY',
  `source_url` varchar(500) DEFAULT NULL, `title` varchar(500) DEFAULT NULL,
  `content` mediumtext, `summary` varchar(1000) DEFAULT NULL,
  `publish_time` datetime DEFAULT NULL, `crawl_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `sentiment_score` decimal(5,4) DEFAULT NULL, `sentiment_label` varchar(20) DEFAULT NULL,
  `keywords` varchar(500) DEFAULT NULL, `affected_sectors` varchar(500) DEFAULT NULL,
  `risk_level` varchar(20) DEFAULT NULL, `stock_symbols` varchar(500) DEFAULT NULL,
  `raw_data` json DEFAULT NULL, `remark` varchar(500) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL, `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `uk_news_id_source` (`news_id`, `source`),
  KEY `idx_publish_time` (`publish_time`), KEY `idx_risk_level` (`risk_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原始新闻表';

-- 8.2 新闻情绪分析结果表
DROP TABLE IF EXISTS `qn_news_sentiment`;
CREATE TABLE `qn_news_sentiment` (
  `pk_id` bigint NOT NULL,
  `news_id` bigint NOT NULL COMMENT '关联 qn_news_raw.pk_id',
  `analysis_model` varchar(50) DEFAULT NULL, `model_version` varchar(20) DEFAULT NULL,
  `overall_sentiment` decimal(5,4) DEFAULT NULL, `confidence` decimal(5,4) DEFAULT NULL,
  `positive_score` decimal(5,4) DEFAULT NULL, `negative_score` decimal(5,4) DEFAULT NULL,
  `neutral_score` decimal(5,4) DEFAULT NULL,
  `key_phrases` json DEFAULT NULL, `risk_assessment` json DEFAULT NULL,
  `recommendation` varchar(500) DEFAULT NULL, `analyst_notes` text,
  `analyze_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `remark` varchar(500) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL, `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`pk_id`),
  KEY `idx_news_id` (`news_id`), KEY `idx_analyze_time` (`analyze_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新闻情绪分析结果表';

-- 8.3 风控日志表
DROP TABLE IF EXISTS `qn_risk_log`;
CREATE TABLE `qn_risk_log` (
  `pk_id` bigint NOT NULL,
  `log_no` varchar(64) NOT NULL, `cycle_id` varchar(64) DEFAULT NULL,
  `risk_rule_id` bigint DEFAULT NULL, `risk_rule_name` varchar(100) DEFAULT NULL,
  `risk_level` varchar(20) NOT NULL COMMENT 'LOW/MEDIUM/HIGH/CRITICAL',
  `risk_category` varchar(32) DEFAULT NULL COMMENT 'MARKET/LIQUIDITY/CREDIT/OPERATIONAL/NEWS',
  `trigger_condition` text, `action_taken` varchar(50) DEFAULT NULL COMMENT 'WARN/REDUCE/HALT/CLOSE_ALL',
  `affected_symbols` varchar(500) DEFAULT NULL, `detail_json` json DEFAULT NULL,
  `operator` varchar(50) DEFAULT NULL, `remark` varchar(500) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL, `update_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `uk_log_no` (`log_no`),
  KEY `idx_cycle_id` (`cycle_id`), KEY `idx_risk_level` (`risk_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控日志表';

-- ============================================================================
-- 9. 财报层（三大报表 / 因子 / AI解读 / 日历 / 同业对比）
-- ============================================================================

-- 9.1 三大报表原始数据表
DROP TABLE IF EXISTS `qf_financial_statement`;
CREATE TABLE `qf_financial_statement` (
  `pk_id` bigint NOT NULL,
  `symbol` varchar(32) NOT NULL, `company_name` varchar(100) DEFAULT NULL,
  `report_period` varchar(20) NOT NULL, `report_type` varchar(20) NOT NULL COMMENT 'Q1/H1/Q3/A',
  `statement_type` varchar(30) NOT NULL COMMENT 'BALANCE_SHEET/INCOME/CASH_FLOW',
  `publish_date` date DEFAULT NULL, `currency` varchar(10) DEFAULT 'CNY', `unit` varchar(10) DEFAULT '万元',
  `data_json` json NOT NULL, `data_source` varchar(30) DEFAULT NULL COMMENT 'TUSHARE/EASTMONEY/CNINFO',
  `crawl_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `is_adjusted` tinyint NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL, `update_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `uk_symbol_period_type` (`symbol`, `report_period`, `statement_type`),
  KEY `idx_symbol` (`symbol`), KEY `idx_report_period` (`report_period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='三大报表原始数据表';

-- 9.2 基本面因子表
DROP TABLE IF EXISTS `qf_financial_factor`;
CREATE TABLE `qf_financial_factor` (
  `pk_id` bigint NOT NULL,
  `symbol` varchar(32) NOT NULL, `report_period` varchar(20) NOT NULL, `calc_date` date NOT NULL,
  `market_price` decimal(18,4) DEFAULT NULL, `market_cap` decimal(18,4) DEFAULT NULL,
  `roe` decimal(10,6) DEFAULT NULL, `roa` decimal(10,6) DEFAULT NULL,
  `gross_margin` decimal(10,6) DEFAULT NULL, `net_margin` decimal(10,6) DEFAULT NULL,
  `revenue` decimal(18,4) DEFAULT NULL, `revenue_yoy` decimal(10,6) DEFAULT NULL,
  `net_profit` decimal(18,4) DEFAULT NULL, `net_profit_yoy` decimal(10,6) DEFAULT NULL,
  `debt_ratio` decimal(10,6) DEFAULT NULL, `current_ratio` decimal(10,4) DEFAULT NULL,
  `operating_cash_flow` decimal(18,4) DEFAULT NULL, `free_cash_flow` decimal(18,4) DEFAULT NULL,
  `ocf_to_net_profit` decimal(10,4) DEFAULT NULL,
  `pe_ttm` decimal(10,4) DEFAULT NULL, `pb` decimal(10,4) DEFAULT NULL,
  `ar_to_revenue` decimal(10,6) DEFAULT NULL, `goodwill_to_equity` decimal(10,6) DEFAULT NULL,
  `factor_quality_score` decimal(5,2) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL, `update_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `uk_symbol_period_date` (`symbol`, `report_period`, `calc_date`),
  KEY `idx_symbol` (`symbol`), KEY `idx_calc_date` (`calc_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基本面因子表';

-- 9.3 AI财报解读结果表
DROP TABLE IF EXISTS `qf_finance_analysis`;
CREATE TABLE `qf_finance_analysis` (
  `pk_id` bigint NOT NULL,
  `analysis_no` varchar(64) NOT NULL, `symbol` varchar(32) NOT NULL,
  `company_name` varchar(100) DEFAULT NULL, `report_period` varchar(20) NOT NULL,
  `report_type` varchar(20) NOT NULL,
  `ai_model` varchar(50) DEFAULT NULL, `ai_signal` varchar(20) NOT NULL COMMENT 'BULLISH/NEUTRAL/BEARISH',
  `confidence` decimal(5,4) DEFAULT NULL, `financial_health_score` decimal(5,2) DEFAULT NULL,
  `key_findings` json DEFAULT NULL, `anomaly_signals` json DEFAULT NULL,
  `peer_comparison` json DEFAULT NULL, `reasoning` text, `recommendation` varchar(500) DEFAULT NULL,
  `composite_score` decimal(5,2) DEFAULT NULL, `factor_snapshot` json DEFAULT NULL,
  `analysis_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `analysis_duration_ms` int DEFAULT NULL,
  `token_usage` json DEFAULT NULL, `status` varchar(20) NOT NULL DEFAULT 'COMPLETED',
  `error_message` varchar(1000) DEFAULT NULL, `remark` varchar(500) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL, `update_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `uk_analysis_no` (`analysis_no`),
  UNIQUE KEY `uk_symbol_period` (`symbol`, `report_period`),
  KEY `idx_symbol` (`symbol`), KEY `idx_analysis_time` (`analysis_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI财报解读结果表';

-- 9.4 财报发布日历表
DROP TABLE IF EXISTS `qf_earnings_calendar`;
CREATE TABLE `qf_earnings_calendar` (
  `pk_id` bigint NOT NULL,
  `symbol` varchar(32) NOT NULL, `company_name` varchar(100) DEFAULT NULL,
  `report_period` varchar(20) NOT NULL, `report_type` varchar(20) NOT NULL COMMENT 'PRELIMINARY/EXPRESS/FORMAL',
  `expected_publish_date` date DEFAULT NULL, `actual_publish_date` date DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PUBLISHED/ANALYZED',
  `pre_alert_triggered` tinyint NOT NULL DEFAULT '0', `auto_analyze_triggered` tinyint NOT NULL DEFAULT '0',
  `analysis_id` bigint DEFAULT NULL, `remark` varchar(500) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL, `update_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `uk_symbol_period_type` (`symbol`, `report_period`, `report_type`),
  KEY `idx_expected_date` (`expected_publish_date`), KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财报发布日历表';

-- 9.5 同业对比表
DROP TABLE IF EXISTS `qf_peer_comparison`;
CREATE TABLE `qf_peer_comparison` (
  `pk_id` bigint NOT NULL,
  `symbol` varchar(32) NOT NULL, `report_period` varchar(20) NOT NULL,
  `sector` varchar(50) DEFAULT NULL,
  `peer_total` int DEFAULT NULL, `rank` int DEFAULT NULL, `percentile` decimal(5,2) DEFAULT NULL,
  `pe_rank` int DEFAULT NULL, `pb_rank` int DEFAULT NULL,
  `roe_rank` int DEFAULT NULL, `gross_margin_rank` int DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL, `update_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`pk_id`),
  UNIQUE KEY `uk_symbol_period` (`symbol`, `report_period`),
  KEY `idx_sector` (`sector`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='同业对比表';

-- ============================================================================
-- 10. 智能体层（外部 Agent 平台 KEY 状态）
-- ============================================================================

-- 10.1 智能体使用周期标识状态表
DROP TABLE IF EXISTS `qa_agent_key_status`;
CREATE TABLE `qa_agent_key_status` (
  `pk_id` bigint NOT NULL,
  `assigned_ref` varchar(108) DEFAULT NULL COMMENT 'AGENT_用户ID_模型编码',
  `api_key` varchar(88) DEFAULT NULL, `account_type` varchar(24) DEFAULT NULL,
  `tenant_id` varchar(32) DEFAULT NULL, `pool_id` varchar(32) DEFAULT NULL,
  `services` varchar(1500) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '0-关闭,1-启用',
  `apply_time` datetime DEFAULT NULL, `startup_time` datetime DEFAULT NULL,
  `shutdown_time` datetime DEFAULT NULL, `remark` varchar(500) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL, `update_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`pk_id`),
  KEY `idx_assigned_ref` (`assigned_ref`), KEY `idx_api_key` (`api_key`), KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体使用周期标识状态表';

-- ============================================================================
-- 完成。共 29 张表。
-- 如需建索引/分表的额外优化（如 qs_order_book/qs_tick_data 按月分表），
-- 请参考设计方案 V21 §7 数据分层存储架构。
-- ============================================================================
