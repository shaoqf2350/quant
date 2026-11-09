package com.quantpf.domain.enums;

/**
 * 容量状态（《详设_1 §15.4》）。
 * <p>对应数据库列 {@code quantpf_capacity_assessment.capacity_status}。</p>
 * <p>LIMITED 限风险规模；HIGH_IMPACT 只允许观察/低规模 Paper；UNTRADEABLE 与 UNKNOWN 不得生成新开仓计划。</p>
 */
public enum CapacityStatus implements QuantDictEnum {

    SUFFICIENT("充足"),
    LIMITED("受限"),
    HIGH_IMPACT("高冲击"),
    UNTRADEABLE("不可交易"),
    UNKNOWN("未知");

    private final String desc;

    CapacityStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 按编码取描述，未知值返回原始编码（防止空指针）。
     */
    public static String descOf(String code) {
        if (code == null) {
            return null;
        }
        for (CapacityStatus item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}