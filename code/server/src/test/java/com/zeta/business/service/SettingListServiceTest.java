package com.zeta.business.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.zeta.business.entities.settinglist.*;
import com.zeta.business.entities.settinglist.dto.SettingListResponse;
import com.zeta.integration.mms.MmsSettingClient;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zeta.business.entities.settinglist.SettingListItem;
import com.zeta.business.entities.settinglist.SettingListItemRepository;
import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.entities.settinglist.dto.SettingListItemResponse;
import com.zeta.business.entities.settinglist.dto.SettingListSaveItemRequest;
import com.zeta.business.service.SettingListTargetService.Target;
import com.zeta.screen.iedsetting.IedSettingItem;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class SettingListServiceTest {
  private SettingListItemRepository repository;
  private SettingCatalogService catalogService;
  private SettingListTargetService targetService;
  private LogicSettingSelectionRepository selections;
  private SettingListService service;
  private AtomicReference<List<SettingListItem>> savedItems;

  @BeforeEach
  void setUp() {
    repository = mock(SettingListItemRepository.class);
    targetService = mock(SettingListTargetService.class);
    selections = mock(LogicSettingSelectionRepository.class);
    catalogService = mock(SettingCatalogService.class);
    service = new SettingListService(repository, targetService, catalogService,
        selections);
    savedItems = new AtomicReference<>();

    when(targetService.require(SettingListScopeType.IED_DEVICE, 12L))
        .thenReturn(new Target(
            SettingListScopeType.IED_DEVICE, 12L, "保护装置", 12L, "IED_A", 3L));
    when(repository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(
            SettingListScopeType.IED_DEVICE, 12L))
        .thenReturn(Collections.emptyList());
    when(repository.saveAll(any()))
        .thenAnswer(invocation -> {
          @SuppressWarnings("unchecked")
          List<SettingListItem> items = (List<SettingListItem>) invocation.getArgument(0);
          savedItems.set(items);
          return items;
        });
  }

  @Test
  void tmmsIntegerCatalogItemIsSavedAsFloat() {
    configureCatalog("IED_A/LD0/PTOC$SG$OpDlTmms", "INTEGER");

    service.replace(
        SettingListScopeType.IED_DEVICE,
        12L,
        Collections.singletonList(request("IED_A/LD0/PTOC$SG$OpDlTmms", "0.25")));

    SettingListItem saved = savedItems.get().get(0);
    assertEquals("FLOAT", saved.getValueType());
    assertEquals("0.25", saved.getBaselineValue());
  }

  @Test
  void ordinaryIntegerCatalogItemStillRejectsDecimal() {
    configureCatalog("IED_A/LD0/PTOC$SG$StrVal", "INTEGER");

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> service.replace(
                SettingListScopeType.IED_DEVICE,
                12L,
                Collections.singletonList(request("IED_A/LD0/PTOC$SG$StrVal", "0.25"))));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    assertEquals("第 1 项为整数定值，不能包含小数", exception.getReason());
  }

  @Test
  void legacyTmmsIntegerItemIsReturnedAsFloat() {
    SettingListItem item = new SettingListItem();
    item.setSettingRef("IED_A/LD0/PTOC$SG$OpDlTmms");
    item.setValueType("INTEGER");

    SettingListItemResponse response = SettingListItemResponse.from(item);

    assertEquals("FLOAT", response.getValueType());
  }

  @Test
  void 逻辑读取装置数值且勾选相互独立() {
    SettingListItem device = deviceItem("定值A", "3", false);
    when(repository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.IED_DEVICE, 12L))
        .thenReturn(Collections.singletonList(device));
    for (SettingListScopeType type : Arrays.asList(SettingListScopeType.LOGIC_DIAGRAM, SettingListScopeType.LOGIC_GROUP)) {
      configureLogic(type, 20L);
      when(selections.findByScopeTypeAndScopeId(type, 20L))
          .thenReturn(Collections.singletonList(selection(type, 20L, "定值A")));
      SettingListResponse response = service.get(type, 20L);
      assertEquals(0, response.getConfiguredItems().size());
      assertEquals(SettingListScopeType.IED_DEVICE, response.getEffectiveScopeType());
      assertTrue(response.getEffectiveItems().get(0).isCompareEnabled());
      assertEquals("3", response.getEffectiveItems().get(0).getBaselineValue());
      assertFalse(device.getCompareEnabled());
      verify(repository, never()).findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(type, 20L);
    }
    device.setBaselineValue("4");
    assertEquals("4", service.resolveForLogic(20L).getItems().get(0).getBaselineValue());
  }

  @Test
  void 未配置选择的逻辑展示全部项目但跳过校验且不召唤() {
    configureLogic(SettingListScopeType.LOGIC_DIAGRAM, 20L);
    when(repository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.IED_DEVICE, 12L))
        .thenReturn(Collections.singletonList(deviceItem("定值A", "3", true)));
    assertFalse(service.get(SettingListScopeType.LOGIC_DIAGRAM, 20L).getEffectiveItems().get(0).isCompareEnabled());
    MmsSettingClient mms = mock(MmsSettingClient.class);
    SettingComparisonService comparison = new SettingComparisonService(service, targetService, catalogService, mms);
    assertEquals("SKIPPED", comparison.checkForLogic(20L).getStatus());
    verifyNoInteractions(mms);
  }

  @Test
  void 选择接口支持空数组且拒绝外部引用() {
    configureLogic(SettingListScopeType.LOGIC_GROUP, 20L);
    service.saveSelection(SettingListScopeType.LOGIC_GROUP, 20L, Collections.emptyList());
    verify(selections).deleteByScopeTypeAndScopeId(SettingListScopeType.LOGIC_GROUP, 20L);
    verify(repository, never()).saveAll(any());
    assertThrows(ResponseStatusException.class, () -> service.saveSelection(
        SettingListScopeType.LOGIC_GROUP, 20L, Collections.singletonList("未知定值")));
  }

  @Test
  void 逻辑作用域禁止旧清单操作() {
    for (SettingListScopeType type : Arrays.asList(SettingListScopeType.LOGIC_DIAGRAM, SettingListScopeType.LOGIC_GROUP)) {
      assertThrows(ResponseStatusException.class, () -> service.replace(type, 20L, Collections.emptyList()));
      assertThrows(ResponseStatusException.class, () -> service.clear(type, 20L));
      SettingListExcelService excel = new SettingListExcelService(service);
      assertThrows(ResponseStatusException.class, () -> excel.exportWorkbook(type, 20L));
      assertThrows(ResponseStatusException.class, () -> excel.importWorkbook(type, 20L, null));
      SettingComparisonService comparison = new SettingComparisonService(service, targetService, catalogService, mock(MmsSettingClient.class));
      assertThrows(ResponseStatusException.class, () -> comparison.summonPreview(type, 20L));
    }
    verifyNoInteractions(repository, selections);
  }

  @Test
  void 装置替换只清理删除项目的选择() {
    configureCatalog("定值A", "FLOAT");
    LogicSettingSelection retained = selection(SettingListScopeType.LOGIC_DIAGRAM, 20L, "定值A");
    LogicSettingSelection removed = selection(SettingListScopeType.LOGIC_DIAGRAM, 20L, "定值B");
    when(targetService.logicScopeIdsForDevice(12L)).thenReturn(Collections.singletonMap(
        SettingListScopeType.LOGIC_DIAGRAM, Collections.singletonList(20L)));
    when(selections.findByScopeTypeAndScopeIdIn(SettingListScopeType.LOGIC_DIAGRAM, Collections.singletonList(20L)))
        .thenReturn(Arrays.asList(retained, removed));
    service.replace(SettingListScopeType.IED_DEVICE, 12L, Collections.singletonList(request("定值A", "4")));
    verify(selections).deleteAll(Collections.singletonList(removed));
    verify(selections, never()).saveAll(any());
  }

  private void configureLogic(SettingListScopeType type, Long id) {
    when(targetService.require(type, id)).thenReturn(new Target(type, id, "逻辑", 12L, "IED_A", 3L));
  }

  private SettingListItem deviceItem(String ref, String value, boolean compare) {
    SettingListItem item = new SettingListItem();
    item.setSettingRef(ref); item.setSettingName(ref); item.setSettingFc("SG");
    item.setBaselineValue(value); item.setValueType("FLOAT"); item.setSortOrder(0); item.setCompareEnabled(compare);
    return item;
  }

  private LogicSettingSelection selection(SettingListScopeType type, Long id, String ref) {
    LogicSettingSelection item = new LogicSettingSelection();
    item.setScopeType(type); item.setScopeId(id); item.setSettingRef(ref);
    return item;
  }

  private void configureCatalog(String settingRef, String valueType) {
    IedSettingItem item = new IedSettingItem();
    item.setIedDeviceId(12L);
    item.setSettingName("动作延时");
    item.setSettingRef(settingRef);
    item.setValueType(valueType);
    Map<String, IedSettingItem> catalog = new LinkedHashMap<>();
    catalog.put(settingRef, item);
    when(catalogService.mapByReference(12L)).thenReturn(catalog);
  }

  private SettingListSaveItemRequest request(String settingRef, String baselineValue) {
    SettingListSaveItemRequest request = new SettingListSaveItemRequest();
    request.setSettingRef(settingRef);
    request.setBaselineValue(baselineValue);
    request.setCompareEnabled(true);
    request.setSortOrder(0);
    return request;
  }
}
