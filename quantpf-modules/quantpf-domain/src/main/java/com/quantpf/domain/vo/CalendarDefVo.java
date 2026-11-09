package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 交易日历定义视图对象（§12.3.1）。
 */
@Data
public class CalendarDefVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    private Long pkId;

    /** 日历编码 */
    private String calendarCode;

    /** 日历名称 */
    private String calendarName;

    /** 市场类型 */
    private String marketType;

    /** 地区 */
    private String regionCode;

    /** 交易场所 */
    private String venueCode;

    /** 市场本地时区 */
    private String timezone;

    /** 币种 */
    private String currency;

    /** 级别 */
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