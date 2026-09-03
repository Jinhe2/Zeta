package com.zeta.business.service;

import com.zeta.business.entities.experimentguide.*;
import com.zeta.business.entities.experimentguide.dto.*;
import com.zeta.business.entities.settinglist.*;
import com.zeta.business.entities.settinglist.dto.*;
import com.zeta.business.entities.wiringrequirement.*;
import com.zeta.business.service.WiringRequirementService.ResolvedWiringRequirement;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WholeExperimentMergeServiceTest {
  @Test void 引导入口过滤整组开关并保留来源及图文条目顺序() {
    SettingListService settings = mock(SettingListService.class);
    ExperimentGuideService guides = mock(ExperimentGuideService.class);
    when(settings.resolveForLogic(anyLong())).thenReturn(new SettingListService.ResolvedSettingList(
        null, null, null, Collections.singletonList(setting("甲", true))));
    ExperimentGuideItemStudentResponse hidden = new ExperimentGuideItemStudentResponse(false, 9L,
        ExperimentGuideType.SETTING_LIST, "不在整组显示", null, false, 1, null);
    when(guides.listEnabledByScope(SettingListScopeType.LOGIC_DIAGRAM, 1L))
        .thenReturn(Arrays.asList(hidden, guide(1L, false), guide(2L, true)));
    when(guides.listEnabledByScope(SettingListScopeType.LOGIC_DIAGRAM, 2L))
        .thenReturn(Collections.singletonList(guide(3L, false)));
    WholeExperimentMergeService service = new WholeExperimentMergeService(settings,
        mock(SoftPressboardListService.class), mock(HardPressboardListService.class),
        mock(WiringRequirementService.class), guides, mock(ExperimentPrecheckService.class));
    List<ExperimentGuideItemStudentResponse> result = service.guide(Arrays.asList(1L, 2L));
    assertEquals(Arrays.asList(1L, 3L, 2L), ids(result));
    assertEquals(1L, ((com.zeta.business.entities.wholeexperiment.WholeExperimentGuideItem) result.get(2))
        .getSourceLogicDiagramId());
    assertEquals(1, result.get(2).getSettingItems().size());
  }

  @Test void 并集去重保持装置顺序且不修改源对象() {
    SettingListItem a = setting("甲", false), b = setting("乙", true), a2 = setting("甲", true);
    List<SettingListItem> result = WholeExperimentMergeService.selectedUnion(
        Arrays.asList(Arrays.asList(a, b), Arrays.asList(a2, setting("乙", true))),
        SettingListItem::getSettingRef, SettingListItem::getCompareEnabled);
    assertEquals(Arrays.asList("甲", "乙"), result.stream().map(SettingListItem::getSettingRef).collect(Collectors.toList()));
    assertTrue(result.stream().allMatch(SettingListItem::getCompareEnabled));
    assertFalse(a.getCompareEnabled());
    assertEquals("10", result.get(0).getBaselineValue());
  }

  @Test void 空并集不回退装置全部校验项() {
    assertTrue(WholeExperimentMergeService.selectedUnion(
        Collections.singletonList(Collections.singletonList(setting("甲", false))),
        SettingListItem::getSettingRef, SettingListItem::getCompareEnabled).isEmpty());
  }

  @Test void 最后逻辑有定值时保留其最后条目的原位置() {
    List<ExperimentGuideItemStudentResponse> result = WholeExperimentMergeService.mergeGuide(Arrays.asList(
        Arrays.asList(guide(1, false), guide(2, true)),
        Arrays.asList(guide(3, true), guide(4, false), guide(5, true), guide(6, false))), Collections.emptyList());
    assertEquals(Arrays.asList(1L, 4L, 5L, 6L), ids(result));
  }

  @Test void 最后逻辑无定值时移动至其末尾且只展示并集() {
    List<SettingListItemResponse> selected = Collections.singletonList(SettingListItemResponse.from(setting("甲", true)));
    List<ExperimentGuideItemStudentResponse> result = WholeExperimentMergeService.mergeGuide(Arrays.asList(
        Arrays.asList(guide(1, true), guide(2, false)),
        Arrays.asList(guide(3, false), guide(4, false))), selected);
    assertEquals(Arrays.asList(2L, 3L, 4L, 1L), ids(result));
    assertEquals(selected, result.get(3).getSettingItems());
  }

  @Test void 所有定值隐藏时不自动生成定值引导() {
    assertEquals(Collections.singletonList(2L), ids(WholeExperimentMergeService.mergeGuide(
        Arrays.asList(Collections.emptyList(), Collections.singletonList(guide(2, false))), Collections.emptyList())));
  }

  @Test void 同组单相子集被三相覆盖且不修改输入() {
    ResolvedWiringRequirement single = wire(1L, WiringCategory.VOLTAGE, PhaseMode.SINGLE_PHASE, 1L, null, null, 4L);
    ResolvedWiringRequirement triple = wire(2L, WiringCategory.VOLTAGE, PhaseMode.THREE_PHASE, 1L, 2L, 3L, 4L);
    for (List<ResolvedWiringRequirement> inputs : Arrays.asList(Arrays.asList(single, triple), Arrays.asList(triple, single))) {
      ResolvedWiringRequirement result = WholeExperimentMergeService.mergeWiring(inputs);
      assertEquals(1, result.getConfigs().size());
      assertEquals(PhaseMode.THREE_PHASE, result.getConfigs().get(0).getPhaseMode());
    }
    assertEquals(1L, single.getConfigs().get(0).getId());
    assertEquals(PhaseMode.SINGLE_PHASE, single.getConfigs().get(0).getPhaseMode());
  }

  @Test void 不同端子组及电压电流分别保留() {
    ResolvedWiringRequirement result = WholeExperimentMergeService.mergeWiring(Arrays.asList(
        wire(1L, WiringCategory.VOLTAGE, PhaseMode.THREE_PHASE, 1L, 2L, 3L, 4L),
        wire(2L, WiringCategory.VOLTAGE, PhaseMode.SINGLE_PHASE, 10L, null, null, 40L),
        wire(3L, WiringCategory.CURRENT, PhaseMode.SINGLE_PHASE, 1L, null, null, 4L)));
    assertEquals(3, result.getRequiredConfigs().size());
    assertEquals(3, result.getGroupsByConfigId().size());
  }

  @Test void 完全重复去重但不覆盖不同单相映射() {
    ResolvedWiringRequirement a = wire(1L, WiringCategory.CURRENT, PhaseMode.SINGLE_PHASE, 1L, null, null, 4L);
    ResolvedWiringRequirement b = wire(2L, WiringCategory.CURRENT, PhaseMode.SINGLE_PHASE, null, 2L, null, 4L);
    assertEquals(2, WholeExperimentMergeService.mergeWiring(Arrays.asList(a, a, b)).getConfigs().size());
  }

  @Test void 不接线没有校验配置() {
    ResolvedWiringRequirement a = new ResolvedWiringRequirement(null, Collections.emptyList(),
        Collections.emptyMap(), Collections.emptyMap());
    assertTrue(WholeExperimentMergeService.mergeWiring(Arrays.asList(a, a)).getRequiredConfigs().isEmpty());
  }

  private SettingListItem setting(String ref, boolean enabled) {
    SettingListItem item = new SettingListItem(); item.setSettingRef(ref);
    item.setCompareEnabled(enabled); item.setBaselineValue("10"); return item;
  }
  private ExperimentGuideItemStudentResponse guide(long id, boolean setting) {
    return new ExperimentGuideItemStudentResponse(true, id,
        setting ? ExperimentGuideType.SETTING_LIST : ExperimentGuideType.IMAGE_TEXT,
        "条目" + id, "内容", !setting, (int) id, null);
  }
  private List<Long> ids(List<ExperimentGuideItemStudentResponse> items) {
    return items.stream().map(ExperimentGuideItemStudentResponse::getId).collect(Collectors.toList());
  }
  private ResolvedWiringRequirement wire(Long id, WiringCategory category, PhaseMode mode,
      Long a, Long b, Long c, Long n) {
    WiringRequirementConfig config = new WiringRequirementConfig(); config.setId(id);
    config.setCategory(category); config.setPhaseMode(mode); config.setRequired(true);
    WiringRequirementGroup group = new WiringRequirementGroup(); group.setGroupNo(0);
    group.setTerminalAId(a); group.setTerminalBId(b); group.setTerminalCId(c); group.setTerminalNId(n);
    return new ResolvedWiringRequirement(null, Collections.singletonList(config),
        Collections.singletonMap(id, Collections.singletonList(group)), Collections.emptyMap());
  }
}
