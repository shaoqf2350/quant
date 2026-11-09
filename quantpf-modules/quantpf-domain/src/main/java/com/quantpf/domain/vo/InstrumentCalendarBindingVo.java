package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 标的/日历绑定视图对象（§12.3.4）。
 */
@Data
public class InstrumentCalendarBindingVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    private Long pkId;

    /** 标的/合约/外汇品种ID */
    private Long instrumentId;

    /** 日历ID */
    private Long calendarId;

    /** 绑定类型 */
    private String bindingType;

    /** 绑定类型中文描述 */
    private String bindingTypeText;

    /** 有效开始 */
    private LocalDateTime validFrom;

    /** 有效结束 */
    private LocalDateTime validTo;

    /** 可交易状态 */
    private String tradabilityStatus;

    /** 可交易状态中文描述 */
    private String tradabilityStatusText;

    /** 来源 */
    private String source;

    /** 原因 */
    private String reason;

    /** 版本 */
    private String version;

    /** 创建时间 */
    private LocalDateTime createTime;
}