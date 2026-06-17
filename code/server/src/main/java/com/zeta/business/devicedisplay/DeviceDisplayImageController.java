package com.zeta.business.devicedisplay;

import com.zeta.business.auth.AuthService;
import com.zeta.business.user.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class DeviceDisplayImageController {

    private final DeviceDisplayImageStorage imageStorage;
    private final AuthService authService;

    public DeviceDisplayImageController(
            DeviceDisplayImageStorage imageStorage,
            AuthService authService) {
        this.imageStorage = imageStorage;
        this.authService = authService;
    }

    @PostMapping("/api/admin/device-display-images")
    public UploadImageResponse upload(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("file") MultipartFile file) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return new UploadImageResponse(imageStorage.store(file));
    }

    @Getter
    @AllArgsConstructor
    public static class UploadImageResponse {
        private String imageUrl;
    }
}
