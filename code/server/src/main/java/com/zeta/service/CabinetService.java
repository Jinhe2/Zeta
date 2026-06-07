package com.zeta.service;

import com.zeta.domain.Cabinet;
import com.zeta.repository.CabinetRepository;
import com.zeta.repository.DeviceRepository;
import com.zeta.web.dto.CabinetAdminResponse;
import com.zeta.web.dto.CreateCabinetRequest;
import com.zeta.web.dto.UpdateCabinetRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CabinetService {

    private final CabinetRepository cabinetRepository;
    private final DeviceRepository deviceRepository;

    public CabinetService(CabinetRepository cabinetRepository, DeviceRepository deviceRepository) {
        this.cabinetRepository = cabinetRepository;
        this.deviceRepository = deviceRepository;
    }

    @Transactional(readOnly = true)
    public List<CabinetAdminResponse> listAll() {
        return cabinetRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(this::toAdminResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CabinetAdminResponse getById(Long id) {
        return toAdminResponse(requireCabinet(id));
    }

    @Transactional
    public CabinetAdminResponse create(CreateCabinetRequest request) {
        String code = normalizeCode(request.getCode());
        if (cabinetRepository.existsByCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "屏柜编码已存在");
        }
        Cabinet cabinet = new Cabinet();
        cabinet.setCode(code);
        cabinet.setName(request.getName().trim());
        cabinet.setDescription(trimToNull(request.getDescription()));
        cabinet.setSortOrder(request.getSortOrder());
        cabinet.setEnabled(request.getEnabled() == null || request.getEnabled());
        cabinet.setCreatedAt(Instant.now());
        return toAdminResponse(cabinetRepository.save(cabinet));
    }

    @Transactional
    public CabinetAdminResponse update(Long id, UpdateCabinetRequest request) {
        Cabinet cabinet = requireCabinet(id);
        cabinet.setName(request.getName().trim());
        cabinet.setDescription(trimToNull(request.getDescription()));
        cabinet.setSortOrder(request.getSortOrder());
        cabinet.setEnabled(request.getEnabled());
        return toAdminResponse(cabinetRepository.save(cabinet));
    }

    @Transactional
    public void delete(Long id) {
        Cabinet cabinet = requireCabinet(id);
        long deviceCount = deviceRepository.countByCabinetId(cabinet.getId());
        if (deviceCount > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "屏柜下存在设备，无法删除");
        }
        cabinetRepository.delete(cabinet);
    }

    private Cabinet requireCabinet(Long id) {
        return cabinetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "屏柜不存在"));
    }

    private String normalizeCode(String code) {
        if (code == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入屏柜编码");
        }
        String normalized = code.trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入屏柜编码");
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

    private CabinetAdminResponse toAdminResponse(Cabinet cabinet) {
        int deviceCount = (int) deviceRepository.countByCabinetId(cabinet.getId());
        return new CabinetAdminResponse(
                cabinet.getId(),
                cabinet.getCode(),
                cabinet.getName(),
                cabinet.getDescription(),
                cabinet.getSortOrder(),
                cabinet.getEnabled(),
                deviceCount,
                cabinet.getCreatedAt());
    }
}
