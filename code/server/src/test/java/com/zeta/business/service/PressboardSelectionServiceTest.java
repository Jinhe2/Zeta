package com.zeta.business.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.zeta.business.entities.pressboardselection.*;
import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.entities.softpressboardlist.*;
import com.zeta.business.entities.hardpressboardlist.*;
import com.zeta.business.service.SettingListTargetService.Target;
import java.util.*;
import com.zeta.integration.monitor.IedSoftPressboardStatusClient;
import com.zeta.integration.monitor.HardPressboardStatusClient;
import com.zeta.integration.mms.MmsSettingClient;
import com.zeta.business.entities.settinglist.dto.SettingCheckResponse;
import com.zeta.business.entities.wiringrequirement.WiringRequirementDtos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.web.server.ResponseStatusException;

class PressboardSelectionServiceTest {
  private LogicPressboardSelectionRepository selections;
  private SettingListTargetService targets;
  private PressboardSelectionService service;
  private SoftPressboardListItemRepository softRepository;
  private HardPressboardListItemRepository hardRepository;
  private SoftPressboardListService softService;
  private HardPressboardListService hardService;

  @BeforeEach
  void 初始化() {
    selections = mock(LogicPressboardSelectionRepository.class);
    targets = mock(SettingListTargetService.class);
    service = new PressboardSelectionService(selections, targets);
    softRepository = mock(SoftPressboardListItemRepository.class);
    hardRepository = mock(HardPressboardListItemRepository.class);
    softService = new SoftPressboardListService(softRepository, targets, mock(SoftPressboardCatalogService.class), service);
    hardService = new HardPressboardListService(hardRepository, targets, mock(HardPressboardCatalogService.class), service);
    for (SettingListScopeType type : SettingListScopeType.values()) {
      when(targets.require(type, 10L)).thenReturn(new Target(type, 10L, "逻辑", 12L, "装置", 1L));
    }
  }

