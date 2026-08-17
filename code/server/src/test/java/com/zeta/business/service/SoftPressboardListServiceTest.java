package com.zeta.business.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.entities.softpressboardlist.*;
import com.zeta.business.entities.softpressboardlist.SoftPressboardDtos.*;
import com.zeta.business.service.SettingListTargetService.Target;
import com.zeta.screen.softpressboard.IedSoftPressboardItem;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class SoftPressboardListServiceTest {
  private SoftPressboardListItemRepository repository;
  private SettingListTargetService targetService;
  private SoftPressboardCatalogService catalogService;
  private SoftPressboardListService service;

  @BeforeEach
  void setUp() {
    repository = mock(SoftPressboardListItemRepository.class);
    targetService = mock(SettingListTargetService.class);
    catalogService = mock(SoftPressboardCatalogService.class);
    service = new SoftPressboardListService(repository, targetService, catalogService);
    when(targetService.require(any(), anyLong()))
        .thenReturn(new Target(SettingListScopeType.IED_DEVICE, 2L, "装置", 2L, "IED_A"));
    when(repository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(any(), anyLong()))
        .thenReturn(Collections.emptyList());
  }

  @Test
  void 跨装置引用整批拒绝且不修改数据库() {
    when(catalogService.mapByReference(2L)).thenReturn(Collections.emptyMap());
    ResponseStatusException error = assertThrows(
        ResponseStatusException.class,
        () -> service.replace(SettingListScopeType.IED_DEVICE, 2L, requests("other", true)));
    assertTrue(error.getReason().contains("不属于当前装置"));
    verify(repository, never()).deleteByScopeTypeAndScopeId(any(), anyLong());
  }

  @Test
  void 合法清单执行整套替换并保留退出状态() {
    IedSoftPressboardItem catalog = catalog("ref-1", "差动保护软压板");
    when(catalogService.mapByReference(2L)).thenReturn(Collections.singletonMap("ref-1", catalog));
    List<SaveItemRequest> requests = requests("ref-1", false);
    requests.get(0).setCompareEnabled(false);
    service.replace(SettingListScopeType.IED_DEVICE, 2L, requests);
    verify(repository).deleteByScopeTypeAndScopeId(SettingListScopeType.IED_DEVICE, 2L);
    verify(repository).saveAll(argThat(rows -> {
      SoftPressboardListItem saved = rows.iterator().next();
      return Boolean.FALSE.equals(saved.getBaselineValue())
          && Boolean.FALSE.equals(saved.getCompareEnabled())
          && "差动保护软压板".equals(saved.getPressboardName());
    }));
  }

  @Test
  void 逻辑框图空清单回退到装置级() {
    SoftPressboardListItem deviceItem = new SoftPressboardListItem();
    deviceItem.setPressboardRef("ref-1");
    deviceItem.setPressboardName("差动保护软压板");
    deviceItem.setBaselineValue(true);
    deviceItem.setCompareEnabled(true);
    deviceItem.setSortOrder(0);
    when(targetService.require(SettingListScopeType.LOGIC_DIAGRAM, 8L))
        .thenReturn(new Target(SettingListScopeType.LOGIC_DIAGRAM, 8L, "过流保护", 2L, "IED_A"));
    when(repository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(
        SettingListScopeType.LOGIC_DIAGRAM, 8L)).thenReturn(Collections.emptyList());
    when(repository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(
        SettingListScopeType.IED_DEVICE, 2L)).thenReturn(Collections.singletonList(deviceItem));
    ListResponse result = service.get(SettingListScopeType.LOGIC_DIAGRAM, 8L);
    assertTrue(result.isFallbackToDevice());
    assertEquals(1, result.getEffectiveItems().size());
  }

  private IedSoftPressboardItem catalog(String ref, String name) {
    IedSoftPressboardItem item = new IedSoftPressboardItem();
    item.setIedDeviceId(2L);
    item.setPressboardRef(ref);
    item.setPressboardName(name);
    return item;
  }

  private List<SaveItemRequest> requests(String ref, boolean value) {
    SaveItemRequest request = new SaveItemRequest();
    request.setPressboardRef(ref);
    request.setBaselineValue(value);
    request.setSortOrder(0);
    return Collections.singletonList(request);
  }
}
