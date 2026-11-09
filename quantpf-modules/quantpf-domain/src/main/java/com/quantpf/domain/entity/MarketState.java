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
 * 市场状态快照实体（《详设_1 §14.4》）。
 * <p>对应表 {@code quantpf_market_state}。</p>
 */
@Data
@TableName("quantpf_market_state")
public class MarketState implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long pkId;

    /** 标的ID（NULL=市场级） */
    private Long instrumentId;

    /** 日历ID */
    private Long calendarId;

    /** 市场本地日期 */
    private LocalDate marketLocalDate;

    /** 市场状态（MarketStateCode） */
    private String stateCode;

    /** 状态原因 */
    private String stateReason;

    /** 版本 */
    private String version;

    /** 创建时间 */
    private LocalDateTime createTime;
}