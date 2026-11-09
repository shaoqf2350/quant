package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 交易日记录视图对象（§12.3.2）。
 */
@Data
public class CalendarDayVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    private Long pkId;

    /** 日历ID */
    private Long calendarId;

    /** 市场本地日期 */
    private LocalDate marketLocalDate;

    /** 是否开市 */
    private Boolean isOpen;

    /** 日状态 */
    private String dayStatus;

    /** 日状态中文描述 */
    private String dayStatusText;

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

    /** 质量状态 */
    private String qualityStatus;

    /** 质量状态中文描述 */
    private String qualityStatusText;

    /** 版本 */
    private String version;

    /** 创建时间 */
    private LocalDateTime createTime;
}