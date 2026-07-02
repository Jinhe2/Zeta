package com.zeta.integration.monitor;

import com.zeta.business.auth.AuthService;
import com.zeta.integration.queue.ScreenQueueMessage;
import com.zeta.integration.queue.ScreenQueueProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/monitor")
public class MonitorCommandController {

    private final MonitorCommandService commandService;
    private final AuthService authService;
    private final StringRedisTemplate redisTemplate;
    private final ScreenQueueProperties queueProperties;

    public MonitorCommandController(MonitorCommandService commandService,
                                    AuthService authService,
                                    StringRedisTemplate redisTemplate,
                                    ScreenQueueProperties queueProperties) {
        this.commandService = commandService;
        this.authService = authService;
        this.redisTemplate = redisTemplate;
        this.queueProperties = queueProperties;
    }

    /** 触发压板状态读取 */
    @PostMapping("/commands/pressboard-status")
    public Map<String, Object> triggerPressboardStatus(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, Object> body) {
        authService.requireUser(authorization);

        Long cabinetId = extractCabinetId(body);
        CompletableFuture<ScreenQueueMessage> future = commandService.sendPressboardStatusRequest(cabinetId);

        return awaitResponse(future, "summon_pressboard_status");
    }

    /** 触发端子状态读取 */
    @PostMapping("/commands/terminal-status")
    public Map<String, Object> triggerTerminalStatus(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, Object> body) {
        authService.requireUser(authorization);

        Long cabinetId = extractCabinetId(body);
        CompletableFuture<ScreenQueueMessage> future = commandService.sendTerminalStatusRequest(cabinetId);

        return awaitResponse(future, "summon_terminal_status");
    }

    /** 查询最近一次压板状态 */
    @GetMapping("/pressboard-status/{cabinetId}")
    public Map<String, Object> getPressboardStatus(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long cabinetId) {
        authService.requireUser(authorization);

        Map<String, Object> cached = commandService.getLatestResponse("summon_pressboard_status", cabinetId);
        if (cached == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "暂无压板状态数据，请先触发读取");
        }
        return cached;
    }

    /** 查询最近一次端子状态 */
    @GetMapping("/terminal-status/{cabinetId}")
    public Map<String, Object> getTerminalStatus(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long cabinetId) {
        authService.requireUser(authorization);

        Map<String, Object> cached = commandService.getLatestResponse("summon_terminal_status", cabinetId);
        if (cached == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "暂无端子状态数据，请先触发读取");
        }
        return cached;
    }

    // ── 实验监视 summon_logic_monitor ──────────────────────────────────────

    /**
     * 启动实验监视任务。返回 accepted 响应中的 req_id 作为 taskUuid。
     */
    @PostMapping("/commands/logic-monitor")
    public Map<String, Object> logicMonitorAction(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, Object> body) {
        authService.requireUser(authorization);

        String action = String.valueOf(body.getOrDefault("action", "start"));

        switch (action) {
            case "start": {
                String iedName = (String) body.get("iedName");
                String logicId = (String) body.get("logicId");
                if (iedName == null || logicId == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少 iedName 或 logicId");
                }
                CompletableFuture<ScreenQueueMessage> future = commandService.startLogicMonitor(iedName, logicId);
                Map<String, Object> response = awaitResponse(future, "summon_logic_monitor");
                // 返回 taskUuid（即 req_id）供前端心跳和查询使用
                Map<String, Object> result = new java.util.LinkedHashMap<>(response);
                return result;
            }
            case "heartbeat": {
                String taskUuid = (String) body.get("taskUuid");
                if (taskUuid == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少 taskUuid");
                }
                commandService.sendLogicMonitorHeartbeat(taskUuid);
                return java.util.Collections.singletonMap("status", "sent");
            }
            case "end": {
                String taskUuid = (String) body.get("taskUuid");
                if (taskUuid == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少 taskUuid");
                }
                commandService.endLogicMonitor(taskUuid);
                return java.util.Collections.singletonMap("status", "sent");
            }
            case "abort": {
                String taskUuid = (String) body.get("taskUuid");
                if (taskUuid == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少 taskUuid");
                }
                commandService.abortLogicMonitor(taskUuid);
                return java.util.Collections.singletonMap("status", "sent");
            }
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的 action: " + action);
        }
    }

    /**
     * 查询实验监视任务的完成结果（从内存缓存中读取）。
     */
    @GetMapping("/tasks/{taskUuid}/result")
    public Map<String, Object> getMonitorTaskResult(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String taskUuid) {
        authService.requireUser(authorization);

        Map<String, Object> result = commandService.getMonitorTaskResult(taskUuid);
        if (result == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务结果尚未返回");
        }
        return result;
    }

    /** 队列状态监控 */
    @GetMapping("/queue/status")
    public Map<String, Object> getQueueStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.requireUser(authorization);

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", commandService.isQueueEnabled());
        status.put("outboundKey", queueProperties.getOutboundKey());
        status.put("inboundKey", queueProperties.getInboundKey());

        if (commandService.isQueueEnabled()) {
            try {
                Long outboundLen = redisTemplate.opsForList().size(queueProperties.getOutboundKey());
                Long inboundLen = redisTemplate.opsForList().size(queueProperties.getInboundKey());
                status.put("outboundPending", outboundLen);
                status.put("inboundPending", inboundLen);
                status.put("redisConnected", true);
            } catch (Exception e) {
                status.put("redisConnected", false);
                status.put("redisError", e.getMessage());
            }
        }

        status.put("pendingRequests", commandService.getPendingRequestCount());
        status.putAll(commandService.getStats());
        return status;
    }

    private Map<String, Object> awaitResponse(CompletableFuture<ScreenQueueMessage> future, String command) {
        try {
            ScreenQueueMessage response = future.get(30, TimeUnit.SECONDS);
            if (Boolean.FALSE.equals(response.getSuccess())) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "monitord 返回错误: " + response.getErrorMessage());
            }
            return response.getData();
        } catch (java.util.concurrent.TimeoutException e) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "等待 monitord 响应超时");
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof java.util.concurrent.TimeoutException) {
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "等待 monitord 响应超时");
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "命令执行失败: " + (cause != null ? cause.getMessage() : e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "命令被中断");
        }
    }

    private Long extractCabinetId(Map<String, Object> body) {
        Object val = body.get("cabinetId");
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少 cabinetId 参数");
    }
}
