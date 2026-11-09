-- =====================================================================
-- QuantPF 模块一：既有表主键统一纠正（指导文档 §5.4 规划的 V1_10_1 三步式落地）
-- 纠正范围（模块一施工边界内，模块四 Paper 表另案联动）：
--   quantpf_instrument / quantpf_market_bar / quantpf_trading_calendar
-- 三步式（保留 id 兼容列，二次迁移统一删除）：
--   1) 新增 pk_id 列（BIGINT UNSIGNED，与既有 instrument_id 外键列类型一致）；
--   2) 回填 pk_id = id（存量数据原值迁移，无业务含义变化）；
--   3) 切换主键：pk_id 升为主键，旧列 id 保留 AUTO_INCREMENT + UNIQUE（兼容既有 DDD
--      代码读取与 RETURN_GENERATED_KEYS 插入，见 PaperPersistenceTransactionIntegrationTest）；
--      模块一新表外键切换为引用 pk_id（V1_11_0/V1_13_0/V1_14_0 共 7 处），
--      既有 V1_1_0~V1_9_0 外键保留引用 id（兼容窗口，见下）。
-- 说明：
--   * 旧列 id 保留一个版本以兼容既有 DDD 代码读取（指导文档 §5.4 施工决策点），
--     二次迁移（Paper 域主键适配完成后）统一删除；
--   * quantpf_trading_calendar 为被 quantpf_calendar_def 取代的死表（V1_11_0 注释），
--     仅做主键统一以保持"业务表主键统一 pk_id"约束，不再承载业务。
-- =====================================================================

-- ---------- quantpf_instrument ----------

-- 1. 新增 pk_id 列
ALTER TABLE quantpf_instrument
    ADD COLUMN pk_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '主键（雪花ID，与旧 id 同值回填）' AFTER id;

-- 2. 回填存量
UPDATE quantpf_instrument SET pk_id = id WHERE pk_id = 0;

-- 3. 切换主键（id 先加 UNIQUE 再摘主键，AUTO_INCREMENT 属性保留）
ALTER TABLE quantpf_instrument
    ADD UNIQUE KEY uk_instrument_legacy_id (id);
ALTER TABLE quantpf_instrument
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (pk_id);
-- 去除 DEFAULT 0：此后 INSERT 必须显式提供 pk_id（雪花），防止静默主键冲突
ALTER TABLE quantpf_instrument
    MODIFY COLUMN pk_id BIGINT UNSIGNED NOT NULL COMMENT '主键（雪花ID）';

-- 模块一新表外键切换为引用 pk_id（V1_11_0 ×2 / V1_13_0 ×2 / V1_14_0 ×3）
ALTER TABLE quantpf_instrument_version DROP FOREIGN KEY fk_instrument_version_instrument;
ALTER TABLE quantpf_instrument_version
    ADD CONSTRAINT fk_instrument_version_instrument FOREIGN KEY (instrument_id) REFERENCES quantpf_instrument (pk_id);
ALTER TABLE quantpf_instrument_calendar_binding DROP FOREIGN KEY fk_binding_instrument;
ALTER TABLE quantpf_instrument_calendar_binding
    ADD CONSTRAINT fk_binding_instrument FOREIGN KEY (instrument_id) REFERENCES quantpf_instrument (pk_id);
ALTER TABLE quantpf_corporate_action DROP FOREIGN KEY fk_corporate_action_instrument;
ALTER TABLE quantpf_corporate_action
    ADD CONSTRAINT fk_corporate_action_instrument FOREIGN KEY (instrument_id) REFERENCES quantpf_instrument (pk_id);
ALTER TABLE quantpf_contract_event DROP FOREIGN KEY fk_contract_event_instrument;
ALTER TABLE quantpf_contract_event
    ADD CONSTRAINT fk_contract_event_instrument FOREIGN KEY (instrument_id) REFERENCES quantpf_instrument (pk_id);
ALTER TABLE quantpf_market_state DROP FOREIGN KEY fk_market_state_instrument;
ALTER TABLE quantpf_market_state
    ADD CONSTRAINT fk_market_state_instrument FOREIGN KEY (instrument_id) REFERENCES quantpf_instrument (pk_id);
