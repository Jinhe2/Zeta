package com.zeta.integration.monitor;

import com.zeta.business.entities.devicedisplay.TestInstrumentOutputCode;
import com.zeta.integration.queue.ScreenQueueMessage;
import java.util.ArrayList;
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

/** 调用 monitord 读取端子接线实时状态，转换为 terminalId → 接线状态 映射。 */
@Service
public class TerminalStatusClient {
  private static final long AWAIT_SECONDS = 31;

  private final MonitorCommandService commandService;

  public TerminalStatusClient(MonitorCommandService commandService) {
    this.commandService = commandService;
  }

  public Map<Long, TerminalWiringState> summon(Long cabinetId, List<Long> terminalIds) {
    try {
      ScreenQueueMessage response = commandService
          .sendTerminalStatusRequest(cabinetId, terminalIds)
          .get(AWAIT_SECONDS, TimeUnit.SECONDS);
      if (!Boolean.TRUE.equals(response.getSuccess())) {
        String message = response.getErrorMessage();
        if (message == null || message.trim().isEmpty()) message = response.getError();
        if (message == null || message.trim().isEmpty()) message = "端子状态读取失败";
        throw unavailable(message);
      }
      return parse(response.getData());
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (TimeoutException ex) {
      throw unavailable("读取端子状态超时，请检查装置通讯状态");
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw unavailable("读取端子状态被中断");
    } catch (ExecutionException ex) {
      Throwable cause = ex.getCause();
      throw unavailable("读取端子状态失败：" + (cause == null ? ex.getMessage() : cause.getMessage()));
    }
  }

  private Map<Long, TerminalWiringState> parse(Map<String, Object> data) {
    Object rawItems = data == null ? null : data.get("terminals");
    if (!(rawItems instanceof List<?>)) {
      throw unavailable("端子状态响应格式错误");
    }
    Map<Long, TerminalWiringState> result = new LinkedHashMap<>();
    for (Object rawItem : (List<?>) rawItems) {
      if (!(rawItem instanceof Map<?, ?>)) continue;
      Map<?, ?> item = (Map<?, ?>) rawItem;
      Long terminalId = longValue(item.get("terminal_id"));
      if (terminalId == null) continue;
      boolean readSuccess = !Boolean.FALSE.equals(item.get("read_success"));
      String connectionStatus = item.get("connection_status") == null
          ? null : String.valueOf(item.get("connection_status"));
      List<String> outputCodes = new ArrayList<>();
      Object rawOutputs = item.get("actual_outputs");
      if (rawOutputs instanceof List<?>) {
        for (Object rawOutput : (List<?>) rawOutputs) {
          if (!(rawOutput instanceof Map<?, ?>)) continue;
          Object code = ((Map<?, ?>) rawOutput).get("output_code");
          String canonical = TestInstrumentOutputCode.canonicalize(
              code == null ? null : String.valueOf(code));
          if (canonical != null) outputCodes.add(canonical);
        }
      }
      result.put(terminalId, new TerminalWiringState(readSuccess, connectionStatus, outputCodes));
    }
    return result;
  }

  private Long longValue(Object value) {
    if (value instanceof Number) return ((Number) value).longValue();
    if (value != null) {
      try {
        return Long.valueOf(String.valueOf(value).trim());
      } catch (NumberFormatException ignored) {
      }
    }
    return null;
  }

  private ResponseStatusException unavailable(String message) {
    return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message);
  }

  /** 单个端子的接线状态。 */
  public static class TerminalWiringState {
    private final boolean readSuccess;
    private final String connectionStatus;
    private final List<String> actualOutputCodes;

    public TerminalWiringState(boolean readSuccess, String connectionStatus, List<String> actualOutputCodes) {
      this.readSuccess = readSuccess;
      this.connectionStatus = connectionStatus;
      this.actualOutputCodes = Collections.unmodifiableList(new ArrayList<>(actualOutputCodes));
    }

    public boolean isReadSuccess() { return readSuccess; }
    public String getConnectionStatus() { return connectionStatus; }
    public List<String> getActualOutputCodes() { return actualOutputCodes; }

    public boolean isConnected() { return "CONNECTED".equals(connectionStatus); }

    public String firstOutputCode() {
      return actualOutputCodes.isEmpty() ? null : actualOutputCodes.get(0);
    }
  }
}
