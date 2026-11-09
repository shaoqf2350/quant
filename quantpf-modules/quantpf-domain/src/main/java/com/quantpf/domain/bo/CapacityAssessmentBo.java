package com.quantpf.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 容量评估记录表单（§15）。
 */
@Data
public class CapacityAssessmentBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 标的ID */
    @NotNull(message = "标的ID不能为空")
    private Long instrumentId;

    /** 评估日期 */
    @NotNull(message = "评估日期不能为空")
    private LocalDate assessDate;

    /** 容量状态 */
    @NotBlank(message = "容量状态不能为空")
    private String capacityStatus;

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
}