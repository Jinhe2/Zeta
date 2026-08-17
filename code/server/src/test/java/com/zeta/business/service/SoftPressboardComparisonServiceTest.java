package com.zeta.business.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.entities.softpressboardlist.SoftPressboardDtos.CheckResponse;
import com.zeta.business.entities.softpressboardlist.SoftPressboardListItem;
import com.zeta.business.service.SettingListTargetService.Target;
import com.zeta.business.service.SoftPressboardListService.ResolvedSoftPressboardList;
import com.zeta.integration.monitor.IedSoftPressboardStatusClient;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SoftPressboardComparisonServiceTest {
  private SoftPressboardComparisonService service;
  private Target target;

  @BeforeEach
  void setUp() {
    service = new SoftPressboardComparisonService(
        mock(SettingListTargetService.class), mock(SoftPressboardCatalogService.class),
        mock(IedSoftPressboardStatusClient.class));
    target = new Target(SettingListScopeType.LOGIC_DIAGRAM, 8L, "过流保护", 2L, "IED_A");
  }

  @Test
  void 正确状态匹配且缺失状态失败() {
    List<SoftPressboardListItem> items = Arrays.asList(
        item("IED_A/LD0/GGIO$ST$Ena1$stVal", "差动保护", true),
        item("IED_A/LD0/GGIO$ST$Ena2$stVal", "距离保护", false));
    ResolvedSoftPressboardList resolved = new ResolvedSoftPressboardList(
        target, SettingListScopeType.LOGIC_DIAGRAM, 8L, items);
    CheckResponse result = service.compareResolved(resolved,
        Collections.singletonMap("LD0/GGIO$ST$Ena1$stVal", 1D));
    assertEquals("MISMATCH", result.getStatus());
    assertEquals(1, result.getEqual());
    assertEquals(1, result.getMissing());
    assertNull(result.getItems().get(1).getActualValue());
  }

  @Test
  void 非零一状态按非法值处理() {
    ResolvedSoftPressboardList resolved = new ResolvedSoftPressboardList(
        target, SettingListScopeType.LOGIC_DIAGRAM, 8L,
        Collections.singletonList(item("ref", "差动保护", true)));
    CheckResponse result = service.compareResolved(resolved, Collections.singletonMap("ref", 2D));
    assertEquals("MISMATCH", result.getStatus());
    assertEquals(1, result.getMismatch());
    assertEquals("非法值（2）", result.getItems().get(0).getActualValue());
  }

  private SoftPressboardListItem item(String ref, String name, boolean baseline) {
    SoftPressboardListItem item = new SoftPressboardListItem();
    item.setPressboardRef(ref);
    item.setPressboardName(name);
    item.setBaselineValue(baseline);
    item.setCompareEnabled(true);
    item.setSortOrder(0);
    return item;
  }
}
