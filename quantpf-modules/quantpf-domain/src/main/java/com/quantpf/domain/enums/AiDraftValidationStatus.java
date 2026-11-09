package com.quantpf.domain.enums;

/**
 * AI 研究草稿校验状态（《详设_1 §10.3》）。
 * <p>对应数据库列 {@code quantpf_ai_research_draft.validation_status}。</p>
 */
public enum AiDraftValidationStatus implements QuantDictEnum {

    PENDING("待校验"),
    VERIFIED("已核验"),
    REJECTED("已拒绝"),
    CLAIM_UNVERIFIED("断言未核验");

    private final String desc;

    AiDraftValidationStatus(String desc) {
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
        for (AiDraftValidationStatus item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}