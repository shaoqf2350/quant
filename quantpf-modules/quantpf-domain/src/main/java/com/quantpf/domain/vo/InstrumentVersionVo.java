package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 标的交易属性版本视图对象（§11.7）。
 */
@Data
public class InstrumentVersionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    private Long pkId;

    /** 标的ID */
    private Long instrumentId;

    /** 标的版本号 */
    private String instrumentVersion;

    /** 生效开始时间 */
    private LocalDateTime validFrom;

    /** 生效结束时间 */
    private LocalDateTime validUntil;

    /** 变更原因 */
    private String changeReason;

    /** 来源引用 */
    private String sourceRef;

    /** 最小价格变动 */
    private BigDecimal tickSize;

    /** 最小交易单位 */
    private BigDecimal lotSize;

    /** 保证金率 */
    private BigDecimal marginRate;

    /** 涨跌停规则 */
    private String priceLimit;

    /** 交易时段（JSON） */
    private String tradingSessions;

    /** 上市/停牌/退市状态规则 */
    private String delistingRule;

    /** 创建时间 */
    private LocalDateTime createTime;
}