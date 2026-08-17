package com.zeta.integration.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zeta.integration.queue.ScreenQueueMessage;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class IedSoftPressboardStatusClientTest {
  @Test
  void 解析读取成功的零一状态并忽略失败项() {
    MonitorCommandService commandService = mock(MonitorCommandService.class);
    ScreenQueueMessage response = new ScreenQueueMessage();
    response.setSuccess(true);
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("phase", "completed");
    data.put("soft_pressboards", Arrays.asList(
        item("IED_A/LD0/GGIO$ST$Ena1$stVal", "1", true),
        item("IED_A/LD0/GGIO$ST$Ena2$stVal", "false", true),
        item("IED_A/LD0/GGIO$ST$Ena3$stVal", null, false)));
    response.setData(data);
    when(commandService.sendIedSoftPressboardStatusRequest(2L))
        .thenReturn(CompletableFuture.completedFuture(response));

    IedSoftPressboardStatusClient.StatusResult result =
        new IedSoftPressboardStatusClient(commandService).summon(2L);

    assertEquals(3, result.getCount());
    assertEquals(2, result.getValues().size());
    assertEquals(1D, result.getValues().get("IED_A/LD0/GGIO$ST$Ena1$stVal"));
    assertEquals(0D, result.getValues().get("IED_A/LD0/GGIO$ST$Ena2$stVal"));
  }

  private Map<String, Object> item(String ref, String stateRaw, boolean success) {
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("pressboard_ref", ref);
    item.put("state_raw", stateRaw);
    item.put("read_success", success);
    return item;
  }
}
