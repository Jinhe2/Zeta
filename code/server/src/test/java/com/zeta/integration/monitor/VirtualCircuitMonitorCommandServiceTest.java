package com.zeta.integration.monitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.zeta.business.entities.monitor.MonitorTaskRepository;
import com.zeta.business.entities.snapshot.LogicSnapshotRepository;
import com.zeta.business.service.LogicGroupSnapshotService;
import com.zeta.business.service.WholeExperimentRunService;
import com.zeta.integration.queue.ScreenQueueMessage;
import com.zeta.integration.queue.ScreenQueuePublisher;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class VirtualCircuitMonitorCommandServiceTest {

  @Test
  void sendsExpectedPayloadAndWaitsPastAcceptedForCompleted() {
    ScreenQueuePublisher publisher = mock(ScreenQueuePublisher.class);
    MonitorCommandService service = new MonitorCommandService(
        Optional.of(publisher), mock(LogicSnapshotRepository.class), mock(MonitorTaskRepository.class),
        mock(LogicGroupSnapshotService.class), mock(WholeExperimentRunService.class));
    try {
      CompletableFuture<ScreenQueueMessage> future =
          service.sendVirtualCircuitStatusRequest(7L, 19L);
      ArgumentCaptor<ScreenQueueMessage> captor = ArgumentCaptor.forClass(ScreenQueueMessage.class);
      verify(publisher).publish(captor.capture());
      ScreenQueueMessage request = captor.getValue();
      assertThat(request.getCommand()).isEqualTo("summon_ied_virtual_circuit_status");
      assertThat(request.getData()).containsEntry("cabinet_id", 7L).containsEntry("ied_device_id", 19L);

      ScreenQueueMessage accepted = response(request, true, "accepted");
      service.handleResponse(accepted);
      assertThat(future).isNotDone();

      ScreenQueueMessage completed = response(request, false, "completed");
      completed.setError("PARTIAL_READ");
      completed.setErrorMessage("部分状态不可读");
      service.handleResponse(completed);
      assertThat(future.join()).isSameAs(completed);
    } finally {
      service.shutdown();
    }
  }

  private ScreenQueueMessage response(ScreenQueueMessage request, boolean success, String phase) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("phase", phase);
    data.put("control_blocks", Collections.emptyList());
    ScreenQueueMessage response = new ScreenQueueMessage(request.getCommand(), request.getReqId(), data);
    response.setSuccess(success);
    return response;
  }
}
