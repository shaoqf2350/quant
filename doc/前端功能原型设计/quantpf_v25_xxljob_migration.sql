-- ============================================================================
-- QuantPF V25 XXL-JOB 业务侧迁移补充（MySQL 8.0+）
-- 说明：此脚本只处理 QuantPF 业务库中的任务审计/菜单事实源。
-- XXL-JOB Admin 自身表必须使用所冻结版本的官方 xxl-job.sql 导入独立 xxl_job 数据库。
-- 禁止在 QuantPF Flyway 中创建或维护 XXL-JOB Admin 表。
-- ============================================================================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS qo_job_execution (
  pk_id BIGINT NOT NULL AUTO_INCREMENT,
  execution_id VARCHAR(80) NOT NULL,
  handler_name VARCHAR(100) NOT NULL,
  job_param VARCHAR(2000) DEFAULT NULL,
  scheduled_time DATETIME(3) DEFAULT NULL,
  start_time DATETIME(3) NOT NULL,
  end_time DATETIME(3) DEFAULT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING/SUCCESS/FAILED/SKIPPED/PARTIAL/UNKNOWN',
  trading_date DATE DEFAULT NULL,
  idempotency_key VARCHAR(200) NOT NULL,
  retry_no INT NOT NULL DEFAULT 0,
  processed_count BIGINT NOT NULL DEFAULT 0,
  success_count BIGINT NOT NULL DEFAULT 0,
  failure_count BIGINT NOT NULL DEFAULT 0,
  error_code VARCHAR(64) DEFAULT NULL,
  error_message VARCHAR(2000) DEFAULT NULL,
  trace_id VARCHAR(80) DEFAULT NULL,
  xxl_log_id BIGINT DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (pk_id),
  UNIQUE KEY uk_job_execution_id (execution_id),
  UNIQUE KEY uk_handler_idempotency (handler_name, idempotency_key),
  KEY idx_job_status_time (handler_name, status, start_time),
  KEY idx_job_trading_date (trading_date, handler_name),
  KEY idx_xxl_log_id (xxl_log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='QuantPF XXL-JOB 业务执行记录';

CREATE TABLE IF NOT EXISTS qo_job_migration_audit (
  pk_id BIGINT NOT NULL AUTO_INCREMENT,
  migration_no VARCHAR(80) NOT NULL,
  handler_name VARCHAR(100) NOT NULL,
  source_scheduler VARCHAR(30) NOT NULL DEFAULT 'SNAIL_JOB',
  target_scheduler VARCHAR(30) NOT NULL DEFAULT 'XXL_JOB',
  source_identifier VARCHAR(200) DEFAULT NULL,
  target_identifier VARCHAR(200) DEFAULT NULL,
  migration_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/MIGRATED/VERIFIED/ROLLED_BACK',
  config_snapshot JSON DEFAULT NULL,
  result_hash VARCHAR(128) DEFAULT NULL,
  verified_by BIGINT DEFAULT NULL,
  verified_time DATETIME DEFAULT NULL,
  remark VARCHAR(1000) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (pk_id),
  UNIQUE KEY uk_job_migration_no (migration_no),
  UNIQUE KEY uk_job_handler_target (handler_name, target_scheduler),
  KEY idx_job_migration_status (migration_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SnailJob到XXL-JOB迁移审计';

-- quantpf_ui_menu 存在时，按稳定 menu_code 更新调度入口；不操作 RuoYi 数字主键。
UPDATE quantpf_ui_menu
SET menu_name = 'XXL-JOB 调度中心',
    route_path = 'xxljob',
    component = 'monitor/xxljob/index',
    permission_code = 'monitor:xxljob:list',
    external_link = '/xxl-job-admin/',
    meta_json = JSON_SET(COALESCE(meta_json, JSON_OBJECT()),
                         '$.title', 'XXL-JOB 调度中心',
                         '$.scheduler', 'XXL-JOB',
                         '$.version', '3.1.0'),
    remark = 'V25：由 SnailJob 2.0.2 迁移至 XXL-JOB 3.1.0',
    update_time = CURRENT_TIMESTAMP
WHERE menu_code IN ('monitor:snailjob', 'quant:job', 'quant:calendar')
   OR permission_code = 'monitor:snailjob:list';

-- 角色和 sys_menu 的实际同步由 MenuSyncService 完成：
-- 1. 查找旧 permission=monitor:snailjob:list；
-- 2. 更新 component/permission/path；
-- 3. 保持原角色授权关系；
-- 4. 写 qa_audit_log；
-- 5. 如发现同名 XXL-JOB 菜单冲突则停止，不静默覆盖。
-- ============================================================================
