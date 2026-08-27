package com.zeta.business.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
  private SettingListService service;
  private AtomicReference<List<SettingListItem>> savedItems;

  @BeforeEach
  void setUp() {
    repository = mock(SettingListItemRepository.class);
    SettingListTargetService targetService = mock(SettingListTargetService.class);
    catalogService = mock(SettingCatalogService.class);
    service = new SettingListService(repository, targetService, catalogService);
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
