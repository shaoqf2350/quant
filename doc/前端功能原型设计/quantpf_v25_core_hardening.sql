-- ============================================================================
-- QuantPF V24 P0 核心硬化迁移（MySQL 8.0+ / Flyway 内容）
-- 文件建议：db/migration/V24__core_hardening.sql
-- 前置：连接已选择 quantpf 数据库；V22/V23 基础表已完成迁移。
-- 本文件禁止 CREATE DATABASE、USE、DROP TABLE；不覆盖业务数据。
-- 生产执行：由 Flyway/Liquibase/发布系统执行并记录版本，不手工 source。
-- ============================================================================
SET NAMES utf8mb4;

-- 1. 执行路由：防止 VIRTUAL/REAL、PAPER/LIVE 路由冲突
CREATE TABLE IF NOT EXISTS qt_execution_route (
  pk_id BIGINT NOT NULL AUTO_INCREMENT,
  route_code VARCHAR(80) NOT NULL,
  account_type VARCHAR(20) NOT NULL COMMENT 'VIRTUAL/REAL',
  product_type VARCHAR(20) NOT NULL COMMENT 'STOCK/FUTURE/SPOT',
  broker_provider VARCHAR(32) NOT NULL COMMENT 'PAPER/QMT/CTP/SPOT_HTTP',
  execution_mode VARCHAR(20) NOT NULL COMMENT 'PAPER/SHADOW/READ_ONLY/LIVE',
  adapter_name VARCHAR(128) NOT NULL,
  route_version INT NOT NULL DEFAULT 1,
  enabled TINYINT NOT NULL DEFAULT 0,
  environment_code VARCHAR(32) NOT NULL DEFAULT 'dev',
  effective_time DATETIME DEFAULT NULL,
  expire_time DATETIME DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  create_by BIGINT DEFAULT NULL,
  update_by BIGINT DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY(pk_id),
  UNIQUE KEY uk_route_code(route_code),
  UNIQUE KEY uk_route_key(account_type,product_type,broker_provider,execution_mode,environment_code,route_version),
  KEY idx_route_enabled(enabled,environment_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='QuantPF V24 执行路由';

-- 2. 订单不可变事件流水：支持乱序、重复、断线补拉和审计
CREATE TABLE IF NOT EXISTS qt_order_event (
  pk_id BIGINT NOT NULL AUTO_INCREMENT,
  order_no VARCHAR(64) NOT NULL,
  outer_order_id VARCHAR(128) DEFAULT NULL,
  outer_trade_id VARCHAR(128) DEFAULT NULL,
  outer_event_id VARCHAR(128) DEFAULT NULL,
  event_type VARCHAR(40) NOT NULL,
  from_status VARCHAR(30) DEFAULT NULL,
  to_status VARCHAR(30) DEFAULT NULL,
  payload_json JSON NOT NULL,
  idempotency_key VARCHAR(160) NOT NULL,
  source VARCHAR(30) NOT NULL DEFAULT 'BROKER',
  trace_id VARCHAR(80) DEFAULT NULL,
  occurred_time DATETIME(3) DEFAULT NULL,
  received_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(pk_id),
  UNIQUE KEY uk_order_event_idempotency(idempotency_key),
  KEY idx_order_event(order_no,received_time),
  KEY idx_outer_order(outer_order_id),
  KEY idx_outer_trade(outer_trade_id),
  KEY idx_event_type(event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='QuantPF V24 订单事件流水';

-- 3. Outbox：本地事务提交后异步发送外部网关
CREATE TABLE IF NOT EXISTS qt_outbox_event (
  pk_id BIGINT NOT NULL AUTO_INCREMENT,
  event_id VARCHAR(80) NOT NULL,
  aggregate_type VARCHAR(40) NOT NULL,
  aggregate_id VARCHAR(80) NOT NULL,
  event_type VARCHAR(80) NOT NULL,
  payload_json JSON NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENDING/SENT/FAILED/DEAD',
  retry_count INT NOT NULL DEFAULT 0,
  next_retry_time DATETIME DEFAULT NULL,
  last_error VARCHAR(1000) DEFAULT NULL,
  trace_id VARCHAR(80) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(pk_id),
  UNIQUE KEY uk_outbox_event_id(event_id),
  KEY idx_outbox_status(status,next_retry_time),
  KEY idx_outbox_aggregate(aggregate_type,aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='QuantPF V24 Outbox 事件';

-- 4. 交易日上下文：夜盘统一归属交易所 trading_date
CREATE TABLE IF NOT EXISTS qm_trading_day_context (
  pk_id BIGINT NOT NULL AUTO_INCREMENT,
  exchange VARCHAR(20) NOT NULL,
  product_type VARCHAR(20) NOT NULL,
  calendar_date DATE NOT NULL,
  trading_date DATE NOT NULL,
  session_id BIGINT DEFAULT NULL,
  session_start DATETIME NOT NULL,
  session_end DATETIME NOT NULL,
  timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai',
  is_night TINYINT NOT NULL DEFAULT 0,
  is_trading TINYINT NOT NULL DEFAULT 1,
  source VARCHAR(30) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(pk_id),
  UNIQUE KEY uk_exchange_product_calendar(exchange,product_type,calendar_date,session_id),
  KEY idx_trading_date(exchange,product_type,trading_date),
  KEY idx_session_time(session_start,session_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='QuantPF V24 交易日上下文';

-- 5. 回测数据快照：保证回测结果可复现
CREATE TABLE IF NOT EXISTS qs_backtest_data_snapshot (
  pk_id BIGINT NOT NULL AUTO_INCREMENT,
  data_snapshot_id VARCHAR(80) NOT NULL,
  universe_version VARCHAR(80) NOT NULL,
  price_adjustment_method VARCHAR(20) NOT NULL COMMENT 'RAW/FORWARD/BACKWARD',
  corporate_action_version VARCHAR(80) DEFAULT NULL,
  fee_schedule_version VARCHAR(80) DEFAULT NULL,
  slippage_model_version VARCHAR(80) DEFAULT NULL,
  calendar_version VARCHAR(80) DEFAULT NULL,
  source_watermark JSON DEFAULT NULL,
  symbol_count INT NOT NULL DEFAULT 0,
  data_start_date DATE DEFAULT NULL,
  data_end_date DATE DEFAULT NULL,
  snapshot_hash VARCHAR(128) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'READY',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(pk_id),
  UNIQUE KEY uk_data_snapshot_id(data_snapshot_id),
  UNIQUE KEY uk_snapshot_hash(snapshot_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='QuantPF V24 回测数据快照';

-- 6. 账户/券商持仓对账
CREATE TABLE IF NOT EXISTS qt_broker_position_snapshot (
  pk_id BIGINT NOT NULL AUTO_INCREMENT,
  broker_config_id BIGINT NOT NULL,
  account_id VARCHAR(64) NOT NULL,
  account_type VARCHAR(20) NOT NULL,
  product_type VARCHAR(20) NOT NULL,
  symbol VARCHAR(32) NOT NULL,
  position_side VARCHAR(20) NOT NULL,
  quantity BIGINT NOT NULL DEFAULT 0,
  available_quantity BIGINT NOT NULL DEFAULT 0,
  avg_entry_price DECIMAL(18,6) DEFAULT NULL,
  market_price DECIMAL(18,6) DEFAULT NULL,
  snapshot_time DATETIME(3) NOT NULL,
  outer_position_id VARCHAR(128) DEFAULT NULL,
  raw_payload JSON DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(pk_id),
  KEY idx_broker_position(account_id,snapshot_time),
  KEY idx_broker_symbol(account_id,symbol,snapshot_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='券商持仓快照';

CREATE TABLE IF NOT EXISTS qt_account_reconciliation (
  pk_id BIGINT NOT NULL AUTO_INCREMENT,
  reconciliation_no VARCHAR(80) NOT NULL,
  account_id VARCHAR(64) NOT NULL,
  account_type VARCHAR(20) NOT NULL,
  broker_config_id BIGINT DEFAULT NULL,
  reconciliation_date DATE NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/MATCHED/DIFF/CONFIRMED',
  system_total_asset DECIMAL(18,4) DEFAULT NULL,
  broker_total_asset DECIMAL(18,4) DEFAULT NULL,
  asset_diff DECIMAL(18,4) DEFAULT NULL,
  position_diff_count INT NOT NULL DEFAULT 0,
  detail_json JSON DEFAULT NULL,
  confirmed_by BIGINT DEFAULT NULL,
  confirmed_time DATETIME DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(pk_id),
  UNIQUE KEY uk_reconciliation_no(reconciliation_no),
  UNIQUE KEY uk_account_reconciliation(account_id,reconciliation_date),
  KEY idx_reconciliation_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户资金和持仓对账';

-- 7. 多策略成交归因
CREATE TABLE IF NOT EXISTS qt_trade_allocation (
  pk_id BIGINT NOT NULL AUTO_INCREMENT,
  trade_no VARCHAR(64) NOT NULL,
  strategy_id BIGINT DEFAULT NULL,
  strategy_code VARCHAR(64) DEFAULT NULL,
  signal_id BIGINT DEFAULT NULL,
  allocation_ratio DECIMAL(12,8) NOT NULL DEFAULT 1,
  allocated_quantity BIGINT NOT NULL DEFAULT 0,
  allocated_amount DECIMAL(18,4) NOT NULL DEFAULT 0,
  allocated_fee DECIMAL(18,4) NOT NULL DEFAULT 0,
  attribution_version VARCHAR(40) NOT NULL DEFAULT 'V24.0',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(pk_id),
  UNIQUE KEY uk_trade_strategy(trade_no,strategy_code,attribution_version),
  KEY idx_allocation_strategy(strategy_code,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成交策略归因';

-- 8. 审计和数据质量
CREATE TABLE IF NOT EXISTS qa_audit_log (
  pk_id BIGINT NOT NULL AUTO_INCREMENT,
  audit_no VARCHAR(80) NOT NULL,
  operator_id BIGINT DEFAULT NULL,
  operator_name VARCHAR(100) DEFAULT NULL,
  action_code VARCHAR(80) NOT NULL,
  resource_type VARCHAR(50) NOT NULL,
  resource_id VARCHAR(100) DEFAULT NULL,
  risk_level VARCHAR(20) NOT NULL DEFAULT 'LOW',
  request_json JSON DEFAULT NULL,
  result_json JSON DEFAULT NULL,
  trace_id VARCHAR(80) DEFAULT NULL,
  client_ip VARCHAR(64) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(pk_id),
  UNIQUE KEY uk_audit_no(audit_no),
  KEY idx_audit_resource(resource_type,resource_id,create_time),
  KEY idx_audit_operator(operator_id,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='QuantPF V24 操作审计';

CREATE TABLE IF NOT EXISTS qm_data_quality_issue (
  pk_id BIGINT NOT NULL AUTO_INCREMENT,
  issue_no VARCHAR(80) NOT NULL,
  source_code VARCHAR(64) DEFAULT NULL,
  data_type VARCHAR(40) NOT NULL,
  product_type VARCHAR(20) DEFAULT NULL,
  symbol VARCHAR(32) DEFAULT NULL,
  issue_type VARCHAR(50) NOT NULL,
  severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
  issue_time DATETIME NOT NULL,
  detail_json JSON DEFAULT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/IGNORED/RESOLVED',
  resolved_by BIGINT DEFAULT NULL,
  resolved_time DATETIME DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(pk_id),
  UNIQUE KEY uk_issue_no(issue_no),
  KEY idx_quality_status(status,severity),
  KEY idx_quality_symbol(symbol,issue_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据质量问题';

-- 9. 对既有表补充 V24 路由/交易日/快照字段；迁移前提是基础表已存在。
SET @db := DATABASE();
SET @sql := (SELECT IF(COUNT(*)=0,'ALTER TABLE qt_trade_order ADD COLUMN execution_mode VARCHAR(20) NOT NULL DEFAULT ''PAPER'' COMMENT ''PAPER/SHADOW/READ_ONLY/LIVE''','SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='qt_trade_order' AND column_name='execution_mode'); PREPARE v FROM @sql; EXECUTE v; DEALLOCATE PREPARE v;
SET @sql := (SELECT IF(COUNT(*)=0,'ALTER TABLE qt_trade_order ADD COLUMN broker_provider VARCHAR(32) DEFAULT NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='qt_trade_order' AND column_name='broker_provider'); PREPARE v FROM @sql; EXECUTE v; DEALLOCATE PREPARE v;
SET @sql := (SELECT IF(COUNT(*)=0,'ALTER TABLE qt_trade_order ADD COLUMN adapter_name VARCHAR(128) DEFAULT NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='qt_trade_order' AND column_name='adapter_name'); PREPARE v FROM @sql; EXECUTE v; DEALLOCATE PREPARE v;
SET @sql := (SELECT IF(COUNT(*)=0,'ALTER TABLE qt_trade_order ADD COLUMN route_version INT DEFAULT NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='qt_trade_order' AND column_name='route_version'); PREPARE v FROM @sql; EXECUTE v; DEALLOCATE PREPARE v;
SET @sql := (SELECT IF(COUNT(*)=0,'ALTER TABLE qt_trade_order ADD COLUMN trading_date DATE DEFAULT NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='qt_trade_order' AND column_name='trading_date'); PREPARE v FROM @sql; EXECUTE v; DEALLOCATE PREPARE v;
SET @sql := (SELECT IF(COUNT(*)=0,'ALTER TABLE qt_trade_record ADD COLUMN account_id VARCHAR(64) DEFAULT NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='qt_trade_record' AND column_name='account_id'); PREPARE v FROM @sql; EXECUTE v; DEALLOCATE PREPARE v;
SET @sql := (SELECT IF(COUNT(*)=0,'ALTER TABLE qt_trade_record ADD COLUMN account_type VARCHAR(20) DEFAULT NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='qt_trade_record' AND column_name='account_type'); PREPARE v FROM @sql; EXECUTE v; DEALLOCATE PREPARE v;
SET @sql := (SELECT IF(COUNT(*)=0,'ALTER TABLE qt_trade_record ADD COLUMN strategy_id BIGINT DEFAULT NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='qt_trade_record' AND column_name='strategy_id'); PREPARE v FROM @sql; EXECUTE v; DEALLOCATE PREPARE v;
SET @sql := (SELECT IF(COUNT(*)=0,'ALTER TABLE qt_trade_record ADD COLUMN strategy_code VARCHAR(64) DEFAULT NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='qt_trade_record' AND column_name='strategy_code'); PREPARE v FROM @sql; EXECUTE v; DEALLOCATE PREPARE v;
SET @sql := (SELECT IF(COUNT(*)=0,'ALTER TABLE qt_trade_record ADD COLUMN signal_id BIGINT DEFAULT NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='qt_trade_record' AND column_name='signal_id'); PREPARE v FROM @sql; EXECUTE v; DEALLOCATE PREPARE v;
SET @sql := (SELECT IF(COUNT(*)=0,'ALTER TABLE qt_trade_record ADD COLUMN trading_date DATE DEFAULT NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='qt_trade_record' AND column_name='trading_date'); PREPARE v FROM @sql; EXECUTE v; DEALLOCATE PREPARE v;
SET @sql := (SELECT IF(COUNT(*)=0,'ALTER TABLE qt_trade_signal ADD COLUMN account_id VARCHAR(64) DEFAULT NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='qt_trade_signal' AND column_name='account_id'); PREPARE v FROM @sql; EXECUTE v; DEALLOCATE PREPARE v;
SET @sql := (SELECT IF(COUNT(*)=0,'ALTER TABLE qt_trade_signal ADD COLUMN product_type VARCHAR(20) NOT NULL DEFAULT ''STOCK''','SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='qt_trade_signal' AND column_name='product_type'); PREPARE v FROM @sql; EXECUTE v; DEALLOCATE PREPARE v;
SET @sql := (SELECT IF(COUNT(*)=0,'ALTER TABLE qt_trade_signal ADD COLUMN trading_date DATE DEFAULT NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='qt_trade_signal' AND column_name='trading_date'); PREPARE v FROM @sql; EXECUTE v; DEALLOCATE PREPARE v;

-- 10. 注意：quantpf_ui_menu 的同步由 MenuSyncService 完成。
-- 不在迁移脚本中硬编码 sys_menu/sys_role_menu 主键；以 menu_code + parent_code 做幂等同步。
-- ============================================================================
