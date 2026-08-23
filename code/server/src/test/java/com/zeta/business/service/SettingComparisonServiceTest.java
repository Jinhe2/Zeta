package com.zeta.business.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.zeta.business.entities.settinglist.SettingListItem;
import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.entities.settinglist.dto.SettingCheckResponse;
import com.zeta.business.service.SettingListService.ResolvedSettingList;
import com.zeta.business.service.SettingListTargetService.Target;
import com.zeta.integration.mms.MmsSettingClient;
import com.zeta.integration.mms.MmsSettingClient.SummonResult;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SettingComparisonServiceTest {

  @Test
  void deviceCheckUsesConfiguredDeviceBaseline() {
    SettingListService listService = mock(SettingListService.class);
    MmsSettingClient mmsClient = mock(MmsSettingClient.class);
    SettingComparisonService service = new SettingComparisonService(
        listService,
        mock(SettingListTargetService.class),
        mock(SettingCatalogService.class),
        mmsClient);

    SettingListItem item = item("IED_A/LD0/PTOC$SG$StrVal", "启动定值", "2.5", true);
    ResolvedSettingList resolved = new ResolvedSettingList(
        new Target(SettingListScopeType.IED_DEVICE, 12L, "保护装置", 12L, "IED_A", 3L),
        SettingListScopeType.IED_DEVICE,
        12L,
        Collections.singletonList(item));
    when(listService.resolveForDevice(12L)).thenReturn(resolved);
    Map<String, Double> actual = new LinkedHashMap<>();
    actual.put("IED_A/LD0/PTOC$SG$StrVal", 3D);
    when(mmsClient.summon("IED_A")).thenReturn(new SummonResult(actual));

    SettingCheckResponse response = service.checkForDevice(12L);

    assertEquals("MISMATCH", response.getStatus());
    assertEquals(1, response.getMismatch());
    assertEquals("2.5", response.getItems().get(0).getBaselineValue());
    assertFalse(response.getItems().get(0).isEqual());
    verify(listService).resolveForDevice(12L);
    verify(mmsClient).summon("IED_A");
  }

  @Test
  void deviceCheckSkipsSummonWhenNoBaselineParticipates() {
    SettingListService listService = mock(SettingListService.class);
    MmsSettingClient mmsClient = mock(MmsSettingClient.class);
    SettingComparisonService service = new SettingComparisonService(
        listService,
        mock(SettingListTargetService.class),
        mock(SettingCatalogService.class),
        mmsClient);

    SettingListItem item = item("IED_A/LD0/PTOC$SG$StrVal", "启动定值", "2.5", false);
    when(listService.resolveForDevice(12L)).thenReturn(new ResolvedSettingList(
        new Target(SettingListScopeType.IED_DEVICE, 12L, "保护装置", 12L, "IED_A", 3L),
        SettingListScopeType.IED_DEVICE,
        12L,
        Collections.singletonList(item)));

    SettingCheckResponse response = service.checkForDevice(12L);

    assertEquals("SKIPPED", response.getStatus());
    assertEquals(0, response.getTotal());
    verifyNoInteractions(mmsClient);
  }

  private SettingListItem item(String ref, String name, String baseline, boolean compareEnabled) {
    SettingListItem item = new SettingListItem();
    item.setScopeType(SettingListScopeType.IED_DEVICE);
    item.setScopeId(12L);
    item.setSettingRef(ref);
    item.setSettingName(name);
    item.setValueType("FLOAT");
    item.setCompareEnabled(compareEnabled);
    item.setBaselineValue(baseline);
    item.setSortOrder(0);
    return item;
  }
}
