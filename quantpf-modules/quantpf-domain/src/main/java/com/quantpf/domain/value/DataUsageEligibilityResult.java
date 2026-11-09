package com.quantpf.domain.value;

import com.quantpf.domain.enums.DataUsageEligibility;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * 数据用途资格判定结果（确定性计算，《详设_1 §9.4 用途资格矩阵 / §16.1 资格计算输入》）。
 * <p>由质量状态（叠加授权、时间可见性、标的、时段、容量输入）按矩阵推出各用途资格；
 * 不允许过期/冲突/授权不明数据进入 Paper 新开仓（§24.4 验收门禁红线）。</p>
 */
public class DataUsageEligibilityResult {

    /** 质量状态（矩阵行键） */
    private final String qualityStatus;

    /** 各用途资格（true=允许） */
    private final Map<DataUsageEligibility, Boolean> eligibility;

    /** 阻断原因（普通语言，供前端「原因/下一步」展示） */
    private final String reason;

    public DataUsageEligibilityResult(String qualityStatus, Map<DataUsageEligibility, Boolean> eligibility, String reason) {
        this.qualityStatus = qualityStatus;
        this.eligibility = Collections.unmodifiableMap(eligibility);
        this.reason = reason;
    }

    public String getQualityStatus() {
        return qualityStatus;
    }

    public Map<DataUsageEligibility, Boolean> getEligibility() {
        return eligibility;
    }

    public String getReason() {
        return reason;
    }

