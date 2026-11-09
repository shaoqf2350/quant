package com.quantpf.domain.enums;

/**
 * 日历冲突状态（《详设_1 §12.2.2》）。
 * <p>对应数据库列 {@code quantpf_calendar_conflict.status}，冲突不静默择一，须人工覆盖。</p>
 */
public enum CalendarConflictStatus implements QuantDictEnum {

    OPEN("待处理"),
    REVIEWED("已复核"),
    OVERRIDDEN("已覆盖"),
    CLOSED("已关闭");

    private final String desc;

    CalendarConflictStatus(String desc) {
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
        for (CalendarConflictStatus item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}