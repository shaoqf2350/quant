package com.quantpf.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 标的台账 MyBatis Plus 只读实体（《详设_1 §11》）。
 * <p>映射既有表 {@code quantpf_instrument}（V1_17_0 起主键统一 pk_id，旧 id 列保留兼容）。</p>
 * <p>与遗留 DDD {@code com.quantpf.domain.entity.Instrument} 聚合区分，后续统一收敛。</p>
 */
@Data
@TableName("quantpf_instrument")
public class InstrumentEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID；V1_17_0 起 pk_id 列为表主键，与旧 id 同值回填；旧 id 列保留兼容一个版本） */
    @TableId(value = "pk_id", type = IdType.INPUT)
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

    /** 状态（ACTIVE/SUSPENDED/DELISTED） */
    private String status;

    /** 上市日期 */
    private LocalDate listedDate;

    /** 退市日期 */
    private LocalDate delistedDate;

    /** 创建时间（既有表 created_at 列） */
    private LocalDateTime createdAt;

    /** 更新时间（既有表 updated_at 列） */
    private LocalDateTime updatedAt;
}