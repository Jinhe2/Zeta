package com.zeta.screen.baseline;

import com.zeta.business.cognitiondevice.CognitionDevice;
import com.zeta.business.cognitiondevice.CognitionDeviceService;
import com.zeta.business.cognitiondevice.CognitionDeviceType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class IedBaselineSettingService {
    private final CognitionDeviceService cognitionDeviceService;
    private final IedBaselineSettingItemRepository repository;

    public IedBaselineSettingService(CognitionDeviceService cognitionDeviceService,
                                     IedBaselineSettingItemRepository repository) {
        this.cognitionDeviceService = cognitionDeviceService;
        this.repository = repository;
    }

    @Transactional(value = "screenTransactionManager", readOnly = true)
    public List<IedBaselineSettingResponse> listForCognitionDevice(Long cognitionDeviceId) {
        CognitionDevice device = requireIedOperationDevice(cognitionDeviceId);
        return repository.findByIedDeviceIdOrderBySortOrderAsc(device.getScreenDeviceId()).stream()
                .map(item -> new IedBaselineSettingResponse(item.getSettingDescription(), item.getBaselineValue()))
                .collect(Collectors.toList());
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public Long requireIedDeviceId(Long cognitionDeviceId) {
        return requireIedOperationDevice(cognitionDeviceId).getScreenDeviceId();
    }

    private CognitionDevice requireIedOperationDevice(Long cognitionDeviceId) {
        CognitionDevice device = cognitionDeviceService.requireDevice(cognitionDeviceId);
        if (device.getDeviceType() != CognitionDeviceType.IED_OPERATION || device.getScreenDeviceId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该认知子设备不支持定值整定");
        }
        return device;
    }
}
