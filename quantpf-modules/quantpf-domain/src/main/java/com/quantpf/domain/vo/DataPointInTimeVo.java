package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 点时数据版本视图对象（§8.4）。
 */
@Data
public class DataPointInTimeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
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