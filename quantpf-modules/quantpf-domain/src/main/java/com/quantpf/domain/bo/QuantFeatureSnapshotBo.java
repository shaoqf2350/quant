package com.quantpf.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 量价特征快照记录表单（§23，二期预留）。
 */
@Data
public class QuantFeatureSnapshotBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 标的ID */
    @NotNull(message = "标的ID不能为空")
    private Long instrumentId;

    /** 市场本地日期 */
    @NotNull(message = "市场本地日期不能为空")
    private LocalDate marketLocalDate;

    /** 周期 */
    @NotBlank(message = "周期不能为空")
    private String timeframe;

    /** 特征定义版本 */
    @NotBlank(message = "特征定义版本不能为空")
    private String featureDefinitionVersion;

    /** 源快照ID */
    private Long sourceSnapshotId;

    /** 点时版本ID */
    private Long pointInTimeId;

    /** 收益 */
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
}