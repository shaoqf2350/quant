package com.quantpf.domain.enums;

/**
 * 研究任务状态（《详设_1 §10.2》）。
 * <p>对应数据库列 {@code quantpf_research_task.status}。</p>
 */
public enum ResearchTaskStatus implements QuantDictEnum {

    NEW("新建"),
    RUNNING("运行中"),
    /** AI 草稿校验未通过或适配器不可用时转待人工复核，不判失败（详设_1 §10.4 / 详设_0 §5.7 降级语义） */
    PENDING_REVIEW("待人工复核"),
    SUCCEEDED("成功"),
    FAILED("失败"),
    CANCELED("已取消"),
    RETRYING("重试中");

    private final String desc;

    ResearchTaskStatus(String desc) {
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
        for (ResearchTaskStatus item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}