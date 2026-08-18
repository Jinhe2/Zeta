package com.zeta.business.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.zeta.business.entities.settinglist.*;
import com.zeta.business.entities.settinglist.dto.SettingCheckResponse;
import com.zeta.business.service.SettingListService.ResolvedSettingList;
import com.zeta.business.service.SettingListTargetService.Target;
import com.zeta.integration.mms.MmsSettingClient;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SettingComparisonServiceTest {
  private SettingListService listService;
  private MmsSettingClient mmsClient;
  private SettingComparisonService service;

  @BeforeEach
  void setUp() {
    listService = mock(SettingListService.class);
    mmsClient = mock(MmsSettingClient.class);
    service =
        new SettingComparisonService(
            listService,
            mock(SettingListTargetService.class),
            mock(SettingCatalogService.class),
            mmsClient);
  }

  @Test
  void 两级清单为空时跳过召唤() {
    Target target = new Target(SettingListScopeType.LOGIC_DIAGRAM, 8L, "过流保护", 2L, "IED_A", 3L);
    when(listService.resolveForLogic(8L))
        .thenReturn(new ResolvedSettingList(target, null, null, Collections.emptyList()));

    SettingCheckResponse result = service.checkForLogic(8L);

    assertEquals("SKIPPED", result.getStatus());
    verifyNoInteractions(mmsClient);
  }

  @Test
  void 清单项目全部关闭比对时跳过召唤() {
    Target target = new Target(SettingListScopeType.LOGIC_DIAGRAM, 8L, "过流保护", 2L, "IED_A", 3L);
    SettingListItem disabled = item("IED_A/LD0/PTOC$SG$StrVal", "启动值", "2");
    disabled.setCompareEnabled(false);
    when(listService.resolveForLogic(8L))
        .thenReturn(
            new ResolvedSettingList(
                target, SettingListScopeType.LOGIC_DIAGRAM, 8L, Collections.singletonList(disabled)));

    SettingCheckResponse result = service.checkForLogic(8L);

    assertEquals("SKIPPED", result.getStatus());
    assertEquals(0, result.getTotal());
    verifyNoInteractions(mmsClient);
  }

  @Test
  void 时间定值按秒换算并识别缺失项() {
    Target target = new Target(SettingListScopeType.LOGIC_DIAGRAM, 8L, "过流保护", 2L, "IED_A", 3L);
    SettingListItem time = item("IED_A/LD0/PTOC$SG$OpDlTmms", "动作延时", "1.5");
    SettingListItem missing = item("IED_A/LD0/PTOC$SG$StrVal", "启动值", "2");
    when(listService.resolveForLogic(8L))
        .thenReturn(
            new ResolvedSettingList(
                target, SettingListScopeType.IED_DEVICE, 2L, Arrays.asList(time, missing)));
    when(mmsClient.summon("IED_A"))
        .thenReturn(
            new MmsSettingClient.SummonResult(
                Collections.singletonMap("LD0/PTOC$SG$OpDlTmms", 1500D)));

    SettingCheckResponse result = service.checkForLogic(8L);

    assertEquals("MISMATCH", result.getStatus());
    assertEquals(1, result.getEqual());
    assertEquals(1, result.getMissing());
    assertEquals("1.5", result.getItems().get(0).getActualValue());
    assertEquals("s", result.getItems().get(0).getValueUnit());
  }

  @Test
  void 支持带前缀和无斜杠前缀引用() {
    Map<String, Double> values = new LinkedHashMap<>();
    values.put("IED_ALD0/PTOC$SG$StrVal", 3D);
    assertEquals(
        3D,
        SettingComparisonService.findActual(
            "IED_A", "IED_A/LD0/PTOC$SG$StrVal", values));
    assertEquals(
        4D,
        SettingComparisonService.findActual(
            "IED_A",
            "IED_ALD0/PTOC$SG$StrVal",
            Collections.singletonMap("IED_A/LD0/PTOC$SG$StrVal", 4D)));
  }

  private SettingListItem item(String ref, String name, String value) {
    SettingListItem item = new SettingListItem();
    item.setSettingRef(ref);
    item.setSettingName(name);
    item.setSettingFc("SG");
    item.setValueType("FLOAT");
    item.setCompareEnabled(true);
    item.setBaselineValue(value);
    item.setSortOrder(0);
    return item;
  }
}