    /**
     * 按《详设_1 §9.4》矩阵计算指定质量状态的用途资格。
     * <p>矩阵口径（行为质量状态，列为用途）：</p>
     * <table>
     *   <tr><th>质量状态</th><th>展示</th><th>研究</th><th>筛选</th><th>回测</th><th>Paper新开仓</th></tr>
     *   <tr><td>AVAILABLE</td><td>允许</td><td>允许</td><td>允许</td><td>允许(视历史完整性)</td><td>允许</td></tr>
     *   <tr><td>DELAYED</td><td>提示</td><td>限制</td><td>限制</td><td>禁止</td><td>禁止</td></tr>
     *   <tr><td>PARTIAL</td><td>允许</td><td>提示</td><td>限制</td><td>禁止</td><td>禁止</td></tr>
     *   <tr><td>CONFLICTED</td><td>展示差异</td><td>可人工研究</td><td>禁止</td><td>禁止</td><td>禁止</td></tr>
     *   <tr><td>UNAUTHORIZED</td><td>受限</td><td>禁止</td><td>禁止</td><td>禁止</td><td>禁止</td></tr>
     *   <tr><td>STALE</td><td>历史查看</td><td>复盘</td><td>禁止新判断</td><td>视点时版本</td><td>禁止</td></tr>
     *   <tr><td>INVALID</td><td>显示异常</td><td>禁止</td><td>禁止</td><td>禁止</td><td>禁止</td></tr>
     *   <tr><td>PENDING_REVIEW</td><td>允许查看</td><td>人工复核</td><td>禁止</td><td>禁止</td><td>禁止</td></tr>
     * </table>
     * <p>说明：「提示/限制/人工复核/复盘」按较严格口径折算为对应用途允许但需复核（研究类用途 true，
     * 自动化用途 false）；回测「视点时版本」折算为 STALE 时允许读取点时版本回放。</p>
     *
     * @param qualityStatus 质量状态编码（DataQualityStatus）
     * @param reason        阻断/限制原因（普通语言）
     * @return 用途资格结果
     */
    public static DataUsageEligibilityResult ofQuality(String qualityStatus, String reason) {
        Map<DataUsageEligibility, Boolean> map = new EnumMap<>(DataUsageEligibility.class);
        switch (qualityStatus == null ? "" : qualityStatus) {
            case "AVAILABLE" -> {
                map.put(DataUsageEligibility.DISPLAY_ALLOWED, true);
                map.put(DataUsageEligibility.RESEARCH_ALLOWED, true);
                map.put(DataUsageEligibility.SCREENING_ALLOWED, true);
                map.put(DataUsageEligibility.STRATEGY_ALLOWED, true);
                map.put(DataUsageEligibility.BACKTEST_ALLOWED, true);
                map.put(DataUsageEligibility.PAPER_ALLOWED, true);
                map.put(DataUsageEligibility.RISK_MONITORING_ALLOWED, true);
            }
            case "DELAYED" -> {
                map.put(DataUsageEligibility.DISPLAY_ALLOWED, true);
                map.put(DataUsageEligibility.RESEARCH_ALLOWED, false);
                map.put(DataUsageEligibility.SCREENING_ALLOWED, false);
                map.put(DataUsageEligibility.STRATEGY_ALLOWED, false);
                map.put(DataUsageEligibility.BACKTEST_ALLOWED, false);
                map.put(DataUsageEligibility.PAPER_ALLOWED, false);
                map.put(DataUsageEligibility.RISK_MONITORING_ALLOWED, true);
            }
            case "PARTIAL" -> {
                map.put(DataUsageEligibility.DISPLAY_ALLOWED, true);
                map.put(DataUsageEligibility.RESEARCH_ALLOWED, true);
                map.put(DataUsageEligibility.SCREENING_ALLOWED, false);
                map.put(DataUsageEligibility.STRATEGY_ALLOWED, false);
                map.put(DataUsageEligibility.BACKTEST_ALLOWED, false);
                map.put(DataUsageEligibility.PAPER_ALLOWED, false);
                map.put(DataUsageEligibility.RISK_MONITORING_ALLOWED, true);
            }
            case "CONFLICTED" -> {
                map.put(DataUsageEligibility.DISPLAY_ALLOWED, true);
                map.put(DataUsageEligibility.RESEARCH_ALLOWED, true);
                map.put(DataUsageEligibility.SCREENING_ALLOWED, false);
                map.put(DataUsageEligibility.STRATEGY_ALLOWED, false);
                map.put(DataUsageEligibility.BACKTEST_ALLOWED, false);
                map.put(DataUsageEligibility.PAPER_ALLOWED, false);
                map.put(DataUsageEligibility.RISK_MONITORING_ALLOWED, true);
            }
            case "UNAUTHORIZED" -> {
                map.put(DataUsageEligibility.DISPLAY_ALLOWED, false);
                map.put(DataUsageEligibility.RESEARCH_ALLOWED, false);
                map.put(DataUsageEligibility.SCREENING_ALLOWED, false);
                map.put(DataUsageEligibility.STRATEGY_ALLOWED, false);
                map.put(DataUsageEligibility.BACKTEST_ALLOWED, false);
                map.put(DataUsageEligibility.PAPER_ALLOWED, false);
                map.put(DataUsageEligibility.RISK_MONITORING_ALLOWED, false);
            }
            case "STALE" -> {
                map.put(DataUsageEligibility.DISPLAY_ALLOWED, true);
                map.put(DataUsageEligibility.RESEARCH_ALLOWED, true);
                map.put(DataUsageEligibility.SCREENING_ALLOWED, false);
                map.put(DataUsageEligibility.STRATEGY_ALLOWED, false);
                map.put(DataUsageEligibility.BACKTEST_ALLOWED, true);
                map.put(DataUsageEligibility.PAPER_ALLOWED, false);
                map.put(DataUsageEligibility.RISK_MONITORING_ALLOWED, true);
            }
            case "INVALID" -> {
                map.put(DataUsageEligibility.DISPLAY_ALLOWED, true);
                map.put(DataUsageEligibility.RESEARCH_ALLOWED, false);
                map.put(DataUsageEligibility.SCREENING_ALLOWED, false);
                map.put(DataUsageEligibility.STRATEGY_ALLOWED, false);
                map.put(DataUsageEligibility.BACKTEST_ALLOWED, false);
                map.put(DataUsageEligibility.PAPER_ALLOWED, false);
                map.put(DataUsageEligibility.RISK_MONITORING_ALLOWED, true);
            }
            case "PENDING_REVIEW" -> {
                map.put(DataUsageEligibility.DISPLAY_ALLOWED, true);
                map.put(DataUsageEligibility.RESEARCH_ALLOWED, true);
                map.put(DataUsageEligibility.SCREENING_ALLOWED, false);
                map.put(DataUsageEligibility.STRATEGY_ALLOWED, false);
                map.put(DataUsageEligibility.BACKTEST_ALLOWED, false);
                map.put(DataUsageEligibility.PAPER_ALLOWED, false);
                map.put(DataUsageEligibility.RISK_MONITORING_ALLOWED, true);
            }
            default -> {
                // 未知质量状态按最严格口径处理（§5.6：不能确认时不进入下游）
                map.put(DataUsageEligibility.DISPLAY_ALLOWED, false);
                map.put(DataUsageEligibility.RESEARCH_ALLOWED, false);
                map.put(DataUsageEligibility.SCREENING_ALLOWED, false);
                map.put(DataUsageEligibility.STRATEGY_ALLOWED, false);
                map.put(DataUsageEligibility.BACKTEST_ALLOWED, false);
                map.put(DataUsageEligibility.PAPER_ALLOWED, false);
                map.put(DataUsageEligibility.RISK_MONITORING_ALLOWED, false);
            }
        }
        return new DataUsageEligibilityResult(qualityStatus, map, reason);
    }
}
