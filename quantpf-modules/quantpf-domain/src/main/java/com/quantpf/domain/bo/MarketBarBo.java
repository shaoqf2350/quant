package com.quantpf.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * K 线录入/导入表单（《详设_1 §14.2/§14.3》）。
 * <p>OHLC 合法性（high>=low、high>=open/close、low<=open/close、量额非负）在服务层确定性校验。</p>
 */
@Data
public class MarketBarBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 标的ID */
    @NotNull(message = "标的ID不能为空")
    private Long instrumentId;

    /** 周期 */
    @NotBlank(message = "周期不能为空")
    private String timeframe;

    /** K线时间 */
    @NotNull(message = "K线时间不能为空")
    private LocalDateTime barTime;

    /** 开盘价 */
    @NotNull(message = "开盘价不能为空")
    private BigDecimal openPrice;

    /** 最高价 */
    @NotNull(message = "最高价不能为空")
    private BigDecimal highPrice;

    /** 最低价 */
    @NotNull(message = "最低价不能为空")
    private BigDecimal lowPrice;

    /** 收盘价 */
    @NotNull(message = "收盘价不能为空")
    private BigDecimal closePrice;

    /** 成交量 */
    private BigDecimal volume;

    /** 成交额 */
    private BigDecimal turnover;

    /** 来源 */
    @NotBlank(message = "来源不能为空")
    private String source;
}