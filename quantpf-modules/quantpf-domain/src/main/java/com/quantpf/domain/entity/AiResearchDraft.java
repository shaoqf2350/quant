package com.quantpf.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 研究草稿实体（《详设_1 §10.3》，非事实主表）。
 * <p>对应表 {@code quantpf_ai_research_draft}。</p>
 */
@Data
@TableName("quantpf_ai_research_draft")
public class AiResearchDraft implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long pkId;

    /** 研究任务ID */
    private Long researchTaskId;

    /** 运行ID */
    private String runId;

    /** 模型版本 */
    private String modelVersion;

    /** 新闻/材料摘要 */
    private String summary;

    /** 事实候选（JSON） */
    private String factCandidates;

    /** 影响范围候选 */
    private String impactScopeCandidate;

    /** 重大程度候选 */
    private String impactLevelCandidate;

    /** 方向候选 */
    private String directionCandidate;

    /** 实体/行业/商品候选（JSON） */
    private String subjectCandidates;

    /** 原文引用（JSON） */
    private String citations;

    /** 反方问题（JSON） */
    private String counterQuestions;

    /** 警告 */
    private String warnings;

    /** 原始 AI 响应 */
    private String rawResponse;

    /** 校验状态 */
    private String validationStatus;

    /** 创建时间 */
    private LocalDateTime createTime;
}