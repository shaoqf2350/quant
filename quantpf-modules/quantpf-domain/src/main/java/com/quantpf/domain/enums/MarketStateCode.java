package com.quantpf.domain.enums;

/**
 * 市场状态（《详设_1 §14.4》）。
 * <p>对应数据库列 {@code quantpf_market_state.state_code}，由 Spring Boot 确定性规则产生。</p>
 */
public enum MarketStateCode implements QuantDictEnum {

    TREND_UP("上涨趋势"),
    TREND_DOWN("下跌趋势"),
    RANGE("震荡"),
    HIGH_VOLATILITY("高波动"),
    LOW_LIQUIDITY("低流动性"),
    SYSTEMIC_RISK("系统性风险"),
    DATA_UNAVAILABLE("数据不可用"),
    SPECIAL_RULE("特殊规则");

    private final String desc;

    MarketStateCode(String desc) {
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
        for (MarketStateCode item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}