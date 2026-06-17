package com.zeta.business.cognitiondevice;

import com.zeta.business.auth.AuthService;
import com.zeta.business.user.UserRole;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@Validated
public class CognitionDeviceController {

    private final CognitionDeviceService cognitionDeviceService;
    private final AuthService authService;

    public CognitionDeviceController(
            CognitionDeviceService cognitionDeviceService,
            AuthService authService) {
        this.cognitionDeviceService = cognitionDeviceService;
        this.authService = authService;
    }

    @GetMapping("/api/cabinet-display-items/{itemId}/cognition-devices")
    public List<CognitionDeviceAdminResponse> listByCabinetDisplayItem(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long itemId) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return cognitionDeviceService.listByCabinetDisplayItem(itemId);
    }

    @PostMapping("/api/cabinet-display-items/{itemId}/cognition-devices")
    public CognitionDeviceAdminResponse create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long itemId,
            @Valid @RequestBody CreateCognitionDeviceRequest request) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return cognitionDeviceService.create(itemId, request);
    }

    @GetMapping("/api/admin/cognition-devices/{id}")
    public CognitionDeviceAdminResponse get(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return cognitionDeviceService.getAdmin(id);
    }

    @PutMapping("/api/admin/cognition-devices/{id}")
    public CognitionDeviceAdminResponse update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCognitionDeviceRequest request) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return cognitionDeviceService.update(id, request);
    }

    @DeleteMapping("/api/admin/cognition-devices/{id}")
    public void delete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        authService.requireRole(authorization, UserRole.ADMIN);
        cognitionDeviceService.delete(id);
    }
}
