package com.clinic.controller.miniapp;

import com.clinic.common.result.Result;
import com.clinic.common.util.AgentGuard;
import com.clinic.common.util.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 小程序智能助手代理：clinic-api(:8080) -> AgentScope 主服务(:8000)。
 *
 * 链路：POST /miniapp/agent/chat 触发 run 并把 :8000 的 leader SSE 流
 * 逐行透传给小程序；写操作确认(HITL)经 /confirm 以 USER_CONFIRM_RESULT
 * 恢复 run，事件继续走同一条 SSE 连接。
 *
 * 护栏（AgentGuard）：每用户 6 次/分钟；同用户同时只允许一个活跃 run；
 * watchdog 超时自动调 :8000 interrupt 并关闭 SSE；HITL 确认等待期
 * （REQUIRE_USER_CONFIRM 已透传、患者尚未点确认）静默与绝对上限放宽
 * CONFIRM_GRACE_MS，避免"事办了、回执丢"。
 */
@Slf4j
@Api(tags = "小程序智能助手代理")
@RestController
@RequestMapping("/miniapp/agent")
public class AgentProxyController {

    @Value("${clinic.agent.base-url:http://localhost:8000}")
    private String agentBaseUrl;

    @Value("${clinic.agent.secret:dev-clinic-secret}")
    private String agentSecret;

    /** watchdog 上限：实测 leader 派工 + worker 多轮工具调用可超 120s，取 240s 兜底（小程序端另有手动停止） */
    private static final long WATCHDOG_MS = 240_000;
    /** 流静默上限：leader 可能先答"稍等"就结束本轮(REPLY_END)，worker 结果作为后续唤醒 run 到达；
     *  REPLY_END 后保持订阅，静默超过该阈值才收尾（interrupt 幂等，无活跃 run 时为空操作） */
    private static final long QUIET_MS = 120_000;
    /** HITL 确认等待期：agent 冻结等人点确认卡，人比机器慢。静默与绝对
     *  上限在此期间同步放宽，否则患者犹豫超过 120s 流被收尾、确认后
     *  虽照常执行但最终答复无法再透传（事办了、回执丢）。 */
    private static final long CONFIRM_GRACE_MS = 8 * 60_000;

