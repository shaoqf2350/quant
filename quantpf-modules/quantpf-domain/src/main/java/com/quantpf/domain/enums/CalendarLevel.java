package com.quantpf.domain.enums;

/**
 * 日历级别（《详设_1 §12.3.1》）。
 * <p>对应数据库列 {@code quantpf_trading_calendar.calendar_level}。</p>
 */
public enum CalendarLevel implements QuantDictEnum {

    MARKET("市场"),
    EXCHANGE("交易所"),
    PRODUCT("产品"),
    INSTRUMENT("标的"),
    BROKER("经纪商");

    private final String desc;

    CalendarLevel(String desc) {
        this.desc = desc;
    }

    @Override
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
        for (CalendarLevel item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}
