package com.zeta.business.controller;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

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
import com.zeta.business.entities.devicedisplay.DeviceDisplayItem;
import com.zeta.business.entities.devicedisplay.DeviceDisplayItemRepository;
import com.zeta.business.entities.devicedisplay.dto.*;
import com.zeta.business.entities.drawinglearning.*;
import com.zeta.business.entities.drawinglearning.dto.*;
import com.zeta.business.entities.learningresource.*;
import com.zeta.business.entities.learningresource.LearningResourceRepository;
import com.zeta.business.entities.learningresource.dto.*;
import com.zeta.business.entities.logiclearning.*;
import com.zeta.business.entities.logiclearning.dto.*;
import com.zeta.business.entities.logicnodecognition.*;
import com.zeta.business.entities.logicnodecognition.LogicNodeCognitionItem;
import com.zeta.business.entities.logicnodecognition.LogicNodeCognitionItemRepository;
import com.zeta.business.entities.logicnodecognition.dto.*;
import com.zeta.business.entities.monitor.*;
import com.zeta.business.entities.snapshot.*;
import com.zeta.business.entities.snapshot.dto.*;
import com.zeta.business.entities.samplingtest.SamplingTestItem;
import com.zeta.business.entities.samplingtest.SamplingTestItemRepository;
import com.zeta.business.entities.user.*;
import com.zeta.business.entities.user.UserRole;
import com.zeta.business.entities.user.dto.*;
import com.zeta.business.media.*;
import com.zeta.business.service.*;
import com.zeta.business.storage.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class CognitionVideoController {

  private final CognitionVideoStorage videoStorage;
  private final DeviceDisplayItemRepository deviceRepository;
  private final LogicNodeCognitionItemRepository logicRepository;
  private final LearningResourceRepository learningResourceRepository;
  private final SamplingTestItemRepository samplingTestItemRepository;
  private final AuthService authService;

  public CognitionVideoController(
      CognitionVideoStorage videoStorage,
      DeviceDisplayItemRepository deviceRepository,
      LogicNodeCognitionItemRepository logicRepository,
      LearningResourceRepository learningResourceRepository,
      SamplingTestItemRepository samplingTestItemRepository,
      AuthService authService) {
    this.videoStorage = videoStorage;
    this.deviceRepository = deviceRepository;
    this.logicRepository = logicRepository;
    this.learningResourceRepository = learningResourceRepository;
    this.samplingTestItemRepository = samplingTestItemRepository;
    this.authService = authService;
  }

  @PostMapping("/api/admin/cognition-videos")
  public UploadVideoResponse upload(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam("file") MultipartFile file) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return new UploadVideoResponse(videoStorage.store(file));
  }

  @DeleteMapping("/api/admin/cognition-videos")
  public void deleteUnreferenced(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam("path") String path) {
    authService.requireRole(authorization, UserRole.ADMIN);
    String normalized = videoStorage.normalizeManagedPath(path);
    if (deviceRepository.existsByVideoPath(normalized)
        || logicRepository.existsByVideoPath(normalized)
        || samplingTestItemRepository.existsByVideoPath(normalized)
        || learningResourceRepository.existsByFilePath(normalized)) {
      throw new ResponseStatusException(CONFLICT, "视频正在被认知条目使用");
    }
    videoStorage.delete(normalized);
  }

  @GetMapping("/api/videos/device-display/{id}")
  public ResponseEntity<Resource> getDeviceVideo(@PathVariable Long id) {
    DeviceDisplayItem item =
        deviceRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "视频不存在"));
    return videoResponse(item.getVideoPath());
  }

  @GetMapping("/api/videos/logic-node-cognition/{id}")
  public ResponseEntity<Resource> getLogicVideo(@PathVariable Long id) {
    LogicNodeCognitionItem item =
        logicRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "视频不存在"));
    return videoResponse(item.getVideoPath());
  }

  @GetMapping("/api/videos/sampling-test/{id}")
  public ResponseEntity<Resource> getSamplingTestVideo(@PathVariable Long id) {
    SamplingTestItem item = samplingTestItemRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "视频不存在"));
    return videoResponse(item.getVideoPath());
  }

  private ResponseEntity<Resource> videoResponse(String videoPath) {
    if (videoPath == null) {
      throw new ResponseStatusException(NOT_FOUND, "视频不存在");
    }
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("video/mp4"))
        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
        .cacheControl(CacheControl.noCache())
        .body(videoStorage.load(videoPath));
  }

  @Getter
  @AllArgsConstructor
  public static class UploadVideoResponse {
    private String videoPath;
  }
}
