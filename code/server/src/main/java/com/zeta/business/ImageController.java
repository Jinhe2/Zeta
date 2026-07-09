package com.zeta.business;

import com.zeta.business.cabinetdisplay.CabinetDisplayItem;
import com.zeta.business.cabinetdisplay.CabinetDisplayItemRepository;
import com.zeta.business.devicedisplay.DeviceDisplayItem;
import com.zeta.business.devicedisplay.DeviceDisplayItemRepository;
import com.zeta.business.logicnodecognition.LogicNodeCognitionItem;
import com.zeta.business.logicnodecognition.LogicNodeCognitionItemRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

/**
 * 图片访问 API — 从数据库读取图片二进制数据。
 */
@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final CabinetDisplayItemRepository cabinetRepository;
    private final DeviceDisplayItemRepository deviceRepository;
    private final LogicNodeCognitionItemRepository logicNodeRepository;

    public ImageController(CabinetDisplayItemRepository cabinetRepository,
                           DeviceDisplayItemRepository deviceRepository,
                           LogicNodeCognitionItemRepository logicNodeRepository) {
        this.cabinetRepository = cabinetRepository;
        this.deviceRepository = deviceRepository;
        this.logicNodeRepository = logicNodeRepository;
    }

    @GetMapping("/cabinet-display/{id}")
    public ResponseEntity<?> getCabinetImage(@PathVariable Long id) {
        CabinetDisplayItem item = cabinetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图片不存在"));

        if (item.getImageData() == null || item.getImageData().length == 0) {
            if (StringUtils.hasText(item.getImageUrl())) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(item.getImageUrl()))
                        .cacheControl(CacheControl.noStore())
                        .build();
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图片数据为空");
        }

        String contentType = item.getImageContentType() != null
                ? item.getImageContentType()
                : "image/jpeg";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.noStore())
                .body(item.getImageData());
    }

    @GetMapping("/device-display/{id}")
    public ResponseEntity<?> getDeviceImage(@PathVariable Long id) {
        DeviceDisplayItem item = deviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图片不存在"));

        if (item.getImageData() == null || item.getImageData().length == 0) {
            if (StringUtils.hasText(item.getImageUrl())) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(item.getImageUrl()))
                        .cacheControl(CacheControl.noStore())
                        .build();
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图片数据为空");
        }

        String contentType = item.getImageContentType() != null
                ? item.getImageContentType()
                : "image/jpeg";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.noStore())
                .body(item.getImageData());
    }

    @GetMapping("/logic-node-cognition/{id}")
    public ResponseEntity<?> getLogicNodeCognitionImage(@PathVariable Long id) {
        LogicNodeCognitionItem item = logicNodeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图片不存在"));

        if (item.getImageData() == null || item.getImageData().length == 0) {
            if (StringUtils.hasText(item.getImageUrl())) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(item.getImageUrl()))
                        .cacheControl(CacheControl.noStore())
                        .build();
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图片数据为空");
        }

        String contentType = item.getImageContentType() != null
                ? item.getImageContentType()
                : "image/jpeg";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.noStore())
                .body(item.getImageData());
    }
}
