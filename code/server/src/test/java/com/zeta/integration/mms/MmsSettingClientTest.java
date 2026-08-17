package com.zeta.integration.mms;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeta.integration.queue.ScreenQueueMessage;
import com.zeta.integration.queue.ScreenQueueProperties;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.redis.core.StringRedisTemplate;

class MmsSettingClientTest {
  @TempDir Path tempDir;

  @Test
  void 通过模拟Redis响应读取并清理定值文件() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    ScreenQueueProperties properties = new ScreenQueueProperties();
    properties.setEnabled(true);
    properties.setMmsTempDir(tempDir.toString());
    properties.setMmsTimeoutSeconds(2);
    MmsSettingClient client = new MmsSettingClient(redis, properties, mapper);
    AtomicReference<Path> responseFile = new AtomicReference<>();

    doAnswer(
            invocation -> {
              ScreenQueueMessage request =
                  mapper.readValue(invocation.getArgument(1, String.class), ScreenQueueMessage.class);
              Path file = tempDir.resolve("settings.json");
              Files.write(
                  file,
                  "{\"data\":[{\"reference\":\"LD0/PTOC$SG$StrVal\",\"value\":1.25}]}"
                      .getBytes(StandardCharsets.UTF_8));
              responseFile.set(file);
              Map<String, Object> data = new LinkedHashMap<>();
              data.put("saved", true);
              data.put("filename", file.getFileName().toString());
              ScreenQueueMessage response = new ScreenQueueMessage();
              response.setCommand("summon_ied_data");
              response.setReqId(request.getReqId());
              response.setSuccess(true);
              response.setData(data);
              client.handleResponse(mapper.writeValueAsString(response));
              return null;
            })
        .when(redis)
        .convertAndSend(eq("mms_command_request"), anyString());

    MmsSettingClient.SummonResult result = client.summon("IED_A");

    assertEquals(1.25D, result.getValues().get("LD0/PTOC$SG$StrVal"));
    assertFalse(Files.exists(responseFile.get()));
    client.shutdown();
  }

  @Test
  void 返回绝对路径时不依赖配置的临时目录() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    ScreenQueueProperties properties = new ScreenQueueProperties();
    properties.setEnabled(true);
    properties.setMmsTempDir(tempDir.resolve("现场配置错误的目录").toString());
    properties.setMmsTimeoutSeconds(2);
    MmsSettingClient client = new MmsSettingClient(redis, properties, mapper);
    Path file = tempDir.resolve("monitord-returned-settings.json");
    Files.write(
        file,
        "{\"data\":[{\"reference\":\"LD0/PTOC$SG$StrVal\",\"value\":2.5}]}"
            .getBytes(StandardCharsets.UTF_8));

    doAnswer(
            invocation -> {
              ScreenQueueMessage request =
                  mapper.readValue(invocation.getArgument(1, String.class), ScreenQueueMessage.class);
              Map<String, Object> data = new LinkedHashMap<>();
              data.put("saved", true);
              data.put("filename", file.toAbsolutePath().toString());
              ScreenQueueMessage response = new ScreenQueueMessage();
              response.setCommand("summon_ied_data");
              response.setReqId(request.getReqId());
              response.setSuccess(true);
              response.setData(data);
              client.handleResponse(mapper.writeValueAsString(response));
              return null;
            })
        .when(redis)
        .convertAndSend(eq("mms_command_request"), anyString());

    MmsSettingClient.SummonResult result = client.summon("IED_A");

    assertEquals(2.5D, result.getValues().get("LD0/PTOC$SG$StrVal"));
    assertFalse(Files.exists(file));
    client.shutdown();
  }
}
