package com.quantpf.domain.enums;

/**
 * 事件证据类型（《详设_1 §19.1》）。
 * <p>对应数据库列 {@code quantpf_event_evidence.evidence_type}。</p>
 */
public enum EvidenceType implements QuantDictEnum {

    FACT("事实"),
    OPINION("观点"),
    COUNTER("反方");

    private final String desc;

    EvidenceType(String desc) {
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
        for (EvidenceType item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}