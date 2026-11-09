package com.quantpf.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 点时数据版本实体（《详设_1 §8.3/§8.4》）。
 * <p>对应表 {@code quantpf_data_point_in_time}，任何修订保留旧版本。</p>
 */
@Data
@TableName("quantpf_data_point_in_time")
public class DataPointInTime implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long pkId;

    /** 对象类型 */
    private String subjectType;

    /** 对象ID */
    private Long subjectId;

    /** 快照版本 */
    private String snapshotVersion;

    /** 决策时点 */
    private LocalDateTime decisionTime;

    /** 点时可获得时间 */
    private LocalDateTime visibleAt;

    /** 业务生效时间 */
    private LocalDateTime effectiveAt;

    /** 修订时间 */
    private LocalDateTime revisedAt;

    /** 修订原因 */
    private String revisionReason;

    /** 修订来源 */
    private String revisionSource;

    /** 点时数据引用 */
    private String payloadRef;

    /** 受影响对象 */
    private String affectedObjects;

    /** 是否需重算 */
    private Boolean needRecompute;

    /** 创建时间 */
    private LocalDateTime createTime;
}