package com.quantpf.domain.enums;

/**
 * 数据质量问题生命周期状态（《详设_1 §9.5》）。
 * <p>对应数据库列 {@code quantpf_data_quality_issue.issue_status}。</p>
 */
public enum DataQualityIssueStatus implements QuantDictEnum {

    OPEN("待处理"),
    ANALYZING("分析中"),
    WAITING_SOURCE("待来源"),
    WAITING_HUMAN("待人工"),
    RESOLVED("已解决"),
    VERIFIED("已验证"),
    CLOSED("已关闭");

    private final String desc;

    DataQualityIssueStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 按编码取状态描述，未知值返回原始编码（防止空指针）。
     *
     * @param code 状态编码
     * @return 状态中文描述；code 为空返回 null
     */
    public static String descOf(String code) {
        if (code == null) {
            return null;
        }
        for (DataQualityIssueStatus item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}