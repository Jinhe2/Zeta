package com.zeta.business.controller;

import com.zeta.business.auth.AuthService;
import com.zeta.business.entities.samplingtest.dto.*;
import com.zeta.business.entities.user.UserRole;
import com.zeta.business.service.SamplingTestService;
import com.zeta.business.service.DigitalSamplingTopologyService;
import com.zeta.screen.sampling.SamplingSignalCatalogService;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class SamplingTestController {
  private final SamplingTestService service;
  private final AuthService authService;
  private final SamplingSignalCatalogService samplingSignalCatalogService;
  private final DigitalSamplingTopologyService digitalSamplingTopologyService;

  public SamplingTestController(
      SamplingTestService service,
      AuthService authService,
      SamplingSignalCatalogService samplingSignalCatalogService,
      DigitalSamplingTopologyService digitalSamplingTopologyService) {
    this.service = service;
    this.authService = authService;
    this.samplingSignalCatalogService = samplingSignalCatalogService;
    this.digitalSamplingTopologyService = digitalSamplingTopologyService;
  }

  @GetMapping("/api/admin/sampling-tests/cabinets/{cabinetId}/items")
  public List<SamplingTestItemResponse> listAdmin(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long cabinetId) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return service.listAdmin(cabinetId);
  }

  @PostMapping("/api/admin/sampling-tests/cabinets/{cabinetId}/items")
  public SamplingTestItemResponse create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long cabinetId,
      @Valid @RequestBody SamplingTestItemRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return service.create(cabinetId, request);
  }

  @PutMapping("/api/admin/sampling-tests/items/{id}")
  public SamplingTestItemResponse update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @Valid @RequestBody SamplingTestItemRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return service.update(id, request);
  }

  @DeleteMapping("/api/admin/sampling-tests/items/{id}")
  public void delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    authService.requireRole(authorization, UserRole.ADMIN);
    service.delete(id);
  }

  @GetMapping("/api/knowledge/cabinets/{cabinetId}/sampling-test-items")
  public List<SamplingTestItemResponse> listStudent(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long cabinetId) {
    authService.requireUser(authorization);
    return service.listEnabled(cabinetId);
  }

  @GetMapping("/api/admin/sampling-tests/cabinets/{cabinetId}/digital-devices")
  public List<Map<String, Object>> listDigitalDevices(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long cabinetId) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return samplingSignalCatalogService.listDevices(cabinetId);
  }

  @GetMapping("/api/admin/sampling-tests/cabinets/{cabinetId}/digital-channels")
  public Map<String, Object> listDigitalChannels(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long cabinetId,
      @RequestParam Long iedDeviceId) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return samplingSignalCatalogService.listCandidates(cabinetId, iedDeviceId);
  }

  @GetMapping("/api/knowledge/sampling-test-items/{itemId}/digital-topology")
  public Map<String, Object> digitalTopology(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long itemId) {
    authService.requireUser(authorization);
    return digitalSamplingTopologyService.getTopology(itemId);
  }
}
