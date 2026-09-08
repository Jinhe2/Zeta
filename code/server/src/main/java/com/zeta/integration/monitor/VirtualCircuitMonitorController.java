package com.zeta.integration.monitor;

import com.zeta.business.auth.AuthService;
import com.zeta.integration.queue.ScreenQueueMessage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** 对接既有 monitord 虚回路状态命令的 Web 接口。 */
@RestController
@RequestMapping("/api/monitor/commands")
public class VirtualCircuitMonitorController {

  private final MonitorCommandService commandService;
  private final AuthService authService;

  public VirtualCircuitMonitorController(
      MonitorCommandService commandService, AuthService authService) {
    this.commandService = commandService;
    this.authService = authService;
  }

  @PostMapping("/virtual-circuit-status")
  public Map<String, Object> trigger(
      @RequestHeader("Authorization") String authorization,
      @RequestBody Map<String, Object> body) {
    authService.requireUser(authorization);
    long cabinetId = requirePositiveLong(body.get("cabinetId"), "缺少 cabinetId 参数");
    long iedDeviceId = requirePositiveLong(body.get("iedDeviceId"), "缺少 iedDeviceId 参数");
    return await(commandService.sendVirtualCircuitStatusRequest(cabinetId, iedDeviceId));
  }

  private Map<String, Object> await(CompletableFuture<ScreenQueueMessage> future) {
    try {
      ScreenQueueMessage response = future.get(30, TimeUnit.SECONDS);
      Map<String, Object> data = response.getData();
      // PARTIAL_READ 虽然 success=false，但仍包含已完成的逐信号数据。
      if (data != null && "completed".equals(String.valueOf(data.getOrDefault("phase", "")))) {
        Map<String, Object> result = new LinkedHashMap<>(data);
        result.put("success", !Boolean.FALSE.equals(response.getSuccess()));
        if (response.getError() != null) result.put("error", response.getError());
        if (response.getErrorMessage() != null) result.put("errorMessage", response.getErrorMessage());
        return result;
      }
      if (Boolean.FALSE.equals(response.getSuccess())) {
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR, "monitord 返回错误: " + response.getErrorMessage());
      }
      return data;
    } catch (java.util.concurrent.TimeoutException e) {
      throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "等待 monitord 响应超时");
    } catch (java.util.concurrent.ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof java.util.concurrent.TimeoutException) {
        throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "等待 monitord 响应超时");
      }
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "命令执行失败: " + (cause != null ? cause.getMessage() : e.getMessage()));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "命令被中断");
    }
  }

  private long requirePositiveLong(Object value, String message) {
    if (!(value instanceof Number) || ((Number) value).longValue() <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    return ((Number) value).longValue();
  }
}
