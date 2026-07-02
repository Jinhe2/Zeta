package com.zeta.integration.monitor;

import com.zeta.business.auth.AuthService;
import com.zeta.integration.queue.ScreenQueueMessage;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/monitor")
public class MonitorCommandController {

    private final MonitorCommandService commandService;
    private final AuthService authService;

    public MonitorCommandController(MonitorCommandService commandService, AuthService authService) {
        this.commandService = commandService;
        this.authService = authService;
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
