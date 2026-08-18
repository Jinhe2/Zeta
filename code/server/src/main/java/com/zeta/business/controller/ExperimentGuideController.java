package com.zeta.business.controller;

import com.zeta.business.auth.AuthService;
import com.zeta.business.entities.experimentguide.dto.CreateExperimentGuideItemRequest;
import com.zeta.business.entities.experimentguide.dto.ExperimentGuideItemAdminResponse;
import com.zeta.business.entities.experimentguide.dto.UpdateExperimentGuideItemRequest;
import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.entities.user.UserRole;
import com.zeta.business.service.ExperimentGuideService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/experiment-guides")
public class ExperimentGuideController {

  private final ExperimentGuideService service;
  private final AuthService authService;

  public ExperimentGuideController(ExperimentGuideService service, AuthService authService) {
    this.service = service;
    this.authService = authService;
  }

  @GetMapping("/{scopeType}/{scopeId}")
  public List<ExperimentGuideItemAdminResponse> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable SettingListScopeType scopeType,
      @PathVariable Long scopeId) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return service.listByScope(scopeType, scopeId);
  }

  @PostMapping("/{scopeType}/{scopeId}")
  public ExperimentGuideItemAdminResponse create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable SettingListScopeType scopeType,
      @PathVariable Long scopeId,
      @Valid @RequestBody CreateExperimentGuideItemRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return service.create(scopeType, scopeId, request);
  }

  @PutMapping("/items/{id}")
  public ExperimentGuideItemAdminResponse update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @Valid @RequestBody UpdateExperimentGuideItemRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return service.update(id, request);
  }

  @DeleteMapping("/items/{id}")
  public Map<String, Object> delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    authService.requireRole(authorization, UserRole.ADMIN);
    service.delete(id);
    return Collections.singletonMap("deleted", true);
  }
}
