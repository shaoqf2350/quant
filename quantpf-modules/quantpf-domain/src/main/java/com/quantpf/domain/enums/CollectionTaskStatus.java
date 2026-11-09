package com.quantpf.domain.enums;

/**
 * 数据采集任务状态（《详设_1 §7.4》）。
 * <p>对应数据库列 {@code quantpf_data_collection_task.status}。</p>
 */
public enum CollectionTaskStatus implements QuantDictEnum {

    CREATED("已创建"),
    RUNNING("运行中"),
    SUCCEEDED("成功"),
    PARTIAL_SUCCESS("部分成功"),
    FAILED("失败"),
    RETRYING("重试中"),
    CANCELED("已取消");

    private final String desc;

    CollectionTaskStatus(String desc) {
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
        for (CollectionTaskStatus item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}