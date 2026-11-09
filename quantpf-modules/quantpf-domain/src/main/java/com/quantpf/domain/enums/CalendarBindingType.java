package com.quantpf.domain.enums;

/**
 * 标的/日历绑定类型（《详设_1 §12.3.4》）。
 * <p>对应数据库列 {@code quantpf_instrument_calendar_binding.binding_type}。</p>
 */
public enum CalendarBindingType {

    DEFAULT("默认"),
    OVERRIDE("覆盖");

    private final String desc;

    CalendarBindingType(String desc) {
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
        for (CalendarBindingType item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}