package com.quantpf.domain.enums;

/**
 * 标的身份状态（《详设_1 §11.6 / §17.4》）。
 * <p>对应数据库列 {@code quantpf_instrument.status}。</p>
 * <p>身份状态机：DISCOVERED → MAPPING_REQUIRED → VERIFIED → ACTIVE → SUSPENDED/EXPIRED → RETIRED。
 * 既有代码使用 ACTIVE/SUSPENDED/DELISTED 子集，本枚举按详设补全；DELISTED 语义映射到 EXPIRED/RETIRED。
 * 旧值兼容：DELISTED 视为已退市（终态）。</p>
 */
public enum InstrumentIdentityStatus implements QuantDictEnum {

    DISCOVERED("已发现"),
    MAPPING_REQUIRED("待身份映射"),
    VERIFIED("已核验"),
    ACTIVE("启用"),
    SUSPENDED("已暂停"),
    /** 既有 DELISTED 语义兼容：合约到期/退市 */
    EXPIRED("已到期/退市"),
    RETIRED("已退役"),
    /** 历史存量值兼容（等价 EXPIRED） */
    DELISTED("已退市");

    private final String desc;

    InstrumentIdentityStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 按编码取状态描述，未知值返回原始编码（防止空指针，保证前端可展示）。
     */
    public static String descOf(String code) {
        if (code == null) {
            return null;
        }
        // 存量值 DELISTED 展示为已退市
        if (DELISTED.name().equals(code)) {
            return DELISTED.desc;
        }
        for (InstrumentIdentityStatus item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }

    /**
     * 是否可交易状态（ACTIVE 才可交易）。
     */
    public static boolean tradableOf(String code) {
        return ACTIVE.name().equals(code);
    }
}
