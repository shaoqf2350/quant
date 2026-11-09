package com.quantpf.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * K 线行情实体（《详设_1 §14》，映射既有表 quantpf_market_bar）。
 * <p>V1_17_0 起主键统一为 pk_id（与旧 id 同值回填）；旧 id 列保留 AUTO_INCREMENT+UNIQUE 兼容。</p>
 */
@Data
@TableName("quantpf_market_bar")
public class MarketBar implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID；V1_17_0 起 pk_id 列为表主键，与旧 id 同值回填；旧 id 列保留兼容一个版本） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long id;

    /** 标的ID */
    private Long instrumentId;

    /** 周期（30M/DAY/WEEK/MONTH） */
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

    /** 创建时间（既有表 created_at 列） */
    private LocalDateTime createdAt;
}