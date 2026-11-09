package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 交易时段模板视图对象（§12.3.3）。
 */
@Data
public class CalendarSessionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    private Long pkId;

    /** 日历ID */
    private Long calendarId;

    /** 时段类型 */
    private String sessionType;

    /** 时段类型中文描述 */
    private String sessionTypeText;

    /** 顺序号 */
    private Integer sequenceNo;

    /** 本地开始时间 */
    private LocalTime startLocalTime;

    /** 本地结束时间 */
    private LocalTime endLocalTime;

    /** 是否跨自然日 */
    private Boolean crossesMidnight;

    /** 交易日归属 */
    private String tradingDayAssignment;

    /** 是否可交易 */
    private Boolean isTradable;

    /** 版本 */
    private String version;

    /** 创建时间 */
    private LocalDateTime createTime;
}