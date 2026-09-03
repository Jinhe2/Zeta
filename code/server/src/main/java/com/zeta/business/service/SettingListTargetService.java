package com.zeta.business.service;

import com.zeta.business.entities.logicgroup.LogicGroup;
import com.zeta.business.entities.logicgroup.LogicGroupRepository;
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
  private final LogicGroupRepository logicGroupRepository;

  public SettingListTargetService(
      DeviceRepository deviceRepository,
      ProtectionLogicRepository logicRepository,
      LogicGroupRepository logicGroupRepository) {
    this.deviceRepository = deviceRepository;
    this.logicRepository = logicRepository;
    this.logicGroupRepository = logicGroupRepository;
  }

  @Transactional(value = "screenTransactionManager", readOnly = true)
  public Target require(SettingListScopeType scopeType, Long scopeId) {
    if (scopeType == SettingListScopeType.IED_DEVICE) {
      Device device =
          deviceRepository
              .findById(scopeId)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "装置不存在"));
      return new Target(scopeType, scopeId, device.getName(), device.getId(), device.getIedName(),
          device.getCabinet().getId());
    }
    if (scopeType == SettingListScopeType.LOGIC_GROUP) {
      LogicGroup group =
          logicGroupRepository
              .findById(scopeId)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "组合逻辑不存在"));
      Device device =
          deviceRepository
              .findById(group.getIedDeviceId())
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "装置不存在"));
      return new Target(scopeType, scopeId, group.getName(), device.getId(), device.getIedName(),
          device.getCabinet().getId());
    }
    ProtectionLogic logic =
        logicRepository
            .findById(scopeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "逻辑框图不存在"));
    Device device = logic.getDevice();
    return new Target(scopeType, scopeId, logic.getTitle(), device.getId(), device.getIedName(),
        device.getCabinet().getId());
  }

  @Transactional(value = "screenTransactionManager", readOnly = true)
  public java.util.Map<SettingListScopeType, java.util.List<Long>> logicScopeIdsForDevice(Long deviceId) {
    java.util.Map<SettingListScopeType, java.util.List<Long>> result = new java.util.EnumMap<>(SettingListScopeType.class);
    result.put(SettingListScopeType.LOGIC_DIAGRAM, logicRepository.findByDeviceIdOrderByIdAsc(deviceId)
        .stream().map(ProtectionLogic::getId).collect(java.util.stream.Collectors.toList()));
    result.put(SettingListScopeType.LOGIC_GROUP, logicGroupRepository.findByIedDeviceIdOrderBySortOrderAscIdAsc(deviceId)
        .stream().map(LogicGroup::getId).collect(java.util.stream.Collectors.toList()));
    return result;
  }

  @Getter
  @AllArgsConstructor
  public static class Target {
    private SettingListScopeType scopeType;
    private Long scopeId;
    private String scopeName;
    private Long iedDeviceId;
    private String iedName;
    private Long cabinetId;
  }
}
