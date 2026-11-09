package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 研究草稿视图对象（§10.3）。
 */
@Data
public class AiResearchDraftVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    private Long pkId;

    /** 研究任务ID */
    private Long researchTaskId;

    /** 运行ID */
    private String runId;

    /** 模型版本 */
    private String modelVersion;

    /** 摘要 */
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

    /** 校验状态 */
    private String validationStatus;

    /** 校验状态中文描述 */
    private String validationStatusText;

    /** 创建时间 */
    private LocalDateTime createTime;
}