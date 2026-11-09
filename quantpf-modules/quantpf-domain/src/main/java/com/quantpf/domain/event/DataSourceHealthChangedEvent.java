package com.quantpf.domain.event;

import lombok.Getter;

import java.io.Serial;

/**
 * 数据源健康状态已变更事件（《详设_1》§16.3 DataSourceHealthChangedEvent）。
 * <p>来源组核验后（主备切换/部分可用/冲突/不可用）发布；消费端应据此调整采集依赖与降级策略，
 * 并暂停依赖不可用来源的新决策（详设_1 §4.3 降级矩阵）。</p>
 */
@Getter
public class DataSourceHealthChangedEvent extends QuantDomainEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 来源组ID */
    private final Long sourceGroupId;

    /** 来源组编码 */
    private final String groupCode;

    /** 核验后的来源组状态（SourceGroupStatus） */
    private final String groupStatus;

    /** 是否发生了主备切换 */
    private final boolean primarySwitched;

    public DataSourceHealthChangedEvent(Long sourceGroupId, String groupCode, String groupStatus, boolean primarySwitched) {
        super("marketdata");
        this.sourceGroupId = sourceGroupId;
        this.groupCode = groupCode;
        this.groupStatus = groupStatus;
        this.primarySwitched = primarySwitched;
    }

    @Override
    public String getAggregateType() {
        return "SourceGroup";
    }

    @Override
    public String getAggregateId() {
        return String.valueOf(sourceGroupId);
    }
}
