package com.quantpf.research.listener;

import com.quantpf.domain.event.ResearchEventPublishedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 研究事件发布监听器（《详设_1 §10.6》）。
 * <p>当前为下游消费占位；模块二（候选发现）后续在此扩展异步处理，事件幂等由 idempotencyKey 保证。</p>
 * <p>AFTER_COMMIT：事务提交后才触发，事务回滚不会让下游收到未落库的事件（§6.6 事件契约）。</p>
 */
@Slf4j
@Component
public class ResearchEventPublishedListener {

    /**
     * 处理研究事件发布（示例：记录日志，下游候选发现/深研/风险复核由此接入）。
     *
     * @param event 研究事件发布事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onResearchEventPublished(ResearchEventPublishedEvent event) {
        log.info("收到研究事件发布，eventCode={}，researchEventId={}，idempotencyKey={}",
            event.getEventCode(), event.getResearchEventId(), event.getIdempotencyKey());
    }
}