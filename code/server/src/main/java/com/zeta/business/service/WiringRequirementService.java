package com.zeta.business.service;

import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.entities.wiringrequirement.*;
import com.zeta.business.entities.wiringrequirement.WiringRequirementDtos.*;
import com.zeta.business.service.SettingListTargetService.Target;
import com.zeta.screen.terminal.Terminal;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WiringRequirementService {
  private final WiringRequirementConfigRepository configRepository;
  private final WiringRequirementGroupRepository groupRepository;
  private final SettingListTargetService targetService;
  private final TerminalCatalogService terminalCatalogService;

  public WiringRequirementService(
      WiringRequirementConfigRepository configRepository,
      WiringRequirementGroupRepository groupRepository,
      SettingListTargetService targetService,
      TerminalCatalogService terminalCatalogService) {
    this.configRepository = configRepository;
    this.groupRepository = groupRepository;
    this.targetService = targetService;
    this.terminalCatalogService = terminalCatalogService;
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public GetResponse get(Long logicDiagramId) {
    Target target = targetService.require(SettingListScopeType.LOGIC_DIAGRAM, logicDiagramId);
    Resolved resolved = load(logicDiagramId);
    return new GetResponse(logicDiagramId, target.getScopeName(), target.getCabinetId(),
        Arrays.asList(
            categoryResponse(WiringCategory.VOLTAGE, resolved),
            categoryResponse(WiringCategory.CURRENT, resolved)));
  }

  @Transactional("businessTransactionManager")
  public GetResponse replace(Long logicDiagramId, SaveRequest request) {
    Target target = targetService.require(SettingListScopeType.LOGIC_DIAGRAM, logicDiagramId);
    List<BuiltConfig> built = validateAndBuild(target.getCabinetId(), request);
    deleteAll(logicDiagramId);
    for (BuiltConfig item : built) {
      item.config.setLogicDiagramId(logicDiagramId);
      WiringRequirementConfig saved = configRepository.save(item.config);
      for (WiringRequirementGroup group : item.groups) {
        group.setConfigId(saved.getId());
        groupRepository.save(group);
      }
    }
    configRepository.flush();
    return get(logicDiagramId);
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public ResolvedWiringRequirement resolveForLogic(Long logicDiagramId) {
    Target target = targetService.require(SettingListScopeType.LOGIC_DIAGRAM, logicDiagramId);
    Resolved resolved = load(logicDiagramId);
    return new ResolvedWiringRequirement(target, resolved.configs, resolved.groupsByConfigId,
        resolved.terminalsById);
  }

  private Resolved load(Long logicDiagramId) {
    List<WiringRequirementConfig> configs = configRepository.findByLogicDiagramIdOrderByIdAsc(logicDiagramId);
    Map<Long, List<WiringRequirementGroup>> groupsByConfigId = new LinkedHashMap<>();
    Set<Long> terminalIds = new LinkedHashSet<>();
    for (WiringRequirementConfig config : configs) {
      List<WiringRequirementGroup> groups = groupRepository.findByConfigIdOrderByGroupNoAscIdAsc(config.getId());
      groupsByConfigId.put(config.getId(), groups);
      for (WiringRequirementGroup group : groups) {
        addTerminal(terminalIds, group.getTerminalAId());
        addTerminal(terminalIds, group.getTerminalBId());
        addTerminal(terminalIds, group.getTerminalCId());
        addTerminal(terminalIds, group.getTerminalNId());
      }
    }
    Map<Long, Terminal> terminalsById = terminalCatalogService.byId(terminalIds);
    return new Resolved(configs, groupsByConfigId, terminalsById);
  }

  private void addTerminal(Set<Long> ids, Long id) {
    if (id != null) ids.add(id);
  }

  private CategoryResponse categoryResponse(WiringCategory category, Resolved resolved) {
    WiringRequirementConfig config = resolved.configs.stream()
        .filter(c -> c.getCategory() == category)
        .findFirst().orElse(null);
    if (config == null) {
      return new CategoryResponse(category, false, PhaseMode.THREE_PHASE, Collections.emptyList());
    }
    List<WiringRequirementGroup> groups = resolved.groupsByConfigId.getOrDefault(
        config.getId(), Collections.emptyList());
    List<GroupResponse> groupResponses = groups.stream()
        .map(g -> new GroupResponse(g.getGroupNo(),
            terminalRef(g.getTerminalAId(), resolved),
            terminalRef(g.getTerminalBId(), resolved),
            terminalRef(g.getTerminalCId(), resolved),
            terminalRef(g.getTerminalNId(), resolved)))
        .collect(Collectors.toList());
    return new CategoryResponse(category, Boolean.TRUE.equals(config.getRequired()),
        config.getPhaseMode(), groupResponses);
  }

  private TerminalRef terminalRef(Long terminalId, Resolved resolved) {
    if (terminalId == null) return null;
    Terminal terminal = resolved.terminalsById.get(terminalId);
    if (terminal == null) return new TerminalRef(terminalId, null, null, null);
    return new TerminalRef(terminalId, terminal.getTerminalLabel(),
        terminal.getTerminalStrip() == null ? null : terminal.getTerminalStrip().getId(),
        terminal.getTerminalStrip() == null ? null : terminal.getTerminalStrip().getName());
  }

  private List<BuiltConfig> validateAndBuild(Long cabinetId, SaveRequest request) {
    if (request == null || request.getCategories() == null || request.getCategories().isEmpty()) {
      return Collections.emptyList();
    }
    Map<WiringCategory, CategoryRequest> byCategory = new LinkedHashMap<>();
    for (CategoryRequest categoryRequest : request.getCategories()) {
      if (categoryRequest == null) continue;
      WiringCategory category = categoryRequest.getCategory();
      if (category == null) throw badRequest("接线类别不能为空");
      if (byCategory.containsKey(category)) throw badRequest("接线类别重复：" + category);
      byCategory.put(category, categoryRequest);
    }

    // 收集所有需校验的端子，批量读取并校验归属。
    Set<Long> referencedIds = new LinkedHashSet<>();
    for (Map.Entry<WiringCategory, CategoryRequest> entry : byCategory.entrySet()) {
      if (!Boolean.TRUE.equals(entry.getValue().getRequired())) continue;
      List<GroupRequest> groups = entry.getValue().getGroups();
      if (groups == null || groups.isEmpty()) {
        throw badRequest(categoryLabel(entry.getKey()) + "需要接入时必须至少配置一组端子");
      }
      for (GroupRequest group : groups) {
        collectGroupTerminals(referencedIds, group);
      }
    }
    Map<Long, Terminal> terminalsById = terminalCatalogService.byId(referencedIds);

    List<BuiltConfig> result = new ArrayList<>();
    for (Map.Entry<WiringCategory, CategoryRequest> entry : byCategory.entrySet()) {
      WiringCategory category = entry.getKey();
      CategoryRequest categoryRequest = entry.getValue();
      if (!Boolean.TRUE.equals(categoryRequest.getRequired())) continue;

      PhaseMode mode = categoryRequest.getPhaseMode();
      if (mode == null) throw badRequest(categoryLabel(category) + "接线方式不能为空");

      WiringRequirementConfig config = new WiringRequirementConfig();
      config.setCategory(category);
      config.setRequired(true);
      config.setPhaseMode(mode);
      List<WiringRequirementGroup> groups = new ArrayList<>();
      int index = 0;
      for (GroupRequest groupRequest : categoryRequest.getGroups()) {
        groups.add(buildGroup(cabinetId, category, mode, index, groupRequest, terminalsById));
        index++;
      }
      result.add(new BuiltConfig(config, groups));
    }
    return result;
  }

  private WiringRequirementGroup buildGroup(
      Long cabinetId, WiringCategory category, PhaseMode mode, int index,
      GroupRequest request, Map<Long, Terminal> terminalsById) {
    if (request == null) throw badRequest(categoryLabel(category) + "第 " + (index + 1) + " 组不能为空");
    Long a = request.getTerminalAId();
    Long b = request.getTerminalBId();
    Long c = request.getTerminalCId();
    Long n = request.getTerminalNId();
    if (mode == PhaseMode.THREE_PHASE) {
      if (a == null || b == null || c == null || n == null) {
        throw badRequest(categoryLabel(category) + "第 " + (index + 1) + " 组三相接线必须配置 A、B、C、N 四相端子");
      }
    } else {
      if (n == null || (a == null && b == null && c == null)) {
        throw badRequest(categoryLabel(category) + "第 " + (index + 1) + " 组单相接线必须配置 N 相端子以及 A、B、C 中至少一相端子");
      }
    }
    List<Long> filled = new ArrayList<>();
    for (Long id : new Long[] {a, b, c, n}) {
      if (id != null) filled.add(id);
    }
    Set<Long> distinct = new HashSet<>(filled);
    if (distinct.size() != filled.size()) {
      throw badRequest(categoryLabel(category) + "第 " + (index + 1) + " 组内端子不能重复");
    }
    for (Long id : filled) {
      Terminal terminal = terminalsById.get(id);
      if (terminal == null) throw badRequest(categoryLabel(category) + "第 " + (index + 1) + " 组关联端子不存在：" + id);
      if (terminal.getCabinet() == null || !cabinetId.equals(terminal.getCabinet().getId())) {
        throw badRequest(categoryLabel(category) + "第 " + (index + 1) + " 组关联端子不属于当前屏柜：" + id);
      }
    }

    WiringRequirementGroup group = new WiringRequirementGroup();
    group.setGroupNo(index);
    group.setTerminalAId(a);
    group.setTerminalBId(b);
    group.setTerminalCId(c);
    group.setTerminalNId(n);
    return group;
  }

  private void collectGroupTerminals(Set<Long> ids, GroupRequest group) {
    if (group == null) return;
    for (Long id : new Long[] {group.getTerminalAId(), group.getTerminalBId(),
        group.getTerminalCId(), group.getTerminalNId()}) {
      if (id != null) ids.add(id);
    }
  }

  private void deleteAll(Long logicDiagramId) {
    List<WiringRequirementConfig> existing = configRepository.findByLogicDiagramIdOrderByIdAsc(logicDiagramId);
    for (WiringRequirementConfig config : existing) {
      groupRepository.deleteByConfigId(config.getId());
    }
    configRepository.deleteByLogicDiagramId(logicDiagramId);
    configRepository.flush();
  }

  private String categoryLabel(WiringCategory category) {
    return category == WiringCategory.VOLTAGE ? "电压" : "电流";
  }

  private ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  private static class BuiltConfig {
    final WiringRequirementConfig config;
    final List<WiringRequirementGroup> groups;

    BuiltConfig(WiringRequirementConfig config, List<WiringRequirementGroup> groups) {
      this.config = config;
      this.groups = groups;
    }
  }

  private static class Resolved {
    final List<WiringRequirementConfig> configs;
    final Map<Long, List<WiringRequirementGroup>> groupsByConfigId;
    final Map<Long, Terminal> terminalsById;

    Resolved(List<WiringRequirementConfig> configs,
        Map<Long, List<WiringRequirementGroup>> groupsByConfigId,
        Map<Long, Terminal> terminalsById) {
      this.configs = configs;
      this.groupsByConfigId = groupsByConfigId;
      this.terminalsById = terminalsById;
    }
  }

  public static class ResolvedWiringRequirement {
    private final Target target;
    private final List<WiringRequirementConfig> configs;
    private final Map<Long, List<WiringRequirementGroup>> groupsByConfigId;
    private final Map<Long, Terminal> terminalsById;

    public ResolvedWiringRequirement(
        Target target, List<WiringRequirementConfig> configs,
        Map<Long, List<WiringRequirementGroup>> groupsByConfigId,
        Map<Long, Terminal> terminalsById) {
      this.target = target;
      this.configs = configs;
      this.groupsByConfigId = groupsByConfigId;
      this.terminalsById = terminalsById;
    }

    public Target getTarget() { return target; }
    public List<WiringRequirementConfig> getConfigs() { return configs; }
    public Map<Long, List<WiringRequirementGroup>> getGroupsByConfigId() { return groupsByConfigId; }
    public Map<Long, Terminal> getTerminalsById() { return terminalsById; }

    public List<WiringRequirementConfig> getRequiredConfigs() {
      return configs.stream()
          .filter(c -> Boolean.TRUE.equals(c.getRequired()))
          .collect(Collectors.toList());
    }
  }
}
