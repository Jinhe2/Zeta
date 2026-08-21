package com.zeta.integration.mms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeta.integration.queue.ScreenQueueMessage;
import com.zeta.integration.queue.ScreenQueueProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.annotation.PreDestroy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** 直接调用 MmsClientDyn 召唤装置 SG 定值。 */
@Service
public class MmsSettingClient {
  private static final long MAX_RESPONSE_FILE_BYTES = 20L * 1024L * 1024L;

  private final StringRedisTemplate redisTemplate;
  private final ScreenQueueProperties properties;
  private final ObjectMapper objectMapper;
  private final ScheduledExecutorService scheduler =
      Executors.newSingleThreadScheduledExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "zeta-mms-timeout");
            thread.setDaemon(true);
            return thread;
          });
  private final ConcurrentHashMap<String, CompletableFuture<ScreenQueueMessage>> pending =
      new ConcurrentHashMap<>();

  public MmsSettingClient(
      StringRedisTemplate redisTemplate,
      ScreenQueueProperties properties,
      ObjectMapper objectMapper) {
    this.redisTemplate = redisTemplate;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  public SummonResult summon(String iedName) {
    if (!properties.isEnabled()) {
      throw unavailable("Redis 队列未启用，无法召唤装置定值");
    }
    String reqId = "zeta_setting__" + UUID.randomUUID();
    CompletableFuture<ScreenQueueMessage> future = new CompletableFuture<>();
    pending.put(reqId, future);
    scheduler.schedule(
        () -> {
          CompletableFuture<ScreenQueueMessage> waiting = pending.remove(reqId);
          if (waiting != null) {
            waiting.completeExceptionally(new TimeoutException("召唤装置定值超时"));
          }
        },
        properties.getMmsTimeoutSeconds(),
        TimeUnit.SECONDS);

    try {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("ied_name", iedName);
      ScreenQueueMessage request = new ScreenQueueMessage("summon_ied_data", reqId, data);
      redisTemplate.convertAndSend(
          properties.getMmsOutboundKey(), objectMapper.writeValueAsString(request));
      ScreenQueueMessage response = future.get(properties.getMmsTimeoutSeconds() + 1, TimeUnit.SECONDS);
      if (!Boolean.TRUE.equals(response.getSuccess())) {
        throw unavailable(
            response.getErrorMessage() == null ? "装置定值召唤失败" : response.getErrorMessage());
      }
      return readResultFile(response);
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (TimeoutException ex) {
      throw unavailable("召唤装置定值超时，请检查装置通讯状态");
    } catch (Exception ex) {
      Throwable cause = ex instanceof ExecutionException ? ex.getCause() : ex;
      throw unavailable("召唤装置定值失败：" + (cause == null ? ex.getMessage() : cause.getMessage()));
    } finally {
      pending.remove(reqId);
    }
  }

  public void handleResponse(String payload) {
    try {
      ScreenQueueMessage message = objectMapper.readValue(payload, ScreenQueueMessage.class);
      if (!"summon_ied_data".equals(message.getCommand()) || message.getReqId() == null) {
        return;
      }
      CompletableFuture<ScreenQueueMessage> future = pending.remove(message.getReqId());
      if (future != null) {
        future.complete(message);
      }
    } catch (Exception ignored) {
      // 非本客户端请求或格式异常的消息交给其他订阅者处理。
    }
  }

  private SummonResult readResultFile(ScreenQueueMessage response) throws IOException {
    Map<String, Object> data = response.getData();
    if (data == null || !Boolean.TRUE.equals(data.get("saved")) || data.get("filename") == null) {
      throw unavailable("定值响应缺少有效的结果文件");
    }
    Path root = Paths.get(properties.getMmsTempDir()).toAbsolutePath().normalize();
    Path supplied = Paths.get(String.valueOf(data.get("filename")));
    boolean returnedAbsolutePath = supplied.isAbsolute();
    Path file = returnedAbsolutePath ? supplied.normalize() : root.resolve(supplied).normalize();
    if (!returnedAbsolutePath && !file.startsWith(root)) {
      throw unavailable("定值响应文件不在允许的临时目录内");
    }
    if (Files.isSymbolicLink(file)) {
      throw unavailable("定值响应文件不能是符号链接");
    }
    if (!Files.isRegularFile(file) || Files.size(file) > MAX_RESPONSE_FILE_BYTES) {
      throw unavailable("定值响应文件不存在或超过大小限制");
    }
    Path realFile = file.toRealPath();
    if (!returnedAbsolutePath) {
      Path realRoot = root.toRealPath();
      if (!realFile.startsWith(realRoot)) {
        throw unavailable("定值响应文件不在允许的临时目录内");
      }
    }
    try {
      JsonNode document =
          objectMapper.readTree(new String(Files.readAllBytes(realFile), StandardCharsets.UTF_8));
      JsonNode rows = document.get("data");
      if (rows == null || !rows.isArray()) {
        throw unavailable("定值响应文件格式错误");
      }
      Map<String, Double> values = new LinkedHashMap<>();
      for (JsonNode row : rows) {
        JsonNode ref = row.get("reference");
        JsonNode value = row.get("value");
        if (ref == null || value == null) continue;
        Double parsed = parseValue(value);
        if (parsed != null && Double.isFinite(parsed)) {
          values.put(ref.asText(), parsed);
        }
      }
      if (values.isEmpty()) {
        throw unavailable("装置未返回可用的定值数据");
      }
      return new SummonResult(values);
    } finally {
      Files.deleteIfExists(realFile);
    }
  }

  private Double parseValue(JsonNode value) {
    try {
      if (value.isBoolean()) return value.asBoolean() ? 1D : 0D;
      if (value.isNumber()) return value.asDouble();
      if (value.isTextual() && !value.asText().trim().isEmpty()) {
        return Double.parseDouble(value.asText().trim());
      }
    } catch (NumberFormatException ignored) {
    }
    return null;
  }

  private ResponseStatusException unavailable(String message) {
    return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message);
  }

  @PreDestroy
  void shutdown() {
    pending.forEach(
        (reqId, future) ->
            future.completeExceptionally(new CancellationException("应用正在关闭")));
    pending.clear();
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException ex) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  public static class SummonResult {
    private final Map<String, Double> values;

    public SummonResult(Map<String, Double> values) { this.values = values; }
    public Map<String, Double> getValues() { return values; }
    public int getCount() { return values.size(); }
  }
}
