package com.quantpf.domain.enums;

/**
 * 日历来源接口目录（《详设_1 §12.2》施工期 Mock 适配）。
 * <p>对应交易日历导入的来源 Tushare 接口名；存库值为小写接口标识，因含下划线且需与既有
 * Mock 入参一致，故覆写 {@link #value()}。</p>
 */
public enum CalendarEndpoint implements QuantDictEnum {

    TRADE_CAL("trade_cal", "A股交易日历"),
    FUT_TRADE_CAL("fut_trade_cal", "期货交易日历"),
    HK_TRADECAL("hk_tradecal", "港股交易日历"),
    US_TRADECAL("us_tradecal", "美股交易日历");

    /** 存库值（Tushare 接口名） */
    private final String value;

    private final String desc;

    CalendarEndpoint(String value, String desc) {
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
        for (CalendarEndpoint item : values()) {
            if (item.value.equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}
