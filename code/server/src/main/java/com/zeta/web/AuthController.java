package com.zeta.web;

import com.zeta.service.AuthService;
import com.zeta.web.dto.AuthTokenResponse;
import com.zeta.web.dto.ChangePasswordRequest;
import com.zeta.web.dto.LoginRequest;
import com.zeta.web.dto.RefreshTokenRequest;
import com.zeta.web.dto.UserProfileResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthTokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.getUsername(), request.getPassword());
    }

    @PostMapping("/refresh")
    public AuthTokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.getRefreshToken());
    }

    @GetMapping("/me")
    public UserProfileResponse me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return authService.profile(authorization);
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null) {
            authService.logout(request.getRefreshToken());
        }
    }

    @PostMapping("/change-password")
    public void changePassword(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authorization, request.getOldPassword(), request.getNewPassword());
    }
}
