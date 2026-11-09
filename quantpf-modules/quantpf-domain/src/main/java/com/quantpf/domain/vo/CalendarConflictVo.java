package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日历冲突视图对象（§12.2.2）。
 */
@Data
public class CalendarConflictVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    private Long pkId;

    /** 冲突编码 */
    private String conflictCode;

    /** 日历ID */
    private Long calendarId;

    /** 涉及市场本地日期 */
    private LocalDate marketLocalDate;

    /** 冲突类型 */
    private String conflictType;

    /** 来源A */
    private String sourceA;

    /** 来源B */
    private String sourceB;

    /** 冲突描述 */
    private String description;

    /** 状态 */
    private String status;

    /** 状态中文描述 */
    private String statusText;

    /** 人工覆盖结论 */
    private String overrideDecision;

    /** 影响范围 */
    private String impactScope;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}