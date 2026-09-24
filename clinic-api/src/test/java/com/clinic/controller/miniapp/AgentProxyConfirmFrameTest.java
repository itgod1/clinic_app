package com.clinic.controller.miniapp;

import com.clinic.common.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HITL 确认帧识别单测：确认帧必须命中（放宽收尾阈值的前提），
 * 其他透传帧不得误伤（否则正常对话被无限拖延收尾）。
 */
class AgentProxyConfirmFrameTest {

    @Test
    void failureResultCarriesExplicitJsonContentType() {
        // 客户端 Accept: text/event-stream 时，失败分支裸返回 Result 会被
        // 内容协商盖成 406——asJson 显式声明 JSON，跳过协商
        ResponseEntity<Result<Object>> r = AgentProxyController.asJson(Result.error(429, "请求过于频繁"));
        assertEquals(200, r.getStatusCodeValue());
        assertEquals(MediaType.APPLICATION_JSON, r.getHeaders().getContentType());
        assertNotNull(r.getBody());
        assertEquals(429, r.getBody().getCode());
        assertEquals("请求过于频繁", r.getBody().getMessage());
    }

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
