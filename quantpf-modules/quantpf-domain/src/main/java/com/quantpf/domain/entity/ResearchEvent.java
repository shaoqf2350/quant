package com.quantpf.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 研究事件实体（《详设_1 §10.4》，Spring Boot 校验后的事实主表）。
 * <p>对应表 {@code quantpf_research_event}。</p>
 */
@Data
@TableName("quantpf_research_event")
public class ResearchEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long pkId;

    /** 研究事件业务ID */
    private String eventCode;

    /** 事件类型候选 */
    private String eventType;

    /** 影响层级 */
    private String impactScope;

    /** 重大程度 */
    private String impactLevel;

    /** 方向 */
    private String direction;

    /** 主体类型 */
    private String subjectType;

    /** 主体ID */
    private Long subjectId;

    /** 事件状态 */
    private String status;

    /** 版本 */
    private String version;

    /** 来源/材料引用 */
    private String sourceRefs;

    /** 幂等键 */
    private String idempotencyKey;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}