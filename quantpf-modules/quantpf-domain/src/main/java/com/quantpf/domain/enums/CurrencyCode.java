package com.quantpf.domain.enums;

/**
 * 币种代码（《详设_1 §12.3.1》）。
 * <p>对应数据库列 {@code quantpf_trading_calendar.currency}。</p>
 */
public enum CurrencyCode implements QuantDictEnum {

    CNY("人民币"),
    USD("美元"),
    HKD("港币");

    private final String desc;

    CurrencyCode(String desc) {
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
        for (CurrencyCode item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}
