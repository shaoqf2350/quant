package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 容量评估视图对象（§15）。
 */
@Data
public class CapacityAssessmentVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    private Long pkId;

    /** 标的ID */
    private Long instrumentId;

    /** 评估日期 */
    private LocalDate assessDate;

    /** 容量状态 */
    private String capacityStatus;

    /** 容量状态中文描述 */
    private String capacityStatusText;

    /** 日均成交额 */
    private BigDecimal avgTurnover;

    /** 日均成交量 */
    private BigDecimal avgVolume;

    /** 买卖价差 */
    private BigDecimal bidAskSpread;

    /** 换手率 */
    private BigDecimal turnoverRate;

    /** 流通市值 */
    private BigDecimal floatMarketCap;

    /** 计划金额占成交额比例 */
    private BigDecimal plannedRatio;

    /** 预计滑点 */
    private BigDecimal slippageEstimate;

    /** 明细（JSON） */
    private String detail;

    /** 创建时间 */
    private LocalDateTime createTime;
}