package com.quantpf.research.adaptor;

import com.quantpf.domain.entity.ResearchTask;

/**
 * Dify/TradingAgents 研究适配器接口（《详设_1 §10 / ADR-11》）。
 * <p>只返回研究草稿，不直连数据库、不修改业务状态。</p>
 */
public interface IDifyResearchAdapter {

    /**
     * 调用 Dify Workflow / TradingAgents 对研究任务做结构化分析。
     *
     * @param task 研究任务（含材料引用、点时上下文）
     * @return 结构化研究草稿
     */
    DifyDraftResult analyze(ResearchTask task);
}