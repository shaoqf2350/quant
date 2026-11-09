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
 * 外汇/海外经纪商品时段规则实体（《详设_1 §12.2.5》，一期 schema 预留）。
 * <p>对应表 {@code quantpf_fx_session_rule}。</p>
 */
@Data
@TableName("quantpf_fx_session_rule")
public class FxSessionRule implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long pkId;

    /** Tushare/FX 品种代码 */
    private String tsCode;

    /** 品种分类 */
    private String classify;

    /** 交易商/交易所 */
    private String exchange;

    /** 交易时段原文 */
    private String tradingHours;

    /** 休市时段原文 */
    private String breakTime;

    /** 标准化时段规则（JSON） */
    private String normalizedSessions;

    /** 来源时区 */
    private String timezone;

    /** 生效开始 */
    private LocalDate validFrom;

    /** 生效结束 */
    private LocalDate validTo;

    /** 版本 */
    private String version;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}