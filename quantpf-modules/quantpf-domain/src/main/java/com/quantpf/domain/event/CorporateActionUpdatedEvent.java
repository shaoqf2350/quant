package com.quantpf.domain.event;

import lombok.Getter;

import java.io.Serial;

/**
 * 公司行为已更新事件（《详设_1》§16.3 CorporateActionUpdatedEvent）。
 * <p>公司行为（除权除息/停复牌/ST/退市等）新增或变更后发布，供下游候选过滤/策略/回测/风险重算消费。</p>
 */
@Getter
public class CorporateActionUpdatedEvent extends QuantDomainEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 标的ID */
    private final Long instrumentId;

    /** 行为业务编码 */
    private final String actionCode;

    /** 行为类型 */
    private final String actionType;

    public CorporateActionUpdatedEvent(Long instrumentId, String actionCode, String actionType) {
        super("marketdata");
        this.instrumentId = instrumentId;
        this.actionCode = actionCode;
        this.actionType = actionType;
    }

    @Override
    public String getAggregateType() {
        return "CorporateAction";
    }

    @Override
    public String getAggregateId() {
        return actionCode;
    }
}
