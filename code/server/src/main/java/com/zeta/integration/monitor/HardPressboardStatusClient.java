package com.zeta.integration.monitor;

import com.zeta.integration.queue.ScreenQueueMessage;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** 调用 monitord 读取硬压板实时状态，并转换为可比的 台账id/状态(0/1) 映射。 */
@Service
public class HardPressboardStatusClient {
  private static final long AWAIT_SECONDS = 31;

  private final MonitorCommandService commandService;

  public HardPressboardStatusClient(MonitorCommandService commandService) {
    this.commandService = commandService;
  }

  public StatusResult summon(Long cabinetId) {
    try {
      ScreenQueueMessage response = commandService.sendPressboardStatusRequest(cabinetId)
          .get(AWAIT_SECONDS, TimeUnit.SECONDS);
      if (!Boolean.TRUE.equals(response.getSuccess())) {
        String message = response.getErrorMessage();
        if (message == null || message.trim().isEmpty()) message = response.getError();
        if (message == null || message.trim().isEmpty()) message = "硬压板读取失败";
        throw unavailable(message);
      }
      return parse(response.getData());
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (TimeoutException ex) {
      throw unavailable("读取硬压板超时，请检查装置通讯状态");
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw unavailable("读取硬压板被中断");
    } catch (ExecutionException ex) {
      Throwable cause = ex.getCause();
      throw unavailable("读取硬压板失败：" +
          (cause == null ? ex.getMessage() : cause.getMessage()));
    }
  }

  private StatusResult parse(Map<String, Object> data) {
    Object rawItems = data == null ? null : data.get("pressboards");
    if (!(rawItems instanceof List<?>)) {
      throw unavailable("硬压板响应格式错误");
    }
    List<?> items = (List<?>) rawItems;
    Map<String, Double> values = new LinkedHashMap<>();
    for (Object rawItem : items) {
      if (!(rawItem instanceof Map<?, ?>)) continue;
      Map<?, ?> item = (Map<?, ?>) rawItem;
      Object id = item.get("pressboard_id");
      if (id == null) id = item.get("id");
      if (id == null) continue;
      Double state = parseState(item.get("state"));
      if (state != null) values.put(String.valueOf(id), state);
    }
    return new StatusResult(items.size(), values);
  }

  private Double parseState(Object value) {
    if (value instanceof Boolean) return Boolean.TRUE.equals(value) ? 1D : 0D;
    if (value instanceof Number) {
      double number = ((Number) value).doubleValue();
      return Double.compare(number, 0D) == 0 || Double.compare(number, 1D) == 0 ? number : null;
    }
    if (value != null) {
      String text = String.valueOf(value).trim();
      if ("1".equals(text) || "true".equalsIgnoreCase(text) || isOn(text)) return 1D;
      if ("0".equals(text) || "false".equalsIgnoreCase(text) || isOff(text)) return 0D;
    }
    return null;
  }

  private boolean isOn(String text) {
    String t = text.toUpperCase();
    return "投入".equals(text) || "投".equals(text)
        || "合闸".equals(text) || "闭合".equals(text) || "合位".equals(text)
        || "ON".equals(t) || "CLOSE".equals(t) || "CLOSED".equals(t)
        || "CONNECTED".equals(t) || "YES".equals(t) || "Y".equals(t);
  }

  private boolean isOff(String text) {
    String t = text.toUpperCase();
    return "退出".equals(text) || "退".equals(text)
        || "分闸".equals(text) || "断开".equals(text) || "分位".equals(text)
        || "OFF".equals(t) || "OPEN".equals(t) || "OPENED".equals(t)
        || "DISCONNECTED".equals(t) || "NO".equals(t) || "N".equals(t);
  }

  private ResponseStatusException unavailable(String message) {
    return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message);
  }

  public static class StatusResult {
    private final int count;
    private final Map<String, Double> values;

    public StatusResult(int count, Map<String, Double> values) {
      this.count = count;
      this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public int getCount() { return count; }
    public Map<String, Double> getValues() { return values; }
  }
}
