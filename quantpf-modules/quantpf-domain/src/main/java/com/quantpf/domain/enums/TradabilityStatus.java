package com.quantpf.domain.enums;

/**
 * 可交易状态（《详设_1 §12.3.4》）。
 * <p>对应数据库列 {@code quantpf_instrument_calendar_binding.tradability_status}。</p>
 */
public enum TradabilityStatus {

    ACTIVE("可交易"),
    SUSPENDED("已暂停"),
    EXPIRED("已到期");

    private final String desc;

    TradabilityStatus(String desc) {
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
        for (TradabilityStatus item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}