package com.quantpf.domain.enums;

/**
 * 事件主体类型（《详设_1 §19.1》）。
 * <p>对应数据库列 {@code quantpf_event.subject_type}。</p>
 */
public enum SubjectType implements QuantDictEnum {

    MATERIAL("研究材料"),
    MARKET_DATA("行情数据"),
    INSTRUMENT("标的"),
    CALENDAR("日历");

    private final String desc;

    SubjectType(String desc) {
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
        for (SubjectType item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}
