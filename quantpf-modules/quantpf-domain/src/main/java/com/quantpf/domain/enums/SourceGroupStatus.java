package com.quantpf.domain.enums;

/**
 * 来源组状态（《详设_1 §6.2》）。
 * <p>对应数据库列 {@code quantpf_source_group.status}。</p>
 */
public enum SourceGroupStatus implements QuantDictEnum {

    CONFIGURED("已配置"),
    HEALTHY("健康"),
    PARTIALLY_AVAILABLE("部分可用"),
    CONFLICTED("冲突"),
    DEGRADED("降级"),
    UNAVAILABLE("不可用");

    private final String desc;

    SourceGroupStatus(String desc) {
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
        for (SourceGroupStatus item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}