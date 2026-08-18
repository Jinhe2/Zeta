package com.zeta.business.controller;

import com.zeta.business.auth.AuthService;
import com.zeta.business.entities.logicgroup.LogicGroupSnapshot;
import com.zeta.business.entities.user.User;
import com.zeta.business.service.LogicGroupSnapshotService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logic-group-snapshots")
public class LogicGroupSnapshotController {

  private final LogicGroupSnapshotService service;
  private final AuthService authService;

  public LogicGroupSnapshotController(
      LogicGroupSnapshotService service, AuthService authService) {
    this.service = service;
    this.authService = authService;
  }

  @GetMapping
  public List<Map<String, Object>> list(
      @RequestHeader("Authorization") String authorization, @RequestParam Long groupId) {
    User user = authService.requireUser(authorization);
    List<Map<String, Object>> result = new ArrayList<>();
    for (LogicGroupSnapshot snapshot : service.listByGroup(user.getId(), groupId)) {
      result.add(toSummary(snapshot));
    }
    return result;
  }

  @GetMapping("/{id}")
  public Map<String, Object> get(
      @RequestHeader("Authorization") String authorization, @PathVariable Long id) {
    User user = authService.requireUser(authorization);
    return toDetail(service.get(user.getId(), id));
  }

  private Map<String, Object> toSummary(LogicGroupSnapshot snapshot) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", snapshot.getId());
    map.put("groupId", snapshot.getGroupId());
    map.put("groupName", snapshot.getGroupName());
    map.put("totalTransitions", snapshot.getTotalTransitions());
    map.put("experimentPassed", snapshot.getExperimentPassed());
    map.put("status", snapshot.getStatus());
    map.put("source", snapshot.getSource());
    map.put("createdAt", snapshot.getCreatedAt());
    map.put("completedAt", snapshot.getCompletedAt());
    return map;
  }

  private Map<String, Object> toDetail(LogicGroupSnapshot snapshot) {
    Map<String, Object> map = toSummary(snapshot);
    map.put("snapshotJson", snapshot.getSnapshotJson());
    return map;
  }
}
