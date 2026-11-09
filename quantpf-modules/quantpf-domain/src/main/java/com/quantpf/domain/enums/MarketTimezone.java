package com.quantpf.domain.enums;

/**
 * 市场时区（《详设_1 §12.3.1》）。
 * <p>对应数据库列 {@code quantpf_trading_calendar.timezone}；存库值为 IANA 时区标识，
 * 因含特殊字符（{@code /}、{@code _}）无法作为枚举名，故覆写 {@link #value()}。</p>
 */
public enum MarketTimezone implements QuantDictEnum {

    ASIA_SHANGHAI("Asia/Shanghai", "北京时间"),
    GMT("GMT", "格林尼治时间"),
    AMERICA_NEW_YORK("America/New_York", "纽约时间"),
    EUROPE_LONDON("Europe/London", "伦敦时间");

    /** 存库值（IANA 时区标识） */
    private final String value;

    private final String desc;

    MarketTimezone(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    /**
     * 按存库值取描述，未知值返回原始值（防止空指针）。
     */
    public static String descOf(String code) {
        if (code == null) {
            return null;
        }
        for (MarketTimezone item : values()) {
            if (item.value.equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}
