package com.clinic.controller.miniapp;

import com.clinic.controller.miniapp.AgentProxyController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HITL 确认帧识别单测：确认帧必须命中（放宽收尾阈值的前提），
 * 其他透传帧不得误伤（否则正常对话被无限拖延收尾）。
 */
class AgentProxyConfirmFrameTest {

    @Test
    void confirmFrameWithCompactJson() {
        assertTrue(AgentProxyController.isConfirmFrame(
                "{\"type\":\"REQUIRE_USER_CONFIRM\",\"reply_id\":\"r1\",\"tool_calls\":[]}"));
    }

    @Test
    void confirmFrameWithSpacedJson() {
        assertTrue(AgentProxyController.isConfirmFrame(
                "{\"type\": \"REQUIRE_USER_CONFIRM\", \"reply_id\": \"r1\"}"));
    }

    @Test
    void ordinaryFramesDoNotMatch() {
        assertFalse(AgentProxyController.isConfirmFrame(
                "{\"type\":\"TEXT_BLOCK_DELTA\",\"delta\":\"正在查询排班…\"}"));
        assertFalse(AgentProxyController.isConfirmFrame(
                "{\"type\":\"PROXY_QUIET_END\"}"));
        // 工具入参里若恰含该词组也不算确认帧（tool_call 输入是业务参数）
        assertFalse(AgentProxyController.isConfirmFrame(
                "{\"type\":\"TOOL_CALL_BEGIN\",\"input\":{\"note\":\"REQUIRE_USER_CONFIRM 教程\"}}")
                || AgentProxyController.isConfirmFrame(
                "{\"type\":\"TOOL_CALL_BEGIN\",\"input\":{\"note\":\"require_user_confirm\"}}"));
    }

    @Test
    void nullAndEmptySafe() {
        assertFalse(AgentProxyController.isConfirmFrame(null));
        assertFalse(AgentProxyController.isConfirmFrame(""));
    }
}
