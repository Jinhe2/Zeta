package com.zeta.business.service;

import com.zeta.business.entities.experiment.ExperimentPrecheckResponse;
import com.zeta.business.entities.hardpressboardlist.HardPressboardDtos.CheckResponse;
import com.zeta.business.entities.settinglist.dto.SettingCheckResponse;
import com.zeta.business.entities.softpressboardlist.SoftPressboardDtos;
import com.zeta.business.entities.wiringrequirement.WiringRequirementConfig;
import com.zeta.business.entities.wiringrequirement.WiringRequirementDtos;
import com.zeta.business.entities.wiringrequirement.WiringRequirementGroup;
import com.zeta.business.service.HardPressboardListService.ResolvedHardPressboardList;
import com.zeta.business.service.SettingListService.ResolvedSettingList;
import com.zeta.business.service.SoftPressboardListService.ResolvedSoftPressboardList;
import com.zeta.business.service.WiringRequirementService.ResolvedWiringRequirement;
import com.zeta.integration.mms.MmsSettingClient;
import com.zeta.integration.monitor.TerminalStatusClient.TerminalWiringState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ExperimentPrecheckService {
  private final SettingListService settingListService;
  private final SoftPressboardListService softPressboardListService;
  private final HardPressboardListService hardPressboardListService;
  private final WiringRequirementService wiringRequirementService;
  private final SettingComparisonService settingComparisonService;
  private final SoftPressboardComparisonService softPressboardComparisonService;
  private final HardPressboardComparisonService hardPressboardComparisonService;
  private final WiringComparisonService wiringComparisonService;
  private final MmsSettingClient mmsSettingClient;

  public ExperimentPrecheckService(
      SettingListService settingListService,
      SoftPressboardListService softPressboardListService,
      HardPressboardListService hardPressboardListService,
      WiringRequirementService wiringRequirementService,
      SettingComparisonService settingComparisonService,
      SoftPressboardComparisonService softPressboardComparisonService,
      HardPressboardComparisonService hardPressboardComparisonService,
      WiringComparisonService wiringComparisonService,
      MmsSettingClient mmsSettingClient) {
    this.settingListService = settingListService;
    this.softPressboardListService = softPressboardListService;
    this.hardPressboardListService = hardPressboardListService;
    this.wiringRequirementService = wiringRequirementService;
    this.settingComparisonService = settingComparisonService;
    this.softPressboardComparisonService = softPressboardComparisonService;
    this.hardPressboardComparisonService = hardPressboardComparisonService;
    this.wiringComparisonService = wiringComparisonService;
    this.mmsSettingClient = mmsSettingClient;
  }

  public ExperimentPrecheckResponse check(Long logicDiagramId) {
    return runCheck(
        settingListService.resolveForLogic(logicDiagramId),
        softPressboardListService.resolveForLogic(logicDiagramId),
        hardPressboardListService.resolveForLogic(logicDiagramId),
        wiringRequirementService.resolveForLogic(logicDiagramId));
  }

  public ExperimentPrecheckResponse checkForGroup(Long groupId) {
    return runCheck(
        settingListService.resolveForGroup(groupId),
        softPressboardListService.resolveForGroup(groupId),
        hardPressboardListService.resolveForGroup(groupId),
        wiringRequirementService.resolveForGroup(groupId));
  }

  private ExperimentPrecheckResponse runCheck(
      ResolvedSettingList settings,
      ResolvedSoftPressboardList pressboards,
      ResolvedHardPressboardList hardPressboards,
      ResolvedWiringRequirement wiring) {
    boolean checkSettings = settingComparisonService.hasEnabledItems(settings);
    boolean checkPressboards = softPressboardComparisonService.hasEnabledItems(pressboards);
    boolean checkHardPressboards = hardPressboardComparisonService.hasEnabledItems(hardPressboards);
    boolean checkWiring = wiringComparisonService.hasEnabledConfigs(wiring);
    Map<String, Double> actualSettingValues = checkSettings
        ? mmsSettingClient.summon(settings.getTarget().getIedName()).getValues()
        : Collections.emptyMap();
    Map<String, Double> actualPressboardValues = checkPressboards
        ? softPressboardComparisonService.summonCurrentValues(
            pressboards.getTarget().getIedDeviceId())
        : Collections.emptyMap();
    Map<String, Double> actualHardPressboardValues = checkHardPressboards
        ? hardPressboardComparisonService.summonCurrentValues(
            hardPressboards.getTarget().getCabinetId())
        : Collections.emptyMap();
    Map<Long, TerminalWiringState> actualWiringStates = checkWiring
        ? wiringComparisonService.summonCurrentStates(
            wiring.getTarget().getCabinetId(), collectTerminalIds(wiring))
        : Collections.emptyMap();
    SettingCheckResponse settingCheck = checkSettings
        ? settingComparisonService.compareResolved(settings, actualSettingValues)
        : settingComparisonService.skipped(settings);
    SoftPressboardDtos.CheckResponse pressboardCheck = checkPressboards
        ? softPressboardComparisonService.compareResolved(pressboards, actualPressboardValues)
        : softPressboardComparisonService.skipped(pressboards);
    CheckResponse hardPressboardCheck = checkHardPressboards
        ? hardPressboardComparisonService.compareResolved(hardPressboards, actualHardPressboardValues)
        : hardPressboardComparisonService.skipped(hardPressboards);
    WiringRequirementDtos.CheckResponse wiringCheck = checkWiring
        ? wiringComparisonService.compareResolved(wiring, actualWiringStates)
        : wiringComparisonService.skipped(wiring);
    String status;
    if ("MISMATCH".equals(settingCheck.getStatus())
        || "MISMATCH".equals(pressboardCheck.getStatus())
        || "MISMATCH".equals(hardPressboardCheck.getStatus())
        || "MISMATCH".equals(wiringCheck.getStatus())) {
      status = "MISMATCH";
    } else if ("SKIPPED".equals(settingCheck.getStatus())
        && "SKIPPED".equals(pressboardCheck.getStatus())
        && "SKIPPED".equals(hardPressboardCheck.getStatus())
        && "SKIPPED".equals(wiringCheck.getStatus())) {
      status = "SKIPPED";
    } else {
      status = "MATCHED";
    }
    return new ExperimentPrecheckResponse(
        status, settingCheck, pressboardCheck, hardPressboardCheck, wiringCheck);
  }

  private List<Long> collectTerminalIds(ResolvedWiringRequirement wiring) {
    Set<Long> ids = new HashSet<>();
    for (WiringRequirementConfig config : wiring.getRequiredConfigs()) {
      List<WiringRequirementGroup> groups =
          wiring.getGroupsByConfigId().getOrDefault(config.getId(), Collections.emptyList());
      for (WiringRequirementGroup group : groups) {
        for (Long id : new Long[] {group.getTerminalAId(), group.getTerminalBId(),
            group.getTerminalCId(), group.getTerminalNId()}) {
          if (id != null) ids.add(id);
        }
      }
    }
    return new ArrayList<>(ids);
  }
}
