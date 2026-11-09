package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据质量问题视图对象（§9，含状态中文描述）。
 */
@Data
public class DataQualityIssueVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    private Long pkId;

    /** 问题编码 */
    private String issueCode;

    /** 关联原始快照ID */
    private Long snapshotId;

    /** 数据源ID */
    private Long sourceId;

    /** 影响对象类型 */
    private String subjectType;

    /** 影响对象ID */
    private Long subjectId;

    /** 质量维度 */
    private String dimension;

    /** 数据质量状态 */
    private String qualityStatus;

    /** 数据质量状态中文描述 */
    private String qualityStatusText;

    /** 问题生命周期状态 */
    private String issueStatus;

    /** 问题生命周期状态中文描述 */
    private String issueStatusText;

    /** 严重级别 */
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