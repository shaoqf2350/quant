package com.quantpf.domain.event;

import lombok.Getter;

import java.io.Serial;

/**
 * 市场状态已变更事件（《详设_1》§16.3 MarketStateChangedEvent）。
 * <p>市场状态（趋势/波动/流动性/系统性风险等）变化后发布，供下游策略/风控消费。</p>
 */
@Getter
public class MarketStateChangedEvent extends QuantDomainEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 标的ID（NULL=市场级） */
    private final Long instrumentId;

    /** 市场状态 */
    private final String stateCode;

    public MarketStateChangedEvent(Long instrumentId, String stateCode) {
        super("marketdata");
        this.instrumentId = instrumentId;
        this.stateCode = stateCode;
    }

    @Override
    public String getAggregateType() {
        return "MarketState";
    }

    @Override
    public String getAggregateId() {
        return instrumentId == null ? "MARKET" : String.valueOf(instrumentId);
    }
}
