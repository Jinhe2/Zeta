package com.zeta.business.devicedisplay;

import com.zeta.business.cabinetdisplay.TemporaryImage;
import com.zeta.business.cabinetdisplay.TemporaryImageRepository;
import com.zeta.business.cognitiondevice.CognitionDevice;
import com.zeta.business.cognitiondevice.CognitionDeviceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DeviceDisplayItemService {

    private final CognitionDeviceService cognitionDeviceService;
    private final DeviceDisplayItemRepository displayItemRepository;
    private final DeviceDisplayImageStorage imageStorage;
    private final TemporaryImageRepository temporaryImageRepository;

    public DeviceDisplayItemService(
            CognitionDeviceService cognitionDeviceService,
            DeviceDisplayItemRepository displayItemRepository,
            DeviceDisplayImageStorage imageStorage,
            TemporaryImageRepository temporaryImageRepository) {
        this.cognitionDeviceService = cognitionDeviceService;
        this.displayItemRepository = displayItemRepository;
        this.imageStorage = imageStorage;
        this.temporaryImageRepository = temporaryImageRepository;
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public List<DeviceDisplayItemAdminResponse> listByCognitionDevice(Long cognitionDeviceId) {
        cognitionDeviceService.requireDevice(cognitionDeviceId);
        return displayItemRepository.findByCognitionDeviceIdOrderBySortOrderAscIdAsc(cognitionDeviceId).stream()
                .map(this::toAdminResponse)
                .collect(Collectors.toList());
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public List<DeviceDisplayItemResponse> listEnabledByCognitionDevice(Long cognitionDeviceId) {
        cognitionDeviceService.requireDevice(cognitionDeviceId);
        return displayItemRepository.findByCognitionDeviceIdOrderBySortOrderAscIdAsc(cognitionDeviceId).stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .map(this::toLearnerResponse)
                .collect(Collectors.toList());
    }

    @Transactional("businessTransactionManager")
    public DeviceDisplayItemAdminResponse create(Long cognitionDeviceId, CreateDeviceDisplayItemRequest request) {
        cognitionDeviceService.requireDevice(cognitionDeviceId);
        DeviceDisplayItem item = new DeviceDisplayItem();
        item.setCognitionDeviceId(cognitionDeviceId);
        item.setTitle(request.getTitle().trim());
        applyImageData(item, request.getImageId(), request.getImageUrl());
        applyHighlightRegion(item, request.getLeftPercent(), request.getTopPercent(),
                request.getWidthPercent(), request.getHeightPercent());
        item.setContent(request.getContent().trim());
        item.setSortOrder(request.getSortOrder());
        item.setEnabled(request.getEnabled() == null || request.getEnabled());
        item.setCreatedAt(Instant.now());
        return toAdminResponse(displayItemRepository.save(item));
    }

    @Transactional("businessTransactionManager")
    public DeviceDisplayItemAdminResponse update(Long id, UpdateDeviceDisplayItemRequest request) {
        DeviceDisplayItem item = requireItem(id);
        String previousImageUrl = item.getImageUrl();

        item.setTitle(request.getTitle().trim());
        applyImageData(item, request.getImageId(), request.getImageUrl());
        applyHighlightRegion(item, request.getLeftPercent(), request.getTopPercent(),
                request.getWidthPercent(), request.getHeightPercent());
        item.setContent(request.getContent().trim());
        item.setSortOrder(request.getSortOrder());
        item.setEnabled(request.getEnabled());

        DeviceDisplayItem saved = displayItemRepository.save(item);
        String nextImageUrl = item.getImageUrl();
        if (!Objects.equals(nextImageUrl, previousImageUrl)) {
            imageStorage.deleteIfManaged(previousImageUrl);
        }
        return toAdminResponse(saved);
    }

    @Transactional("businessTransactionManager")
    public void delete(Long id) {
        DeviceDisplayItem item = requireItem(id);
        imageStorage.deleteIfManaged(item.getImageUrl());
        displayItemRepository.delete(item);
    }

    private DeviceDisplayItem requireItem(Long id) {
        return displayItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "设备展示条目不存在"));
    }

    private void applyImageData(DeviceDisplayItem item, Long imageId, String imageUrl) {
        if (imageId != null) {
            TemporaryImage tempImage = temporaryImageRepository.findById(imageId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "临时图片不存在或已过期"));
            item.setImageData(tempImage.getImageData());
            item.setImageContentType(tempImage.getContentType());
            item.setImageUrl(null);
            temporaryImageRepository.deleteById(imageId);
        } else if (StringUtils.hasText(imageUrl)) {
            item.setImageUrl(normalizeImageUrl(imageUrl));
            item.setImageData(null);
            item.setImageContentType(null);
        } else if (hasExistingImage(item)) {
            return;
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传认知图片");
        }
    }

    private boolean hasExistingImage(DeviceDisplayItem item) {
        return StringUtils.hasText(item.getImageUrl())
                || (item.getImageData() != null && item.getImageData().length > 0);
    }

    private void applyHighlightRegion(
            DeviceDisplayItem item,
            Double left,
            Double top,
            Double width,
            Double height) {
        if (left == null && top == null && width == null && height == null) {
            item.setLeftPercent(null);
            item.setTopPercent(null);
            item.setWidthPercent(null);
            item.setHeightPercent(null);
            return;
        }
        if (left == null || top == null || width == null || height == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "高亮区域坐标不完整");
        }
        validateRegion(left, top, width, height);
        item.setLeftPercent(left);
        item.setTopPercent(top);
        item.setWidthPercent(width);
        item.setHeightPercent(height);
    }

    private void validateRegion(Double left, Double top, Double width, Double height) {
        if (left < 0 || top < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "高亮区域坐标不能小于 0");
        }
        if (width <= 0 || height <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "高亮区域宽高必须大于 0");
        }
        if (left + width > 100.01 || top + height > 100.01) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "高亮区域超出图片范围");
        }
    }

    private String normalizeImageUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传认知图片");
        }
        String trimmed = imageUrl.trim();
        if (!trimmed.startsWith("/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片地址格式不正确");
        }
        return trimmed;
    }

    private DeviceDisplayItemAdminResponse toAdminResponse(DeviceDisplayItem item) {
        CognitionDevice cognitionDevice = cognitionDeviceService.requireDevice(item.getCognitionDeviceId());
        return new DeviceDisplayItemAdminResponse(
                item.getId(),
                item.getCognitionDeviceId(),
                cognitionDevice.getTitle(),
                item.getTitle(),
                item.getImageUrl(),
                item.getLeftPercent(),
                item.getTopPercent(),
                item.getWidthPercent(),
                item.getHeightPercent(),
                item.getContent(),
                item.getSortOrder(),
                item.getEnabled(),
                item.getCreatedAt());
    }

    private DeviceDisplayItemResponse toLearnerResponse(DeviceDisplayItem item) {
        return new DeviceDisplayItemResponse(
                item.getId(),
                item.getTitle(),
                item.getImageUrl(),
                item.getLeftPercent(),
                item.getTopPercent(),
                item.getWidthPercent(),
                item.getHeightPercent(),
                item.getContent(),
                item.getSortOrder());
    }
}
