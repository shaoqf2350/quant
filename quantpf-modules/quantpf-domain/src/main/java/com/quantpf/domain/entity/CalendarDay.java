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
 * 交易日记录实体（《详设_1 §12.3.2》）。
 * <p>对应表 {@code quantpf_calendar_day}，日期以市场本地日期保存。</p>
 */
@Data
@TableName("quantpf_calendar_day")
public class CalendarDay implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long pkId;

    /** 日历ID（关联 quantpf_calendar_def.pk_id） */
    private Long calendarId;

    /** 市场本地日期 */
    private LocalDate marketLocalDate;

    /** 是否开市（标准布尔） */
    private Boolean isOpen;

    /** 来源 is_open 原始值（字符串/整数原样保留，解析失败置 INVALID，不默认开市 §12.2.6） */
    private String isOpenRaw;

    /** 日状态 */
    private String dayStatus;

    /** 上一交易日 */
    private LocalDate previousTradeDate;

    /** 下一交易日 */
    private LocalDate nextTradeDate;

    /** 节假日编码 */
    private String holidayCode;

    /** 特殊原因 */
    private String specialReason;

    /** 来源数据源ID */
    private Long sourceId;

    /** 来源记录哈希 */
    private String sourceRecordHash;

    /** 质量状态 */
    private String qualityStatus;

    /** 版本 */
    private String version;

    /** 有效开始 */
    private LocalDateTime validFrom;

    /** 有效结束 */
    private LocalDateTime validTo;

    /** 当前有效版本标记 */
    private Boolean currentFlag;

    /** 创建时间 */
    private LocalDateTime createTime;
}