  @ParameterizedTest
  @EnumSource(value = SettingListScopeType.class, names = {"LOGIC_DIAGRAM", "LOGIC_GROUP"})
  void 两类逻辑都使用装置基准且勾选互相独立(SettingListScopeType type) {
    SoftPressboardListItem soft = new SoftPressboardListItem();
    soft.setPressboardRef("A"); soft.setPressboardName("软压板"); soft.setBaselineValue(true); soft.setCompareEnabled(false);
    HardPressboardListItem hard = new HardPressboardListItem();
    hard.setPressboardRef("1"); hard.setPressboardName("硬压板"); hard.setBaselineValue(false); hard.setCompareEnabled(true);
    when(softRepository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.IED_DEVICE, 12L))
        .thenReturn(Collections.singletonList(soft));
    when(hardRepository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.IED_DEVICE, 12L))
        .thenReturn(Collections.singletonList(hard));
    when(selections.findByPressboardKindAndScopeTypeAndScopeId(PressboardKind.SOFT, type, 10L))
        .thenReturn(Collections.singletonList(selection(PressboardKind.SOFT, type, "A")));
    SoftPressboardDtos.ListResponse sr = softService.get(type, 10L);
    HardPressboardDtos.ListResponse hr = hardService.get(type, 10L);
    assertTrue(sr.getConfiguredItems().isEmpty()); assertTrue(hr.getConfiguredItems().isEmpty());
    assertEquals(SettingListScopeType.IED_DEVICE, sr.getEffectiveScopeType());
    assertEquals(12L, hr.getEffectiveScopeId().longValue());
    assertTrue(sr.getEffectiveItems().get(0).isBaselineValue());
    assertTrue(sr.getEffectiveItems().get(0).isCompareEnabled());
    assertFalse(hr.getEffectiveItems().get(0).isCompareEnabled());
    assertFalse(soft.getCompareEnabled()); assertTrue(hard.getCompareEnabled());
    soft.setBaselineValue(false); hard.setBaselineValue(true);
    assertFalse(softService.get(type, 10L).getEffectiveItems().get(0).isBaselineValue());
    assertTrue(hardService.get(type, 10L).getEffectiveItems().get(0).isBaselineValue());
    verify(softRepository, never()).findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(type, 10L);
    verify(hardRepository, never()).findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(type, 10L);
  }

  @ParameterizedTest
  @EnumSource(PressboardKind.class)
  void 只保存引用且允许空选择(PressboardKind kind) {
    Set<String> available = new HashSet<>(Arrays.asList("A", "B"));
    service.replace(kind, SettingListScopeType.LOGIC_DIAGRAM, 10L, Arrays.asList("A", "A"), available);
    verify(selections).saveAll(argThat(rows -> {
      List<LogicPressboardSelection> list = new ArrayList<>(); rows.forEach(list::add);
      return list.size() == 1 && list.get(0).getPressboardRef().equals("A") && list.get(0).getPressboardKind() == kind;
    }));
    service.replace(kind, SettingListScopeType.LOGIC_GROUP, 10L, Collections.emptyList(), available);
    verify(selections).saveAll(Collections.emptyList());
  }

  @ParameterizedTest
  @EnumSource(PressboardKind.class)
  void 拒绝装置清单以外的引用和装置作用域选择(PressboardKind kind) {
    assertThrows(ResponseStatusException.class, () -> service.replace(kind, SettingListScopeType.LOGIC_DIAGRAM,
        10L, Collections.singletonList("外部引用"), Collections.singleton("A")));
    assertThrows(ResponseStatusException.class, () -> service.replace(kind, SettingListScopeType.IED_DEVICE,
        10L, Collections.emptyList(), Collections.emptySet()));
    verifyNoInteractions(selections);
  }

  @ParameterizedTest
  @EnumSource(PressboardKind.class)
  void 删除装置项目只清理对应引用并保留其余选择(PressboardKind kind) {
    when(targets.logicScopeIdsForDevice(12L)).thenReturn(Collections.singletonMap(
        SettingListScopeType.LOGIC_DIAGRAM, Collections.singletonList(10L)));
    LogicPressboardSelection keep = selection(kind, SettingListScopeType.LOGIC_DIAGRAM, "A");
    LogicPressboardSelection remove = selection(kind, SettingListScopeType.LOGIC_DIAGRAM, "B");
    when(selections.findByPressboardKindAndScopeTypeAndScopeIdIn(kind, SettingListScopeType.LOGIC_DIAGRAM, Collections.singletonList(10L)))
        .thenReturn(Arrays.asList(keep, remove));
    service.removeMissing(kind, 12L, Collections.singleton("A"));
    verify(selections).deleteAll(Collections.singletonList(remove));
    verify(selections, never()).saveAll(any());
  }

  @ParameterizedTest
  @EnumSource(value = SettingListScopeType.class, names = {"LOGIC_DIAGRAM", "LOGIC_GROUP"})
  void 逻辑层禁止维护独立压板清单(SettingListScopeType type) {
    assertThrows(ResponseStatusException.class, () -> softService.replace(type, 10L, Collections.emptyList()));
    assertThrows(ResponseStatusException.class, () -> hardService.replace(type, 10L, Collections.emptyList()));
    assertThrows(ResponseStatusException.class, () -> softService.clear(type, 10L));
    assertThrows(ResponseStatusException.class, () -> hardService.clear(type, 10L));
    assertThrows(ResponseStatusException.class, () -> new SoftPressboardListExcelService(softService).exportWorkbook(type, 10L));
    assertThrows(ResponseStatusException.class, () -> new HardPressboardListExcelService(hardService).importWorkbook(type, 10L, null));
    verifyNoInteractions(softRepository, hardRepository, selections);
  }

  @ParameterizedTest
  @EnumSource(value = SettingListScopeType.class, names = {"LOGIC_DIAGRAM", "LOGIC_GROUP"})
  void 全不勾选时联合预检跳过且不召唤压板(SettingListScopeType type) {
    SoftPressboardListItem soft = new SoftPressboardListItem(); soft.setPressboardRef("A"); soft.setCompareEnabled(true);
    HardPressboardListItem hard = new HardPressboardListItem(); hard.setPressboardRef("1"); hard.setCompareEnabled(true);
    when(softRepository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.IED_DEVICE, 12L))
        .thenReturn(Collections.singletonList(soft));
    when(hardRepository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.IED_DEVICE, 12L))
        .thenReturn(Collections.singletonList(hard));
    IedSoftPressboardStatusClient softClient = mock(IedSoftPressboardStatusClient.class);
    HardPressboardStatusClient hardClient = mock(HardPressboardStatusClient.class);
    MmsSettingClient mms = mock(MmsSettingClient.class);
    SettingComparisonService settingComparison = mock(SettingComparisonService.class);
    SettingCheckResponse skippedSetting = mock(SettingCheckResponse.class);
    when(skippedSetting.getStatus()).thenReturn("SKIPPED");
    when(settingComparison.skipped(null)).thenReturn(skippedSetting);
    WiringComparisonService wiringComparison = mock(WiringComparisonService.class);
    WiringRequirementDtos.CheckResponse skippedWiring = mock(WiringRequirementDtos.CheckResponse.class);
    when(skippedWiring.getStatus()).thenReturn("SKIPPED");
    when(wiringComparison.skipped(null)).thenReturn(skippedWiring);
    ExperimentPrecheckService precheck = new ExperimentPrecheckService(mock(SettingListService.class), softService, hardService,
        mock(WiringRequirementService.class), settingComparison,
        new SoftPressboardComparisonService(targets, mock(SoftPressboardCatalogService.class), softClient),
        new HardPressboardComparisonService(targets, mock(HardPressboardCatalogService.class), hardClient), wiringComparison, mms);
    assertEquals("SKIPPED", (type == SettingListScopeType.LOGIC_DIAGRAM ? precheck.check(10L) : precheck.checkForGroup(10L)).getStatus());
    verifyNoInteractions(softClient, hardClient, mms);
  }

  private LogicPressboardSelection selection(PressboardKind kind, SettingListScopeType type, String ref) {
    LogicPressboardSelection row = new LogicPressboardSelection();
    row.setPressboardKind(kind); row.setScopeType(type); row.setScopeId(10L); row.setPressboardRef(ref); return row;
  }
}
