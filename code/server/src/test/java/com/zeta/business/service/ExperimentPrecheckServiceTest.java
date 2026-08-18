package com.zeta.business.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.zeta.business.entities.experiment.ExperimentPrecheckResponse;
import com.zeta.business.entities.hardpressboardlist.HardPressboardDtos;
import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.entities.settinglist.dto.SettingCheckResponse;
import com.zeta.business.entities.softpressboardlist.SoftPressboardDtos.CheckResponse;
import com.zeta.business.entities.wiringrequirement.WiringRequirementDtos;
import com.zeta.business.service.HardPressboardListService.ResolvedHardPressboardList;
import com.zeta.business.service.SettingListService.ResolvedSettingList;
import com.zeta.business.service.SettingListTargetService.Target;
import com.zeta.business.service.SoftPressboardListService.ResolvedSoftPressboardList;
import com.zeta.business.service.WiringRequirementService.ResolvedWiringRequirement;
import com.zeta.integration.mms.MmsSettingClient;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExperimentPrecheckServiceTest {
  private SettingListService settingListService;
  private SoftPressboardListService pressboardListService;
  private HardPressboardListService hardPressboardListService;
  private WiringRequirementService wiringRequirementService;
  private SettingComparisonService settingComparisonService;
  private SoftPressboardComparisonService pressboardComparisonService;
  private HardPressboardComparisonService hardPressboardComparisonService;
  private WiringComparisonService wiringComparisonService;
  private MmsSettingClient mmsClient;
  private ExperimentPrecheckService service;
  private ResolvedSettingList settings;
  private ResolvedSoftPressboardList pressboards;
  private ResolvedHardPressboardList hardPressboards;
  private ResolvedWiringRequirement wiring;

  @BeforeEach
  void setUp() {
    settingListService = mock(SettingListService.class);
    pressboardListService = mock(SoftPressboardListService.class);
    hardPressboardListService = mock(HardPressboardListService.class);
    wiringRequirementService = mock(WiringRequirementService.class);
    settingComparisonService = mock(SettingComparisonService.class);
    pressboardComparisonService = mock(SoftPressboardComparisonService.class);
    hardPressboardComparisonService = mock(HardPressboardComparisonService.class);
    wiringComparisonService = mock(WiringComparisonService.class);
    mmsClient = mock(MmsSettingClient.class);
    service = new ExperimentPrecheckService(settingListService, pressboardListService,
        hardPressboardListService, wiringRequirementService, settingComparisonService,
        pressboardComparisonService, hardPressboardComparisonService, wiringComparisonService,
        mmsClient);
    Target target = new Target(SettingListScopeType.LOGIC_DIAGRAM, 8L, "过流保护", 2L, "IED_A", 3L);
    settings = new ResolvedSettingList(target, null, null, Collections.emptyList());
    pressboards = new ResolvedSoftPressboardList(target, null, null, Collections.emptyList());
    hardPressboards = new ResolvedHardPressboardList(target, null, null, Collections.emptyList());
    wiring = new ResolvedWiringRequirement(target, Collections.emptyList(),
        Collections.emptyMap(), Collections.emptyMap());
    when(settingListService.resolveForLogic(8L)).thenReturn(settings);
    when(pressboardListService.resolveForLogic(8L)).thenReturn(pressboards);
    when(hardPressboardListService.resolveForLogic(8L)).thenReturn(hardPressboards);
    when(wiringRequirementService.resolveForLogic(8L)).thenReturn(wiring);
    when(settingComparisonService.skipped(settings)).thenReturn(setting("SKIPPED"));
    when(pressboardComparisonService.skipped(pressboards)).thenReturn(pressboard("SKIPPED"));
    when(hardPressboardComparisonService.skipped(hardPressboards))
        .thenReturn(hardPressboard("SKIPPED"));
    when(wiringComparisonService.skipped(wiring)).thenReturn(wiring("SKIPPED"));
  }

  @Test
  void 四类均跳过时不召唤装置() {
    ExperimentPrecheckResponse result = service.check(8L);
    assertEquals("SKIPPED", result.getStatus());
    verifyNoInteractions(mmsClient);
  }

  @Test
  void 定值与软压板分别读取且硬压板接线跳过() {
    when(settingComparisonService.hasEnabledItems(settings)).thenReturn(true);
    when(pressboardComparisonService.hasEnabledItems(pressboards)).thenReturn(true);
    Map<String, Double> values = Collections.singletonMap("ref", 1D);
    when(mmsClient.summon("IED_A")).thenReturn(new MmsSettingClient.SummonResult(values));
    when(pressboardComparisonService.summonCurrentValues(2L)).thenReturn(values);
    when(settingComparisonService.compareResolved(settings, values)).thenReturn(setting("MATCHED"));
    when(pressboardComparisonService.compareResolved(pressboards, values)).thenReturn(pressboard("MISMATCH"));
    ExperimentPrecheckResponse result = service.check(8L);
    assertEquals("MISMATCH", result.getStatus());
    verify(mmsClient, times(1)).summon("IED_A");
    verify(pressboardComparisonService, times(1)).summonCurrentValues(2L);
  }

  @Test
  void 仅校验软压板时不召唤定值() {
    when(pressboardComparisonService.hasEnabledItems(pressboards)).thenReturn(true);
    Map<String, Double> values = Collections.singletonMap("ref", 0D);
    when(pressboardComparisonService.summonCurrentValues(2L)).thenReturn(values);
    when(pressboardComparisonService.compareResolved(pressboards, values))
        .thenReturn(pressboard("MATCHED"));

    assertEquals("MATCHED", service.check(8L).getStatus());
    verifyNoInteractions(mmsClient);
  }

  @Test
  void 硬压板不一致时整体判定不通过() {
    when(hardPressboardComparisonService.hasEnabledItems(hardPressboards)).thenReturn(true);
    Map<String, Double> values = Collections.singletonMap("3", 0D);
    when(hardPressboardComparisonService.summonCurrentValues(3L)).thenReturn(values);
    when(hardPressboardComparisonService.compareResolved(hardPressboards, values))
        .thenReturn(hardPressboard("MISMATCH"));
    ExperimentPrecheckResponse result = service.check(8L);
    assertEquals("MISMATCH", result.getStatus());
    verify(hardPressboardComparisonService, times(1)).summonCurrentValues(3L);
  }

  @Test
  void 接线不一致时整体判定不通过() {
    when(wiringComparisonService.hasEnabledConfigs(wiring)).thenReturn(true);
    Map<Long, com.zeta.integration.monitor.TerminalStatusClient.TerminalWiringState> states =
        Collections.emptyMap();
    when(wiringComparisonService.summonCurrentStates(3L, Collections.emptyList())).thenReturn(states);
    when(wiringComparisonService.compareResolved(wiring, states)).thenReturn(wiring("MISMATCH"));
    ExperimentPrecheckResponse result = service.check(8L);
    assertEquals("MISMATCH", result.getStatus());
    verify(wiringComparisonService, times(1)).summonCurrentStates(eq(3L), anyList());
  }

  private SettingCheckResponse setting(String status) {
    return new SettingCheckResponse(status, null, null, 0, 0, 0, 0, Collections.emptyList());
  }

  private CheckResponse pressboard(String status) {
    return new CheckResponse(status, null, null, 0, 0, 0, 0, Collections.emptyList());
  }

  private HardPressboardDtos.CheckResponse hardPressboard(String status) {
    return new HardPressboardDtos.CheckResponse(status, null, null, 0, 0, 0, 0, Collections.emptyList());
  }

  private WiringRequirementDtos.CheckResponse wiring(String status) {
    return new WiringRequirementDtos.CheckResponse(status, Collections.emptyList());
  }
}
