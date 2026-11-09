package com.quantpf.domain.enums;

/**
 * 研究事件状态（《详设_1 §10.4》）。
 * <p>对应数据库列 {@code quantpf_research_event.status}。</p>
 */
public enum ResearchEventStatus implements QuantDictEnum {

    NEW("新建"),
    COLLECTED("已采集"),
    DEDUPLICATED("已去重"),
    AI_ANALYZED("AI已分析"),
    VERIFIED("已核验"),
    VARIABLE_MAPPED("变量已映射"),
    PUBLISHED("已发布"),
    DISMISSED("已忽略"),
    EXPIRED("已过期"),
    REVISED("已修订");

    private final String desc;

    ResearchEventStatus(String desc) {
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
        for (ResearchEventStatus item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}