package com.quantpf.domain.enums;

/**
 * 数据源生命周期状态（《详设_1 §5.3》）。
 * <p>对应数据库列 {@code quantpf_data_source.status}。</p>
 */
public enum DataSourceStatus implements QuantDictEnum {

    DRAFT("草稿"),
    PENDING_CONFIRMATION("待确认"),
    ACTIVE("启用"),
    DEGRADED("降级"),
    EXPIRING("即将到期"),
    EXPIRED("已到期"),
    SUSPENDED("已暂停"),
    RETIRED("已退役");

    private final String desc;

    DataSourceStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 按编码取状态描述，未知值返回原始编码（防止空指针，保证前端可展示）。
     *
     * @param code 状态编码
     * @return 状态中文描述；code 为空返回 null
     */
    public static String descOf(String code) {
        if (code == null) {
            return null;
        }
        for (DataSourceStatus item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }

    /**
     * 按编码解析枚举实例。
     *
     * @param code 状态编码
     * @return 枚举实例；code 为空或未知返回 null
     */
    public static DataSourceStatus of(String code) {
        if (code == null) {
            return null;
        }
        for (DataSourceStatus item : values()) {
            if (item.name().equals(code)) {
                return item;
            }
        }
        return null;
    }
}