package com.quantpf.marketdata.listener;

import com.quantpf.domain.event.CapacityStatusChangedEvent;
import com.quantpf.domain.event.ContractEventUpdatedEvent;
import com.quantpf.domain.event.CorporateActionUpdatedEvent;
import com.quantpf.domain.event.DataSourceHealthChangedEvent;
import com.quantpf.domain.event.MarketStateChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 公司行为 / 合约事件更新监听器（《详设_1 §13.4 / §16.3》）。
 * <p>监听统一 AFTER_COMMIT：事务提交后才触发，回滚不泄漏事件（指导文档 §6.6 事件契约）。</p>
 * <p>当前为下游重算占位；模块四（风险/交易）后续在此接入可开仓数量、保证金占用、组合压力测试、交易计划有效性与 Paper 风险资格重算。</p>
 */
@Slf4j
@Component
public class MarketEventPublishedListener {

    /**
     * 处理公司行为更新（除权除息/停复牌/ST/退市等 → 下游候选过滤/策略/回测重算）。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCorporateActionUpdated(CorporateActionUpdatedEvent event) {
        log.info("收到公司行为更新，instrumentId={}，actionCode={}，actionType={}",
            event.getInstrumentId(), event.getActionCode(), event.getActionType());
    }

    /**
     * 处理合约事件更新（保证金/涨跌停/限仓/移仓/交割等 → 下游风险与 Paper 资格重算）。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onContractEventUpdated(ContractEventUpdatedEvent event) {
        log.info("收到合约事件更新，instrumentId={}，eventCode={}，eventType={}",
            event.getInstrumentId(), event.getEventCode(), event.getEventType());
    }

    /**
     * 处理市场状态变更（趋势/波动/流动性/系统性风险等 → 下游策略/风控消费）。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onMarketStateChanged(MarketStateChangedEvent event) {
        log.info("收到市场状态变更，instrumentId={}，stateCode={}",
            event.getInstrumentId(), event.getStateCode());
    }

    /**
     * 处理容量状态变更（→ 下游交易计划资格判断消费）。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCapacityStatusChanged(CapacityStatusChangedEvent event) {
        log.info("收到容量状态变更，instrumentId={}，capacityStatus={}",
            event.getInstrumentId(), event.getCapacityStatus());
    }

    /**
     * 处理数据源健康状态变更（§6.3 来源组核验后：主备切换/部分可用/冲突/不可用）。
     * <p>消费端应据此调整采集依赖与降级策略：UNAVAILABLE/CONFLICTED 组暂停依赖其来源的新决策；
     * PARTIALLY_AVAILABLE/DEGRADED 组按用途资格矩阵限制下游（不伪造 HEALTHY）。</p>
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDataSourceHealthChanged(DataSourceHealthChangedEvent event) {
        log.info("收到数据源健康状态变更，eventId={}，groupCode={}，groupStatus={}，primarySwitched={}",
            event.getEventId(), event.getGroupCode(), event.getGroupStatus(), event.isPrimarySwitched());
    }
}