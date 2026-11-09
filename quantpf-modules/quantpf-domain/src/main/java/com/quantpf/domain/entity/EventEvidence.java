package com.quantpf.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 研究事件证据实体（《详设_1 §19.1》）。
 * <p>对应表 {@code quantpf_event_evidence}。</p>
 */
@Data
@TableName("quantpf_event_evidence")
public class EventEvidence implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long pkId;

    /** 研究事件ID */
    private Long researchEventId;

    /** 证据类型（FACT/OPINION/COUNTER） */
    private String evidenceType;

    /** 来源数据源ID */
    private Long sourceId;

    /** 来源引用 */
    private String sourceRef;

    /** 原文发布时间 */
    private LocalDateTime publishedAt;

    /** 内容哈希 */
    private String contentHash;

    /** 原文片段引用 */
    private String quoteText;

    /** 创建时间 */
    private LocalDateTime createTime;
}