package com.quantpf.research.adaptor;

import com.quantpf.domain.entity.ResearchTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mock 实现：施工期以样例草稿打通研究链（《详设_1 §25.2.1》：Dify 联调前用 Mock + 单元测试打通）。
 * <p>真实实现待 Dify 部署后用 WebClient 消费 Workflow API 流式响应（ADR-11 方案 A）替换。</p>
 */
@Slf4j
@Component
public class MockDifyResearchAdapter implements IDifyResearchAdapter {

    @Override
    public DifyDraftResult analyze(ResearchTask task) {
        log.info("MockDifyResearchAdapter 分析研究任务 taskCode={}，taskType={}", task.getTaskCode(), task.getTaskType());
        DifyDraftResult result = new DifyDraftResult();
        result.setRunId("mock-run-" + task.getTaskCode());
        result.setModelVersion("mock-model-1.0");
        result.setSummary("（Mock）针对材料【" + defaultIfBlank(task.getMaterialIds(), "无") + "】的结构化研究草稿摘要。");
        result.setFactCandidates("[{\"statement\":\"示例事实候选\",\"quoteRef\":\"示例原文引用\"}]");
        result.setImpactScopeCandidate("INDUSTRY");
        result.setImpactLevelCandidate("IMPORTANT");
        result.setDirectionCandidate("BULLISH");
        result.setSubjectCandidates("[{\"subjectType\":\"INSTRUMENT\",\"subjectName\":\"示例标的\"}]");
        result.setCitations("[{\"sourceRef\":\"示例来源\",\"quote\":\"示例原文片段\"}]");
        result.setCounterQuestions("[{\"question\":\"反方向是否成立？\"}]");
        result.setWarnings("Mock 草稿，仅用于施工期联调，不作为事实。");
        result.setRawResponse("{\"mock\":true}");
        return result;
    }

    private String defaultIfBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}