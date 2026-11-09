package com.quantpf.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 标的/日历绑定实体（《详设_1 §12.3.4》）。
 * <p>对应表 {@code quantpf_instrument_calendar_binding}。</p>
 */
@Data
@TableName("quantpf_instrument_calendar_binding")
public class InstrumentCalendarBinding implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long pkId;

    /** 标的/合约/外汇品种ID */
    private Long instrumentId;

    /** 日历ID */
    private Long calendarId;

    /** 绑定类型（DEFAULT/OVERRIDE） */
    private String bindingType;

    /** 有效开始 */
    private LocalDateTime validFrom;

    /** 有效结束 */
    private LocalDateTime validTo;

    /** 可交易状态 */
    private String tradabilityStatus;

    /** 来源 */
    private String source;

    /** 原因 */
    private String reason;

    /** 版本 */
    private String version;

    /** 创建时间 */
    private LocalDateTime createTime;
}