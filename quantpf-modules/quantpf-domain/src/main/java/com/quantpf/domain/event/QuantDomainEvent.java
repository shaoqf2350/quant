package com.quantpf.domain.event;

import cn.hutool.core.util.IdUtil;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模块一领域事件基类（《详设_1》§20.3 / 指导文档 §6.6 事件契约）。
 * <p>统一契约字段：eventId / eventType / occurredAt / availableAt / schemaVersion / source；
 * 各事件类补齐自身幂等键与聚合标识（aggregateType / aggregateId）。</p>
 * <p>事件经 {@code @TransactionalEventListener(AFTER_COMMIT)} 消费；事务回滚不泄漏事件，
 * 重复幂等键不重复产生下游副作用。</p>
 */
@Getter
public abstract class QuantDomainEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 事件ID（每次发布唯一） */
    private final String eventId = IdUtil.fastSimpleUUID();

    /** Schema 版本（契约 contract/events/quantpf-*-event.v1.schema.json） */
    private final String schemaVersion = "1";

    /** 事件发生时间 */
    private final LocalDateTime occurredAt = LocalDateTime.now();

    /** 事件对下游可见时间（默认与发生时间一致；异步场景可覆写） */
    private final LocalDateTime availableAt = LocalDateTime.now();

    /** 事件发布方（模块名） */
    private final String source;

    protected QuantDomainEvent(String source) {
        this.source = source;
    }

    /** 事件类型（类名，与契约 eventType 枚举一致） */
    public String getEventType() {
        return getClass().getSimpleName();
    }

    /** 聚合对象类型（与契约 aggregateType 枚举一致） */
    public abstract String getAggregateType();

    /** 聚合对象ID（字符串化） */
    public abstract String getAggregateId();
}
