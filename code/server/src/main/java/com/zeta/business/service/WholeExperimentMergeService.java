package com.zeta.business.service;

import com.zeta.business.entities.experiment.ExperimentPrecheckResponse;
import com.zeta.business.entities.experimentguide.*;
import com.zeta.business.entities.experimentguide.dto.ExperimentGuideItemStudentResponse;
import com.zeta.business.entities.settinglist.*;
import com.zeta.business.entities.settinglist.dto.SettingListItemResponse;
import com.zeta.business.entities.softpressboardlist.SoftPressboardListItem;
import com.zeta.business.entities.hardpressboardlist.HardPressboardListItem;
import com.zeta.business.entities.wiringrequirement.*;
import com.zeta.business.entities.wholeexperiment.WholeExperimentGuideItem;
import com.zeta.business.service.SettingListService.ResolvedSettingList;
import com.zeta.business.service.SoftPressboardListService.ResolvedSoftPressboardList;
import com.zeta.business.service.HardPressboardListService.ResolvedHardPressboardList;
import com.zeta.business.service.WiringRequirementService.ResolvedWiringRequirement;
import com.zeta.screen.terminal.Terminal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WholeExperimentMergeService {
  private final SettingListService settings;
  private final SoftPressboardListService soft;
  private final HardPressboardListService hard;
  private final WiringRequirementService wiring;
  private final ExperimentGuideService guides;
  private final ExperimentPrecheckService prechecks;

  public ExperimentPrecheckResponse check(List<Long> ids) {
    return prechecks.runCheck(mergeSettings(ids), mergeSoft(ids), mergeHard(ids),
        mergeWiring(ids.stream().map(wiring::resolveForLogic).collect(Collectors.toList())));
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public ResolvedSettingList mergeSettings(List<Long> ids) {
    List<ResolvedSettingList> lists = ids.stream().map(settings::resolveForLogic).collect(Collectors.toList());
    ResolvedSettingList first = lists.get(0);
    List<SettingListItem> union = selectedUnion(lists.stream().map(ResolvedSettingList::getItems)
        .collect(Collectors.toList()), SettingListItem::getSettingRef, SettingListItem::getCompareEnabled);
    union.sort(Comparator.comparing(SettingListItem::getSortOrder, Comparator.nullsLast(Integer::compareTo)));
    return new ResolvedSettingList(first.getTarget(), first.getEffectiveScopeType(),
        first.getEffectiveScopeId(), union);
  }

  private ResolvedSoftPressboardList mergeSoft(List<Long> ids) {
    List<ResolvedSoftPressboardList> lists = ids.stream().map(soft::resolveForLogic).collect(Collectors.toList());
    ResolvedSoftPressboardList first = lists.get(0);
    return new ResolvedSoftPressboardList(first.getTarget(), first.getEffectiveScopeType(),
        first.getEffectiveScopeId(), selectedUnion(lists.stream().map(ResolvedSoftPressboardList::getItems)
            .collect(Collectors.toList()), SoftPressboardListItem::getPressboardRef,
            SoftPressboardListItem::getCompareEnabled));
  }

  private ResolvedHardPressboardList mergeHard(List<Long> ids) {
    List<ResolvedHardPressboardList> lists = ids.stream().map(hard::resolveForLogic).collect(Collectors.toList());
    ResolvedHardPressboardList first = lists.get(0);
    return new ResolvedHardPressboardList(first.getTarget(), first.getEffectiveScopeType(),
        first.getEffectiveScopeId(), selectedUnion(lists.stream().map(ResolvedHardPressboardList::getItems)
            .collect(Collectors.toList()), HardPressboardListItem::getPressboardRef,
            HardPressboardListItem::getCompareEnabled));
  }

  /** 只取勾选项，使用装置清单中的首次出现顺序；不修改输入对象。 */
  static <T> List<T> selectedUnion(List<List<T>> lists, Function<T, String> ref, Function<T, Boolean> enabled) {
    Set<String> selected = new HashSet<>();
    for (List<T> list : lists) {
      for (T item : list) if (Boolean.TRUE.equals(enabled.apply(item))) selected.add(ref.apply(item));
    }
    Map<String, T> result = new LinkedHashMap<>();
    for (List<T> list : lists) {
      for (T item : list) {
        if (selected.contains(ref.apply(item))) {
          // 勾选视图取启用的对象，避免后续比较器误跳过同一引用。
          if (!result.containsKey(ref.apply(item))) result.put(ref.apply(item), item);
          if (Boolean.TRUE.equals(enabled.apply(item))) result.put(ref.apply(item), item);
        }
      }
    }
    return new ArrayList<>(result.values());
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public List<ExperimentGuideItemStudentResponse> guide(List<Long> ids) {
    List<List<ExperimentGuideItemStudentResponse>> parts = ids.stream()
        .map(id -> guides.listEnabledByScope(SettingListScopeType.LOGIC_DIAGRAM, id).stream()
            .filter(ExperimentGuideItemStudentResponse::isShowInWholeExperiment)
            .map(item -> (ExperimentGuideItemStudentResponse) new WholeExperimentGuideItem(item, id, item.getSettingItems()))
            .collect(Collectors.toList()))
        .collect(Collectors.toList());
    List<SettingListItemResponse> selected = mergeSettings(ids).getItems().stream()
        .map(SettingListItemResponse::from).collect(Collectors.toList());
    return mergeGuide(parts, selected);
  }

  static List<ExperimentGuideItemStudentResponse> mergeGuide(
      List<List<ExperimentGuideItemStudentResponse>> parts, List<SettingListItemResponse> selected) {
    ExperimentGuideItemStudentResponse retained = null;
    boolean belongsToLast = false;
    for (int i = 0; i < parts.size(); i++) {
      for (ExperimentGuideItemStudentResponse item : parts.get(i)) {
        if (item.getType() == ExperimentGuideType.SETTING_LIST) {
          retained = item;
          belongsToLast = i == parts.size() - 1;
        }
      }
    }
    List<ExperimentGuideItemStudentResponse> result = new ArrayList<>();
    for (List<ExperimentGuideItemStudentResponse> part : parts) {
      for (ExperimentGuideItemStudentResponse item : part) {
        if (item.getType() != ExperimentGuideType.SETTING_LIST) result.add(item);
        else if (item == retained && belongsToLast) result.add(withSettings(item, selected));
      }
    }
    if (retained != null && !belongsToLast) result.add(withSettings(retained, selected));
    return result;
  }

  private static ExperimentGuideItemStudentResponse withSettings(
      ExperimentGuideItemStudentResponse item, List<SettingListItemResponse> selected) {
    if (item instanceof WholeExperimentGuideItem) {
      return new WholeExperimentGuideItem(item, ((WholeExperimentGuideItem) item).getSourceLogicDiagramId(), selected);
    }
    return new ExperimentGuideItemStudentResponse(true, item.getId(), item.getType(),
        item.getTitle(), item.getContent(), item.isHasImage(), item.getSortOrder(), selected);
  }

  /** 相同端子映射去重；三相仅覆盖属于它的单相映射，不丢弃其他端子组。 */
  static ResolvedWiringRequirement mergeWiring(List<ResolvedWiringRequirement> inputs) {
    List<WireGroup> merged = new ArrayList<>();
    Map<Long, Terminal> terminals = new LinkedHashMap<>();
    for (ResolvedWiringRequirement input : inputs) {
      terminals.putAll(input.getTerminalsById());
      for (WiringRequirementConfig config : input.getRequiredConfigs()) {
        for (WiringRequirementGroup group : input.getGroupsByConfigId()
            .getOrDefault(config.getId(), Collections.emptyList())) {
          WireGroup next = new WireGroup(config.getCategory(), config.getPhaseMode(), group);
          if (merged.stream().anyMatch(existing -> covers(existing, next))) continue;
          merged.removeIf(existing -> covers(next, existing));
          merged.add(next);
        }
      }
    }
    List<WiringRequirementConfig> configs = new ArrayList<>();
    Map<Long, List<WiringRequirementGroup>> groups = new LinkedHashMap<>();
    for (WireGroup item : merged) {
      // 合成独立对象及临时键，避免数据库 ID 或各逻辑组号相互碰撞。
      long key = -(configs.size() + 1L);
      WiringRequirementConfig config = new WiringRequirementConfig();
      config.setId(key);
      config.setCategory(item.category);
      config.setPhaseMode(item.mode);
      config.setRequired(true);
      configs.add(config);
      WiringRequirementGroup copy = new WiringRequirementGroup();
      copy.setConfigId(key);
      copy.setGroupNo((int) -key - 1);
      copy.setTerminalAId(item.group.getTerminalAId());
      copy.setTerminalBId(item.group.getTerminalBId());
      copy.setTerminalCId(item.group.getTerminalCId());
      copy.setTerminalNId(item.group.getTerminalNId());
      groups.put(key, Collections.singletonList(copy));
    }
    return new ResolvedWiringRequirement(inputs.get(0).getTarget(), configs, groups, terminals);
  }

  private static boolean covers(WireGroup a, WireGroup b) {
    if (a.category != b.category) return false;
    Long[] left = terminalIds(a.group), right = terminalIds(b.group);
    if (a.mode == b.mode) return Arrays.equals(left, right);
    if (a.mode != PhaseMode.THREE_PHASE || b.mode != PhaseMode.SINGLE_PHASE) return false;
    for (int i = 0; i < left.length; i++) {
      if (right[i] != null && !Objects.equals(left[i], right[i])) return false;
    }
    return true;
  }

  private static Long[] terminalIds(WiringRequirementGroup group) {
    return new Long[] {group.getTerminalAId(), group.getTerminalBId(),
        group.getTerminalCId(), group.getTerminalNId()};
  }

  private static class WireGroup {
    final WiringCategory category;
    final PhaseMode mode;
    final WiringRequirementGroup group;
    WireGroup(WiringCategory category, PhaseMode mode, WiringRequirementGroup group) {
      this.category = category;
      this.mode = mode;
      this.group = group;
    }
  }
}
