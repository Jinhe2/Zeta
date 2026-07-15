package com.zeta.integration.monitor;

import com.zeta.business.snapshot.LogicSnapshot;
import com.zeta.business.snapshot.LogicSnapshotRepository;
import com.zeta.integration.queue.ScreenQueueMessage;
import com.zeta.integration.queue.ScreenQueuePublisher;
import com.zeta.business.monitor.MonitorTask;
import com.zeta.business.monitor.MonitorTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

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
    private final LogicSnapshotRepository logicSnapshotRepository;
    private final MonitorTaskRepository monitorTaskRepository;
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

    public MonitorCommandService(Optional<ScreenQueuePublisher> publisher,
                                 LogicSnapshotRepository logicSnapshotRepository,
                                 MonitorTaskRepository monitorTaskRepository) {
        this.publisher = publisher.orElse(null);
        this.logicSnapshotRepository = logicSnapshotRepository;
        this.monitorTaskRepository = monitorTaskRepository;
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

        return sendCommand("summon_pressboard_status", null, data, "pressboard:" + cabinetId);
    }

    /**
     * 发送端子状态读取命令。
     */
    public CompletableFuture<ScreenQueueMessage> sendTerminalStatusRequest(Long cabinetId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cabinet_id", cabinetId);
        data.put("refresh", true);
        data.put("terminal_ids", Collections.emptyList());

        return sendCommand("summon_terminal_status", null, data, "terminal:" + cabinetId);
    }

    /**
     * 发送 IED 通讯状态读取命令。
     */
    public CompletableFuture<ScreenQueueMessage> sendIedCommStatusRequest(Long cabinetId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cabinet_id", cabinetId);
        data.put("ied_device_ids", Collections.emptyList());

        return sendCommand("summon_ied_comm_status", null, data, "ied-comm:" + cabinetId);
    }

    // ── 实验监视 summon_logic_monitor ──────────────────────────────────────

    /**
     * 启动实验监视任务。返回 accepted 响应（含 req_id 作为 taskUuid）。
     */
    public CompletableFuture<ScreenQueueMessage> startLogicMonitor(String iedName, String logicId,
                                                                    Long userId, String username) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("action", "start");
        data.put("ied_name", iedName);
        data.put("logic_id", logicId);

        return sendCommand("summon_logic_monitor", String.valueOf(userId), data, null);
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
     * 从 monitor_task 读取断面数据，创建 logic_snapshot 记录到业务库。
     */
    @Transactional("businessTransactionManager")
    private void createLogicSnapshotFromTask(Long taskId, String userIdStr) {
        MonitorTask task = monitorTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            log.warn("monitor_task {} not found, skip snapshot creation", taskId);
            return;
        }
        if (task.getSnapshotJson() == null || task.getSnapshotJson().isEmpty()) {
            log.warn("monitor_task {} has no snapshot_json, skip", taskId);
            return;
        }

        Long userId = null;
        if (userIdStr != null) {
            try { userId = Long.parseLong(userIdStr); } catch (NumberFormatException ignored) {}
        }

        LogicSnapshot snapshot = new LogicSnapshot();
        snapshot.setUserId(userId);
        snapshot.setLogicId(task.getLogicDiagramId());
        snapshot.setLogicCode(null);
        snapshot.setLogicName(null);
        snapshot.setSnapshotJson(task.getSnapshotJson());
        snapshot.setTotalTransitions(task.getTotalTransitions() != null ? task.getTotalTransitions() : 0);
        snapshot.setStatus("COMPLETED");
        snapshot.setSource("MONITOR");
        snapshot.setCreatedAt(Instant.now());
        snapshot.setCompletedAt(Instant.now());

        logicSnapshotRepository.save(snapshot);
        log.info("Created logic_snapshot id={} from monitor_task={} userId={}", snapshot.getId(), taskId, userIdStr);
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

        // 实验监视完成响应：按 req_id（taskUuid）缓存，成功时创建 logic_snapshot
        if ("summon_logic_monitor".equals(command) && data != null) {
            String result = String.valueOf(data.getOrDefault("result", ""));
            if ("success".equals(result) || "failed".equals(result)) {
                // 合并顶层 error 字段到 data 缓存中
                if (message.getError() != null) {
                    data.put("error", message.getError());
                }
                if (message.getErrorMessage() != null) {
                    data.put("error_message", message.getErrorMessage());
                }
                monitorTaskResults.put(reqId, data);
                log.info("Cached monitor task result for req_id={} result={}", reqId, result);

                // 成功且有 snapshot_path → 读取 monitor_task 创建 logic_snapshot
                if ("success".equals(result)) {
                    String snapshotPath = String.valueOf(data.getOrDefault("snapshot_path", ""));
                    String userIdStr = message.getUserData();
                    if (!snapshotPath.isEmpty() && !snapshotPath.equals("null") && userIdStr != null) {
                        try {
                            createLogicSnapshotFromTask(Long.parseLong(snapshotPath), userIdStr);
                        } catch (Exception e) {
                            log.error("Failed to create logic_snapshot from monitor_task {}: {}", snapshotPath, e.getMessage());
                        }
                    }
                }
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

        // 完成等待中的 Future。压板/端子会先返回 accepted，再返回 completed；
        // HTTP 调用需要等待 completed，否则前端拿不到实际状态数据。
        if (reqId != null) {
            if (shouldCompletePending(command, message)) {
                CompletableFuture<ScreenQueueMessage> future = pendingRequests.remove(reqId);
                if (future != null) {
                    future.complete(message);
                }
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

    private CompletableFuture<ScreenQueueMessage> sendCommand(String command, String userData,
                                                                Map<String, Object> data, String cachePrefix) {
        if (publisher == null) {
            CompletableFuture<ScreenQueueMessage> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Redis 队列未启用，无法发送命令"));
            return future;
        }

        String reqId = UUID.randomUUID().toString();
        ScreenQueueMessage message = new ScreenQueueMessage(command, reqId, userData, data);

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

    private boolean shouldCompletePending(String command, ScreenQueueMessage message) {
        if (Boolean.FALSE.equals(message.getSuccess())) {
            return true;
        }
        if ("summon_pressboard_status".equals(command)
                || "summon_terminal_status".equals(command)
                || "summon_ied_comm_status".equals(command)) {
            Map<String, Object> data = message.getData();
            return data != null && "completed".equals(String.valueOf(data.getOrDefault("phase", "")));
        }
        return true;
    }
}
