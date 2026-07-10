package com.zeta.business.media;

import com.zeta.business.auth.AuthService;
import com.zeta.business.devicedisplay.DeviceDisplayItem;
import com.zeta.business.devicedisplay.DeviceDisplayItemRepository;
import com.zeta.business.logicnodecognition.LogicNodeCognitionItem;
import com.zeta.business.logicnodecognition.LogicNodeCognitionItemRepository;
import com.zeta.business.user.UserRole;
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

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
public class CognitionVideoController {

    private final CognitionVideoStorage videoStorage;
    private final DeviceDisplayItemRepository deviceRepository;
    private final LogicNodeCognitionItemRepository logicRepository;
    private final AuthService authService;

    public CognitionVideoController(
            CognitionVideoStorage videoStorage,
            DeviceDisplayItemRepository deviceRepository,
            LogicNodeCognitionItemRepository logicRepository,
            AuthService authService) {
        this.videoStorage = videoStorage;
        this.deviceRepository = deviceRepository;
        this.logicRepository = logicRepository;
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
        if (deviceRepository.existsByVideoPath(normalized) || logicRepository.existsByVideoPath(normalized)) {
            throw new ResponseStatusException(CONFLICT, "视频正在被认知条目使用");
        }
        videoStorage.delete(normalized);
    }

    @GetMapping("/api/videos/device-display/{id}")
    public ResponseEntity<Resource> getDeviceVideo(@PathVariable Long id) {
        DeviceDisplayItem item = deviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "视频不存在"));
        return videoResponse(item.getVideoPath());
    }

    @GetMapping("/api/videos/logic-node-cognition/{id}")
    public ResponseEntity<Resource> getLogicVideo(@PathVariable Long id) {
        LogicNodeCognitionItem item = logicRepository.findById(id)
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
