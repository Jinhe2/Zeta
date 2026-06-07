package com.zeta.web;

import com.zeta.domain.UserRole;
import com.zeta.service.AuthService;
import com.zeta.service.ProtectionLogicAdminService;
import com.zeta.web.dto.CreateProtectionLogicRequest;
import com.zeta.web.dto.ProtectionLogicAdminResponse;
import com.zeta.web.dto.ProtectionLogicConfigResponse;
import com.zeta.web.dto.UpdateProtectionLogicConfigRequest;
import com.zeta.web.dto.UpdateProtectionLogicRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@Validated
public class ProtectionLogicAdminController {

    private final ProtectionLogicAdminService protectionLogicAdminService;
    private final AuthService authService;

    public ProtectionLogicAdminController(
            ProtectionLogicAdminService protectionLogicAdminService,
            AuthService authService) {
        this.protectionLogicAdminService = protectionLogicAdminService;
        this.authService = authService;
    }

    @GetMapping("/api/devices/{deviceId}/protection-logics")
    public List<ProtectionLogicAdminResponse> listByDevice(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long deviceId) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return protectionLogicAdminService.listByDevice(deviceId);
    }

    @PostMapping("/api/devices/{deviceId}/protection-logics")
    public ProtectionLogicAdminResponse create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long deviceId,
            @Valid @RequestBody CreateProtectionLogicRequest request) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return protectionLogicAdminService.create(deviceId, request);
    }

    @GetMapping("/api/admin/protection-logics/{id}")
    public ProtectionLogicAdminResponse detail(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return protectionLogicAdminService.getById(id);
    }

    @PutMapping("/api/admin/protection-logics/{id}")
    public ProtectionLogicAdminResponse update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody UpdateProtectionLogicRequest request) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return protectionLogicAdminService.update(id, request);
    }

    @DeleteMapping("/api/admin/protection-logics/{id}")
    public void delete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        authService.requireRole(authorization, UserRole.ADMIN);
        protectionLogicAdminService.delete(id);
    }

    @GetMapping("/api/admin/protection-logics/{id}/config")
    public ProtectionLogicConfigResponse getConfig(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return protectionLogicAdminService.getConfig(id);
    }

    @PutMapping("/api/admin/protection-logics/{id}/config")
    public ProtectionLogicConfigResponse updateConfig(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody UpdateProtectionLogicConfigRequest request) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return protectionLogicAdminService.updateConfig(id, request);
    }
}
