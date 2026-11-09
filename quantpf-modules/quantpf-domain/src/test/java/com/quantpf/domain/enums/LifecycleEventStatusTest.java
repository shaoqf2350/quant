package com.quantpf.domain.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 公司行为/合约事件生命周期状态机单元测试（《详设_1 §13.3》）。
 */
class LifecycleEventStatusTest {

    @Test
    void 正常生命周期链路合法() {
        assertTrue(LifecycleEventStatus.canTransition("DISCOVERED", "VERIFIED"));
        assertTrue(LifecycleEventStatus.canTransition("VERIFIED", "SCHEDULED"));
        assertTrue(LifecycleEventStatus.canTransition("SCHEDULED", "EFFECTIVE"));
        assertTrue(LifecycleEventStatus.canTransition("EFFECTIVE", "EXPIRED"));
    }

    @Test
    void 任意非终态可被新版本废止() {
        assertTrue(LifecycleEventStatus.canTransition("DISCOVERED", "SUPERSEDED"));
        assertTrue(LifecycleEventStatus.canTransition("VERIFIED", "SUPERSEDED"));
        assertTrue(LifecycleEventStatus.canTransition("SCHEDULED", "SUPERSEDED"));
        assertTrue(LifecycleEventStatus.canTransition("EFFECTIVE", "SUPERSEDED"));
    }

    @Test
    void 越级迁移非法_未核验事件不得直接生效() {
        assertFalse(LifecycleEventStatus.canTransition("DISCOVERED", "EFFECTIVE"));
        assertFalse(LifecycleEventStatus.canTransition("DISCOVERED", "SCHEDULED"));
        assertFalse(LifecycleEventStatus.canTransition("VERIFIED", "EFFECTIVE"));
        assertFalse(LifecycleEventStatus.canTransition("VERIFIED", "EXPIRED"));
    }

    @Test
    void 终态不可再迁移() {
        assertFalse(LifecycleEventStatus.canTransition("EXPIRED", "EFFECTIVE"));
        assertFalse(LifecycleEventStatus.canTransition("EXPIRED", "VERIFIED"));
        assertFalse(LifecycleEventStatus.canTransition("SUPERSEDED", "DISCOVERED"));
        assertFalse(LifecycleEventStatus.canTransition("SUPERSEDED", "EFFECTIVE"));
    }

    @Test
    void 未知或空状态非法() {
        assertFalse(LifecycleEventStatus.canTransition(null, "VERIFIED"));
        assertFalse(LifecycleEventStatus.canTransition("DISCOVERED", null));
        assertFalse(LifecycleEventStatus.canTransition("NOT_A_STATUS", "VERIFIED"));
        assertFalse(LifecycleEventStatus.canTransition("DISCOVERED", "NOT_A_STATUS"));
    }

    @Test
    void 同状态迁移视为非法_必须显式变更() {
        assertFalse(LifecycleEventStatus.canTransition("DISCOVERED", "DISCOVERED"));
    }
}
