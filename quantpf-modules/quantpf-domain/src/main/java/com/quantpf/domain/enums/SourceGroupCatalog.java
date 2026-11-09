package com.quantpf.domain.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 数据源分组目录（《详设_1 §25.2.2》）。
 * <p>{@code group_code} 的事实源：前端分组下拉从目录接口动态拉取，不在前端硬编码（ADR-15 事实源统一在后端）。</p>
 */
public enum SourceGroupCatalog {

    NEWS_POLICY("新闻/政策", "宏观新闻与政策线索（D 级，仅观察与情绪）"),
    CORP_REPORT("公司公告/财报", "上市公司公告与财报线索"),
    RESEARCH("研报/行业报告", "券商/机构研报与行业报告线索"),
    MARKET_PRICE("行情/量价", "行情与量价线索"),
    COMMODITY("商品供需", "商品供需线索"),
    TRADING_RULE("交易规则", "交易所规则变更线索");

    /** 分组默认名称（用户新增数据源时可自动带出） */
    private final String defaultName;

    /** 分组说明 */
    private final String description;

    SourceGroupCatalog(String defaultName, String description) {
        this.defaultName = defaultName;
        this.description = description;
    }

    /** 分组编码（枚举名，如 NEWS_POLICY） */
    public String getCode() {
        return name();
    }

    public String getDefaultName() {
        return defaultName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 按编码解析目录项（严格大小写）。
     *
     * @param code 分组编码
     * @return 目录项；code 为空或未知返回 null
     */
    public static SourceGroupCatalog of(String code) {
        if (code == null) {
            return null;
        }
        for (SourceGroupCatalog item : values()) {
            if (item.name().equals(code)) {
                return item;
            }
        }
        return null;
    }

    /** 目录全量列表（供字典接口返回） */
    public static List<SourceGroupCatalog> list() {
        return Arrays.asList(values());
    }
}
