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
 * 量价特征快照实体（《详设_1 §23》，二期时机优化预留）。
 * <p>对应表 {@code quantpf_quant_feature_snapshot}。</p>
 */
@Data
@TableName("quantpf_quant_feature_snapshot")
public class QuantFeatureSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long pkId;

    /** 标的ID */
    private Long instrumentId;

    /** 市场本地日期 */
    private LocalDate marketLocalDate;

    /** 周期（30M/DAY/WEEK/MONTH） */
    private String timeframe;

    /** 特征定义版本 */
    private String featureDefinitionVersion;

    /** 源快照ID */
    private Long sourceSnapshotId;

    /** 点时版本ID */
    private Long pointInTimeId;

    /** 收益（标准化） */
    private BigDecimal returnValue;

    /** 波动 */
    private BigDecimal volatility;

    /** 相对强弱 */
    private BigDecimal relativeStrength;

    /** 成交量分位 */
    private BigDecimal volumePercentile;

    /** 价格位置 */
    private BigDecimal pricePosition;

    /** 状态 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;
}