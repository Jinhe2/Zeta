package com.zeta.business.service;

import com.zeta.business.entities.monitor.MonitorTaskRepository;
import com.zeta.business.entities.monitor.MonitorTask;
import com.zeta.business.entities.snapshot.LogicSnapshot;
import com.zeta.business.entities.snapshot.LogicSnapshotRepository;
import com.zeta.integration.monitor.MonitorCommandService;
import com.zeta.integration.queue.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

class WholeExperimentMonitorProtocolTest {
  @Test void 单逻辑完成结果应返回持久化历史快照标识() {
    LogicSnapshotRepository snapshots = mock(LogicSnapshotRepository.class);
    MonitorTaskRepository tasks = mock(MonitorTaskRepository.class);
    MonitorTask task = new MonitorTask();
    task.setId(42L);
    task.setLogicDiagramId(9L);
    task.setSnapshotJson("{\"version\":\"2.3\",\"effectiveValues\":[]}");
    task.setTotalTransitions(3);
    when(tasks.findById(42L)).thenReturn(Optional.of(task));
    when(snapshots.save(any(LogicSnapshot.class))).thenAnswer((InvocationOnMock invocation) -> {
      LogicSnapshot snapshot = invocation.getArgument(0);
      snapshot.setId(88L);
      return snapshot;
    });
    MonitorCommandService commands = new MonitorCommandService(
        Optional.empty(), snapshots, tasks,
        mock(LogicGroupSnapshotService.class), mock(WholeExperimentRunService.class));
    try {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("result", "success");
      data.put("snapshot_path", "42");
      ScreenQueueMessage response = new ScreenQueueMessage(
          "summon_logic_monitor", "任务标识", "7", data);
      response.setSuccess(true);

      commands.handleResponse(response);

      Map<String, Object> result = commands.getMonitorTaskResult("任务标识");
      assertEquals(88L, result.get("snapshot_id"));
      ArgumentCaptor<LogicSnapshot> snapshot = ArgumentCaptor.forClass(LogicSnapshot.class);
      verify(snapshots).save(snapshot.capture());
      assertEquals(7L, snapshot.getValue().getUserId());
      assertEquals(9L, snapshot.getValue().getLogicId());
      assertEquals(task.getSnapshotJson(), snapshot.getValue().getSnapshotJson());
    } finally {
      ReflectionTestUtils.invokeMethod(commands, "shutdown");
    }
  }

  @Test void 整组复用命令且保留成员顺序和预分配任务标识() throws Exception {
    ScreenQueuePublisher publisher = mock(ScreenQueuePublisher.class);
    WholeExperimentRunService runs = mock(WholeExperimentRunService.class);
    MonitorCommandService commands = new MonitorCommandService(Optional.of(publisher),
        mock(LogicSnapshotRepository.class), mock(MonitorTaskRepository.class),
        mock(LogicGroupSnapshotService.class), runs);
    try {
      CompletableFuture<ScreenQueueMessage> future = commands.startWholeExperimentMonitor(
          "运行编号", "装置", Arrays.asList("逻辑乙", "逻辑甲", "逻辑丙"), 9L);
      ArgumentCaptor<ScreenQueueMessage> capture = ArgumentCaptor.forClass(ScreenQueueMessage.class);
      verify(publisher).publish(capture.capture());
      ScreenQueueMessage request = capture.getValue();
      assertEquals("summon_logic_group_monitor", request.getCommand());
      assertEquals("运行编号", request.getReqId());
      assertEquals("9", request.getUserData());
      assertEquals(Arrays.asList("逻辑乙", "逻辑甲", "逻辑丙"), request.getData().get("logic_ids"));
      assertEquals("装置", request.getData().get("ied_name"));
      ScreenQueueMessage accepted = new ScreenQueueMessage("summon_logic_group_monitor", "运行编号",
          Collections.singletonMap("status", "accepted"));
      accepted.setSuccess(true);
      when(runs.handleResponse(accepted)).thenReturn(true);
      commands.handleResponse(accepted);
      assertSame(accepted, future.get(1, TimeUnit.SECONDS));
    } finally {
      ReflectionTestUtils.invokeMethod(commands, "shutdown");
    }
  }

  @Test void 整组心跳和结束使用现有监测协议() {
    ScreenQueuePublisher publisher = mock(ScreenQueuePublisher.class);
    MonitorCommandService commands = new MonitorCommandService(Optional.of(publisher),
        mock(LogicSnapshotRepository.class), mock(MonitorTaskRepository.class),
        mock(LogicGroupSnapshotService.class), mock(WholeExperimentRunService.class));
    try {
      commands.sendLogicGroupMonitorHeartbeat("任务");
      commands.endLogicGroupMonitor("任务");
      commands.abortLogicGroupMonitor("任务");
      ArgumentCaptor<ScreenQueueMessage> capture = ArgumentCaptor.forClass(ScreenQueueMessage.class);
      verify(publisher, times(3)).publish(capture.capture());
      List<String> actions = new ArrayList<>();
      for (ScreenQueueMessage message : capture.getAllValues()) {
        assertEquals("summon_logic_group_monitor", message.getCommand());
        assertEquals("任务", message.getData().get("task_uuid"));
        actions.add(String.valueOf(message.getData().get("action")));
      }
      assertEquals(Arrays.asList("heartbeat", "end", "abort"), actions);
    } finally {
      ReflectionTestUtils.invokeMethod(commands, "shutdown");
    }
  }
}
