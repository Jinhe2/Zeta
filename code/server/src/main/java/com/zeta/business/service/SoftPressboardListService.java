package com.zeta.business.service;

import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.entities.softpressboardlist.*;
import com.zeta.business.entities.softpressboardlist.SoftPressboardDtos.*;
import com.zeta.business.service.SettingListTargetService.Target;
import com.zeta.business.entities.pressboardselection.PressboardKind;
import com.zeta.screen.softpressboard.IedSoftPressboardItem;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SoftPressboardListService {
  private final SoftPressboardListItemRepository repository;
  private final SettingListTargetService targetService;
  private final SoftPressboardCatalogService catalogService;
  private final PressboardSelectionService selectionService;

  public SoftPressboardListService(
      SoftPressboardListItemRepository repository,
      SettingListTargetService targetService,
      SoftPressboardCatalogService catalogService, PressboardSelectionService selectionService) {
    this.repository = repository;
    this.targetService = targetService;
    this.catalogService = catalogService;
    this.selectionService = selectionService;
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public ListResponse get(SettingListScopeType scopeType, Long scopeId) {
    Target target = targetService.require(scopeType, scopeId);
    boolean device = scopeType == SettingListScopeType.IED_DEVICE;
    List<SoftPressboardListItem> items = device ? find(scopeType, scopeId) : resolveForScope(scopeType, scopeId).getItems();
    return new ListResponse(scopeType, scopeId, target.getScopeName(), target.getIedDeviceId(), target.getIedName(),
        SettingListScopeType.IED_DEVICE, target.getIedDeviceId(), false,
        device ? responses(items) : Collections.emptyList(), responses(items));
  }

  @Transactional("businessTransactionManager")
  public ListResponse replace(
      SettingListScopeType scopeType, Long scopeId, List<SaveItemRequest> requests) {
    PressboardSelectionService.requireDeviceScope(scopeType);
    Target target = targetService.require(scopeType, scopeId);
    Map<String, IedSoftPressboardItem> catalog =
        catalogService.mapByReference(target.getIedDeviceId());
    List<SoftPressboardListItem> replacements = validateAndBuild(scopeType, scopeId, requests, catalog);
    repository.deleteByScopeTypeAndScopeId(scopeType, scopeId);
    repository.flush();
    repository.saveAll(replacements);
    selectionService.removeMissing(PressboardKind.SOFT, scopeId,
        replacements.stream().map(SoftPressboardListItem::getPressboardRef).collect(Collectors.toSet()));
    repository.flush();
    return get(scopeType, scopeId);
  }

  @Transactional("businessTransactionManager")
  public ListResponse clear(SettingListScopeType scopeType, Long scopeId) {
    PressboardSelectionService.requireDeviceScope(scopeType);
    targetService.require(scopeType, scopeId);
    selectionService.removeMissing(PressboardKind.SOFT, scopeId, Collections.emptySet());
    repository.deleteByScopeTypeAndScopeId(scopeType, scopeId);
    repository.flush();
    return get(scopeType, scopeId);
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public ResolvedSoftPressboardList resolveForLogic(Long logicDiagramId) {
    return resolveForScope(SettingListScopeType.LOGIC_DIAGRAM, logicDiagramId);
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public ResolvedSoftPressboardList resolveForGroup(Long groupId) {
    return resolveForScope(SettingListScopeType.LOGIC_GROUP, groupId);
  }

  private ResolvedSoftPressboardList resolveForScope(SettingListScopeType type, Long id) {
    Target target = targetService.require(type, id);
    Set<String> selected = selectionService.selected(PressboardKind.SOFT, type, id);
    // 创建非托管视图，逻辑的勾选不得修改装置实体。
    List<SoftPressboardListItem> items = find(SettingListScopeType.IED_DEVICE, target.getIedDeviceId()).stream()
        .map(source -> {
          SoftPressboardListItem item = new SoftPressboardListItem();
          item.setPressboardRef(source.getPressboardRef());
          item.setPressboardName(source.getPressboardName());
          item.setBaselineValue(source.getBaselineValue());
          item.setSortOrder(source.getSortOrder());
          item.setCompareEnabled(selected.contains(source.getPressboardRef()));
          return item;
        }).collect(Collectors.toList());
    return new ResolvedSoftPressboardList(target, SettingListScopeType.IED_DEVICE, target.getIedDeviceId(), items);
  }

  @Transactional("businessTransactionManager")
  public ListResponse saveSelection(SettingListScopeType type, Long id, List<String> refs) {
    Target target = targetService.require(type, id);
    Set<String> available = find(SettingListScopeType.IED_DEVICE, target.getIedDeviceId()).stream()
        .map(SoftPressboardListItem::getPressboardRef).collect(Collectors.toSet());
    selectionService.replace(PressboardKind.SOFT, type, id, refs, available);
    return get(type, id);
  }

  private List<SoftPressboardListItem> find(SettingListScopeType type, Long id) {
    return repository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(type, id);
  }

  private List<SoftPressboardListItem> validateAndBuild(
      SettingListScopeType scopeType, Long scopeId, List<SaveItemRequest> requests,
      Map<String, IedSoftPressboardItem> catalog) {
    if (requests == null) throw badRequest("软压板基准清单不能为空");
    Set<String> seen = new HashSet<>();
    List<SoftPressboardListItem> result = new ArrayList<>();
    for (int index = 0; index < requests.size(); index++) {
      SaveItemRequest request = requests.get(index);
      String ref = request.getPressboardRef() == null ? "" : request.getPressboardRef().trim();
      if (ref.isEmpty()) throw badRequest("第 " + (index + 1) + " 项软压板引用不能为空");
      if (!seen.add(ref)) throw badRequest("第 " + (index + 1) + " 项软压板引用重复：" + ref);
      IedSoftPressboardItem catalogItem = catalog.get(ref);
      if (catalogItem == null) throw badRequest("第 " + (index + 1) + " 项不属于当前装置：" + ref);
      if (request.getBaselineValue() == null) {
        throw badRequest("第 " + (index + 1) + " 项软压板基准状态不能为空");
      }
      SoftPressboardListItem item = new SoftPressboardListItem();
      item.setScopeType(scopeType);
      item.setScopeId(scopeId);
      item.setPressboardRef(catalogItem.getPressboardRef());
      item.setPressboardName(catalogItem.getPressboardName());
      item.setBaselineValue(request.getBaselineValue());
      item.setCompareEnabled(!Boolean.FALSE.equals(request.getCompareEnabled()));
      item.setSortOrder(request.getSortOrder() == null ? index : request.getSortOrder());
      result.add(item);
    }
    return result;
  }

  private List<ItemResponse> responses(List<SoftPressboardListItem> items) {
    return items.stream().map(ItemResponse::from).collect(Collectors.toList());
  }

  private ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  public static class ResolvedSoftPressboardList {
    private final Target target;
    private final SettingListScopeType effectiveScopeType;
    private final Long effectiveScopeId;
    private final List<SoftPressboardListItem> items;

    public ResolvedSoftPressboardList(
        Target target, SettingListScopeType effectiveScopeType, Long effectiveScopeId,
        List<SoftPressboardListItem> items) {
      this.target = target;
      this.effectiveScopeType = effectiveScopeType;
      this.effectiveScopeId = effectiveScopeId;
      this.items = items;
    }

    public Target getTarget() { return target; }
    public SettingListScopeType getEffectiveScopeType() { return effectiveScopeType; }
    public Long getEffectiveScopeId() { return effectiveScopeId; }
    public List<SoftPressboardListItem> getItems() { return items; }
  }
}
