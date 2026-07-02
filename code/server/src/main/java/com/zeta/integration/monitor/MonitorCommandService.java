package com.zeta.integration.monitor;

import com.zeta.integration.queue.ScreenQueueMessage;
import com.zeta.integration.queue.ScreenQueuePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;

/**
 * 向 monitord 发送命令并处理响应的核心服务。
 * 维护 req_id → CompletableFuture 映射，实现请求-响应关联。
 */
@Service
public class MonitorCommandService {

    private static final Logger log = LoggerFactory.getLogger(MonitorCommandService.class);
    private static final long DEFAULT_TIMEOUT_SECONDS = 30;

    private final ScreenQueuePublisher publisher;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /** req_id → 等待响应的 Future */
    private final ConcurrentHashMap<String, CompletableFuture<ScreenQueueMessage>> pendingRequests = new ConcurrentHashMap<>();

    /** 最近一次响应缓存（command:cabinetId → response data） */
    private final ConcurrentHashMap<String, Map<String, Object>> latestResponses = new ConcurrentHashMap<>();

    /** 实验监视完成响应缓存（taskUuid → response data） */
    private final ConcurrentHashMap<String, Map<String, Object>> monitorTaskResults = new ConcurrentHashMap<>();

    /** 统计数据 */
    private final java.util.concurrent.atomic.AtomicLong totalSent = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong totalReceived = new java.util.concurrent.atomic.AtomicLong(0);
    private volatile long lastSentAt = 0;
    private volatile long lastReceivedAt = 0;
    private final ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicLong> sentByCommand = new ConcurrentHashMap<>();

    /** 最近消息日志（环形缓冲，保留最近 20 条） */
    private final java.util.concurrent.ConcurrentLinkedDeque<Map<String, Object>> recentMessages = new java.util.concurrent.ConcurrentLinkedDeque<>();
    private static final int MAX_RECENT = 20;

    public MonitorCommandService(Optional<ScreenQueuePublisher> publisher) {
        this.publisher = publisher.orElse(null);
    }

    /**
     * 发送压板状态读取命令。
     */
    public CompletableFuture<ScreenQueueMessage> sendPressboardStatusRequest(Long cabinetId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cabinet_id", cabinetId);
        data.put("refresh", true);
        data.put("pressboard_ids", Collections.emptyList());
        data.put("types", Arrays.asList("FUNCTION", "EXPORT"));
        data.put("include_spare", false);

        return sendCommand("summon_pressboard_status", data, "pressboard:" + cabinetId);
    }

    /**
     * 发送端子状态读取命令。
     */
    public CompletableFuture<ScreenQueueMessage> sendTerminalStatusRequest(Long cabinetId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cabinet_id", cabinetId);
        data.put("refresh", true);
        data.put("terminal_ids", Collections.emptyList());

        return sendCommand("summon_terminal_status", data, "terminal:" + cabinetId);
    }

    // ── 实验监视 summon_logic_monitor ──────────────────────────────────────

    /**
     * 启动实验监视任务。返回 accepted 响应（含 req_id 作为 taskUuid）。
     */
    public CompletableFuture<ScreenQueueMessage> startLogicMonitor(String iedName, String logicId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("action", "start");
        data.put("ied_name", iedName);
        data.put("logic_id", logicId);

        return sendCommand("summon_logic_monitor", data, null);
    }

    /**
     * 发送心跳（fire-and-forget，不等待响应）。
     */
    public void sendLogicMonitorHeartbeat(String taskUuid) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("action", "heartbeat");
        data.put("task_uuid", taskUuid);

