package com.quantpf.domain.event;

import lombok.Getter;

import java.io.Serial;

/**
 * 研究事件已发布事件（《详设_1》§10.2 最后一步 / §16.3）。
 * <p>由 Spring Boot 在保存并校验研究事件后发布，供模块二（候选发现）等下游消费；
 * 幂等键保证重复发布不产生下游副作用（§12.1 验收门禁）。</p>
 */
@Getter
public class ResearchEventPublishedEvent extends QuantDomainEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 研究事件主键 */
    private final Long researchEventId;

    /** 研究事件业务ID */
    private final String eventCode;

    /** 幂等键 */
    private final String idempotencyKey;

    public ResearchEventPublishedEvent(Long researchEventId, String eventCode, String idempotencyKey) {
        super("research");
        this.researchEventId = researchEventId;
        this.eventCode = eventCode;
        this.idempotencyKey = idempotencyKey;
    }

    @Override
    public String getAggregateType() {
        return "ResearchEvent";
    }

    @Override
    public String getAggregateId() {
        return String.valueOf(researchEventId);
    }
}
