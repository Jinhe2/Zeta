package com.zeta.business.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeta.business.entities.wholeexperiment.*;
import com.zeta.business.entities.monitor.*;
import com.zeta.integration.queue.ScreenQueueMessage;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WholeExperimentRunServiceTest {
  WholeExperimentRunRepository runs = mock(WholeExperimentRunRepository.class);
  WholeExperimentRepository experiments = mock(WholeExperimentRepository.class);
  MonitorTaskRepository tasks = mock(MonitorTaskRepository.class);
  WholeExperimentRunService service = new WholeExperimentRunService(runs, experiments,
      mock(WholeExperimentService.class), tasks, mock(LogicSnapshotService.class),
      new ObjectMapper().findAndRegisterModules());
  WholeExperimentRun run;
  WholeExperiment experiment;

  @BeforeEach void 配置() {
    run = new WholeExperimentRun(); run.setTaskUuid("任务"); run.setUserId(1L); run.setExperimentId(10L);
    run.setMembersJson("[]");
    experiment = new WholeExperiment(); experiment.setId(10L);
    when(runs.lockById("任务")).thenReturn(Optional.of(run));
    when(runs.findByTaskUuidAndUserId("任务", 1L)).thenReturn(Optional.of(run));
    when(experiments.lockById(10L)).thenReturn(Optional.of(experiment));
  }

  @Test void 接受启动才计入历史且重复回调幂等() {
    ScreenQueueMessage accepted = response(true, Collections.singletonMap("status", "accepted"));
    assertTrue(service.handleResponse(accepted));
    Instant last = experiment.getLastStartedAt();
    service.handleResponse(accepted);
    assertEquals(last, experiment.getLastStartedAt());
    assertEquals("WATCHING", run.getStatus());
    verify(experiments, times(1)).lockById(10L);
  }

  @Test void 拒绝启动不计入历史() {
    service.handleResponse(response(false, Collections.emptyMap()));
    assertEquals("START_FAILED", run.getStatus());
    assertNull(experiment.getLastStartedAt());
  }

  @Test void 超时不终结任务也不阻止迟到接受() {
    service.sendFailed("任务", true, "响应超时");
    assertFalse(run.isTerminal());
    assertEquals("RESPONSE_TIMEOUT", run.getStatus());
    service.handleResponse(response(true, Collections.emptyMap()));
    assertEquals("WATCHING", run.getStatus());
  }

  @Test void 完成后迟到接受或重复完成不覆盖结果() {
    Map<String, Object> data = new HashMap<>(); data.put("result", "success"); data.put("experiment_passed", false);
    service.handleResponse(response(true, data));
    Instant completed = run.getCompletedAt();
    service.handleResponse(response(true, Collections.singletonMap("status", "accepted")));
    service.handleResponse(response(true, data));
    assertEquals("COMPLETED", run.getStatus());
    assertEquals(completed, run.getCompletedAt());
    assertFalse(run.getExperimentPassed());
  }

  @Test void 旧任务迟到不覆盖较新的最近使用时间() {
    Instant newer = run.getCreatedAt().plusSeconds(100);
    experiment.setLastStartedAt(newer);
    service.handleResponse(response(true, Collections.emptyMap()));
    assertEquals(newer, experiment.getLastStartedAt());
  }

  @Test void 其他学员不可访问或停止任务() {
    assertThrows(ResponseStatusException.class, () -> service.require(2L, "任务"));
    assertThrows(ResponseStatusException.class, () -> service.stopping(1L, 99L, "任务"));
  }

  @Test void 重启后仅依靠持久化上下文处理完成() {
    MonitorTask task = new MonitorTask(); task.setUuid("任务"); task.setId(7L);
    task.setSnapshotJson("{\"version\":\"3.0\"}"); task.setTotalTransitions(5);
    when(tasks.findById(7L)).thenReturn(Optional.of(task));
    Map<String, Object> data = new HashMap<>(); data.put("result", "success"); data.put("snapshot_path", "7");
    service.handleResponse(response(true, data));
    assertEquals(task.getSnapshotJson(), run.getSnapshotJson());
    assertEquals(5, run.getTotalTransitions());
  }

  @Test void 拒绝关联其他任务断面() {
    MonitorTask task = new MonitorTask(); task.setUuid("其他任务"); task.setSnapshotJson("{}");
    when(tasks.findById(7L)).thenReturn(Optional.of(task));
    Map<String, Object> data = new HashMap<>(); data.put("result", "success"); data.put("snapshot_path", "7");
    service.handleResponse(response(true, data));
    assertNull(run.getSnapshotJson());
  }

  @Test void 未知任务不被误识别为整组实验() {
    ScreenQueueMessage message = response(true, Collections.emptyMap()); message.setReqId("未知任务");
    assertFalse(service.handleResponse(message));
  }

  @Test void 从监测数据库恢复丢失的完成消息() {
    MonitorTask task = new MonitorTask(); task.setUuid("任务"); task.setState(MonitorTask.TaskState.COMPLETED);
    task.setSnapshotJson("{\"experimentPassed\":true}");
    when(tasks.findByUuid("任务")).thenReturn(Optional.of(task));
    assertEquals("success", service.result(1L, "任务").get("result"));
    assertTrue(run.getExperimentPassed());
    assertNotNull(experiment.getLastStartedAt());
  }

  @Test void 旧记录按运行时成员排序且拒绝非成员回放() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    run.setStatus("COMPLETED");
    run.setMembersJson("[{\"logicDiagramId\":16,\"code\":\"logic_16\",\"title\":\"历史第三逻辑\",\"sortOrder\":1},"
        + "{\"logicDiagramId\":12,\"code\":\"logic_12\",\"title\":\"历史第二逻辑\",\"sortOrder\":2},"
        + "{\"logicDiagramId\":1,\"code\":\"logic_1\",\"title\":\"历史第一逻辑\",\"sortOrder\":3}]");
    try (java.io.InputStream input = getClass().getResourceAsStream("/snapshots/logic-group-snapshot-v3.json")) {
      run.setSnapshotJson(mapper.readTree(input).toString());
    }
    assertEquals(16L, service.listMembers(1L, "任务").get(0).getLogicDiagramId());
    assertEquals("SNAPSHOT_READY", service.summary(run).get("resultStatus"));
    assertThrows(ResponseStatusException.class, () -> service.member(1L, "任务", 99L));
  }

  private ScreenQueueMessage response(boolean success, Map<String, Object> data) {
    ScreenQueueMessage message = new ScreenQueueMessage("summon_logic_group_monitor", "任务", data);
    message.setSuccess(success); return message;
  }
}
