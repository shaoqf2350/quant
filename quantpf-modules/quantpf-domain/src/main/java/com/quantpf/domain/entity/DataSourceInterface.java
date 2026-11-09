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
 * 数据源接口目录实体（《详设_1 §5.2》）。
 * <p>对应表 {@code quantpf_data_source_interface}。</p>
 */
@Data
@TableName("quantpf_data_source_interface")
public class DataSourceInterface implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long pkId;

    /** 数据源ID（关联 quantpf_data_source.pk_id） */
    private Long sourceId;

    /** 数据产品编码 */
    private String productCode;

    /** 接口名（如 trade_cal / fut_trade_cal） */
    private String endpointName;

    /** 字段摘要 */
    private String fieldSummary;

    /** 历史数据起始 */
    private LocalDate historyFrom;

    /** 历史数据截止 */
    private LocalDate historyTo;

    /** 更新频率（DAILY/MINUTELY/ON_DEMAND） */
    private String updateFrequency;

    /** 延迟（REALTIME/DELAYED_1D...） */
    private String latency;

    /** 调用额度与并发限制 */
    private String quotaLimit;

    /** 授权状态 */
    private String authorizationStatus;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}