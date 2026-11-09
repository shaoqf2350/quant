package com.quantpf.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日线数据就绪状态实体（《详设_1 §12.3.5》）。
 * <p>对应表 {@code quantpf_market_data_readiness}。</p>
 */
@Data
@TableName("quantpf_market_data_readiness")
public class MarketDataReadiness implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long pkId;

    /** 日历ID */
    private Long calendarId;

    /** 市场本地日期 */
    private LocalDate marketLocalDate;

    /** 是否应到 */
    private Boolean expectedFlag;

    /** 是否已到 */
    private Boolean receivedFlag;

    /** 是否校验通过 */
    private Boolean validatedFlag;

    /** 策略是否可用 */
    private Boolean strategyReadyFlag;

    /** Paper 是否可新增风险 */
    private Boolean paperReadyFlag;

    /** 最后一次有效快照ID */
    private Long lastValidSnapshotId;

    /** 阻断原因 */
    private String blockReason;

    /** 版本 */
    private String version;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}