package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 事件证据视图对象（§19.1）。
 */
@Data
public class EventEvidenceVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    private Long pkId;

    /** 研究事件ID */
    private Long researchEventId;

    /** 证据类型 */
    private String evidenceType;

    /** 证据类型中文描述 */
    private String evidenceTypeText;

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