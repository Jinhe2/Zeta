package com.zeta.business.controller;

import com.zeta.business.auth.AuthService;
import com.zeta.business.entities.user.UserRole;
import com.zeta.business.entities.wiringrequirement.WiringRequirementDtos.*;
import com.zeta.business.service.WiringRequirementService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/wiring-requirements")
public class WiringRequirementController {
  private final AuthService authService;
  private final WiringRequirementService service;

  public WiringRequirementController(AuthService authService, WiringRequirementService service) {
    this.authService = authService;
    this.service = service;
  }

  @GetMapping("/{logicDiagramId}")
  public GetResponse get(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long logicDiagramId) {
    requireAdmin(authorization);
    return service.get(logicDiagramId);
  }

  @PutMapping("/{logicDiagramId}")
  public GetResponse replace(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long logicDiagramId,
      @Valid @RequestBody SaveRequest request) {
    requireAdmin(authorization);
    return service.replace(logicDiagramId, request);
  }

  private void requireAdmin(String authorization) {
    authService.requireRole(authorization, UserRole.ADMIN);
  }
}
