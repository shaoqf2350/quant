package com.quantpf.domain.enums;

/**
 * 公司行为/合约事件生命周期状态（《详设_1 §13.3》）。
 * <p>对应数据库列 {@code quantpf_corporate_action.status} / {@code quantpf_contract_event.status}。</p>
 */
public enum LifecycleEventStatus implements QuantDictEnum {

    DISCOVERED("已发现"),
    VERIFIED("已验证"),
    SCHEDULED("已排期"),
    EFFECTIVE("已生效"),
    EXPIRED("已过期"),
    SUPERSEDED("已废止");

    private final String desc;

    LifecycleEventStatus(String desc) {
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
        for (LifecycleEventStatus item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }

    /**
     * 按编码解析枚举实例；code 为空或未知返回 null。
     */
    public static LifecycleEventStatus of(String code) {
        if (code == null) {
            return null;
        }
        for (LifecycleEventStatus item : values()) {
            if (item.name().equals(code)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 生命周期合法迁移判定（《详设_1 §13.3》）。
     * <pre>
     * DISCOVERED → VERIFIED → SCHEDULED → EFFECTIVE → EXPIRED
     * 任意非终态 → SUPERSEDED（被新版本废止）
     * EXPIRED / SUPERSEDED 为终态，不允许再迁移
     * </pre>
     * 越级迁移（如 DISCOVERED→EFFECTIVE）必须拒绝：未核验的事件不得直接生效。
     *
     * @param from 当前状态编码
     * @param to   目标状态编码
     * @return true 表示迁移合法
     */
    public static boolean canTransition(String from, String to) {
        LifecycleEventStatus fromStatus = of(from);
        LifecycleEventStatus toStatus = of(to);
        if (fromStatus == null || toStatus == null) {
            return false;
        }
        return switch (fromStatus) {
            case DISCOVERED -> toStatus == VERIFIED || toStatus == SUPERSEDED;
            case VERIFIED -> toStatus == SCHEDULED || toStatus == SUPERSEDED;
            case SCHEDULED -> toStatus == EFFECTIVE || toStatus == SUPERSEDED;
            case EFFECTIVE -> toStatus == EXPIRED || toStatus == SUPERSEDED;
            case EXPIRED, SUPERSEDED -> false;
        };
    }
}