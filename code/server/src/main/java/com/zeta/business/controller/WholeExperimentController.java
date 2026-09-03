package com.zeta.business.controller;

import com.zeta.business.auth.AuthService;
import com.zeta.business.entities.wholeexperiment.*;
import com.zeta.business.entities.wholeexperiment.WholeExperimentDtos.*;
import com.zeta.business.entities.experiment.ExperimentPrecheckResponse;
import com.zeta.business.entities.experimentguide.dto.ExperimentGuideItemStudentResponse;
import com.zeta.business.entities.logicgroup.dto.LogicGroupSnapshotDtos.MemberSummaryResponse;
import com.zeta.business.service.*;
import com.zeta.integration.monitor.MonitorCommandService;
import com.zeta.integration.queue.ScreenQueueMessage;
import java.util.*;
import java.util.concurrent.*;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WholeExperimentController {
  private final AuthService auth;
  private final WholeExperimentService experiments;
  private final WholeExperimentMergeService merge;
  private final WholeExperimentRunService runs;
  private final MonitorCommandService monitor;

  @PostMapping("/whole-experiments")
  public Detail create(@RequestHeader("Authorization") String authorization,
      @Valid @RequestBody CreateRequest request) {
    return experiments.create(user(authorization), request);
  }

  @GetMapping("/whole-experiments/recent")
  public List<Detail> recent(@RequestHeader("Authorization") String authorization, @RequestParam Long deviceId) {
    return experiments.recent(user(authorization), deviceId);
  }

  @GetMapping("/whole-experiments/{id}")
  public Detail detail(@RequestHeader("Authorization") String authorization, @PathVariable Long id) {
    return experiments.detail(user(authorization), id);
  }

  @GetMapping("/whole-experiments/{id}/guide")
  public List<ExperimentGuideItemStudentResponse> guide(
      @RequestHeader("Authorization") String authorization, @PathVariable Long id) {
    return merge.guide(WholeExperimentService.ids(experiments.validatedMembers(user(authorization), id)));
  }

  @PostMapping("/whole-experiments/{id}/precheck")
  public ExperimentPrecheckResponse precheck(@RequestHeader("Authorization") String authorization,
      @PathVariable Long id) {
    return merge.check(WholeExperimentService.ids(experiments.validatedMembers(user(authorization), id)));
  }

  @PostMapping("/whole-experiments/{id}/monitor")
  public Map<String, Object> monitor(@RequestHeader("Authorization") String authorization,
      @PathVariable Long id, @Valid @RequestBody MonitorRequest request) {
    Long userId = user(authorization);
    experiments.require(userId, id);
    if (!Arrays.asList("start", "heartbeat", "end", "abort").contains(request.getAction())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的监测操作");
    }
    if ("start".equals(request.getAction())) {
      // 服务端再次校验，避免直接调用启动接口绕过基准校验。
      ExperimentPrecheckResponse check = merge.check(
          WholeExperimentService.ids(experiments.validatedMembers(userId, id)));
      if (!"MATCHED".equals(check.getStatus()) && !"SKIPPED".equals(check.getStatus())) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "整组实验基准校核未通过，请重新检查定值、压板和接线");
      }
      WholeExperimentRun run = runs.prepare(userId, id);
      try {
        ScreenQueueMessage response = monitor.startWholeExperimentMonitor(run.getTaskUuid(),
            run.getIedName(), runs.logicCodes(run), userId).get(30, TimeUnit.SECONDS);
        // 回调已负责持久化；响应始终带本地任务 UUID，即使监测端返回拒绝也可查看记录。
        Map<String, Object> result = new LinkedHashMap<>(runs.result(userId, run.getTaskUuid()));
        result.put("req_id", run.getTaskUuid());
        result.put("success", !Boolean.FALSE.equals(response.getSuccess()));
        return result;
      } catch (Exception ex) {
        // 队列启用时，连接异常也不能证明命令未送达；保留待确认状态，禁止盲目重启。
        boolean timeout = monitor.isQueueEnabled();
        if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
        String message = timeout ? "启动响应超时，设备可能仍在监测，请查看或结束该任务，勿重复启动"
            : "监测命令发送失败，请检查监测服务连接";
        runs.sendFailed(run.getTaskUuid(), timeout, message);
        Map<String, Object> result = new LinkedHashMap<>(runs.result(userId, run.getTaskUuid()));
        result.put("req_id", run.getTaskUuid());
        result.put("success", false);
        return result;
      }
    }
    WholeExperimentRun run = runs.require(userId, request.getTaskUuid());
    if (!id.equals(run.getExperimentId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "整组实验记录不存在");
    if (!run.isTerminal()) {
      if (!monitor.isQueueEnabled()) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "监测队列未启用，无法发送操作");
      }
      switch (request.getAction()) {
        case "heartbeat":
          monitor.sendLogicGroupMonitorHeartbeat(run.getTaskUuid());
          break;
        case "end":
        case "abort":
          if ("end".equals(request.getAction())) monitor.endLogicGroupMonitor(run.getTaskUuid());
          else monitor.abortLogicGroupMonitor(run.getTaskUuid());
          runs.stopping(userId, id, run.getTaskUuid());
          break;
        default:
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的监测操作");
      }
    }
    return Collections.singletonMap("status", "sent");
  }

  @GetMapping("/whole-experiments/{id}/runs")
  public List<Map<String, Object>> list(@RequestHeader("Authorization") String authorization, @PathVariable Long id) {
    return runs.list(user(authorization), id);
  }

  @GetMapping("/whole-experiment-runs/{taskUuid}")
  public Map<String, Object> result(@RequestHeader("Authorization") String authorization, @PathVariable String taskUuid) {
    return runs.result(user(authorization), taskUuid);
  }

  @GetMapping("/whole-experiment-runs/{taskUuid}/members")
  public List<MemberSummaryResponse> members(@RequestHeader("Authorization") String authorization,
      @PathVariable String taskUuid) {
    return runs.listMembers(user(authorization), taskUuid);
  }

  @GetMapping("/whole-experiment-runs/{taskUuid}/members/{logicDiagramId}")
  public Map<String, Object> member(@RequestHeader("Authorization") String authorization,
      @PathVariable String taskUuid, @PathVariable Long logicDiagramId) {
    return runs.member(user(authorization), taskUuid, logicDiagramId);
  }

  private Long user(String authorization) {
    return auth.requireUser(authorization).getId();
  }
}
