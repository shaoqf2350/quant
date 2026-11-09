package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 外汇时段规则视图对象（§12.2.5）。
 */
@Data
public class FxSessionRuleVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    private Long pkId;

    /** Tushare/FX 品种代码 */
    private String tsCode;

    /** 品种分类 */
    private String classify;

    /** 交易商/交易所 */
    private String exchange;

    /** 交易时段原文 */
    private String tradingHours;

    /** 休市时段原文 */
    private String breakTime;

    /** 标准化时段规则（JSON） */
    private String normalizedSessions;

    /** 来源时区 */
    private String timezone;

    /** 生效开始 */
    private LocalDate validFrom;

    /** 生效结束 */
    private LocalDate validTo;

    /** 版本 */
    private String version;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}