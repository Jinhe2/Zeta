package com.zeta.web;

import com.zeta.domain.UserRole;
import com.zeta.service.AuthService;
import com.zeta.service.CabinetService;
import com.zeta.web.dto.CabinetAdminResponse;
import com.zeta.web.dto.CreateCabinetRequest;
import com.zeta.web.dto.UpdateCabinetRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/cabinets")
@Validated
public class CabinetController {

    private final CabinetService cabinetService;
    private final AuthService authService;

    public CabinetController(CabinetService cabinetService, AuthService authService) {
        this.cabinetService = cabinetService;
        this.authService = authService;
    }

    @GetMapping
    public List<CabinetAdminResponse> list(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return cabinetService.listAll();
    }

    @GetMapping("/{id}")
    public CabinetAdminResponse detail(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return cabinetService.getById(id);
    }

    @PostMapping
    public CabinetAdminResponse create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateCabinetRequest request) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return cabinetService.create(request);
    }

    @PutMapping("/{id}")
    public CabinetAdminResponse update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCabinetRequest request) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return cabinetService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        authService.requireRole(authorization, UserRole.ADMIN);
        cabinetService.delete(id);
    }
}
