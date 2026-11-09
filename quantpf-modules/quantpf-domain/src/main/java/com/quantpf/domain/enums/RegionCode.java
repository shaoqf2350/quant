package com.quantpf.domain.enums;

/**
 * 地区代码（《详设_1 §12.3.1》）。
 * <p>对应数据库列 {@code quantpf_trading_calendar.region_code}。</p>
 */
public enum RegionCode implements QuantDictEnum {

    CN("中国大陆"),
    HK("中国香港"),
    US("美国"),
    GLOBAL("全球");

    private final String desc;

    RegionCode(String desc) {
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
        for (RegionCode item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}
