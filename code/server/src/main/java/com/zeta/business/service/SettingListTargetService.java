package com.zeta.business.service;

import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.screen.ieddevice.Device;
import com.zeta.screen.ieddevice.DeviceRepository;
import com.zeta.screen.logicdiagram.ProtectionLogic;
import com.zeta.screen.logicdiagram.ProtectionLogicRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SettingListTargetService {
  private final DeviceRepository deviceRepository;
  private final ProtectionLogicRepository logicRepository;

  public SettingListTargetService(
      DeviceRepository deviceRepository, ProtectionLogicRepository logicRepository) {
    this.deviceRepository = deviceRepository;
    this.logicRepository = logicRepository;
  }

  @Transactional(value = "screenTransactionManager", readOnly = true)
  public Target require(SettingListScopeType scopeType, Long scopeId) {
    if (scopeType == SettingListScopeType.IED_DEVICE) {
      Device device =
          deviceRepository
              .findById(scopeId)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "装置不存在"));
      return new Target(scopeType, scopeId, device.getName(), device.getId(), device.getIedName());
    }
    ProtectionLogic logic =
        logicRepository
            .findById(scopeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "逻辑框图不存在"));
    Device device = logic.getDevice();
    return new Target(scopeType, scopeId, logic.getTitle(), device.getId(), device.getIedName());
  }

  @Getter
  @AllArgsConstructor
  public static class Target {
    private SettingListScopeType scopeType;
    private Long scopeId;
    private String scopeName;
    private Long iedDeviceId;
    private String iedName;
  }
}
