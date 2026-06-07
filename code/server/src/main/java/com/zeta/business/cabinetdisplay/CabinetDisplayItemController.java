package com.zeta.business.cabinetdisplay;

import com.zeta.business.auth.AuthService;
import com.zeta.business.user.UserRole;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@Validated
public class CabinetDisplayItemController {

    private final CabinetDisplayItemService displayItemService;
    private final AuthService authService;

    public CabinetDisplayItemController(
            CabinetDisplayItemService displayItemService,
            AuthService authService) {
        this.displayItemService = displayItemService;
        this.authService = authService;
    }

    @GetMapping("/api/cabinets/{cabinetId}/display-items")
    public List<CabinetDisplayItemAdminResponse> listByCabinet(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long cabinetId) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return displayItemService.listByScreenCabinet(cabinetId);
    }

    @PostMapping("/api/cabinets/{cabinetId}/display-items")
    public CabinetDisplayItemAdminResponse create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long cabinetId,
            @Valid @RequestBody CreateCabinetDisplayItemRequest request) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return displayItemService.create(cabinetId, request);
    }

    @PutMapping("/api/admin/cabinet-display-items/{id}")
    public CabinetDisplayItemAdminResponse update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCabinetDisplayItemRequest request) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return displayItemService.update(id, request);
    }

    @DeleteMapping("/api/admin/cabinet-display-items/{id}")
    public void delete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        authService.requireRole(authorization, UserRole.ADMIN);
        displayItemService.delete(id);
    }
}
