package com.quantpf.domain.enums;

/**
 * 采集任务类型（《详设_1 §14.3》）。
 * <p>对应数据库列 {@code quantpf_collection_task.task_type}。</p>
 */
public enum CollectionTaskType implements QuantDictEnum {

    NEWS("新闻"),
    ANNOUNCEMENT("公告"),
    KLINE("K线行情"),
    CALENDAR("交易日历"),
    FINANCIAL("财务数据"),
    RULE("交易规则");

    private final String desc;

    CollectionTaskType(String desc) {
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
        for (CollectionTaskType item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}
