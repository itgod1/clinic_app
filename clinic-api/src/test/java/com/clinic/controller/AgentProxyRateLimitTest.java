package com.clinic.controller;

import com.clinic.common.util.AgentGuard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentGuard 纯逻辑单测：滑动窗口限流 + 活跃 run 互斥
 */
class AgentProxyRateLimitTest {

    @Test
    void sixPerMinuteThen429() {
        AgentGuard g = new AgentGuard();
        for (int i = 0; i < 6; i++) {
            assertTrue(g.tryAcquire("patient:7"), "前 6 次应放行, 第" + (i + 1) + "次被拒");
        }
        assertFalse(g.tryAcquire("patient:7"), "第 7 次应限流");
    }

    @Test
    void limitIsPerUser() {
        AgentGuard g = new AgentGuard();
        for (int i = 0; i < 6; i++) g.tryAcquire("patient:7");
        assertFalse(g.tryAcquire("patient:7"));
        assertTrue(g.tryAcquire("patient:8"), "其他用户不受影响");
    }

    @Test
    void activeRunBlocksNewChat() {
        AgentGuard g = new AgentGuard();
        g.markActive("patient:7", "sess-1");
        assertTrue(g.hasActive("patient:7"));
        g.clearActive("patient:7");
        assertFalse(g.hasActive("patient:7"));
    }

    @Test
    void windowSlidesAfterAMinute() {
        AgentGuard g = new AgentGuard(60_000, 2); // 窗口 2 次, 便于验证滑动
        assertTrue(g.tryAcquire("patient:9"));
        assertTrue(g.tryAcquire("patient:9"));
        assertFalse(g.tryAcquire("patient:9"));
        g.expireNow("patient:9"); // 测试钩子：模拟窗口已滑过
        assertTrue(g.tryAcquire("patient:9"), "窗口滑过后应重新放行");
    }
}
