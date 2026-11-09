package com.quantpf.domain.enums;

/**
 * 期货合约生命周期/制度事件类型（《详设_1 §13.2》）。
 * <p>对应数据库列 {@code quantpf_contract_event.event_type}。</p>
 */
public enum ContractEventType implements QuantDictEnum {

    LISTING("上市"),
    MARGIN_CHANGE("保证金调整"),
    PRICE_LIMIT_CHANGE("涨跌停调整"),
    SESSION_CHANGE("交易时段调整"),
    POSITION_LIMIT("限仓变化"),
    LAST_TRADE("最后交易日"),
    DELIVERY("交割"),
    ROLLOVER("移仓"),
    DELIST("摘牌");

    private final String desc;

    ContractEventType(String desc) {
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
        for (ContractEventType item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}