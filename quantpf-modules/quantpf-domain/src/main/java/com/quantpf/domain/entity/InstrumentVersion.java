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
 * 标的交易属性版本实体（《详设_1 §11.7》）。
 * <p>对应表 {@code quantpf_instrument_version}。</p>
 */
@Data
@TableName("quantpf_instrument_version")
public class InstrumentVersion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long pkId;

    /** 标的ID（关联 quantpf_instrument.id） */
    private Long instrumentId;

    /** 标的版本号 */
    private String instrumentVersion;

    /** 生效开始时间 */
    private LocalDateTime validFrom;

    /** 生效结束时间（NULL=当前有效） */
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