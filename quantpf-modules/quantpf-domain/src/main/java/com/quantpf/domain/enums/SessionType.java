package com.quantpf.domain.enums;

/**
 * 交易时段类型（《详设_1 §12.3.3》）。
 * <p>对应数据库列 {@code quantpf_calendar_session.session_type}。</p>
 */
public enum SessionType implements QuantDictEnum {

    REGULAR("常规交易"),
    NIGHT("夜盘"),
    AUCTION("集合竞价"),
    SPECIAL("特殊时段");

    private final String desc;

    SessionType(String desc) {
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
        for (SessionType item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}