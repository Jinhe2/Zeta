package com.zeta.service;

import com.zeta.domain.Device;
import com.zeta.domain.DeviceCognitionItem;
import com.zeta.repository.DeviceCognitionItemRepository;
import com.zeta.repository.DeviceRepository;
import com.zeta.web.dto.CreateDeviceCognitionItemRequest;
import com.zeta.web.dto.DeviceCognitionItemAdminResponse;
import com.zeta.web.dto.DeviceCognitionItemResponse;
import com.zeta.web.dto.UpdateDeviceCognitionItemRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeviceCognitionItemService {

    private final DeviceRepository deviceRepository;
    private final DeviceCognitionItemRepository cognitionItemRepository;

    public DeviceCognitionItemService(
            DeviceRepository deviceRepository,
            DeviceCognitionItemRepository cognitionItemRepository) {
        this.deviceRepository = deviceRepository;
        this.cognitionItemRepository = cognitionItemRepository;
    }

    @Transactional(readOnly = true)
    public List<DeviceCognitionItemAdminResponse> listByDevice(Long deviceId) {
        requireDevice(deviceId);
        return cognitionItemRepository.findByDeviceIdOrderBySortOrderAscIdAsc(deviceId).stream()
                .map(this::toAdminResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DeviceCognitionItemResponse> listEnabledByDevice(Long deviceId) {
        requireDevice(deviceId);
        return cognitionItemRepository.findByDeviceIdOrderBySortOrderAscIdAsc(deviceId).stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .map(this::toLearnerResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeviceCognitionItemAdminResponse getById(Long id) {
        return toAdminResponse(requireItem(id));
    }

    @Transactional
    public DeviceCognitionItemAdminResponse create(Long deviceId, CreateDeviceCognitionItemRequest request) {
        Device device = requireDevice(deviceId);
        DeviceCognitionItem item = new DeviceCognitionItem();
        item.setDevice(device);
        item.setTitle(request.getTitle().trim());
        item.setContent(request.getContent().trim());
        item.setSortOrder(request.getSortOrder());
        item.setEnabled(request.getEnabled() == null || request.getEnabled());
        item.setCreatedAt(Instant.now());
        return toAdminResponse(cognitionItemRepository.save(item));
    }

    @Transactional
    public DeviceCognitionItemAdminResponse update(Long id, UpdateDeviceCognitionItemRequest request) {
        DeviceCognitionItem item = requireItem(id);
        item.setTitle(request.getTitle().trim());
        item.setContent(request.getContent().trim());
        item.setSortOrder(request.getSortOrder());
        item.setEnabled(request.getEnabled());
        return toAdminResponse(cognitionItemRepository.save(item));
    }

    @Transactional
    public void delete(Long id) {
        DeviceCognitionItem item = requireItem(id);
        cognitionItemRepository.delete(item);
    }

    private Device requireDevice(Long id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "设备不存在"));
    }

    private DeviceCognitionItem requireItem(Long id) {
        return cognitionItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "设备认知条目不存在"));
    }

    private DeviceCognitionItemAdminResponse toAdminResponse(DeviceCognitionItem item) {
        Device device = item.getDevice();
        return new DeviceCognitionItemAdminResponse(
                item.getId(),
                device != null ? device.getId() : null,
                device != null ? device.getName() : null,
                item.getTitle(),
                item.getContent(),
                item.getSortOrder(),
                item.getEnabled(),
                item.getCreatedAt());
    }

    private DeviceCognitionItemResponse toLearnerResponse(DeviceCognitionItem item) {
        return new DeviceCognitionItemResponse(
                item.getId(),
                item.getTitle(),
                item.getContent(),
                item.getSortOrder());
    }
}
