package com.zeta.business.controller;

import com.zeta.business.auth.*;
import com.zeta.business.auth.dto.*;
import com.zeta.business.entities.binding.*;
import com.zeta.business.entities.binding.dto.*;
import com.zeta.business.entities.cabinetdisplay.*;
import com.zeta.business.entities.cabinetdisplay.CabinetDisplayItem;
import com.zeta.business.entities.cabinetdisplay.CabinetDisplayItemRepository;
import com.zeta.business.entities.cabinetdisplay.dto.*;
import com.zeta.business.entities.cognitiondevice.*;
import com.zeta.business.entities.cognitiondevice.dto.*;
import com.zeta.business.entities.devicedisplay.*;
import com.zeta.business.entities.devicedisplay.DeviceDisplayItem;
import com.zeta.business.entities.devicedisplay.DeviceDisplayItemRepository;
import com.zeta.business.entities.devicedisplay.dto.*;
import com.zeta.business.entities.drawinglearning.*;
import com.zeta.business.entities.drawinglearning.DrawingPage;
import com.zeta.business.entities.drawinglearning.DrawingPageRepository;
import com.zeta.business.entities.drawinglearning.dto.*;
import com.zeta.business.entities.learningresource.*;
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
import com.zeta.business.entities.user.dto.*;
import com.zeta.business.media.*;
import com.zeta.business.service.*;
import com.zeta.business.storage.*;
import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** 图片访问 API — 从数据库读取图片二进制数据。 */
@RestController
@RequestMapping("/api/images")
public class ImageController {

  private final CabinetDisplayItemRepository cabinetRepository;
  private final DeviceDisplayItemRepository deviceRepository;
  private final LogicNodeCognitionItemRepository logicNodeRepository;
  private final DrawingPageRepository drawingPageRepository;
  private final SamplingTestItemRepository samplingTestItemRepository;

  public ImageController(
      CabinetDisplayItemRepository cabinetRepository,
      DeviceDisplayItemRepository deviceRepository,
      LogicNodeCognitionItemRepository logicNodeRepository,
      DrawingPageRepository drawingPageRepository,
      SamplingTestItemRepository samplingTestItemRepository) {
    this.cabinetRepository = cabinetRepository;
    this.deviceRepository = deviceRepository;
    this.logicNodeRepository = logicNodeRepository;
    this.drawingPageRepository = drawingPageRepository;
    this.samplingTestItemRepository = samplingTestItemRepository;
  }

  @GetMapping("/cabinet-display/{id}")
  public ResponseEntity<?> getCabinetImage(@PathVariable Long id) {
    CabinetDisplayItem item =
        cabinetRepository
            .findById(id)
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

    String contentType =
        item.getImageContentType() != null ? item.getImageContentType() : "image/jpeg";

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .cacheControl(CacheControl.noStore())
        .body(item.getImageData());
  }

  @GetMapping("/device-display/{id}")
  public ResponseEntity<?> getDeviceImage(@PathVariable Long id) {
    DeviceDisplayItem item =
        deviceRepository
            .findById(id)
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

    String contentType =
        item.getImageContentType() != null ? item.getImageContentType() : "image/jpeg";

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .cacheControl(CacheControl.noStore())
        .body(item.getImageData());
  }

  @GetMapping("/logic-node-cognition/{id}")
  public ResponseEntity<?> getLogicNodeCognitionImage(@PathVariable Long id) {
    LogicNodeCognitionItem item =
        logicNodeRepository
            .findById(id)
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

    String contentType =
        item.getImageContentType() != null ? item.getImageContentType() : "image/jpeg";

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .cacheControl(CacheControl.noStore())
        .body(item.getImageData());
  }

  @GetMapping("/drawing-page/{id}")
  public ResponseEntity<?> getDrawingPageImage(@PathVariable Long id) {
    DrawingPage page =
        drawingPageRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图片不存在"));

    if (page.getImageData() == null || page.getImageData().length == 0) {
      if (StringUtils.hasText(page.getImageUrl())) {
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(page.getImageUrl()))
            .cacheControl(CacheControl.noStore())
            .build();
      }
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图片数据为空");
    }

    String contentType =
        page.getImageContentType() != null ? page.getImageContentType() : "image/jpeg";

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .cacheControl(CacheControl.noStore())
        .body(page.getImageData());
  }

  @GetMapping("/sampling-test/{id}")
  public ResponseEntity<?> getSamplingTestImage(@PathVariable Long id) {
    SamplingTestItem item = samplingTestItemRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图片不存在"));
    if (item.getImageData() == null || item.getImageData().length == 0) {
      if (StringUtils.hasText(item.getImageUrl())) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(item.getImageUrl()))
            .cacheControl(CacheControl.noStore()).build();
      }
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图片数据为空");
    }
    String contentType = item.getImageContentType() != null ? item.getImageContentType() : "image/jpeg";
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
        .cacheControl(CacheControl.noStore()).body(item.getImageData());
  }
}
