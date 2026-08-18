package com.zeta.business.controller;

import com.zeta.business.auth.AuthService;
import com.zeta.business.entities.cabinetdisplay.TemporaryImage;
import com.zeta.business.entities.cabinetdisplay.TemporaryImageRepository;
import com.zeta.business.entities.user.UserRole;
import com.zeta.business.storage.DeviceDisplayImageStorage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ExperimentGuideImageController {

  private final DeviceDisplayImageStorage imageStorage;
  private final TemporaryImageRepository temporaryImageRepository;
  private final AuthService authService;

  public ExperimentGuideImageController(
      DeviceDisplayImageStorage imageStorage,
      TemporaryImageRepository temporaryImageRepository,
      AuthService authService) {
    this.imageStorage = imageStorage;
    this.temporaryImageRepository = temporaryImageRepository;
    this.authService = authService;
  }

  @PostMapping("/api/admin/experiment-guide-images")
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
