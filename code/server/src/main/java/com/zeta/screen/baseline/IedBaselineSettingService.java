package com.zeta.screen.baseline;

import com.zeta.business.entities.cognitiondevice.CognitionDevice;
import com.zeta.business.entities.cognitiondevice.CognitionDeviceType;
import com.zeta.business.entities.settinglist.SettingListItemRepository;
import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.service.CognitionDeviceService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IedBaselineSettingService {
    private final CognitionDeviceService cognitionDeviceService;
    private final SettingListItemRepository settingListItemRepository;

    public IedBaselineSettingService(CognitionDeviceService cognitionDeviceService,
                                     SettingListItemRepository settingListItemRepository) {
        this.cognitionDeviceService = cognitionDeviceService;
        this.settingListItemRepository = settingListItemRepository;
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public List<IedBaselineSettingResponse> listForCognitionDevice(Long cognitionDeviceId) {
        CognitionDevice device = requireIedOperationDevice(cognitionDeviceId);
        return settingListItemRepository
                .findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(
                        SettingListScopeType.IED_DEVICE, device.getScreenDeviceId())
                .stream()
                .filter(item -> !Boolean.FALSE.equals(item.getCompareEnabled()))
                .map(item -> new IedBaselineSettingResponse(item.getSettingName(), item.getBaselineValue()))
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
