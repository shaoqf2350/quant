package com.quantpf.domain.enums;

/**
 * 研究任务类型（《详设_1 §10.2》）。
 * <p>对应数据库列 {@code quantpf_research_task.task_type}。</p>
 */
public enum ResearchTaskType implements QuantDictEnum {

    NEWS_ANALYSIS("新闻分析"),
    DEEP_RESEARCH("深度研究"),
    RISK_REVIEW("风险复核");

    private final String desc;

    ResearchTaskType(String desc) {
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
        for (ResearchTaskType item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}
