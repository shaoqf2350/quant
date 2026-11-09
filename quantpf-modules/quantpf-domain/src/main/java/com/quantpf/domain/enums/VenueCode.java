package com.quantpf.domain.enums;

/**
 * 交易场所代码（《详设_1 §12.3.1》）。
 * <p>对应数据库列 {@code quantpf_trading_calendar.venue_code}。</p>
 */
public enum VenueCode implements QuantDictEnum {

    SSE("上交所"),
    SZSE("深交所"),
    BSE("北交所"),
    HKEX("港交所"),
    SHFE("上期所"),
    CFFEX("中金所"),
    CZCE("郑商所"),
    DCE("大商所"),
    INE("上期能源"),
    GFEX("广期所");

    private final String desc;

    VenueCode(String desc) {
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
        for (VenueCode item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}
