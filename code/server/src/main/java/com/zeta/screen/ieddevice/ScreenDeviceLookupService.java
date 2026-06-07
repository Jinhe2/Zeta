package com.zeta.screen.ieddevice;

import com.zeta.screen.ieddevice.Device;
import com.zeta.screen.ieddevice.DeviceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 屏柜库设备只读查询，供业务层校验跨库引用。
 */
@Service
public class ScreenDeviceLookupService {

    private final DeviceRepository deviceRepository;

    public ScreenDeviceLookupService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Transactional(value = "screenTransactionManager", readOnly = true)
    public Device requireDevice(Long screenDeviceId) {
        return deviceRepository.findById(screenDeviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "设备不存在"));
    }

    @Transactional(value = "screenTransactionManager", readOnly = true)
    public String getDeviceName(Long screenDeviceId) {
        return requireDevice(screenDeviceId).getName();
    }
}
