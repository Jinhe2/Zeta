package com.zeta.business.service;

import com.zeta.business.entities.settinglist.SettingListItem;
import com.zeta.business.entities.settinglist.dto.*;
import com.zeta.business.service.SettingListService.ResolvedSettingList;
import com.zeta.integration.mms.MmsSettingClient;
import com.zeta.integration.mms.MmsSettingClient.SummonResult;
import com.zeta.screen.iedsetting.IedSettingItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class SettingComparisonService {
  private static final double EPSILON = 1e-6;

  private final SettingListService settingListService;
  private final SettingListTargetService targetService;
  private final SettingCatalogService catalogService;
  private final MmsSettingClient mmsSettingClient;

  public SettingComparisonService(
      SettingListService settingListService,
      SettingListTargetService targetService,
      SettingCatalogService catalogService,
      MmsSettingClient mmsSettingClient) {
    this.settingListService = settingListService;
    this.targetService = targetService;
    this.catalogService = catalogService;
    this.mmsSettingClient = mmsSettingClient;
  }

  public SettingCheckResponse checkForLogic(Long logicDiagramId) {
    ResolvedSettingList resolved = settingListService.resolveForLogic(logicDiagramId);
    if (resolved.getItems().isEmpty()) {
      return new SettingCheckResponse("SKIPPED", null, null, 0, 0, 0, 0, Collections.emptyList());
    }
    List<SettingListItem> comparedItems = new ArrayList<>();
    for (SettingListItem item : resolved.getItems()) {
      if (!Boolean.FALSE.equals(item.getCompareEnabled())) comparedItems.add(item);
    }
    if (comparedItems.isEmpty()) {
      return new SettingCheckResponse(
          "SKIPPED",
          resolved.getEffectiveScopeType(),
          resolved.getEffectiveScopeId(),
          0,
          0,
          0,
          0,
          Collections.emptyList());
    }
    SummonResult summon = mmsSettingClient.summon(resolved.getTarget().getIedName());
    List<SettingCheckItemResponse> rows = new ArrayList<>();
    int equal = 0;
    int mismatch = 0;
    int missing = 0;
    for (SettingListItem item : comparedItems) {
      Double rawActual = findActual(resolved.getTarget().getIedName(), item.getSettingRef(), summon.getValues());
      boolean isTime = item.getSettingRef().contains("Tmms");
      Double actual = rawActual == null ? null : normalizeActual(rawActual, isTime);
      double baseline = normalizeBaseline(Double.parseDouble(item.getBaselineValue()), isTime);
      boolean matched = actual != null;
      boolean same = matched && Math.abs(baseline - actual) <= EPSILON;
      if (!matched) missing++;
      else if (same) equal++;
      else mismatch++;
      rows.add(
          new SettingCheckItemResponse(
              item.getSettingRef(),
              item.getSettingName(),
              item.getValueType(),
              format(baseline),
              actual == null ? null : format(actual),
              isTime ? "s" : null,
              matched,
              same));
    }
    String status = mismatch == 0 && missing == 0 ? "MATCHED" : "MISMATCH";
    return new SettingCheckResponse(
        status,
        resolved.getEffectiveScopeType(),
        resolved.getEffectiveScopeId(),
        rows.size(),
        equal,
        mismatch,
        missing,
        rows);
  }

  public SettingSummonResponse summonPreview(
      com.zeta.business.entities.settinglist.SettingListScopeType scopeType, Long scopeId) {
    SettingListTargetService.Target target = targetService.require(scopeType, scopeId);
    List<IedSettingItem> catalog = catalogService.list(target.getIedDeviceId());
    SummonResult summon = mmsSettingClient.summon(target.getIedName());
    List<SettingListItemResponse> rows = new ArrayList<>();
    int order = 0;
    for (IedSettingItem item : catalog) {
      Double actual = findActual(target.getIedName(), item.getSettingRef(), summon.getValues());
      if (actual == null) continue;
      if (item.getSettingRef().contains("Tmms")) actual = normalizeActual(actual, true);
      rows.add(
          new SettingListItemResponse(
              item.getSettingRef(),
              "SG",
              item.getSettingName(),
              item.getValueType(),
              true,
              format(actual),
              order++));
    }
    return new SettingSummonResponse(summon.getCount(), catalog.size(), rows.size(), rows);
  }

  static Double findActual(String iedName, String reference, Map<String, Double> actualValues) {
    LinkedHashSet<String> candidates = new LinkedHashSet<>();
    candidates.add(reference);
    String prefix = iedName + "/";
    if (reference.startsWith(prefix)) {
      String withoutPrefix = reference.substring(prefix.length());
      candidates.add(withoutPrefix);
      candidates.add(iedName + withoutPrefix);
    } else if (reference.startsWith(iedName)
        && reference.length() > iedName.length()
        && reference.charAt(iedName.length()) != '/') {
      candidates.add(iedName + "/" + reference.substring(iedName.length()));
    } else {
      candidates.add(iedName + reference);
    }
    for (String candidate : candidates) {
      if (actualValues.containsKey(candidate)) return actualValues.get(candidate);
    }
    return null;
  }

  private static double normalizeActual(double value, boolean time) {
    return time ? roundTwo(value / 1000D) : value;
  }

  private static double normalizeBaseline(double value, boolean time) {
    return time ? roundTwo(value) : value;
  }

  private static double roundTwo(double value) {
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
  }

  private static String format(double value) {
    return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
  }
}
