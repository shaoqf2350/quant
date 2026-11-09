package com.quantpf.domain.enums;

/**
 * 行情 K 线周期（《详设_1 §12.3.3》）。
 * <p>对应数据库列 {@code quantpf_market_data.timeframe}；存库值 {@code 30M} 以数字开头
 * 无法作为枚举名，故覆写 {@link #value()}。</p>
 */
public enum BarTimeframe implements QuantDictEnum {

    M30("30M", "30分钟"),
    DAY("DAY", "日线"),
    WEEK("WEEK", "周线"),
    MONTH("MONTH", "月线");

    /** 存库值 */
    private final String value;

    private final String desc;

    BarTimeframe(String value, String desc) {
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
        for (BarTimeframe item : values()) {
            if (item.value.equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}
