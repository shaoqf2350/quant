package com.quantpf.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 交易时段模板实体（《详设_1 §12.3.3》）。
 * <p>对应表 {@code quantpf_calendar_session}。</p>
 */
@Data
@TableName("quantpf_calendar_session")
public class CalendarSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long pkId;

    /** 日历ID */
    private Long calendarId;

    /** 时段类型 */
    private String sessionType;

    /** 市场本地时区 */
    private String marketTimezone;

    /** 顺序号 */
    private Integer sequenceNo;

    /** 本地开始时间 */
    private LocalTime startLocalTime;

    /** 本地结束时间 */
    private LocalTime endLocalTime;

    /** 是否跨自然日（夜盘） */
    private Boolean crossesMidnight;

    /** 交易日归属 */
    private String tradingDayAssignment;

    /** 是否可交易 */
    private Boolean isTradable;

    /** 生效开始日期 */
    private LocalDate effectiveFrom;

    /** 生效结束日期 */
    private LocalDate effectiveTo;

    /** 版本 */
    private String version;

    /** 创建时间 */
    private LocalDateTime createTime;
}