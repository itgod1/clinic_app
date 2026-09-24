package com.clinic.common.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 小程序 agent 代理的轻量护栏：滑动窗口限流 + 活跃 run 互斥。
 *
 * 纯逻辑无 Spring 依赖，便于单测；watchdog（超时 interrupt）由
 * AgentProxyController 用 ScheduledExecutor 驱动，不在这里。
 */
public class AgentGuard {

    private final long windowMs;
    private final int limit;
    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();
    /** key -> 进行中的 sessionId（同一用户同时只允许一个 run） */
    private final Map<String, String> active = new ConcurrentHashMap<>();

    public AgentGuard() {
        this(60_000, 6);
    }

    public AgentGuard(long windowMs, int limit) {
        this.windowMs = windowMs;
        this.limit = limit;
    }

    /** 滑动窗口限流：窗口内不足 limit 次则记一次并放行 */
    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        Deque<Long> q = windows.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() >= windowMs) {
                q.pollFirst();
            }
            if (q.size() >= limit) {
                return false;
            }
            q.addLast(now);
            return true;
        }
    }

    /** 测试钩子：清空窗口内时间戳，模拟时间已滑过窗口 */
    public void expireNow(String key) {
        Deque<Long> q = windows.get(key);
        if (q == null) {
            return;
        }
        synchronized (q) {
            q.clear();
        }
    }

    public void markActive(String key, String sessionId) {
        active.put(key, sessionId);
    }

    public boolean hasActive(String key) {
        return active.containsKey(key);
    }

    public void clearActive(String key) {
        active.remove(key);
    }
}
