package com.zeta.web;

import com.zeta.domain.UserRole;
import com.zeta.service.AuthService;
import com.zeta.service.DeviceService;
import com.zeta.web.dto.CreateDeviceRequest;
import com.zeta.web.dto.DeviceAdminResponse;
import com.zeta.web.dto.UpdateDeviceRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@Validated
public class DeviceController {

    private final DeviceService deviceService;
    private final AuthService authService;

    public DeviceController(DeviceService deviceService, AuthService authService) {
        this.deviceService = deviceService;
        this.authService = authService;
    }

    @GetMapping("/api/cabinets/{cabinetId}/devices")
    public List<DeviceAdminResponse> listByCabinet(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long cabinetId) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return deviceService.listByCabinet(cabinetId);
    }

    @PostMapping("/api/cabinets/{cabinetId}/devices")
    public DeviceAdminResponse create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long cabinetId,
            @Valid @RequestBody CreateDeviceRequest request) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return deviceService.create(cabinetId, request);
    }

    @GetMapping("/api/devices/{id}")
    public DeviceAdminResponse detail(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return deviceService.getById(id);
    }

    @PutMapping("/api/devices/{id}")
    public DeviceAdminResponse update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody UpdateDeviceRequest request) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return deviceService.update(id, request);
    }

    @DeleteMapping("/api/devices/{id}")
    public void delete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        authService.requireRole(authorization, UserRole.ADMIN);
        deviceService.delete(id);
    }
}
