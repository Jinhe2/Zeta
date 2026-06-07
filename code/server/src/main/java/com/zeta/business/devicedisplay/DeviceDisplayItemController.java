package com.zeta.business.devicedisplay;

import com.zeta.business.auth.AuthService;
import com.zeta.business.user.UserRole;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@Validated
public class DeviceDisplayItemController {

    private final DeviceDisplayItemService displayItemService;
    private final AuthService authService;

    public DeviceDisplayItemController(
            DeviceDisplayItemService displayItemService,
            AuthService authService) {
        this.displayItemService = displayItemService;
        this.authService = authService;
    }

    @GetMapping("/api/devices/{deviceId}/display-items")
    public List<DeviceDisplayItemAdminResponse> listByDevice(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long deviceId) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return displayItemService.listByScreenDevice(deviceId);
    }

    @PostMapping("/api/devices/{deviceId}/display-items")
    public DeviceDisplayItemAdminResponse create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long deviceId,
            @Valid @RequestBody CreateDeviceDisplayItemRequest request) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return displayItemService.create(deviceId, request);
    }

    @PutMapping("/api/admin/device-display-items/{id}")
    public DeviceDisplayItemAdminResponse update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody UpdateDeviceDisplayItemRequest request) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return displayItemService.update(id, request);
    }

    @DeleteMapping("/api/admin/device-display-items/{id}")
    public void delete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        authService.requireRole(authorization, UserRole.ADMIN);
        displayItemService.delete(id);
    }
}
