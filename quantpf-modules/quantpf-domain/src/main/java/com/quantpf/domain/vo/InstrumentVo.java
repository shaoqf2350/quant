package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 标的台账视图对象（§11）。
 */
@Data
public class InstrumentVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（既有表 id） */
    private Long id;

    /** 标的代码 */
    private String instrumentCode;

    /** 品种类型（STOCK/FUTURE） */
    private String productType;

    /** 交易所代码 */
    private String exchangeCode;

    /** 标的名称 */
    private String instrumentName;

    /** 币种 */
    private String currency;

    /** 最小价格变动 */
    private BigDecimal tickSize;

    /** 最小交易单位 */
    private BigDecimal lotSize;

    /** 状态 */
    private String status;

    /** 状态中文描述 */
    private String statusText;

    /** 上市日期 */
    private LocalDate listedDate;

    /** 退市日期 */
    private LocalDate delistedDate;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}