package com.quantpf.domain.enums;

/**
 * 股票公司行为类型（《详设_1 §13.1》）。
 * <p>对应数据库列 {@code quantpf_corporate_action.action_type}。</p>
 */
public enum CorporateActionType implements QuantDictEnum {

    DIVIDEND("分红"),
    SPLIT("送转股"),
    RIGHTS("配股"),
    SUSPEND("停牌"),
    RESUME("复牌"),
    RESTRUCTURE("重组"),
    ST("ST风险"),
    DELIST_RISK("退市风险"),
    DELIST("退市"),
    FINANCIAL_DISCLOSURE("财报披露"),
    INQUIRY("问询");

    private final String desc;

    CorporateActionType(String desc) {
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
        for (CorporateActionType item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}