package com.quantpf.research.adaptor;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Dify/TradingAgents 研究草稿结果（《详设_1 §10.3》）。
 * <p>适配器返回给 Spring Boot 的结构化草稿，校验前一律视为草稿。</p>
 */
@Data
public class DifyDraftResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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

    /** 方向候选（利多/利空/中性/双向/不确定） */
    private String directionCandidate;

    /** 实体/行业/商品候选（JSON） */
    private String subjectCandidates;

    /** 原文引用（JSON） */
    private String citations;

    /** 反方问题（JSON） */
    private String counterQuestions;

    /** 警告 */
    private String warnings;

    /** 原始 AI 响应（保留） */
    private String rawResponse;
}