        fireAndForget("summon_logic_monitor", data);
    }

    /**
     * 正常结束监视任务。
     */
    public void endLogicMonitor(String taskUuid) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("action", "end");
        data.put("task_uuid", taskUuid);

        fireAndForget("summon_logic_monitor", data);
    }

    /**
     * 立即中止监视任务。
     */
    public void abortLogicMonitor(String taskUuid) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("action", "abort");
        data.put("task_uuid", taskUuid);

        fireAndForget("summon_logic_monitor", data);
    }

    /**
     * 获取实验监视任务的完成结果。
     */
    public Map<String, Object> getMonitorTaskResult(String taskUuid) {
        return monitorTaskResults.get(taskUuid);
    }

    /**
     * 处理 monitord 返回的响应消息。
     */
    public void handleResponse(ScreenQueueMessage message) {
        String reqId = message.getReqId();
        String command = message.getCommand();
        log.info("Received monitord response: command={} req_id={} success={}", command, reqId, message.getSuccess());

        Map<String, Object> data = message.getData();
        totalReceived.incrementAndGet();
        lastReceivedAt = System.currentTimeMillis();
        addRecentMessage("IN", command, reqId, data);

        // 实验监视完成响应：按 req_id（taskUuid）缓存
        if ("summon_logic_monitor".equals(command) && data != null) {
            String result = String.valueOf(data.getOrDefault("result", ""));
            if ("success".equals(result) || "failed".equals(result)) {
                monitorTaskResults.put(reqId, data);
                log.info("Cached monitor task result for req_id={} result={}", reqId, result);
            }
        }

        // 压板/端子 completed 响应缓存
        if (data != null) {
            String phase = String.valueOf(data.getOrDefault("phase", ""));
            if ("completed".equals(phase)) {
                Long cabinetId = extractCabinetId(data);
                if (cabinetId != null) {
                    String cacheKey = command + ":" + cabinetId;
                    latestResponses.put(cacheKey, data);
                    log.info("Cached latest response for {}", cacheKey);
                }
            }
        }

        // 完成等待中的 Future
        if (reqId != null) {
            CompletableFuture<ScreenQueueMessage> future = pendingRequests.remove(reqId);
            if (future != null) {
                future.complete(message);
            }
        }
    }

    /**
     * 获取最近一次缓存的响应。
     */
    public Map<String, Object> getLatestResponse(String command, Long cabinetId) {
        return latestResponses.get(command + ":" + cabinetId);
    }

    /**
     * 队列是否可用。
     */
    public boolean isQueueEnabled() {
        return publisher != null;
    }

    /**
     * 当前等待响应的请求数。
     */
    public int getPendingRequestCount() {
        return pendingRequests.size();
    }

    /**
     * 获取统计信息和最近消息日志。
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalSent", totalSent.get());
        stats.put("totalReceived", totalReceived.get());
        stats.put("lastSentAt", lastSentAt);
        stats.put("lastReceivedAt", lastReceivedAt);
        stats.put("pendingRequests", pendingRequests.size());

        Map<String, Long> byCommand = new LinkedHashMap<>();
        for (Map.Entry<String, java.util.concurrent.atomic.AtomicLong> e : sentByCommand.entrySet()) {
            byCommand.put(e.getKey(), e.getValue().get());
        }
        stats.put("sentByCommand", byCommand);

        List<Map<String, Object>> messages = new ArrayList<>();
        for (Map<String, Object> msg : recentMessages) {
            messages.add(msg);
        }
        stats.put("recentMessages", messages);

        return stats;
    }

    private void addRecentMessage(String direction, String command, String reqId, Map<String, Object> data) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("time", System.currentTimeMillis());
        entry.put("direction", direction);
        entry.put("command", command);
        entry.put("reqId", reqId);
        entry.put("data", data);
        recentMessages.addFirst(entry);
        while (recentMessages.size() > MAX_RECENT) {
            recentMessages.removeLast();
        }
    }

    /**
     * 发送命令但不等待响应（用于 heartbeat/end/abort）。
     */
    private void fireAndForget(String command, Map<String, Object> data) {
        if (publisher == null) {
            log.warn("Redis 队列未启用，无法发送 {} 命令", command);
            return;
        }
        String reqId = UUID.randomUUID().toString();
        ScreenQueueMessage message = new ScreenQueueMessage(command, reqId, data);
        publisher.publish(message);
        totalSent.incrementAndGet();
        lastSentAt = System.currentTimeMillis();
        sentByCommand.computeIfAbsent(command, k -> new java.util.concurrent.atomic.AtomicLong(0)).incrementAndGet();
        addRecentMessage("OUT", command, reqId, data);
        log.info("Sent monitord fire-and-forget: {} req_id={} data={}", command, reqId, data);
    }

    private CompletableFuture<ScreenQueueMessage> sendCommand(String command, Map<String, Object> data, String cachePrefix) {
        if (publisher == null) {
            CompletableFuture<ScreenQueueMessage> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Redis 队列未启用，无法发送命令"));
            return future;
        }

        String reqId = UUID.randomUUID().toString();
        ScreenQueueMessage message = new ScreenQueueMessage(command, reqId, data);

        CompletableFuture<ScreenQueueMessage> future = new CompletableFuture<>();
        pendingRequests.put(reqId, future);

        // 超时处理
        scheduler.schedule(() -> {
            CompletableFuture<ScreenQueueMessage> pending = pendingRequests.remove(reqId);
            if (pending != null && !pending.isDone()) {
                pending.completeExceptionally(new TimeoutException("命令超时（" + DEFAULT_TIMEOUT_SECONDS + "s）"));
            }
        }, DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        publisher.publish(message);
        totalSent.incrementAndGet();
        lastSentAt = System.currentTimeMillis();
        sentByCommand.computeIfAbsent(command, k -> new java.util.concurrent.atomic.AtomicLong(0)).incrementAndGet();
        addRecentMessage("OUT", command, reqId, data);
        log.info("Sent monitord command: {} req_id={} data={}", command, reqId, data);

        return future;
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdown();
    }

    private Long extractCabinetId(Map<String, Object> data) {
        Object val = data.get("cabinet_id");
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return null;
    }
}
