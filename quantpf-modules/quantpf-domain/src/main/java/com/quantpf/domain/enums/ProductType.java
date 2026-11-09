package com.quantpf.domain.enums;
public enum ProductType implements QuantDictEnum {

    STOCK("股票"),
    FUTURE("期货");

    private final String desc;

    ProductType(String desc) {
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
        for (ProductType item : values()) {
            if (item.name().equals(code)) {
                return item.desc;
            }
        }
        return code;
    }
}