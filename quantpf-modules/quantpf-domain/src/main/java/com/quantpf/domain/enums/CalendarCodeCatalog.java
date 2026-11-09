package com.quantpf.domain.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 交易日历代码目录（《详设_1 §12.3.1》）。
 * <p>{@code calendar_code} 的事实源：前端日历代码下拉从目录接口动态拉取，不在前端硬编码（ADR-15 事实源统一在后端）。</p>
 */
public enum CalendarCodeCatalog {

    CN_STOCK_SSE("沪股", "上海证券交易所 A 股"),
    CN_STOCK_SZSE("深股", "深圳证券交易所 A 股"),
    CN_STOCK_BSE("北交所", "北京证券交易所"),
    CN_FUTURE_CFFEX("中金所", "中国金融期货交易所"),
    CN_FUTURE_SHFE("上期所", "上海期货交易所"),
    CN_FUTURE_CZCE("郑商所", "郑州商品交易所"),
    CN_FUTURE_DCE("大商所", "大连商品交易所"),
    CN_FUTURE_INE("上期能源", "上海国际能源交易中心"),
    CN_FUTURE_GFEX("广期所", "广州期货交易所（人工维护）"),
    HK_STOCK_HKEX("港股", "香港交易所"),
    US_STOCK_GENERIC("美股", "美国通用交易日历");

    /** 日历代码默认名称（用户新增日历时可自动带出） */
    private final String defaultName;

    /** 日历代码说明 */
    private final String description;

    CalendarCodeCatalog(String defaultName, String description) {
        this.defaultName = defaultName;
        this.description = description;
    }

    /** 日历代码（枚举名，如 CN_STOCK_SSE） */
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
     * @param code 日历代码
     * @return 目录项；code 为空或未知返回 null
     */
    public static CalendarCodeCatalog of(String code) {
        if (code == null) {
            return null;
        }
        for (CalendarCodeCatalog item : values()) {
            if (item.name().equals(code)) {
                return item;
            }
        }
        return null;
    }

    /** 目录全量列表（供字典接口返回） */
    public static List<CalendarCodeCatalog> list() {
        return Arrays.asList(values());
    }
}
