package com.zeta.business.service;

import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.entities.softpressboardlist.SoftPressboardDtos.*;
import com.zeta.business.entities.softpressboardlist.SoftPressboardListItem;
import com.zeta.business.service.SoftPressboardListService.ResolvedSoftPressboardList;
import com.zeta.integration.monitor.IedSoftPressboardStatusClient;
import com.zeta.screen.softpressboard.IedSoftPressboardItem;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SoftPressboardComparisonService {
  private final SettingListTargetService targetService;
  private final SoftPressboardCatalogService catalogService;
  private final IedSoftPressboardStatusClient statusClient;

  public SoftPressboardComparisonService(
      SettingListTargetService targetService,
      SoftPressboardCatalogService catalogService, IedSoftPressboardStatusClient statusClient) {
    this.targetService = targetService;
    this.catalogService = catalogService;
    this.statusClient = statusClient;
  }

  public SummonResponse summonPreview(SettingListScopeType scopeType, Long scopeId) {
    SettingListTargetService.Target target = targetService.require(scopeType, scopeId);
    List<IedSoftPressboardItem> catalog = catalogService.list(target.getIedDeviceId());
    IedSoftPressboardStatusClient.StatusResult summon =
        statusClient.summon(target.getIedDeviceId());
    List<ItemResponse> rows = new ArrayList<>();
    int order = 0;
    for (IedSoftPressboardItem item : catalog) {
      Double actual = SettingComparisonService.findActual(
          target.getIedName(), item.getPressboardRef(), summon.getValues());
      if (actual == null) continue;
      Boolean state = state(actual);
      if (state == null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY,
            "装置返回的软压板状态不是 0 或 1：" + item.getPressboardName());
      }
      rows.add(new ItemResponse(
          item.getPressboardRef(), item.getPressboardName(), state, true, order++));
    }
    return new SummonResponse(summon.getCount(), catalog.size(), rows.size(), rows);
  }

  public boolean hasEnabledItems(ResolvedSoftPressboardList resolved) {
    for (SoftPressboardListItem item : resolved.getItems()) {
      if (!Boolean.FALSE.equals(item.getCompareEnabled())) return true;
    }
    return false;
  }

  public Map<String, Double> summonCurrentValues(Long iedDeviceId) {
    return statusClient.summon(iedDeviceId).getValues();
  }

  public CheckResponse skipped(ResolvedSoftPressboardList resolved) {
    return new CheckResponse(
        "SKIPPED",
        resolved.getItems().isEmpty() ? null : resolved.getEffectiveScopeType(),
        resolved.getItems().isEmpty() ? null : resolved.getEffectiveScopeId(),
        0, 0, 0, 0, Collections.emptyList());
  }

  public CheckResponse compareResolved(
      ResolvedSoftPressboardList resolved, Map<String, Double> actualValues) {
    List<CheckItemResponse> rows = new ArrayList<>();
    int equal = 0;
    int mismatch = 0;
    int missing = 0;
    for (SoftPressboardListItem item : resolved.getItems()) {
      if (Boolean.FALSE.equals(item.getCompareEnabled())) continue;
      Double raw = SettingComparisonService.findActual(
          resolved.getTarget().getIedName(), item.getPressboardRef(), actualValues);
      Boolean actual = raw == null ? null : state(raw);
      boolean matched = raw != null;
      boolean same = actual != null && actual.equals(item.getBaselineValue());
      if (!matched) missing++;
      else if (same) equal++;
      else mismatch++;
      rows.add(new CheckItemResponse(
          item.getPressboardRef(), item.getPressboardName(), display(item.getBaselineValue()),
          raw == null ? null : actual == null ? "非法值（" + format(raw) + "）" : display(actual),
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

  private String format(double value) {
    return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
  }
}
