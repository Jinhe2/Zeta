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
import java.io.InputStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
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
  public ResponseEntity<?> getDeviceVideo(
      @PathVariable Long id, @RequestHeader HttpHeaders headers) {
    DeviceDisplayItem item =
        deviceRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "视频不存在"));
    return videoResponse(item.getVideoPath(), headers);
  }

  @GetMapping("/api/videos/logic-node-cognition/{id}")
  public ResponseEntity<?> getLogicVideo(
      @PathVariable Long id, @RequestHeader HttpHeaders headers) {
    LogicNodeCognitionItem item =
        logicRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "视频不存在"));
    return videoResponse(item.getVideoPath(), headers);
  }

  @GetMapping("/api/videos/sampling-test/{id}")
  public ResponseEntity<?> getSamplingTestVideo(
      @PathVariable Long id, @RequestHeader HttpHeaders headers) {
    SamplingTestItem item = samplingTestItemRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "视频不存在"));
    return videoResponse(item.getVideoPath(), headers);
  }

  private ResponseEntity<?> videoResponse(String videoPath, HttpHeaders requestHeaders) {
    if (videoPath == null) {
      throw new ResponseStatusException(NOT_FOUND, "视频不存在");
    }
    Resource video = videoStorage.load(videoPath);
    long contentLength = contentLength(video);
    if (!requestHeaders.getRange().isEmpty()) {
      HttpRange range = requestHeaders.getRange().get(0);
      long start = range.getRangeStart(contentLength);
      long end = range.getRangeEnd(contentLength);
      long rangeLength = end - start + 1;
      return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
          .contentType(MediaType.parseMediaType("video/mp4"))
          .contentLength(rangeLength)
          .header(HttpHeaders.ACCEPT_RANGES, "bytes")
          .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + contentLength)
          .cacheControl(CacheControl.noCache())
          .body(rangeResource(video, start, rangeLength));
    }
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("video/mp4"))
        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
        .contentLength(contentLength)
        .cacheControl(CacheControl.noCache())
        .body(video);
  }

  private Resource rangeResource(Resource video, long start, long length) {
    try {
      InputStream inputStream = video.getInputStream();
      skipFully(inputStream, start);
      return new InputStreamResource(new BoundedInputStream(inputStream, length)) {
        @Override
        public long contentLength() {
          return length;
        }

        @Override
        public String getFilename() {
          return video.getFilename();
        }
      };
    } catch (java.io.IOException ex) {
      throw new ResponseStatusException(NOT_FOUND, "视频不存在");
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

  private long contentLength(Resource video) {
    try {
      return video.contentLength();
    } catch (Exception ex) {
      throw new ResponseStatusException(NOT_FOUND, "视频不存在");
    }
  }

  @Getter
  @AllArgsConstructor
  public static class UploadVideoResponse {
    private String videoPath;
  }
}
