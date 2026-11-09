package com.quantpf.domain.enums;

/**
 * 数据质量状态（《详设_1 §9.2》）。
 * <p>对应数据库列 {@code quantpf_data_quality_issue.quality_status}。</p>
 */
public enum DataQualityStatus implements QuantDictEnum {

    AVAILABLE("可用"),
    DELAYED("延迟"),
    PARTIAL("部分"),
    CONFLICTED("冲突"),
    UNAUTHORIZED("未授权"),
    STALE("过期"),
    INVALID("无效"),
    PENDING_REVIEW("待复核");

    private final String desc;

    DataQualityStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 按编码取状态描述，未知值返回原始编码（防止空指针）。
     *
     * @param code 状态编码
     * @return 状态中文描述；code 为空返回 null
     */
    public static String descOf(String code) {
        if (code == null) {
            return null;
        }
        for (DataQualityStatus item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}