package com.zeta.business.devicedisplay;

import com.zeta.business.cognitiondevice.CognitionDevice;
import com.zeta.business.cognitiondevice.CognitionDeviceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeviceDisplayItemService {

    private final CognitionDeviceService cognitionDeviceService;
    private final DeviceDisplayItemRepository displayItemRepository;
    private final DeviceDisplayImageStorage imageStorage;

    public DeviceDisplayItemService(
            CognitionDeviceService cognitionDeviceService,
            DeviceDisplayItemRepository displayItemRepository,
            DeviceDisplayImageStorage imageStorage) {
        this.cognitionDeviceService = cognitionDeviceService;
        this.displayItemRepository = displayItemRepository;
        this.imageStorage = imageStorage;
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
        item.setImageUrl(normalizeImageUrl(request.getImageUrl()));
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
        String nextImageUrl = normalizeImageUrl(request.getImageUrl());

        item.setTitle(request.getTitle().trim());
        item.setImageUrl(nextImageUrl);
        item.setContent(request.getContent().trim());
        item.setSortOrder(request.getSortOrder());
        item.setEnabled(request.getEnabled());

        DeviceDisplayItem saved = displayItemRepository.save(item);
        if (!nextImageUrl.equals(previousImageUrl)) {
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
                item.getContent(),
                item.getSortOrder());
    }
}
