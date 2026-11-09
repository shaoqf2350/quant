package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 市场状态视图对象（§14.4）。
 */
@Data
public class MarketStateVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    private Long pkId;

    /** 标的ID（NULL=市场级） */
    private Long instrumentId;

    /** 日历ID */
    private Long calendarId;

    /** 市场本地日期 */
    private LocalDate marketLocalDate;

    /** 市场状态 */
    private String stateCode;

    /** 市场状态中文描述 */
    private String stateCodeText;

    /** 状态原因 */
    private String stateReason;

    /** 版本 */
    private String version;

    /** 创建时间 */
    private LocalDateTime createTime;
}