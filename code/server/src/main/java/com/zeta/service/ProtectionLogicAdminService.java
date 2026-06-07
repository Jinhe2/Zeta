package com.zeta.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeta.domain.Device;
import com.zeta.domain.ProtectionLogic;
import com.zeta.repository.DeviceRepository;
import com.zeta.repository.ProtectionLogicRepository;
import com.zeta.web.dto.CreateProtectionLogicRequest;
import com.zeta.web.dto.ProtectionLogicAdminResponse;
import com.zeta.web.dto.ProtectionLogicConfigResponse;
import com.zeta.web.dto.UpdateProtectionLogicConfigRequest;
import com.zeta.web.dto.UpdateProtectionLogicRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProtectionLogicAdminService {

    private static final String EMPTY_CONFIG_JSON =
            "{\"version\":\"1.0\",\"name\":\"\",\"description\":\"\",\"inputs\":[],\"gates\":[],\"timers\":[],\"outputs\":[],\"settings\":[]}";

    private final DeviceRepository deviceRepository;
    private final ProtectionLogicRepository protectionLogicRepository;
    private final ProtectionLogicConfigValidator configValidator;
    private final ObjectMapper objectMapper;

    public ProtectionLogicAdminService(
            DeviceRepository deviceRepository,
            ProtectionLogicRepository protectionLogicRepository,
            ProtectionLogicConfigValidator configValidator,
            ObjectMapper objectMapper) {
        this.deviceRepository = deviceRepository;
        this.protectionLogicRepository = protectionLogicRepository;
        this.configValidator = configValidator;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ProtectionLogicAdminResponse> listByDevice(Long deviceId) {
        requireDevice(deviceId);
        return protectionLogicRepository.findByDeviceIdOrderBySortOrderAscIdAsc(deviceId).stream()
                .map(this::toAdminResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProtectionLogicAdminResponse getById(Long id) {
        return toAdminResponse(requireLogic(id));
    }

    @Transactional
    public ProtectionLogicAdminResponse create(Long deviceId, CreateProtectionLogicRequest request) {
        Device device = requireDevice(deviceId);
        String code = normalizeCode(request.getCode());
        if (protectionLogicRepository.existsByCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "保护逻辑编码已存在");
        }
        ProtectionLogic logic = new ProtectionLogic();
        logic.setDevice(device);
        logic.setCode(code);
        logic.setTitle(request.getTitle().trim());
        logic.setDescription(trimToNull(request.getDescription()));
        logic.setCategory(request.getCategory().trim());
        logic.setSortOrder(request.getSortOrder());
        logic.setConfigJson(EMPTY_CONFIG_JSON);
        logic.setEnabled(request.getEnabled() == null || request.getEnabled());
        logic.setCreatedAt(Instant.now());
        return toAdminResponse(protectionLogicRepository.save(logic));
    }

    @Transactional
    public ProtectionLogicAdminResponse update(Long id, UpdateProtectionLogicRequest request) {
        ProtectionLogic logic = requireLogic(id);
        logic.setTitle(request.getTitle().trim());
        logic.setDescription(trimToNull(request.getDescription()));
        logic.setCategory(request.getCategory().trim());
        logic.setSortOrder(request.getSortOrder());
        logic.setEnabled(request.getEnabled());
        return toAdminResponse(protectionLogicRepository.save(logic));
    }

    @Transactional
    public void delete(Long id) {
        ProtectionLogic logic = requireLogic(id);
        protectionLogicRepository.delete(logic);
    }

    @Transactional(readOnly = true)
    public ProtectionLogicConfigResponse getConfig(Long id) {
        ProtectionLogic logic = requireLogic(id);
        return toConfigResponse(logic);
    }

    @Transactional
    public ProtectionLogicConfigResponse updateConfig(Long id, UpdateProtectionLogicConfigRequest request) {
        ProtectionLogic logic = requireLogic(id);
        String normalized = configValidator.validateAndNormalize(request.getConfigJson());
        logic.setConfigJson(normalized);
        return toConfigResponse(protectionLogicRepository.save(logic));
    }

    private ProtectionLogicConfigResponse toConfigResponse(ProtectionLogic logic) {
        Device device = logic.getDevice();
        String prettyJson = logic.getConfigJson();
        try {
            Object parsed = objectMapper.readValue(logic.getConfigJson(), Object.class);
            prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
        } catch (Exception ignored) {
            // 返回原始文本，便于管理员修复非法 JSON
        }
        return new ProtectionLogicConfigResponse(
                logic.getId(),
                device != null ? device.getId() : null,
                logic.getCode(),
                logic.getTitle(),
                prettyJson);
    }

    private Device requireDevice(Long id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "设备不存在"));
    }

    private ProtectionLogic requireLogic(Long id) {
        return protectionLogicRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "保护逻辑不存在"));
    }

    private String normalizeCode(String code) {
        if (code == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入逻辑编码");
        }
        String normalized = code.trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入逻辑编码");
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

    private ProtectionLogicAdminResponse toAdminResponse(ProtectionLogic logic) {
        Device device = logic.getDevice();
        return new ProtectionLogicAdminResponse(
                logic.getId(),
                device != null ? device.getId() : null,
                device != null ? device.getName() : null,
                logic.getCode(),
                logic.getTitle(),
                logic.getDescription(),
                logic.getCategory(),
                logic.getSortOrder(),
                logic.getEnabled(),
                logic.getCreatedAt());
    }
}
