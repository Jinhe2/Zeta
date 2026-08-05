package com.zeta.business.controller;

import com.zeta.business.auth.*;
import com.zeta.business.auth.AuthService;
import com.zeta.business.auth.dto.*;
import com.zeta.business.entities.binding.*;
import com.zeta.business.entities.binding.dto.*;
import com.zeta.business.entities.cabinetdisplay.*;
import com.zeta.business.entities.cabinetdisplay.dto.*;
import com.zeta.business.entities.cognitiondevice.*;
import com.zeta.business.entities.cognitiondevice.dto.*;
import com.zeta.business.entities.devicedisplay.*;
import com.zeta.business.entities.devicedisplay.dto.*;
import com.zeta.business.entities.drawinglearning.*;
import com.zeta.business.entities.drawinglearning.dto.*;
import com.zeta.business.entities.learningresource.*;
import com.zeta.business.entities.learningresource.dto.*;
import com.zeta.business.entities.logiclearning.*;
import com.zeta.business.entities.logiclearning.dto.*;
import com.zeta.business.entities.logicnodecognition.*;
import com.zeta.business.entities.logicnodecognition.dto.*;
import com.zeta.business.entities.monitor.*;
import com.zeta.business.entities.snapshot.*;
import com.zeta.business.entities.snapshot.dto.*;
import com.zeta.business.entities.user.*;
import com.zeta.business.entities.user.User;
import com.zeta.business.entities.user.UserRole;
import com.zeta.business.entities.user.dto.*;
import com.zeta.business.media.*;
import com.zeta.business.service.*;
import com.zeta.business.storage.*;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

@RestController
public class LearningResourceController {
  private final LearningResourceService service;
  private final LearningResourceStorage storage;
  private final AuthService authService;

  public LearningResourceController(
      LearningResourceService service, LearningResourceStorage storage, AuthService authService) {
    this.service = service;
    this.storage = storage;
    this.authService = authService;
  }

  @GetMapping("/api/admin/learning-resources")
  public List<LearningResourceResponse> listAdmin(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(required = false) LearningResourceType type,
      @RequestParam(required = false) Long cabinetId) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return service.listAdmin(type, cabinetId);
  }

  @PostMapping(
      value = "/api/admin/learning-resources",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public LearningResourceResponse create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam String name,
      @RequestParam String description,
      @RequestParam LearningResourceType resourceType,
      @RequestParam(required = false) Long cabinetId,
      @RequestParam("file") MultipartFile file) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return service.create(name, description, resourceType, cabinetId, file);
  }

  @PutMapping(
      value = "/api/admin/learning-resources/{id}",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public LearningResourceResponse update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestParam String name,
      @RequestParam String description,
      @RequestParam LearningResourceType resourceType,
      @RequestParam(required = false) Long cabinetId,
      @RequestParam(value = "file", required = false) MultipartFile file) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return service.update(id, name, description, resourceType, cabinetId, file);
  }

  @DeleteMapping("/api/admin/learning-resources/{id}")
  public void delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    authService.requireRole(authorization, UserRole.ADMIN);
    service.delete(id);
  }

  @GetMapping("/api/learning-resources")
  public List<LearningResourceResponse> listForLearner(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam LearningResourceType type,
      @RequestParam String bindId,
      @RequestParam(required = false) Long cabinetId) {
    User user = authService.requireUser(authorization);
    return service.listForBoundCabinet(type, bindId, cabinetId, user.getRole() == UserRole.ADMIN);
  }

  @GetMapping("/api/learning-resources/{id}")
  public LearningResourceResponse getForLearner(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestParam String bindId,
      @RequestParam(required = false) Long cabinetId) {
    User user = authService.requireUser(authorization);
    return service.getForBoundCabinet(id, bindId, cabinetId, user.getRole() == UserRole.ADMIN);
  }

  /** 嵌入 video/iframe 无法携带 Bearer Header，因此用绑定 ID 再次校验资料范围。 */
  @GetMapping("/api/learning-resources/{id}/content")
  public ResponseEntity<Resource> content(
      @PathVariable Long id,
      @RequestParam String bindId,
      @RequestParam(required = false) Long cabinetId) {
    LearningResource item =
        service.getFileForBoundCabinet(id, bindId, cabinetId, cabinetId != null);
    String filename = item.getOriginalFilename();
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(item.getContentType()))
        .contentLength(item.getFileSize())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "inline; filename*=UTF-8''" + UriUtils.encodePathSegment(filename, "UTF-8"))
        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
        .cacheControl(CacheControl.noCache())
        .body(storage.load(item.getFilePath()));
  }
}
