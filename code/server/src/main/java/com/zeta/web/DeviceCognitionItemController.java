package com.zeta.web;

import com.zeta.domain.UserRole;
import com.zeta.service.AuthService;
import com.zeta.service.DeviceCognitionItemService;
import com.zeta.web.dto.CreateDeviceCognitionItemRequest;
import com.zeta.web.dto.DeviceCognitionItemAdminResponse;
import com.zeta.web.dto.UpdateDeviceCognitionItemRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@Validated
public class DeviceCognitionItemController {

    private final DeviceCognitionItemService cognitionItemService;
    private final AuthService authService;

    public DeviceCognitionItemController(
            DeviceCognitionItemService cognitionItemService,
            AuthService authService) {
        this.cognitionItemService = cognitionItemService;
        this.authService = authService;
    }

    @GetMapping("/api/devices/{deviceId}/cognition-items")
    public List<DeviceCognitionItemAdminResponse> listByDevice(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long deviceId) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return cognitionItemService.listByDevice(deviceId);
    }

    @PostMapping("/api/devices/{deviceId}/cognition-items")
    public DeviceCognitionItemAdminResponse create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long deviceId,
            @Valid @RequestBody CreateDeviceCognitionItemRequest request) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return cognitionItemService.create(deviceId, request);
    }

    @PutMapping("/api/admin/device-cognition-items/{id}")
    public DeviceCognitionItemAdminResponse update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody UpdateDeviceCognitionItemRequest request) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return cognitionItemService.update(id, request);
    }

    @DeleteMapping("/api/admin/device-cognition-items/{id}")
    public void delete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        authService.requireRole(authorization, UserRole.ADMIN);
        cognitionItemService.delete(id);
    }
}
