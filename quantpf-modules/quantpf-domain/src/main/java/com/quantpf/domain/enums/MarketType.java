package com.quantpf.domain.enums;

/**
 * 市场类型（《详设_1 §12.3.1》）。
 * <p>对应数据库列 {@code quantpf_trading_calendar.market_type}。</p>
 */
public enum MarketType implements QuantDictEnum {

    STOCK("股票"),
    FUTURE("期货"),
    FX("外汇"),
    SPOT("现货"),
    INDEX("指数");

    private final String desc;

    MarketType(String desc) {
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
        for (MarketType item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}
