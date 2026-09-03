package com.zeta.business.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeta.business.entities.wholeexperiment.*;
import com.zeta.business.entities.wholeexperiment.WholeExperimentDtos.*;
import com.zeta.business.entities.logicgroup.dto.LogicGroupSnapshotDtos.*;
import com.zeta.business.entities.monitor.*;
import com.zeta.integration.queue.ScreenQueueMessage;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 整组运行上下文先提交再下发，回调依赖数据库而不是进程内映射。 */
@Service
@RequiredArgsConstructor
@Transactional(value = "businessTransactionManager", readOnly = true)
public class WholeExperimentRunService {
  private final WholeExperimentRunRepository runs;
  private final WholeExperimentRepository experiments;
  private final WholeExperimentService service;
  private final MonitorTaskRepository monitorTasks;
  private final LogicSnapshotService snapshots;
  private final ObjectMapper mapper;

  @Transactional("businessTransactionManager")
  public WholeExperimentRun prepare(Long userId, Long experimentId) {
    service.require(userId, experimentId);
    experiments.lockById(experimentId).orElseThrow(() -> missing());
    if (runs.existsByExperimentIdAndStatusIn(experimentId,
        Arrays.asList("STARTING", "WATCHING", "RESPONSE_TIMEOUT", "STOPPING"))) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "该整组实验仍在监测或状态未确认，请先查看并结束现有任务");
    }
    List<Member> members = service.validatedMembers(userId, experimentId);
    Detail detail = service.detail(userId, experimentId);
    WholeExperimentRun run = new WholeExperimentRun();
    run.setTaskUuid(UUID.randomUUID().toString());
    run.setExperimentId(experimentId);
    run.setUserId(userId);
    run.setIedName(detail.getIedName());
    run.setMembersJson(json(members));
    return runs.saveAndFlush(run);
  }

  public WholeExperimentRun require(Long userId, String taskUuid) {
    if (taskUuid == null) throw missing();
    return runs.findByTaskUuidAndUserId(taskUuid, userId).orElseThrow(() -> missing());
  }

  public List<String> logicCodes(WholeExperimentRun run) {
    return members(run).stream().map(Member::getCode).collect(Collectors.toList());
  }

  public List<Member> members(WholeExperimentRun run) {
    try {
      return mapper.readValue(run.getMembersJson(), new TypeReference<List<Member>>() {});
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "整组实验成员记录异常");
    }
  }

  @Transactional("businessTransactionManager")
  public boolean handleResponse(ScreenQueueMessage message) {
    if (message.getReqId() == null) return false;
    WholeExperimentRun run = runs.lockById(message.getReqId()).orElse(null);
    if (run == null) return false;
    Map<String, Object> data = message.getData() == null ? Collections.emptyMap() : message.getData();
    String result = String.valueOf(data.getOrDefault("result", ""));
    boolean completed = "success".equals(result) || "failed".equals(result);
    if (run.isTerminal()) {
      // 启动响应可能晚于完成消息；仅补接受时间，不覆盖最终结果。
      if (!completed && Boolean.TRUE.equals(message.getSuccess())) accepted(run);
      attachSnapshot(run, data);
      return true;
    }
    if (completed) {
      if ("success".equals(result) || run.getAcceptedAt() != null) accepted(run);
      run.setStatus("success".equals(result) ? "COMPLETED" : "FAILED");
      run.setCompletedAt(Instant.now());
      Map<String, Object> stored = new LinkedHashMap<>(data);
      if (message.getErrorMessage() != null) stored.put("error_message", message.getErrorMessage());
      run.setResultJson(json(stored));
      Object passed = data.containsKey("experiment_passed") ? data.get("experiment_passed") : data.get("experimentPassed");
      run.setExperimentPassed(passed instanceof Boolean ? (Boolean) passed : null);
      run.setErrorMessage(message.getErrorMessage() != null ? message.getErrorMessage()
          : Objects.toString(data.get("error_message"), null));
      attachSnapshot(run, data);
    } else if (Boolean.FALSE.equals(message.getSuccess())) {
      run.setStatus("START_FAILED");
      run.setErrorMessage(message.getErrorMessage() == null ? "监测端拒绝启动实验" : message.getErrorMessage());
      run.setCompletedAt(Instant.now());
    } else if (Boolean.TRUE.equals(message.getSuccess())) {
      accepted(run);
      if (!"STOPPING".equals(run.getStatus())) run.setStatus("WATCHING");
      run.setErrorMessage(null);
    }
    return true;
  }

  private void accepted(WholeExperimentRun run) {
    if (run.getAcceptedAt() != null) return;
    run.setAcceptedAt(run.getCreatedAt());
    // 用启动时间排序，迟到的旧回调不把旧组合顶到最近新实验之前。
    WholeExperiment experiment = experiments.lockById(run.getExperimentId()).orElseThrow(() -> missing());
    if (experiment.getLastStartedAt() == null || experiment.getLastStartedAt().isBefore(run.getCreatedAt())) {
      experiment.setLastStartedAt(run.getCreatedAt());
    }
  }

  private void attachSnapshot(WholeExperimentRun run, Map<String, Object> data) {
    if (run.getSnapshotJson() != null) return;
    MonitorTask task = null;
    Object path = data.get("snapshot_path");
    if (path != null) {
      try {
        task = monitorTasks.findById(Long.parseLong(String.valueOf(path))).orElse(null);
      } catch (NumberFormatException ignored) {
        // 非数字断面引用不作为数据库 ID，改用本次任务 UUID 查询。
      }
    }
    if (task != null && !run.getTaskUuid().equals(task.getUuid())) task = null;
    if (task == null) task = monitorTasks.findByUuid(run.getTaskUuid()).orElse(null);
    if (task != null && task.getSnapshotJson() != null && !task.getSnapshotJson().trim().isEmpty()) {
      run.setSnapshotJson(task.getSnapshotJson());
      run.setTotalTransitions(task.getTotalTransitions() == null ? 0 : task.getTotalTransitions());
    }
  }

  @Transactional("businessTransactionManager")
  public void sendFailed(String taskUuid, boolean timeout, String message) {
    WholeExperimentRun run = runs.lockById(taskUuid).orElseThrow(() -> missing());
    if (run.isTerminal() || run.getAcceptedAt() != null) return;
    run.setStatus(timeout ? "RESPONSE_TIMEOUT" : "START_FAILED");
    run.setErrorMessage(message);
    if (!timeout) run.setCompletedAt(Instant.now());
  }

  @Transactional("businessTransactionManager")
  public void stopping(Long userId, Long experimentId, String taskUuid) {
    WholeExperimentRun owned = require(userId, taskUuid);
    if (!experimentId.equals(owned.getExperimentId())) throw missing();
    WholeExperimentRun run = runs.lockById(taskUuid).orElseThrow(() -> missing());
    if (!run.isTerminal()) run.setStatus("STOPPING");
  }

  public List<Map<String, Object>> list(Long userId, Long experimentId) {
    service.require(userId, experimentId);
    return runs.findByExperimentIdAndUserIdOrderByCreatedAtDesc(experimentId, userId).stream()
        .map(this::summary).collect(Collectors.toList());
  }

  @Transactional("businessTransactionManager")
  public Map<String, Object> result(Long userId, String taskUuid) {
    require(userId, taskUuid);
    WholeExperimentRun run = runs.lockById(taskUuid).orElseThrow(() -> missing());
    reconcileMonitorTask(run);
    if (run.isTerminal()) attachSnapshot(run, Collections.emptyMap());
    Map<String, Object> response = new LinkedHashMap<>();
    if (run.getResultJson() != null) {
      try {
        response.putAll(mapper.readValue(run.getResultJson(), new TypeReference<Map<String, Object>>() {}));
      } catch (Exception ex) {
        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "实验结果记录异常");
      }
    }
    response.putAll(summary(run));
    response.put("result", run.isTerminal() ? ("COMPLETED".equals(run.getStatus()) ? "success" : "failed") : "pending");
    response.put("experiment_passed", run.getExperimentPassed());
    response.put("error_message", run.getErrorMessage());
    return response;
  }

  /** 服务重启或完成消息丢失后，从监测端持久化任务恢复终态，不重新发送启动命令。 */
  private void reconcileMonitorTask(WholeExperimentRun run) {
    if (run.isTerminal()) return;
    MonitorTask task = monitorTasks.findByUuid(run.getTaskUuid()).orElse(null);
    if (task == null || task.getState() == null) {
      if ("STARTING".equals(run.getStatus()) && run.getCreatedAt().isBefore(Instant.now().minusSeconds(30))) {
        run.setStatus("RESPONSE_TIMEOUT");
        run.setErrorMessage("启动响应超时，设备状态待确认，请勿重复启动");
      }
      return;
    }
    if (task.getState() == MonitorTask.TaskState.PENDING) return;
    accepted(run);
    switch (task.getState()) {
      case COMPLETED:
      case FAILED:
      case TIMEOUT:
      case CANCELLED:
        boolean success = task.getState() == MonitorTask.TaskState.COMPLETED;
        run.setStatus(success ? "COMPLETED" : "FAILED");
        run.setCompletedAt(task.getCompletedAt() == null ? Instant.now() : task.getCompletedAt());
        run.setErrorMessage(task.getErrorMessage());
        run.setSnapshotJson(task.getSnapshotJson());
        run.setTotalTransitions(task.getTotalTransitions() == null ? 0 : task.getTotalTransitions());
        if (task.getSnapshotJson() != null) {
          try {
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(task.getSnapshotJson());
            if (root.has("experimentPassed") && root.get("experimentPassed").isBoolean()) {
              run.setExperimentPassed(root.get("experimentPassed").asBoolean());
            }
          } catch (Exception ignored) {
            // 保留异常断面，在记录中明确提示，不能当作有效结果。
          }
        }
        break;
      default:
        if (!"STOPPING".equals(run.getStatus())) run.setStatus("WATCHING");
    }
  }

  public Map<String, Object> summary(WholeExperimentRun run) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", run.getTaskUuid());
    result.put("taskUuid", run.getTaskUuid());
    result.put("wholeExperimentId", run.getExperimentId());
    result.put("members", members(run));
    result.put("createdAt", run.getCreatedAt());
    result.put("completedAt", run.getCompletedAt());
    result.put("status", run.getStatus());
    result.put("experimentPassed", run.getExperimentPassed());
    result.put("totalTransitions", run.getTotalTransitions());
    result.put("errorMessage", run.getErrorMessage());
    result.put("resultStatus", resultStatus(run));
    return result;
  }

  private Set<Long> memberIds(WholeExperimentRun run) {
    return new HashSet<>(WholeExperimentService.ids(members(run)));
  }

  private LogicGroupSnapshotParser parser() {
    return new LogicGroupSnapshotParser(mapper, snapshots);
  }

  private String resultStatus(WholeExperimentRun run) {
    if (!run.isTerminal()) return "PENDING";
    if (run.getSnapshotJson() == null) return "DEVICE_NOT_STARTED";
    try {
      List<MemberSummaryResponse> parsed = parser().listMembers(run.getSnapshotJson(), memberIds(run));
      return parsed.size() == members(run).size() ? "SNAPSHOT_READY" : "INVALID_SNAPSHOT";
    } catch (ResponseStatusException ex) {
      return "INVALID_SNAPSHOT";
    }
  }

  public List<MemberSummaryResponse> listMembers(Long userId, String taskUuid) {
    WholeExperimentRun run = require(userId, taskUuid);
    List<MemberSummaryResponse> result = parser().listMembers(run.getSnapshotJson(), memberIds(run));
    Map<Long, Integer> order = members(run).stream()
        .collect(Collectors.toMap(Member::getLogicDiagramId, Member::getSortOrder));
    result.sort(Comparator.comparingInt(m -> order.get(m.getLogicDiagramId())));
    return result;
  }

  public Map<String, Object> member(Long userId, String taskUuid, Long logicDiagramId) {
    WholeExperimentRun run = require(userId, taskUuid);
    if (!"SNAPSHOT_READY".equals(resultStatus(run))) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "该实验没有可查看的有效断面");
    }
    MemberDetailResponse detail = parser().getMember(null, null, run.getSnapshotJson(),
        memberIds(run), logicDiagramId);
    Map<String, Object> result = mapper.convertValue(detail, new TypeReference<Map<String, Object>>() {});
    result.put("wholeRunId", taskUuid);
    result.put("wholeExperimentId", run.getExperimentId());
    return result;
  }

  private String json(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "整组实验数据保存失败");
    }
  }

  private ResponseStatusException missing() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "整组实验记录不存在");
  }
}
