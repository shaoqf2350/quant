package com.quantpf.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据质量问题实体（《详设_1 §9》）。
 * <p>对应表 {@code quantpf_data_quality_issue}。</p>
 */
@Data
@TableName("quantpf_data_quality_issue")
public class DataQualityIssue implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long pkId;

    /** 问题编码 */
    private String issueCode;

    /** 关联原始快照ID */
    private Long snapshotId;

    /** 数据源ID */
    private Long sourceId;

    /** 影响对象类型（MATERIAL/MARKET_DATA/INSTRUMENT/CALENDAR） */
    private String subjectType;

    /** 影响对象ID */
    private Long subjectId;

    /** 质量维度（COMPLETENESS/VALIDITY/IDENTITY/TIME/AUTHORIZATION/CONSISTENCY/TIMELINESS/REVISION） */
    private String dimension;

    /** 数据质量状态（DataQualityStatus） */
    private String qualityStatus;

    /** 问题生命周期状态（DataQualityIssueStatus） */
    private String issueStatus;

    /** 严重级别（LOW/MEDIUM/HIGH/CRITICAL） */
    private String severity;

    /** 业务影响 */
    private String businessImpact;

    /** 处理人 */
    private String handler;

    /** 处理动作 */
    private String actionTaken;

    /** 处理依据 */
    private String basisRef;

    /** 修复版本 */
    private String fixVersion;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}