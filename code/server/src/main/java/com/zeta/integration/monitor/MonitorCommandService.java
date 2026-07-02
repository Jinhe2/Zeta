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

    /**
     * 处理 monitord 返回的响应消息。
     */
    public void handleResponse(ScreenQueueMessage message) {
        String reqId = message.getReqId();
        String command = message.getCommand();
        log.info("Received monitord response: command={} req_id={} success={}", command, reqId, message.getSuccess());

        // 缓存最新响应
        Map<String, Object> data = message.getData();
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
