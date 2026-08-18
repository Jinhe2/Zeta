package com.zeta.business.controller;

import com.zeta.business.auth.AuthService;
import com.zeta.business.entities.logicgroup.dto.LogicGroupDtos.*;
import com.zeta.business.entities.user.UserRole;
import com.zeta.business.service.LogicGroupService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/logic-learning")
public class LogicGroupController {

  private final LogicGroupService service;
  private final AuthService authService;

  public LogicGroupController(LogicGroupService service, AuthService authService) {
    this.service = service;
    this.authService = authService;
  }

  @GetMapping("/devices/{deviceId}/logic-groups")
  public List<LogicGroupResponse> listByDevice(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long deviceId) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return service.listByDevice(deviceId);
  }

  @PostMapping("/devices/{deviceId}/logic-groups")
  public LogicGroupDetailResponse create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long deviceId,
      @Valid @RequestBody CreateLogicGroupRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return service.create(deviceId, request);
  }

  @GetMapping("/groups/{groupId}")
  public LogicGroupDetailResponse get(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long groupId) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return service.get(groupId);
  }

  @PutMapping("/groups/{groupId}")
  public LogicGroupDetailResponse update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long groupId,
      @Valid @RequestBody UpdateLogicGroupRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return service.update(groupId, request);
  }

  @DeleteMapping("/groups/{groupId}")
  public Map<String, Object> delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long groupId) {
    authService.requireRole(authorization, UserRole.ADMIN);
    service.delete(groupId);
    return Collections.singletonMap("deleted", true);
  }
}
