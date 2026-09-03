package com.zeta.business.service;

import com.zeta.business.entities.hardpressboardlist.HardPressboardDtos.*;
import com.zeta.business.entities.hardpressboardlist.HardPressboardListItem;
import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.service.HardPressboardListService.ResolvedHardPressboardList;
import com.zeta.integration.monitor.HardPressboardStatusClient;
import com.zeta.screen.hardpressboard.HardPressboard;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HardPressboardComparisonService {
  private final SettingListTargetService targetService;
  private final HardPressboardCatalogService catalogService;
  private final HardPressboardStatusClient statusClient;

  public HardPressboardComparisonService(
      SettingListTargetService targetService,
      HardPressboardCatalogService catalogService, HardPressboardStatusClient statusClient) {
    this.targetService = targetService;
    this.catalogService = catalogService;
    this.statusClient = statusClient;
  }

  public SummonResponse summonPreview(SettingListScopeType scopeType, Long scopeId) {
    PressboardSelectionService.requireDeviceScope(scopeType);
    SettingListTargetService.Target target = targetService.require(scopeType, scopeId);
    List<HardPressboard> catalog = catalogService.list(target.getCabinetId());
    HardPressboardStatusClient.StatusResult summon =
        statusClient.summon(target.getCabinetId());
    List<ItemResponse> rows = new ArrayList<>();
    int order = 0;
    for (HardPressboard item : catalog) {
      Double actual = summon.getValues().get(String.valueOf(item.getId()));
      if (actual == null) continue;
      Boolean state = state(actual);
      if (state == null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY,
            "装置返回的硬压板状态不是 0 或 1：" + item.getName());
      }
      rows.add(new ItemResponse(
          String.valueOf(item.getId()), item.getName(), state, true, order++));
    }
    return new SummonResponse(summon.getCount(), catalog.size(), rows.size(), rows);
  }

  public boolean hasEnabledItems(ResolvedHardPressboardList resolved) {
    for (HardPressboardListItem item : resolved.getItems()) {
      if (!Boolean.FALSE.equals(item.getCompareEnabled())) return true;
    }
    return false;
  }

  public Map<String, Double> summonCurrentValues(Long cabinetId) {
    return statusClient.summon(cabinetId).getValues();
  }

  public CheckResponse skipped(ResolvedHardPressboardList resolved) {
    return new CheckResponse(
        "SKIPPED",
        resolved.getItems().isEmpty() ? null : resolved.getEffectiveScopeType(),
        resolved.getItems().isEmpty() ? null : resolved.getEffectiveScopeId(),
        0, 0, 0, 0, Collections.emptyList());
  }

  public CheckResponse compareResolved(
      ResolvedHardPressboardList resolved, Map<String, Double> actualValues) {
    List<CheckItemResponse> rows = new ArrayList<>();
    int equal = 0;
    int mismatch = 0;
    int missing = 0;
    for (HardPressboardListItem item : resolved.getItems()) {
      if (Boolean.FALSE.equals(item.getCompareEnabled())) continue;
      Double raw = actualValues.get(item.getPressboardRef());
      Boolean actual = raw == null ? null : state(raw);
      boolean matched = raw != null;
      boolean same = actual != null && actual.equals(item.getBaselineValue());
      if (!matched) missing++;
      else if (same) equal++;
      else mismatch++;
      rows.add(new CheckItemResponse(
          item.getPressboardRef(), item.getPressboardName(), display(item.getBaselineValue()),
          raw == null ? null : actual == null ? "非法值（" + raw + "）" : display(actual),
          matched, same));
    }
    if (rows.isEmpty()) return skipped(resolved);
    return new CheckResponse(
        mismatch == 0 && missing == 0 ? "MATCHED" : "MISMATCH",
        resolved.getEffectiveScopeType(), resolved.getEffectiveScopeId(), rows.size(), equal,
        mismatch, missing, rows);
  }

  private Boolean state(double value) {
    if (Double.compare(value, 0D) == 0) return false;
    if (Double.compare(value, 1D) == 0) return true;
    return null;
  }

  private String display(Boolean value) { return Boolean.TRUE.equals(value) ? "投入" : "退出"; }
}
