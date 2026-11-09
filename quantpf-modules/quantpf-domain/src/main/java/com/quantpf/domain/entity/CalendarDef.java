package com.quantpf.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 交易日历定义实体（《详设_1 §12.3.1》）。
 * <p>对应表 {@code quantpf_calendar_def}。</p>
 */
@Data
@TableName("quantpf_calendar_def")
public class CalendarDef implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long pkId;

    /** 日历编码（CN_STOCK_SSE/CN_FUTURE_SHFE/HK_STOCK_HKEX/US_STOCK_GENERIC） */
    private String calendarCode;

    /** 日历名称 */
    private String calendarName;

    /** 市场类型（STOCK/FUTURE/FX/SPOT/INDEX） */
    private String marketType;

    /** 地区（CN/HK/US/GLOBAL） */
    private String regionCode;

    /** 交易场所（SSE/SZSE/HKEX/SHFE/...） */
    private String venueCode;

    /** 市场本地时区 */
    private String timezone;

    /** 币种 */
    private String currency;

    /** 级别（MARKET/EXCHANGE/PRODUCT/INSTRUMENT/BROKER） */
    private String calendarLevel;

    /** 来源策略 */
    private String sourcePolicy;

    /** 生效开始 */
    private LocalDate effectiveFrom;

    /** 生效结束 */
    private LocalDate effectiveTo;

    /** 状态 */
    private String status;

    /** 版本 */
    private String version;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 备注 */
    private String remark;
}