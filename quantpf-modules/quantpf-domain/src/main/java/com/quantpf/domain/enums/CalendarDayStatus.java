package com.quantpf.domain.enums;

/**
 * 交易日状态（《详设_1 §12.3.2》）。
 * <p>对应数据库列 {@code quantpf_calendar_day.day_status}。</p>
 */
public enum CalendarDayStatus implements QuantDictEnum {

    OPEN("开市"),
    CLOSED("休市"),
    SPECIAL_OPEN("特殊开市"),
    SPECIAL_CLOSED("特殊休市"),
    UNKNOWN("未知"),
    CONFLICT("冲突");

    private final String desc;

    CalendarDayStatus(String desc) {
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
        for (CalendarDayStatus item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}