    private final AgentGuard guard = new AgentGuard();
    private final ObjectMapper om = new ObjectMapper();
    /** userId -> [agentId, sessionId]，每次 chat 经 bootstrap 刷新 */
    private final Map<Long, String[]> agentCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService watchdog =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "agent-watchdog");
                t.setDaemon(true);
                return t;
            });

    /** 控制类短调用（bootstrap/chat 触发/interrupt/confirm） */
    private final RestTemplate restTemplate = buildRestTemplate(5_000, 15_000);
    /** SSE 流读取：读超时须大于 watchdog，让 watchdog 先生效 */
    private final RestTemplate streamingRestTemplate = buildRestTemplate(5_000, 300_000);

    private static RestTemplate buildRestTemplate(int connectMs, int readMs) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(connectMs);
        f.setReadTimeout(readMs);
        return new RestTemplate(f);
    }

    // ==================================================================
    // 对话：触发 run + SSE 透传
    // ==================================================================

    @ApiOperation("发送消息（SSE 流式）")
    @PostMapping("/chat")
    public Object chat(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("用户未登录");
        }
        String key = "patient:" + userId;
        if (!guard.tryAcquire(key)) {
            return Result.error(429, "请求过于频繁，请一分钟后再试");
        }
        if (guard.hasActive(key)) {
            return Result.error(409, "上一条消息还在处理中，请等它完成或先点停止");
        }
        String jwt = extractJwt(request);
        if (jwt.isEmpty()) {
            return Result.unauthorized("缺少登录凭证");
        }
        String text = body.get("text") instanceof String ? (String) body.get("text") : null;
        if (text == null || text.trim().isEmpty()) {
            return Result.badRequest("消息内容不能为空");
        }

        // 每次都走 bootstrap：注册/续期患者 JWT（TTL 10 分钟）+ find-or-create
        String[] ids = bootstrap(userId, jwt);
        if (ids == null) {
            return Result.error("智能助手暂时不可用，请稍后再试");
        }
        String agentId = ids[0];
        String sessionId = ids[1];

        guard.markActive(key, sessionId);
        SseEmitter emitter = new SseEmitter(0L);

        // ---- 收尾控制：done 幂等开关 + 静默/绝对双阈值 + 定时器列表 ----
        java.util.concurrent.atomic.AtomicBoolean done =
                new java.util.concurrent.atomic.AtomicBoolean(false);
        final long[] lastFrame = {System.currentTimeMillis()};
        List<ScheduledFuture<?>> timers = new java.util.concurrent.CopyOnWriteArrayList<>();
        Runnable cleanup = () -> {
            if (done.compareAndSet(false, true)) {
                for (ScheduledFuture<?> t : timers) {
                    t.cancel(false);
                }
                guard.clearActive(key);
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                }
            }
        };
        String interruptPath = "/sessions/" + sessionId + "/interrupt?agent_id=" + agentId;
        // 绝对超时：watchdog 硬停（interrupt + 前端报错帧）。改为周期检查
        // 可延展的 deadline：HITL 确认等待期放宽（见 relayStream 内确认帧处理）
        java.util.concurrent.atomic.AtomicLong hardDeadline =
                new java.util.concurrent.atomic.AtomicLong(
                        System.currentTimeMillis() + WATCHDOG_MS);
        timers.add(watchdog.scheduleAtFixedRate(() -> {
            if (done.get() || System.currentTimeMillis() <= hardDeadline.get()) {
                return;
            }
            log.warn("agent watchdog 超时，自动停止 session={}", sessionId);
            callAgent(HttpMethod.POST, interruptPath, null, key);
            send(emitter, "{\"type\":\"PROXY_ERROR\",\"message\":\"回答超时，已自动停止\"}");
            cleanup.run();
        }, 10, 10, TimeUnit.SECONDS));
        // 静默超时：流上 QUIET_MS 无新帧即收尾（leader 可能已答完；若有残留 run，interrupt 幂等）
        timers.add(watchdog.scheduleAtFixedRate(() -> {
            if (!done.get() && System.currentTimeMillis() - lastFrame[0] > QUIET_MS) {
                log.info("agent 流静默 {}ms，收尾 session={}", QUIET_MS, sessionId);
                callAgent(HttpMethod.POST, interruptPath, null, key);
                send(emitter, "{\"type\":\"PROXY_QUIET_END\"}");
                cleanup.run();
            }
        }, 10, 10, TimeUnit.SECONDS));

        final String fText = text;
        Thread worker = new Thread(() -> {
            try {
                // 1) 触发 run（fire-and-forget，事件走 stream）
                int status = triggerChat(agentId, sessionId, fText, key);
                if (status >= 400) {
                    send(emitter, "{\"type\":\"PROXY_ERROR\",\"message\":\"触发对话失败("
                            + status + ")，请稍后再试\"}");
                    cleanup.run();
                    return;
                }
                // 首帧告知会话标识，前端可持久化 sessionId
                send(emitter, "{\"type\":\"PROXY_SESSION\",\"session_id\":\""
                        + sessionId + "\",\"agent_id\":\"" + agentId + "\"}");
                // 2) 打开 SSE 流并逐行透传（REPLY_END 不收尾，交给静默计时器）
                relayStream(agentId, sessionId, emitter, key, lastFrame, hardDeadline, cleanup);
            } catch (Exception e) {
                log.error("agent chat 代理失败 userId={}", userId, e);
                send(emitter, "{\"type\":\"PROXY_ERROR\",\"message\":\"连接中断，请重试\"}");
                cleanup.run();
            }
        }, "agent-sse-" + userId);
        worker.setDaemon(true);
        worker.start();
        return emitter;
    }

    /** 读 :8000 leader 会话流，data: 行透传；流关闭或客户端断开即收尾 */
    private void relayStream(String agentId, String sessionId, SseEmitter emitter,
                             String key, long[] lastFrame,
                             java.util.concurrent.atomic.AtomicLong hardDeadline,
                             Runnable cleanup) {
        HttpHeaders headers = agentHeaders(key);
        streamingRestTemplate.execute(
                agentBaseUrl + "/sessions/" + sessionId + "/stream?agent_id=" + agentId,
                HttpMethod.GET,
                req -> req.getHeaders().addAll(headers),
                resp -> {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(resp.getBody(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (!line.startsWith("data:")) {
                                continue;
                            }
                            String payload = line.substring(5).trim();
                            if (payload.isEmpty()) {
                                continue;
                            }
                            lastFrame[0] = System.currentTimeMillis();
                            if (isConfirmFrame(payload)) {
                                // HITL 卡片已透传：agent 冻结等人决定，静默与
                                // 绝对上限同步放宽；确认后帧恢复流动，
                                // 静默判定自动回到正常 120s 节奏
                                lastFrame[0] = System.currentTimeMillis() + CONFIRM_GRACE_MS;
                                hardDeadline.set(System.currentTimeMillis()
                                        + WATCHDOG_MS + CONFIRM_GRACE_MS);
                                log.info("HITL 确认等待，收尾阈值放宽 session={}", sessionId);
                            }
                            if (!send(emitter, payload)) {
                                // 客户端已断开
                                cleanup.run();
                                return null;
                            }
                        }
                    }
                    // 服务端关闭了流（run 结束且无后续唤醒）
                    cleanup.run();
                    return null;
                });
    }

    /** HITL 确认帧识别（对透传 payload 原文）：命中则进入确认等待期，
     *  收尾阈值放宽（见 chat 内计时器与 relayStream）。包可见以便单测。 */
    static boolean isConfirmFrame(String payload) {
        return payload != null && payload.contains("\"REQUIRE_USER_CONFIRM\"");
    }

    // ==================================================================
    // 停止 / 确认 / 历史
    // ==================================================================

    @ApiOperation("停止当前回答")
    @PostMapping("/interrupt")
    public Result<Void> interrupt(@RequestBody(required = false) Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("用户未登录");
        }
        String key = "patient:" + userId;
        String[] ids = agentCache.get(userId);
        String sessionId = body != null && body.get("sessionId") instanceof String
                ? (String) body.get("sessionId")
                : (ids != null ? ids[1] : null);
        if (sessionId == null || ids == null) {
            return Result.badRequest("没有进行中的对话");
        }
        callAgent(HttpMethod.POST,
                "/sessions/" + sessionId + "/interrupt?agent_id=" + ids[0], null, key);
        guard.clearActive(key);
        return Result.success("已请求停止", null);
    }

    @ApiOperation("确认/拒绝写操作（HITL）")
    @PostMapping("/confirm")
    public Result<Void> confirm(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("用户未登录");
        }
        String key = "patient:" + userId;
        String[] ids = agentCache.get(userId);
        String replyId = str(body.get("replyId"));
        Object confirmed = body.get("confirmed");
        Object toolCalls = body.get("toolCalls");
        if (ids == null || replyId == null || confirmed == null
                || !(toolCalls instanceof List) || ((List<?>) toolCalls).isEmpty()) {
            return Result.badRequest("缺少 replyId/confirmed/toolCalls");
        }
        Map<String, Object> confirmResult = new HashMap<>();
        confirmResult.put("confirmed", confirmed);
        confirmResult.put("tool_call", ((List<?>) toolCalls).get(0));
        confirmResult.put("rules", null);
        Map<String, Object> input = new HashMap<>();
        input.put("type", "USER_CONFIRM_RESULT");
        input.put("reply_id", replyId);
        input.put("confirm_results", List.of(confirmResult));
        Map<String, Object> chatBody = new HashMap<>();
        chatBody.put("agent_id", ids[0]);
        chatBody.put("session_id", str(body.get("sessionId")) != null
                ? str(body.get("sessionId")) : ids[1]);
        chatBody.put("input", input);
        int status = postJson("/chat/", chatBody, key);
        if (status >= 400) {
            return Result.error("确认失败(" + status + ")，请重试");
        }
        return Result.success();
    }

    @ApiOperation("历史消息")
    @GetMapping("/history")
    public Result<Object> history(@RequestParam(required = false) String sessionId,
                                  @RequestParam(defaultValue = "50") Integer limit) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("用户未登录");
        }
        String[] ids = agentCache.get(userId);
        String sid = sessionId != null ? sessionId : (ids != null ? ids[1] : null);
        if (sid == null) {
            return Result.success(List.of());
        }
        ResponseEntity<String> resp = callAgent(HttpMethod.GET,
                "/sessions/" + sid + "/messages?limit=" + limit, null, "patient:" + userId);
        if (resp == null || resp.getBody() == null) {
            return Result.success(List.of());
        }
        try {
            JsonNode root = om.readTree(resp.getBody());
            return Result.success(root.get("messages"));
        } catch (Exception e) {
            return Result.success(List.of());
        }
    }

    // ==================================================================
    // 内部：:8000 调用
    // ==================================================================

    /** 注册 JWT + find-or-create，成功返回并刷新 [agentId, sessionId] 缓存 */
    private String[] bootstrap(Long userId, String jwt) {
        Map<String, Object> body = new HashMap<>();
        body.put("user_id", "patient:" + userId);
        body.put("jwt", jwt);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Registry-Key", agentSecret);
        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    agentBaseUrl + "/clinic/bootstrap", HttpMethod.POST,
                    new HttpEntity<>(om.writeValueAsString(body), headers), String.class);
            JsonNode root = om.readTree(resp.getBody());
            String[] ids = {root.get("agent_id").asText(), root.get("session_id").asText()};
            agentCache.put(userId, ids);
            return ids;
        } catch (Exception e) {
            log.error("agent bootstrap 失败 userId={}", userId, e);
            return null;
        }
    }

    private int triggerChat(String agentId, String sessionId, String text, String key) {
        Map<String, Object> input = new HashMap<>();
        input.put("name", "patient");
        input.put("role", "user");
        input.put("content", List.of(Map.of("type", "text", "text", text)));
        Map<String, Object> body = new HashMap<>();
        body.put("agent_id", agentId);
        body.put("session_id", sessionId);
        body.put("input", input);
        return postJson("/chat/", body, key);
    }

    /** key 非空时附带 X-User-ID；返回状态码，异常 -1 */
    private int postJson(String path, Map<String, Object> body, String userIdKey) {
        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    agentBaseUrl + path, HttpMethod.POST,
                    new HttpEntity<>(om.writeValueAsString(body), agentHeaders(userIdKey)),
                    String.class);
            return resp.getStatusCodeValue();
        } catch (Exception e) {
            log.error("agent POST {} 失败", path, e);
            return -1;
        }
    }

    private ResponseEntity<String> callAgent(HttpMethod method, String path,
                                             String jsonBody, String userIdKey) {
        try {
            HttpHeaders headers = agentHeaders(userIdKey);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            return restTemplate.exchange(agentBaseUrl + path, method, entity, String.class);
        } catch (Exception e) {
            log.error("agent {} {} 失败", method, path, e);
            return null;
        }
    }

    private HttpHeaders agentHeaders(String userIdKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (userIdKey != null) {
            headers.set("X-User-ID", userIdKey);
        }
        return headers;
    }

    // ==================================================================
    // SSE 收尾与 watchdog
    // ==================================================================

    /** 返回 false 表示客户端已断开，调用方应停止转发并收尾 */
    private boolean send(SseEmitter emitter, String data) {
        try {
            emitter.send(SseEmitter.event().data(data));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String str(Object o) {
        return o instanceof String ? (String) o : null;
    }

    /** 从 Authorization 头取 JWT（Bearer 前缀可选） */
    private static String extractJwt(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || auth.isEmpty()) {
            return "";
        }
        return auth.startsWith("Bearer ") ? auth.substring(7) : auth;
    }
}
