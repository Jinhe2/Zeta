package com.zeta.business.service;

import com.zeta.business.entities.pressboardselection.*;
import com.zeta.business.entities.settinglist.SettingListScopeType;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PressboardSelectionService {
  private final LogicPressboardSelectionRepository repository;
  private final SettingListTargetService targetService;

  public PressboardSelectionService(LogicPressboardSelectionRepository repository, SettingListTargetService targetService) {
    this.repository = repository;
    this.targetService = targetService;
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public Set<String> selected(PressboardKind kind, SettingListScopeType type, Long id) {
    return repository.findByPressboardKindAndScopeTypeAndScopeId(kind, type, id).stream()
        .map(LogicPressboardSelection::getPressboardRef).collect(Collectors.toSet());
  }

  @Transactional("businessTransactionManager")
  public void replace(PressboardKind kind, SettingListScopeType type, Long id, List<String> refs, Set<String> available) {
    if (type != SettingListScopeType.LOGIC_DIAGRAM && type != SettingListScopeType.LOGIC_GROUP) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "校验项目选择仅支持基础逻辑和组合逻辑");
    }
    if (refs == null || refs.stream().anyMatch(ref -> !available.contains(ref))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "校验项目必须属于当前装置压板清单，请刷新后重试");
    }
    List<LogicPressboardSelection> rows = refs.stream().distinct().map(ref -> {
      LogicPressboardSelection row = new LogicPressboardSelection();
      row.setPressboardKind(kind); row.setScopeType(type); row.setScopeId(id); row.setPressboardRef(ref);
      return row;
    }).collect(Collectors.toList());
    repository.deleteByPressboardKindAndScopeTypeAndScopeId(kind, type, id);
    repository.flush();
    repository.saveAll(rows);
    repository.flush();
  }

  @Transactional("businessTransactionManager")
  public void removeMissing(PressboardKind kind, Long deviceId, Set<String> available) {
    targetService.logicScopeIdsForDevice(deviceId).forEach((type, ids) -> {
      if (!ids.isEmpty()) {
        List<LogicPressboardSelection> removed = repository.findByPressboardKindAndScopeTypeAndScopeIdIn(kind, type, ids)
            .stream().filter(row -> !available.contains(row.getPressboardRef())).collect(Collectors.toList());
        repository.deleteAll(removed);
      }
    });
  }

  public static void requireDeviceScope(SettingListScopeType type) {
    if (type != SettingListScopeType.IED_DEVICE) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "逻辑层仅支持查看装置压板基准和勾选校验项目，请在装置层维护压板清单");
    }
  }
}
