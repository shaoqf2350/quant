package com.quantpf.domain.value;

import com.quantpf.domain.enums.DataUsageEligibility;
import com.quantpf.domain.value.DataUsageEligibilityResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据用途资格矩阵单元测试（《详设_1 §9.4 / §16.1》）。
 * <p>验收门禁红线：过期/冲突/授权不明数据可 Paper 新开仓 → 否决。</p>
 */
class DataUsageEligibilityResultTest {

    @Test
    void 可用数据全用途放开() {
        DataUsageEligibilityResult r = DataUsageEligibilityResult.ofQuality("AVAILABLE", "");
        for (DataUsageEligibility usage : DataUsageEligibility.values()) {
            assertTrue(r.getEligibility().get(usage), usage + " 应允许");
        }
    }

    @Test
    void 授权不明一律禁止_含风险监控() {
        DataUsageEligibilityResult r = DataUsageEligibilityResult.ofQuality("UNAUTHORIZED", "");
        for (DataUsageEligibility usage : DataUsageEligibility.values()) {
            assertFalse(r.getEligibility().get(usage), usage + " 应禁止");
        }
    }

    @Test
    void 冲突数据禁止Paper新开仓() {
        for (String status : new String[]{"CONFLICTED", "DELAYED", "PARTIAL", "STALE", "INVALID", "PENDING_REVIEW"}) {
            DataUsageEligibilityResult r = DataUsageEligibilityResult.ofQuality(status, "");
            assertFalse(r.getEligibility().get(DataUsageEligibility.PAPER_ALLOWED),
                status + " 应禁止 Paper 新开仓");
            assertFalse(r.getEligibility().get(DataUsageEligibility.SCREENING_ALLOWED),
                status + " 应禁止筛选");
        }
    }

    @Test
    void 过期数据可复盘与点时回测_但禁止新判断() {
        DataUsageEligibilityResult r = DataUsageEligibilityResult.ofQuality("STALE", "");
        assertTrue(r.getEligibility().get(DataUsageEligibility.RESEARCH_ALLOWED));
        assertTrue(r.getEligibility().get(DataUsageEligibility.BACKTEST_ALLOWED));
        assertFalse(r.getEligibility().get(DataUsageEligibility.SCREENING_ALLOWED));
        assertFalse(r.getEligibility().get(DataUsageEligibility.PAPER_ALLOWED));
    }

    @Test
    void 冲突数据允许人工研究_不允许自动流转() {
        DataUsageEligibilityResult r = DataUsageEligibilityResult.ofQuality("CONFLICTED", "");
        assertTrue(r.getEligibility().get(DataUsageEligibility.RESEARCH_ALLOWED));
        assertFalse(r.getEligibility().get(DataUsageEligibility.BACKTEST_ALLOWED));
        assertFalse(r.getEligibility().get(DataUsageEligibility.PAPER_ALLOWED));
    }

    @Test
    void 未知质量状态按最严格口径处理() {
        DataUsageEligibilityResult r = DataUsageEligibilityResult.ofQuality("WHATEVER", "");
        for (DataUsageEligibility usage : DataUsageEligibility.values()) {
            assertFalse(r.getEligibility().get(usage), "未知状态 " + usage + " 应按最严格口径禁止");
        }
        r = DataUsageEligibilityResult.ofQuality(null, "");
        assertFalse(r.getEligibility().get(DataUsageEligibility.PAPER_ALLOWED));
    }
}
