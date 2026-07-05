package com.zeta.business.cabinetdisplay;

import com.zeta.business.auth.AuthService;
import com.zeta.business.user.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class CabinetDisplayImageController {

    private final CabinetDisplayImageStorage imageStorage;
    private final TemporaryImageRepository temporaryImageRepository;
    private final AuthService authService;

    public CabinetDisplayImageController(
            CabinetDisplayImageStorage imageStorage,
            TemporaryImageRepository temporaryImageRepository,
            AuthService authService) {
        this.imageStorage = imageStorage;
        this.temporaryImageRepository = temporaryImageRepository;
        this.authService = authService;
    }

    @PostMapping("/api/admin/cabinet-display-images")
    public UploadImageResponse upload(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("file") MultipartFile file) {
        authService.requireRole(authorization, UserRole.ADMIN);

        // 新方式：保存到临时表，返回临时 ID
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
        private Long imageId;  // 临时图片 ID
    }
}
