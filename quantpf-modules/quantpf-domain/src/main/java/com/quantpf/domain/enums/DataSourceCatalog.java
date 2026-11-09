package com.quantpf.domain.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 内置数据源目录（《详设_1 §25.2.1》）：Tushare + 财联社/新浪/雪球。
 * <p>{@code source_code} 的事实源：新增数据源只允许命中本目录，目录扩展必须在本枚举登记后再开放，
 * 前端从目录接口动态拉取，不在前端硬编码（ADR-15 事实源统一在后端）。</p>
 */
public enum DataSourceCatalog {

    /** Tushare Pro：股票/基金/期货结构化行情与基础数据（trade_cal、daily、fund_basic 等） */
    TUSHARE_PRO("Tushare Pro", "股票/基金/期货结构化行情与基础数据（trade_cal/daily/fund_basic 等）"),

    /** 财联社电报：财经快讯/公告新闻源（施工期待授权） */
    CLS_TELEGRAPH("财联社电报", "财经快讯/公告新闻源（施工期待授权）"),

    /** 新浪财经：新闻源（施工期待授权） */
    SINA_NEWS("新浪财经", "新闻源（施工期待授权）"),

    /** 雪球：社区观点/线索源（D 级线索，仅观察与情绪，不单独产生交易信号） */
    XUEQIU("雪球", "社区观点/线索源（D 级线索，仅观察与情绪，不单独产生交易信号）");

    /** 目录默认名称（用户新增数据源时可自动带出） */
    private final String defaultName;

    /** 目录说明 */
    private final String description;

    DataSourceCatalog(String defaultName, String description) {
        this.defaultName = defaultName;
        this.description = description;
    }

    /** 数据源编码（枚举名，如 TUSHARE_PRO） */
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
     * @param code 数据源编码
     * @return 目录项；code 为空或未知返回 null
     */
    public static DataSourceCatalog of(String code) {
        if (code == null) {
            return null;
        }
        for (DataSourceCatalog item : values()) {
            if (item.name().equals(code)) {
                return item;
            }
        }
        return null;
    }

    /** 目录全量列表（供目录接口返回） */
    public static List<DataSourceCatalog> list() {
        return Arrays.asList(values());
    }
}