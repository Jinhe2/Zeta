package com.zeta.business.service;

import com.zeta.business.entities.experiment.ExperimentPrecheckResponse;
import com.zeta.business.entities.settinglist.dto.SettingCheckResponse;
import com.zeta.business.entities.softpressboardlist.SoftPressboardDtos.CheckResponse;
import com.zeta.business.service.SettingListService.ResolvedSettingList;
import com.zeta.business.service.SoftPressboardListService.ResolvedSoftPressboardList;
import com.zeta.integration.mms.MmsSettingClient;
import java.util.Collections;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ExperimentPrecheckService {
  private final SettingListService settingListService;
  private final SoftPressboardListService softPressboardListService;
  private final SettingComparisonService settingComparisonService;
  private final SoftPressboardComparisonService softPressboardComparisonService;
  private final MmsSettingClient mmsSettingClient;

  public ExperimentPrecheckService(
      SettingListService settingListService,
      SoftPressboardListService softPressboardListService,
      SettingComparisonService settingComparisonService,
      SoftPressboardComparisonService softPressboardComparisonService,
      MmsSettingClient mmsSettingClient) {
    this.settingListService = settingListService;
    this.softPressboardListService = softPressboardListService;
    this.settingComparisonService = settingComparisonService;
    this.softPressboardComparisonService = softPressboardComparisonService;
    this.mmsSettingClient = mmsSettingClient;
  }

  public ExperimentPrecheckResponse check(Long logicDiagramId) {
    ResolvedSettingList settings = settingListService.resolveForLogic(logicDiagramId);
    ResolvedSoftPressboardList pressboards =
        softPressboardListService.resolveForLogic(logicDiagramId);
    boolean checkSettings = settingComparisonService.hasEnabledItems(settings);
    boolean checkPressboards = softPressboardComparisonService.hasEnabledItems(pressboards);
    Map<String, Double> actualSettingValues = checkSettings
        ? mmsSettingClient.summon(settings.getTarget().getIedName()).getValues()
        : Collections.emptyMap();
    Map<String, Double> actualPressboardValues = checkPressboards
        ? softPressboardComparisonService.summonCurrentValues(
            pressboards.getTarget().getIedDeviceId())
        : Collections.emptyMap();
    SettingCheckResponse settingCheck = checkSettings
        ? settingComparisonService.compareResolved(settings, actualSettingValues)
        : settingComparisonService.skipped(settings);
    CheckResponse pressboardCheck = checkPressboards
        ? softPressboardComparisonService.compareResolved(pressboards, actualPressboardValues)
        : softPressboardComparisonService.skipped(pressboards);
    String status;
    if ("MISMATCH".equals(settingCheck.getStatus())
        || "MISMATCH".equals(pressboardCheck.getStatus())) {
      status = "MISMATCH";
    } else if ("SKIPPED".equals(settingCheck.getStatus())
        && "SKIPPED".equals(pressboardCheck.getStatus())) {
      status = "SKIPPED";
    } else {
      status = "MATCHED";
    }
    return new ExperimentPrecheckResponse(status, settingCheck, pressboardCheck);
  }
}
