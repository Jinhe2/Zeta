package com.zeta.screen.virtualcircuit;

import com.zeta.business.auth.AuthService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/knowledge/cabinets/{cabinetId}")
public class VirtualCircuitTopologyController {

  private final VirtualCircuitTopologyService service;
  private final AuthService authService;

  public VirtualCircuitTopologyController(
      VirtualCircuitTopologyService service, AuthService authService) {
    this.service = service;
    this.authService = authService;
  }

  @GetMapping("/virtual-circuit-devices")
  public List<Map<String, Object>> listDevices(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long cabinetId) {
    authService.requireUser(authorization);
    return service.listEligibleDevices(cabinetId);
  }

  @GetMapping("/virtual-circuits/{iedDeviceId}")
  public Map<String, Object> topology(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long cabinetId,
      @PathVariable Long iedDeviceId) {
    authService.requireUser(authorization);
    return service.getTopology(cabinetId, iedDeviceId);
  }
}
