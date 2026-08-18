package com.zeta.business.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.zeta.business.entities.settinglist.*;
import com.zeta.business.entities.settinglist.dto.SettingListSaveItemRequest;
import com.zeta.business.service.SettingListTargetService.Target;
import com.zeta.screen.iedsetting.IedSettingItem;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class SettingListServiceTest {
  private SettingListItemRepository repository;
  private SettingListTargetService targetService;
  private SettingCatalogService catalogService;
  private SettingListService service;

  @BeforeEach
  void setUp() {
    repository = mock(SettingListItemRepository.class);
    targetService = mock(SettingListTargetService.class);
    catalogService = mock(SettingCatalogService.class);
    service = new SettingListService(repository, targetService, catalogService);
    when(targetService.require(any(), anyLong()))
        .thenReturn(new Target(SettingListScopeType.IED_DEVICE, 2L, "装置", 2L, "IED_A", 3L));
    when(repository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(any(), anyLong()))
        .thenReturn(Collections.emptyList());
  }

  @Test
  void 整数项目拒绝小数且不修改数据库() {
    IedSettingItem catalog = catalog("ref-1", "INTEGER");
    when(catalogService.mapByReference(2L)).thenReturn(Collections.singletonMap("ref-1", catalog));

    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class,
            () -> service.replace(SettingListScopeType.IED_DEVICE, 2L, items("ref-1", "1.2")));

    assertTrue(error.getReason().contains("不能包含小数"));
    verify(repository, never()).deleteByScopeTypeAndScopeId(any(), anyLong());
  }

  @Test
  void 跨装置引用整批拒绝() {
    when(catalogService.mapByReference(2L)).thenReturn(Collections.emptyMap());
    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class,
            () -> service.replace(SettingListScopeType.IED_DEVICE, 2L, items("other-ref", "1")));
    assertTrue(error.getReason().contains("不属于当前装置"));
  }

  @Test
  void 合法清单执行整套替换() {
    IedSettingItem catalog = catalog("ref-1", "FLOAT");
    when(catalogService.mapByReference(2L)).thenReturn(Collections.singletonMap("ref-1", catalog));

    List<SettingListSaveItemRequest> requests = items("ref-1", "1.500");
    requests.get(0).setCompareEnabled(false);
    service.replace(SettingListScopeType.IED_DEVICE, 2L, requests);

    verify(repository).deleteByScopeTypeAndScopeId(SettingListScopeType.IED_DEVICE, 2L);
    verify(repository)
        .saveAll(
            argThat(
                rows -> {
                  SettingListItem saved = rows.iterator().next();
                  return saved.getBaselineValue().equals("1.5")
                      && Boolean.FALSE.equals(saved.getCompareEnabled());
                }));
  }

  private IedSettingItem catalog(String ref, String type) {
    IedSettingItem item = new IedSettingItem();
    item.setIedDeviceId(2L);
    item.setSettingRef(ref);
    item.setSettingName("定值");
    item.setValueType(type);
    return item;
  }

  private List<SettingListSaveItemRequest> items(String ref, String value) {
    SettingListSaveItemRequest request = new SettingListSaveItemRequest();
    request.setSettingRef(ref);
    request.setBaselineValue(value);
    request.setSortOrder(0);
    return Collections.singletonList(request);
  }
}
