package com.quantpf.domain.enums;

/**
 * 研究事件方向（《详设_1 §10.3》）。
 * <p>对应数据库列 {@code quantpf_research_event.direction}。</p>
 */
public enum ResearchDirection implements QuantDictEnum {

    BULLISH("利多"),
    BEARISH("利空"),
    NEUTRAL("中性"),
    BIDIRECTIONAL("双向"),
    UNCERTAIN("不确定");

    private final String desc;

    ResearchDirection(String desc) {
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
        for (ResearchDirection item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}