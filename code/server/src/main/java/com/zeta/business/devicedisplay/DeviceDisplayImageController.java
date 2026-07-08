package com.zeta.business.devicedisplay;

import com.zeta.business.auth.AuthService;
import com.zeta.business.cabinetdisplay.TemporaryImage;
import com.zeta.business.cabinetdisplay.TemporaryImageRepository;
import com.zeta.business.user.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class DeviceDisplayImageController {

    private final DeviceDisplayImageStorage imageStorage;
    private final TemporaryImageRepository temporaryImageRepository;
    private final AuthService authService;

    public DeviceDisplayImageController(
            DeviceDisplayImageStorage imageStorage,
            TemporaryImageRepository temporaryImageRepository,
            AuthService authService) {
        this.imageStorage = imageStorage;
        this.temporaryImageRepository = temporaryImageRepository;
        this.authService = authService;
    }

    @PostMapping("/api/admin/device-display-images")
    public UploadImageResponse upload(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("file") MultipartFile file) {
        authService.requireRole(authorization, UserRole.ADMIN);

        byte[] imageBytes = imageStorage.readImageBytes(file);
        String contentType = imageStorage.resolveContentType(file.getOriginalFilename());

        TemporaryImage tempImage = new TemporaryImage();
        tempImage.setImageData(imageBytes);
        tempImage.setContentType(contentType);
        TemporaryImage saved = temporaryImageRepository.save(tempImage);

        return new UploadImageResponse(saved.getId());
    }

    @Getter
    @AllArgsConstructor
    public static class UploadImageResponse {
        private Long imageId;
    }
}
