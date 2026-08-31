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
import java.io.InputStream;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
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

  @GetMapping("/api/admin/learning-resources/{id}/content")
  public ResponseEntity<?> adminContent(
      @RequestHeader HttpHeaders requestHeaders,
      @RequestParam(value = "accessToken", required = false) String accessToken,
      @PathVariable Long id) {
    String authorization = requestHeaders.getFirst(HttpHeaders.AUTHORIZATION);
    authService.requireRole(resolveAuthorization(authorization, accessToken), UserRole.ADMIN);
    return contentResponse(service.getFileForAdmin(id), requestHeaders);
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
  public ResponseEntity<?> content(
      @RequestHeader HttpHeaders requestHeaders,
      @PathVariable Long id,
      @RequestParam String bindId,
      @RequestParam(required = false) Long cabinetId) {
    LearningResource item =
        service.getFileForBoundCabinet(id, bindId, cabinetId, cabinetId != null);
    return contentResponse(item, requestHeaders);
  }

  private ResponseEntity<?> contentResponse(LearningResource item, HttpHeaders requestHeaders) {
    String filename = item.getOriginalFilename();
    Resource resource = storage.load(item.getFilePath());
    MediaType contentType = MediaType.parseMediaType(item.getContentType());
    if (!requestHeaders.getRange().isEmpty()) {
      HttpRange range = requestHeaders.getRange().get(0);
      long start = range.getRangeStart(item.getFileSize());
      long end = range.getRangeEnd(item.getFileSize());
      long rangeLength = end - start + 1;
      return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
          .contentType(contentType)
          .contentLength(rangeLength)
          .header(HttpHeaders.CONTENT_DISPOSITION, inlineDisposition(filename))
          .header(HttpHeaders.ACCEPT_RANGES, "bytes")
          .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + item.getFileSize())
          .cacheControl(CacheControl.noCache())
          .body(rangeResource(resource, start, rangeLength, filename));
    }
    return ResponseEntity.ok()
        .contentType(contentType)
        .contentLength(item.getFileSize())
        .header(HttpHeaders.CONTENT_DISPOSITION, inlineDisposition(filename))
        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
        .cacheControl(CacheControl.noCache())
        .body(resource);
  }

  private Resource rangeResource(Resource resource, long start, long length, String filename) {
    try {
      InputStream inputStream = resource.getInputStream();
      skipFully(inputStream, start);
      return new InputStreamResource(new BoundedInputStream(inputStream, length)) {
        @Override
        public long contentLength() {
          return length;
        }

        @Override
        public String getFilename() {
          return filename;
        }
      };
    } catch (java.io.IOException ex) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "学习资料文件不存在");
    }
  }

  private static class BoundedInputStream extends java.io.FilterInputStream {
    private long remaining;

    private BoundedInputStream(InputStream inputStream, long remaining) {
      super(inputStream);
      this.remaining = remaining;
    }

    @Override
    public int read() throws java.io.IOException {
      if (remaining <= 0) {
        return -1;
      }
      int value = super.read();
      if (value != -1) {
        remaining -= 1;
      }
      return value;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws java.io.IOException {
      if (remaining <= 0) {
        return -1;
      }
      int read = super.read(bytes, offset, (int) Math.min(length, remaining));
      if (read == -1) {
        return -1;
      }
      remaining -= read;
      return read;
    }
  }

  private void skipFully(InputStream inputStream, long bytes) throws java.io.IOException {
    long remaining = bytes;
    while (remaining > 0) {
      long skipped = inputStream.skip(remaining);
      if (skipped > 0) {
        remaining -= skipped;
        continue;
      }
      if (inputStream.read() == -1) {
        break;
      }
      remaining -= 1;
    }
  }

  private String inlineDisposition(String filename) {
    return "inline; filename*=UTF-8''" + UriUtils.encodePathSegment(filename, "UTF-8");
  }

  private String resolveAuthorization(String authorization, String accessToken) {
    if (StringUtils.hasText(authorization)) {
      return authorization;
    }
    if (StringUtils.hasText(accessToken)) {
      return "Bearer " + accessToken;
    }
    return null;
  }
}
