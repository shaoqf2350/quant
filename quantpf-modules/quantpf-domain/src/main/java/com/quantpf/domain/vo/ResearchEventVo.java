package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 研究事件视图对象（§10.4）。
 */
@Data
public class ResearchEventVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    private Long pkId;

    /** 研究事件业务ID */
    private String eventCode;

    /** 事件类型 */
    private String eventType;

    /** 影响层级 */
    private String impactScope;

    /** 重大程度 */
    private String impactLevel;

    /** 方向 */
    private String direction;

    /** 方向中文描述 */
    private String directionText;

    /** 主体类型 */
    private String subjectType;

    /** 主体ID */
    private Long subjectId;

    /** 事件状态 */
    private String status;

    /** 事件状态中文描述 */
    private String statusText;

    /** 版本 */
    private String version;

    /** 来源/材料引用 */
    private String sourceRefs;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}