ALTER TABLE quantpf_capacity_assessment DROP FOREIGN KEY fk_capacity_instrument;
ALTER TABLE quantpf_capacity_assessment
    ADD CONSTRAINT fk_capacity_instrument FOREIGN KEY (instrument_id) REFERENCES quantpf_instrument (pk_id);
ALTER TABLE quantpf_quant_feature_snapshot DROP FOREIGN KEY fk_quant_feature_instrument;
ALTER TABLE quantpf_quant_feature_snapshot
    ADD CONSTRAINT fk_quant_feature_instrument FOREIGN KEY (instrument_id) REFERENCES quantpf_instrument (pk_id);

-- 既有表外键统一切换为引用 pk_id（V1_1_0 market_bar / V1_2_0 fin_statement+signal /
-- V1_4_0 position / V1_5_0 paper_order）：
-- 原因：主键统一后 instrument 的权威标识为 pk_id，新代码全部传递 pk_id 值；
-- 新插入行的自增 id 与 pk_id 不再同值，若外键继续引用 id 将拒绝合法的 pk_id 引用。
-- 存量数据兼容性：pk_id 由 id 同值回填，既有 instrument_id 值对新旧引用均有效。
ALTER TABLE quantpf_market_bar DROP FOREIGN KEY fk_quantpf_market_bar_instrument;
ALTER TABLE quantpf_market_bar
    ADD CONSTRAINT fk_quantpf_market_bar_instrument FOREIGN KEY (instrument_id) REFERENCES quantpf_instrument (pk_id);
ALTER TABLE quantpf_financial_statement DROP FOREIGN KEY fk_quantpf_fin_statement_instrument;
ALTER TABLE quantpf_financial_statement
    ADD CONSTRAINT fk_quantpf_fin_statement_instrument FOREIGN KEY (instrument_id) REFERENCES quantpf_instrument (pk_id);
ALTER TABLE quantpf_strategy_signal DROP FOREIGN KEY fk_quantpf_signal_instrument;
ALTER TABLE quantpf_strategy_signal
    ADD CONSTRAINT fk_quantpf_signal_instrument FOREIGN KEY (instrument_id) REFERENCES quantpf_instrument (pk_id);
ALTER TABLE quantpf_position DROP FOREIGN KEY fk_quantpf_position_instrument;
ALTER TABLE quantpf_position
    ADD CONSTRAINT fk_quantpf_position_instrument FOREIGN KEY (instrument_id) REFERENCES quantpf_instrument (pk_id);
ALTER TABLE quantpf_paper_order DROP FOREIGN KEY fk_quantpf_order_instrument;
ALTER TABLE quantpf_paper_order
    ADD CONSTRAINT fk_quantpf_order_instrument FOREIGN KEY (instrument_id) REFERENCES quantpf_instrument (pk_id);

-- ---------- quantpf_market_bar ----------

ALTER TABLE quantpf_market_bar
    ADD COLUMN pk_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '主键（雪花ID，与旧 id 同值回填）' AFTER id;
UPDATE quantpf_market_bar SET pk_id = id WHERE pk_id = 0;
ALTER TABLE quantpf_market_bar
    ADD UNIQUE KEY uk_market_bar_legacy_id (id);
ALTER TABLE quantpf_market_bar
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (pk_id);
ALTER TABLE quantpf_market_bar
    MODIFY COLUMN pk_id BIGINT UNSIGNED NOT NULL COMMENT '主键（雪花ID）';

-- ---------- quantpf_trading_calendar（死表，仅统一主键） ----------

ALTER TABLE quantpf_trading_calendar
    ADD COLUMN pk_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '主键（雪花ID，与旧 id 同值回填）' AFTER id;
UPDATE quantpf_trading_calendar SET pk_id = id WHERE pk_id = 0;
ALTER TABLE quantpf_trading_calendar
    ADD UNIQUE KEY uk_legacy_calendar_id (id);
ALTER TABLE quantpf_trading_calendar
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (pk_id);
ALTER TABLE quantpf_trading_calendar
    MODIFY COLUMN pk_id BIGINT UNSIGNED NOT NULL COMMENT '主键（雪花ID）';
