package com.quantpf.domain.event;

import lombok.Getter;

import java.io.Serial;

/**
 * 容量状态已变更事件（《详设_1》§16.3 CapacityStatusChangedEvent）。
 * <p>容量状态（SUFFICIENT/LIMITED/HIGH_IMPACT/UNTRADEABLE/UNKNOWN）变化后发布，供下游交易计划资格判断消费。</p>
 */
@Getter
public class CapacityStatusChangedEvent extends QuantDomainEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 标的ID */
    private final Long instrumentId;

    /** 容量状态 */
    private final String capacityStatus;

    public CapacityStatusChangedEvent(Long instrumentId, String capacityStatus) {
        super("marketdata");
        this.instrumentId = instrumentId;
        this.capacityStatus = capacityStatus;
    }

    @Override
    public String getAggregateType() {
        return "CapacityAssessment";
    }

    @Override
    public String getAggregateId() {
        return String.valueOf(instrumentId);
    }
}
