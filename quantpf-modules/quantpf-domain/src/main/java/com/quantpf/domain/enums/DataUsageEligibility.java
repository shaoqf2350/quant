package com.quantpf.domain.enums;

/**
 * 数据用途资格（《详设_1 §4.3 / §9.4 用途资格矩阵》）。
 * <p>同一数据对不同下游用途的资格必须分开判断，由确定性规则计算，不由 AI 自由文本决定。</p>
 * <p>对应指导文档 §6.3 {@code DataUsageEligibility}。</p>
 */
public enum DataUsageEligibility {

    /** 允许展示（受限/提示口径由质量状态矩阵决定） */
    DISPLAY_ALLOWED("允许展示"),
    /** 允许进入研究链路 */
    RESEARCH_ALLOWED("允许研究"),
    /** 允许进入候选筛选 */
    SCREENING_ALLOWED("允许筛选"),
    /** 允许作为策略输入 */
    STRATEGY_ALLOWED("允许策略"),
    /** 允许用于回测（需历史完整性） */
    BACKTEST_ALLOWED("允许回测"),
    /** 允许 Paper 新开仓 */
    PAPER_ALLOWED("允许Paper新开仓"),
    /** 允许风险监控 */
    RISK_MONITORING_ALLOWED("允许风险监控");

    private final String desc;

    DataUsageEligibility(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
