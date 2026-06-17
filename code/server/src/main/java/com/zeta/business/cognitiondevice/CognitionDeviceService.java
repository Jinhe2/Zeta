package com.zeta.business.cognitiondevice;

import com.zeta.business.cabinetdisplay.CabinetDisplayItem;
import com.zeta.business.cabinetdisplay.CabinetDisplayItemRepository;
import com.zeta.screen.ieddevice.ScreenDeviceLookupService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CognitionDeviceService {

    private final CabinetDisplayItemRepository cabinetDisplayItemRepository;
    private final CognitionDeviceRepository cognitionDeviceRepository;
    private final ScreenDeviceLookupService screenDeviceLookupService;

    public CognitionDeviceService(
            CabinetDisplayItemRepository cabinetDisplayItemRepository,
            CognitionDeviceRepository cognitionDeviceRepository,
            ScreenDeviceLookupService screenDeviceLookupService) {
        this.cabinetDisplayItemRepository = cabinetDisplayItemRepository;
        this.cognitionDeviceRepository = cognitionDeviceRepository;
        this.screenDeviceLookupService = screenDeviceLookupService;
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public List<CognitionDeviceAdminResponse> listByCabinetDisplayItem(Long cabinetDisplayItemId) {
        requireCabinetDisplayItem(cabinetDisplayItemId);
        return cognitionDeviceRepository.findByCabinetDisplayItemIdOrderBySortOrderAscIdAsc(cabinetDisplayItemId)
                .stream()
                .map(this::toAdminResponse)
                .collect(Collectors.toList());
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public List<CognitionDeviceResponse> listEnabledByCabinetDisplayItem(Long cabinetDisplayItemId) {
        requireCabinetDisplayItem(cabinetDisplayItemId);
        return cognitionDeviceRepository.findByCabinetDisplayItemIdOrderBySortOrderAscIdAsc(cabinetDisplayItemId)
                .stream()
                .filter(device -> Boolean.TRUE.equals(device.getEnabled()))
                .map(this::toLearnerResponse)
                .collect(Collectors.toList());
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public CognitionDeviceAdminResponse getAdmin(Long id) {
        return toAdminResponse(requireDevice(id));
    }

    @Transactional("businessTransactionManager")
    public CognitionDeviceAdminResponse create(Long cabinetDisplayItemId, CreateCognitionDeviceRequest request) {
        requireCabinetDisplayItem(cabinetDisplayItemId);
        validateRegion(request.getLeftPercent(), request.getTopPercent(),
                request.getWidthPercent(), request.getHeightPercent());
        validateDeviceType(request.getDeviceType(), request.getScreenDeviceId());

        CognitionDevice device = new CognitionDevice();
        device.setCabinetDisplayItemId(cabinetDisplayItemId);
        applyRequest(device, request);
        device.setCreatedAt(Instant.now());
        return toAdminResponse(cognitionDeviceRepository.save(device));
    }

    @Transactional("businessTransactionManager")
    public CognitionDeviceAdminResponse update(Long id, UpdateCognitionDeviceRequest request) {
        CognitionDevice device = requireDevice(id);
        validateRegion(request.getLeftPercent(), request.getTopPercent(),
                request.getWidthPercent(), request.getHeightPercent());
        validateDeviceType(request.getDeviceType(), request.getScreenDeviceId());
        applyRequest(device, request);
        return toAdminResponse(cognitionDeviceRepository.save(device));
    }

    @Transactional("businessTransactionManager")
    public void delete(Long id) {
        CognitionDevice device = requireDevice(id);
        cognitionDeviceRepository.delete(device);
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public CognitionDevice requireDevice(Long id) {
        return cognitionDeviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "认知设备不存在"));
    }

    private void applyRequest(CognitionDevice device, CreateCognitionDeviceRequest request) {
        device.setDeviceType(request.getDeviceType());
        device.setScreenDeviceId(request.getDeviceType() == CognitionDeviceType.IED
                ? request.getScreenDeviceId() : null);
        device.setTitle(request.getTitle().trim());
        device.setLeftPercent(request.getLeftPercent());
        device.setTopPercent(request.getTopPercent());
        device.setWidthPercent(request.getWidthPercent());
        device.setHeightPercent(request.getHeightPercent());
        device.setSortOrder(request.getSortOrder());
        device.setEnabled(request.getEnabled() == null || request.getEnabled());
    }

    private void applyRequest(CognitionDevice device, UpdateCognitionDeviceRequest request) {
        device.setDeviceType(request.getDeviceType());
        device.setScreenDeviceId(request.getDeviceType() == CognitionDeviceType.IED
                ? request.getScreenDeviceId() : null);
        device.setTitle(request.getTitle().trim());
        device.setLeftPercent(request.getLeftPercent());
        device.setTopPercent(request.getTopPercent());
        device.setWidthPercent(request.getWidthPercent());
        device.setHeightPercent(request.getHeightPercent());
        device.setSortOrder(request.getSortOrder());
        device.setEnabled(request.getEnabled());
    }

    private void validateDeviceType(CognitionDeviceType type, Long screenDeviceId) {
        if (type == CognitionDeviceType.IED) {
            if (screenDeviceId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IED 类型须选择屏柜库设备");
            }
            screenDeviceLookupService.requireDevice(screenDeviceId);
            return;
        }
        if (screenDeviceId != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅 IED 类型可关联屏柜库设备");
        }
    }

    private void validateRegion(Double left, Double top, Double width, Double height) {
        if (width <= 0 || height <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "区域宽高必须大于 0");
        }
        if (left + width > 100.01 || top + height > 100.01) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "区域超出图片范围");
        }
    }

    private CabinetDisplayItem requireCabinetDisplayItem(Long id) {
        return cabinetDisplayItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "屏柜认知条目不存在"));
    }

    private CognitionDeviceAdminResponse toAdminResponse(CognitionDevice device) {
        CabinetDisplayItem parent = requireCabinetDisplayItem(device.getCabinetDisplayItemId());
        String screenDeviceName = device.getScreenDeviceId() == null
                ? null
                : screenDeviceLookupService.getDeviceName(device.getScreenDeviceId());
        return new CognitionDeviceAdminResponse(
                device.getId(),
                device.getCabinetDisplayItemId(),
                parent.getTitle(),
                device.getDeviceType(),
                device.getScreenDeviceId(),
                screenDeviceName,
                device.getTitle(),
                device.getLeftPercent(),
                device.getTopPercent(),
                device.getWidthPercent(),
                device.getHeightPercent(),
                device.getSortOrder(),
                device.getEnabled(),
                device.getCreatedAt());
    }

    private CognitionDeviceResponse toLearnerResponse(CognitionDevice device) {
        return new CognitionDeviceResponse(
                device.getId(),
                device.getDeviceType(),
                device.getTitle(),
                device.getLeftPercent(),
                device.getTopPercent(),
                device.getWidthPercent(),
                device.getHeightPercent(),
                device.getSortOrder());
    }
}
