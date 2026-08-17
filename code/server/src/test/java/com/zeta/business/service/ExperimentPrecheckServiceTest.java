package com.zeta.business.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.zeta.business.entities.experiment.ExperimentPrecheckResponse;
import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.entities.settinglist.dto.SettingCheckResponse;
import com.zeta.business.entities.softpressboardlist.SoftPressboardDtos.CheckResponse;
import com.zeta.business.service.SettingListService.ResolvedSettingList;
import com.zeta.business.service.SettingListTargetService.Target;
import com.zeta.business.service.SoftPressboardListService.ResolvedSoftPressboardList;
import com.zeta.integration.mms.MmsSettingClient;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExperimentPrecheckServiceTest {
  private SettingListService settingListService;
  private SoftPressboardListService pressboardListService;
  private SettingComparisonService settingComparisonService;
  private SoftPressboardComparisonService pressboardComparisonService;
  private MmsSettingClient mmsClient;
  private ExperimentPrecheckService service;
  private ResolvedSettingList settings;
  private ResolvedSoftPressboardList pressboards;

  @BeforeEach
  void setUp() {
    settingListService = mock(SettingListService.class);
    pressboardListService = mock(SoftPressboardListService.class);
    settingComparisonService = mock(SettingComparisonService.class);
    pressboardComparisonService = mock(SoftPressboardComparisonService.class);
    mmsClient = mock(MmsSettingClient.class);
    service = new ExperimentPrecheckService(settingListService, pressboardListService,
        settingComparisonService, pressboardComparisonService, mmsClient);
    Target target = new Target(SettingListScopeType.LOGIC_DIAGRAM, 8L, "过流保护", 2L, "IED_A");
    settings = new ResolvedSettingList(target, null, null, Collections.emptyList());
    pressboards = new ResolvedSoftPressboardList(target, null, null, Collections.emptyList());
    when(settingListService.resolveForLogic(8L)).thenReturn(settings);
    when(pressboardListService.resolveForLogic(8L)).thenReturn(pressboards);
  }

  @Test
  void 两类均跳过时不召唤装置() {
    SettingCheckResponse settingSkipped = setting("SKIPPED");
    CheckResponse pressboardSkipped = pressboard("SKIPPED");
    when(settingComparisonService.skipped(settings)).thenReturn(settingSkipped);
    when(pressboardComparisonService.skipped(pressboards)).thenReturn(pressboardSkipped);
    ExperimentPrecheckResponse result = service.check(8L);
    assertEquals("SKIPPED", result.getStatus());
    verifyNoInteractions(mmsClient);
  }

  @Test
  void 两类校验共用一次装置召唤() {
    when(settingComparisonService.hasEnabledItems(settings)).thenReturn(true);
    when(pressboardComparisonService.hasEnabledItems(pressboards)).thenReturn(true);
    Map<String, Double> values = Collections.singletonMap("ref", 1D);
    when(mmsClient.summon("IED_A")).thenReturn(new MmsSettingClient.SummonResult(values));
    when(settingComparisonService.compareResolved(settings, values)).thenReturn(setting("MATCHED"));
    when(pressboardComparisonService.compareResolved(pressboards, values)).thenReturn(pressboard("MISMATCH"));
    ExperimentPrecheckResponse result = service.check(8L);
    assertEquals("MISMATCH", result.getStatus());
    verify(mmsClient, times(1)).summon("IED_A");
  }

  private SettingCheckResponse setting(String status) {
    return new SettingCheckResponse(status, null, null, 0, 0, 0, 0, Collections.emptyList());
  }

  private CheckResponse pressboard(String status) {
    return new CheckResponse(status, null, null, 0, 0, 0, 0, Collections.emptyList());
  }
}
