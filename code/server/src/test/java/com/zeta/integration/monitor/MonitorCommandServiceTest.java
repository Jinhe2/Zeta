package com.zeta.integration.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.zeta.business.entities.monitor.MonitorTaskRepository;
import com.zeta.business.entities.snapshot.LogicSnapshotRepository;
import com.zeta.integration.queue.ScreenQueueMessage;
import com.zeta.integration.queue.ScreenQueuePublisher;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MonitorCommandServiceTest {
  @Test
  void 软压板读取发送专用命令和Ied设备参数() {
    ScreenQueuePublisher publisher = mock(ScreenQueuePublisher.class);
    MonitorCommandService service = new MonitorCommandService(
        Optional.of(publisher), mock(LogicSnapshotRepository.class),
        mock(MonitorTaskRepository.class));
    try {
      CompletableFuture<ScreenQueueMessage> future =
          service.sendIedSoftPressboardStatusRequest(123L);

      ArgumentCaptor<ScreenQueueMessage> captor =
          ArgumentCaptor.forClass(ScreenQueueMessage.class);
      verify(publisher).publish(captor.capture());
      ScreenQueueMessage request = captor.getValue();
      assertEquals("summon_ied_soft_pressboard_status", request.getCommand());
      assertEquals(123L, request.getData().get("ied_device_id"));

      ScreenQueueMessage completed = new ScreenQueueMessage();
      completed.setCommand(request.getCommand());
      completed.setReqId(request.getReqId());
      completed.setSuccess(true);
      LinkedHashMap<String, Object> data = new LinkedHashMap<>();
      data.put("phase", "completed");
      data.put("ied_device_id", 123L);
      data.put("soft_pressboards", java.util.Collections.emptyList());
      completed.setData(data);
      service.handleResponse(completed);
      assertEquals(completed, future.getNow(null));
    } finally {
      service.shutdown();
    }
  }
}
