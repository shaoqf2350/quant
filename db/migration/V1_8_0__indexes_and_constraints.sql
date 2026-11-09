ALTER TABLE quantpf_instrument
    ADD KEY idx_quantpf_instrument_product_status (product_type, status),
    ADD KEY idx_quantpf_instrument_exchange (exchange_code);

ALTER TABLE quantpf_market_bar
    ADD KEY idx_quantpf_market_bar_time (bar_time);

ALTER TABLE quantpf_research_news
    ADD KEY idx_quantpf_news_published_at (published_at);

ALTER TABLE quantpf_strategy_signal
    ADD KEY idx_quantpf_signal_type_status (signal_type, status);

ALTER TABLE quantpf_paper_order
    ADD KEY idx_quantpf_order_instrument_created (instrument_id, created_at),
    ADD KEY idx_quantpf_order_request (request_id);

ALTER TABLE quantpf_paper_execution
    ADD KEY idx_quantpf_execution_order_time (order_id, executed_at);

ALTER TABLE quantpf_trading_halt
    ADD KEY idx_quantpf_halt_time (halted_at);

ALTER TABLE quantpf_audit_record
    ADD KEY idx_quantpf_audit_occurred_at (occurred_at);