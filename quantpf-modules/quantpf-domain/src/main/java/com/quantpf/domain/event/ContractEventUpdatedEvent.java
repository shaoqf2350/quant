package com.quantpf.domain.event;

import lombok.Getter;

import java.io.Serial;

/**
 * 期货合约事件已更新事件（《详设_1》§16.3 ContractEventUpdatedEvent）。
 * <p>合约事件（保证金/涨跌停/限仓/移仓/交割等）新增或变更后发布；下游须重算可开仓数量、保证金占用、
 * 组合压力测试、交易计划有效性与 Paper 风险资格（§13.4）。</p>
 */
@Getter
public class ContractEventUpdatedEvent extends QuantDomainEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 合约ID */
    private final Long instrumentId;

    /** 事件业务编码 */
    private final String eventCode;

    /** 事件类型 */
    private final String eventType;

    public ContractEventUpdatedEvent(Long instrumentId, String eventCode, String eventType) {
        super("marketdata");
        this.instrumentId = instrumentId;
        this.eventCode = eventCode;
        this.eventType = eventType;
    }

    @Override
    public String getAggregateType() {
        return "ContractEvent";
    }

    @Override
    public String getAggregateId() {
        return eventCode;
    }
}
