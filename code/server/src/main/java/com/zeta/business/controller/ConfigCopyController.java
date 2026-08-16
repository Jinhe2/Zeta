package com.zeta.business.controller;

import com.zeta.business.auth.AuthService;
import com.zeta.business.entities.configcopy.dto.ConfigCopyExecuteResponse;
import com.zeta.business.entities.configcopy.dto.ConfigCopyPrecheckResponse;
import com.zeta.business.entities.configcopy.dto.ConfigCopyRequest;
import com.zeta.business.entities.user.UserRole;
import com.zeta.business.service.ConfigCopyService;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/config-copies")
public class ConfigCopyController {
  private final ConfigCopyService service;
  private final AuthService authService;

  public ConfigCopyController(ConfigCopyService service, AuthService authService) {
    this.service = service;
    this.authService = authService;
  }

  @PostMapping("/precheck")
  public ConfigCopyPrecheckResponse precheck(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @Valid @RequestBody ConfigCopyRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return service.precheck(request);
  }

  @PostMapping("/execute")
  public ResponseEntity<ConfigCopyExecuteResponse> execute(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @Valid @RequestBody ConfigCopyRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    ConfigCopyExecuteResponse response = service.execute(request);
    return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.status(409).body(response);
  }
}
