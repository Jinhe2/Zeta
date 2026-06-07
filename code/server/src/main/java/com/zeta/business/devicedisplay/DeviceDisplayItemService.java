package com.zeta.business.devicedisplay;

import com.zeta.screen.ieddevice.ScreenDeviceLookupService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeviceDisplayItemService {

    private final ScreenDeviceLookupService screenDeviceLookupService;
    private final DeviceDisplayItemRepository displayItemRepository;

    public DeviceDisplayItemService(
            ScreenDeviceLookupService screenDeviceLookupService,
            DeviceDisplayItemRepository displayItemRepository) {
        this.screenDeviceLookupService = screenDeviceLookupService;
        this.displayItemRepository = displayItemRepository;
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public List<DeviceDisplayItemAdminResponse> listByScreenDevice(Long screenDeviceId) {
        screenDeviceLookupService.requireDevice(screenDeviceId);
        return displayItemRepository.findByScreenDeviceIdOrderBySortOrderAscIdAsc(screenDeviceId).stream()
                .map(this::toAdminResponse)
                .collect(Collectors.toList());
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public List<DeviceDisplayItemResponse> listEnabledByScreenDevice(Long screenDeviceId) {
        screenDeviceLookupService.requireDevice(screenDeviceId);
        return displayItemRepository.findByScreenDeviceIdOrderBySortOrderAscIdAsc(screenDeviceId).stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .map(this::toLearnerResponse)
                .collect(Collectors.toList());
    }

    @Transactional("businessTransactionManager")
    public DeviceDisplayItemAdminResponse create(Long screenDeviceId, CreateDeviceDisplayItemRequest request) {
        screenDeviceLookupService.requireDevice(screenDeviceId);
        DeviceDisplayItem item = new DeviceDisplayItem();
        item.setScreenDeviceId(screenDeviceId);
        item.setTitle(request.getTitle().trim());
        item.setContent(request.getContent().trim());
        item.setSortOrder(request.getSortOrder());
        item.setEnabled(request.getEnabled() == null || request.getEnabled());
        item.setCreatedAt(Instant.now());
        return toAdminResponse(displayItemRepository.save(item));
    }

    @Transactional("businessTransactionManager")
    public DeviceDisplayItemAdminResponse update(Long id, UpdateDeviceDisplayItemRequest request) {
        DeviceDisplayItem item = requireItem(id);
        item.setTitle(request.getTitle().trim());
        item.setContent(request.getContent().trim());
        item.setSortOrder(request.getSortOrder());
        item.setEnabled(request.getEnabled());
        return toAdminResponse(displayItemRepository.save(item));
    }

    @Transactional("businessTransactionManager")
    public void delete(Long id) {
        DeviceDisplayItem item = requireItem(id);
        displayItemRepository.delete(item);
    }

    private DeviceDisplayItem requireItem(Long id) {
        return displayItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "设备展示条目不存在"));
    }

    private DeviceDisplayItemAdminResponse toAdminResponse(DeviceDisplayItem item) {
        String deviceName = screenDeviceLookupService.getDeviceName(item.getScreenDeviceId());
        return new DeviceDisplayItemAdminResponse(
                item.getId(),
                item.getScreenDeviceId(),
                deviceName,
                item.getTitle(),
                item.getContent(),
                item.getSortOrder(),
                item.getEnabled(),
                item.getCreatedAt());
    }

    private DeviceDisplayItemResponse toLearnerResponse(DeviceDisplayItem item) {
        return new DeviceDisplayItemResponse(
                item.getId(),
                item.getTitle(),
                item.getContent(),
                item.getSortOrder());
    }
}
