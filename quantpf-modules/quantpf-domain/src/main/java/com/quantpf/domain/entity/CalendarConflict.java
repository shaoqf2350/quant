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
 * 日历冲突实体（《详设_1 §12.2.2》）。
 * <p>对应表 {@code quantpf_calendar_conflict}，冲突不静默择一，须人工覆盖。</p>
 */
@Data
@TableName("quantpf_calendar_conflict")
public class CalendarConflict implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long pkId;

    /** 冲突编码 */
    private String conflictCode;

    /** 日历ID */
    private Long calendarId;

    /** 涉及市场本地日期 */
    private LocalDate marketLocalDate;

    /** 冲突类型 */
    private String conflictType;

    /** 来源A（记录/哈希） */
    private String sourceA;

    /** 来源B（记录/哈希） */
    private String sourceB;

    /** 冲突描述 */
    private String description;

    /** 状态（CalendarConflictStatus） */
    private String status;

    /** 人工覆盖结论 */
    private String overrideDecision;

    /** 影响范围 */
    private String impactScope;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}