package com.zeta.service;

import com.zeta.domain.Cabinet;
import com.zeta.domain.Device;
import com.zeta.repository.CabinetRepository;
import com.zeta.repository.DeviceCognitionItemRepository;
import com.zeta.repository.DeviceRepository;
import com.zeta.repository.ProtectionLogicRepository;
import com.zeta.web.dto.CreateDeviceRequest;
import com.zeta.web.dto.DeviceAdminResponse;
import com.zeta.web.dto.UpdateDeviceRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeviceService {

    private final CabinetRepository cabinetRepository;
    private final DeviceRepository deviceRepository;
    private final ProtectionLogicRepository protectionLogicRepository;
    private final DeviceCognitionItemRepository cognitionItemRepository;

    public DeviceService(
            CabinetRepository cabinetRepository,
            DeviceRepository deviceRepository,
            ProtectionLogicRepository protectionLogicRepository,
            DeviceCognitionItemRepository cognitionItemRepository) {
        this.cabinetRepository = cabinetRepository;
        this.deviceRepository = deviceRepository;
        this.protectionLogicRepository = protectionLogicRepository;
        this.cognitionItemRepository = cognitionItemRepository;
    }

    @Transactional(readOnly = true)
    public List<DeviceAdminResponse> listByCabinet(Long cabinetId) {
        requireCabinet(cabinetId);
        return deviceRepository.findByCabinetIdOrderBySortOrderAscIdAsc(cabinetId).stream()
                .map(this::toAdminResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeviceAdminResponse getById(Long id) {
        return toAdminResponse(requireDevice(id));
    }

    @Transactional
    public DeviceAdminResponse create(Long cabinetId, CreateDeviceRequest request) {
        Cabinet cabinet = requireCabinet(cabinetId);
        String code = normalizeCode(request.getCode());
        if (deviceRepository.existsByCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "设备编码已存在");
        }
        Device device = new Device();
        device.setCabinet(cabinet);
        device.setCode(code);
        device.setName(request.getName().trim());
        device.setDescription(trimToNull(request.getDescription()));
        device.setSortOrder(request.getSortOrder());
        device.setEnabled(request.getEnabled() == null || request.getEnabled());
        device.setCreatedAt(Instant.now());
        return toAdminResponse(deviceRepository.save(device));
    }

    @Transactional
    public DeviceAdminResponse update(Long id, UpdateDeviceRequest request) {
        Device device = requireDevice(id);
        device.setName(request.getName().trim());
        device.setDescription(trimToNull(request.getDescription()));
        device.setSortOrder(request.getSortOrder());
        device.setEnabled(request.getEnabled());
        return toAdminResponse(deviceRepository.save(device));
    }

    @Transactional
    public void delete(Long id) {
        Device device = requireDevice(id);
        long logicCount = protectionLogicRepository.countByDeviceId(device.getId());
        if (logicCount > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "设备下存在保护逻辑，无法删除");
        }
        deviceRepository.delete(device);
    }

    private Cabinet requireCabinet(Long id) {
        return cabinetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "屏柜不存在"));
    }

    private Device requireDevice(Long id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "设备不存在"));
    }

    private String normalizeCode(String code) {
        if (code == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入设备编码");
        }
        String normalized = code.trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入设备编码");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private DeviceAdminResponse toAdminResponse(Device device) {
        int logicCount = (int) protectionLogicRepository.countByDeviceId(device.getId());
        int cognitionCount = (int) cognitionItemRepository.countByDeviceId(device.getId());
        Cabinet cabinet = device.getCabinet();
        return new DeviceAdminResponse(
                device.getId(),
                cabinet.getId(),
                cabinet.getName(),
                device.getCode(),
                device.getName(),
                device.getDescription(),
                device.getSortOrder(),
                device.getEnabled(),
                logicCount,
                cognitionCount,
                device.getCreatedAt());
    }
}
