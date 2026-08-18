package com.zeta.business.controller;

import com.zeta.business.auth.AuthService;
import com.zeta.business.entities.experiment.ExperimentPrecheckResponse;
import com.zeta.business.service.ExperimentPrecheckService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ExperimentPrecheckController {
  private final AuthService authService;
  private final ExperimentPrecheckService service;

  public ExperimentPrecheckController(AuthService authService, ExperimentPrecheckService service) {
    this.authService = authService;
    this.service = service;
  }

  @PostMapping("/api/experiment-prechecks")
  public ExperimentPrecheckResponse check(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody Map<String, Object> body) {
    authService.requireUser(authorization);
    Object id = body.get("logicDiagramId");
    if (!(id instanceof Number)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少 logicDiagramId 参数");
    }
    return service.check(((Number) id).longValue());
  }

  @PostMapping("/api/experiment-prechecks/group")
  public ExperimentPrecheckResponse checkForGroup(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody Map<String, Object> body) {
    authService.requireUser(authorization);
    Object id = body.get("groupId");
    if (!(id instanceof Number)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少 groupId 参数");
    }
    return service.checkForGroup(((Number) id).longValue());
  }
}
