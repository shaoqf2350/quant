package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * K 线行情视图对象（§14）。
 */
@Data
public class MarketBarVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（既有表 id） */
    private Long id;

    /** 标的ID */
    private Long instrumentId;

    /** 周期 */
    private String timeframe;

    /** K线时间 */
    private LocalDateTime barTime;

    /** 开盘价 */
    private BigDecimal openPrice;

    /** 最高价 */
    private BigDecimal highPrice;

    /** 最低价 */
    private BigDecimal lowPrice;

    /** 收盘价 */
    private BigDecimal closePrice;

    /** 成交量 */
    private BigDecimal volume;

    /** 成交额 */
    private BigDecimal turnover;

    /** 来源 */
    private String source;

    /** 创建时间 */
    private LocalDateTime createdAt;
}