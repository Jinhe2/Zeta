package com.zeta.business.service;

import com.zeta.business.entities.hardpressboardlist.*;
import com.zeta.business.entities.hardpressboardlist.HardPressboardDtos.*;
import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.service.SettingListTargetService.Target;
import com.zeta.screen.hardpressboard.HardPressboard;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HardPressboardListService {
  private final HardPressboardListItemRepository repository;
  private final SettingListTargetService targetService;
  private final HardPressboardCatalogService catalogService;

  public HardPressboardListService(
      HardPressboardListItemRepository repository,
      SettingListTargetService targetService,
      HardPressboardCatalogService catalogService) {
    this.repository = repository;
    this.targetService = targetService;
    this.catalogService = catalogService;
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public ListResponse get(SettingListScopeType scopeType, Long scopeId) {
    Target target = targetService.require(scopeType, scopeId);
    List<HardPressboardListItem> configured = find(scopeType, scopeId);
    List<HardPressboardListItem> effective = configured;
    SettingListScopeType effectiveType = configured.isEmpty() ? null : scopeType;
    Long effectiveId = configured.isEmpty() ? null : scopeId;
    boolean fallback = false;
    if (scopeType == SettingListScopeType.LOGIC_DIAGRAM && configured.isEmpty()) {
      effective = find(SettingListScopeType.IED_DEVICE, target.getIedDeviceId());
      if (!effective.isEmpty()) {
        effectiveType = SettingListScopeType.IED_DEVICE;
        effectiveId = target.getIedDeviceId();
        fallback = true;
      }
    }
    return new ListResponse(
        scopeType, scopeId, target.getScopeName(), target.getCabinetId(),
        effectiveType, effectiveId, fallback, responses(configured), responses(effective));
  }

  @Transactional("businessTransactionManager")
  public ListResponse replace(
      SettingListScopeType scopeType, Long scopeId, List<SaveItemRequest> requests) {
    Target target = targetService.require(scopeType, scopeId);
    Map<String, HardPressboard> catalog =
        catalogService.mapById(target.getCabinetId());
    List<HardPressboardListItem> replacements = validateAndBuild(scopeType, scopeId, requests, catalog);
    repository.deleteByScopeTypeAndScopeId(scopeType, scopeId);
    repository.flush();
    repository.saveAll(replacements);
    repository.flush();
    return get(scopeType, scopeId);
  }

  @Transactional("businessTransactionManager")
  public ListResponse clear(SettingListScopeType scopeType, Long scopeId) {
    targetService.require(scopeType, scopeId);
    repository.deleteByScopeTypeAndScopeId(scopeType, scopeId);
    repository.flush();
    return get(scopeType, scopeId);
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public ResolvedHardPressboardList resolveForLogic(Long logicDiagramId) {
    Target target = targetService.require(SettingListScopeType.LOGIC_DIAGRAM, logicDiagramId);
    List<HardPressboardListItem> items = find(SettingListScopeType.LOGIC_DIAGRAM, logicDiagramId);
    SettingListScopeType type = SettingListScopeType.LOGIC_DIAGRAM;
    Long id = logicDiagramId;
    if (items.isEmpty()) {
      items = find(SettingListScopeType.IED_DEVICE, target.getIedDeviceId());
      type = items.isEmpty() ? null : SettingListScopeType.IED_DEVICE;
      id = items.isEmpty() ? null : target.getIedDeviceId();
    }
    return new ResolvedHardPressboardList(target, type, id, items);
  }

  private List<HardPressboardListItem> find(SettingListScopeType type, Long id) {
    return repository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(type, id);
  }

  private List<HardPressboardListItem> validateAndBuild(
      SettingListScopeType scopeType, Long scopeId, List<SaveItemRequest> requests,
      Map<String, HardPressboard> catalog) {
    if (requests == null) throw badRequest("硬压板基准清单不能为空");
    Set<String> seen = new HashSet<>();
    List<HardPressboardListItem> result = new ArrayList<>();
    for (int index = 0; index < requests.size(); index++) {
      SaveItemRequest request = requests.get(index);
      String ref = request.getPressboardRef() == null ? "" : request.getPressboardRef().trim();
      if (ref.isEmpty()) throw badRequest("第 " + (index + 1) + " 项硬压板引用不能为空");
      if (!seen.add(ref)) throw badRequest("第 " + (index + 1) + " 项硬压板引用重复：" + ref);
      HardPressboard catalogItem = catalog.get(ref);
      if (catalogItem == null) throw badRequest("第 " + (index + 1) + " 项不属于当前屏柜：" + ref);
      if (request.getBaselineValue() == null) {
        throw badRequest("第 " + (index + 1) + " 项硬压板基准状态不能为空");
      }
      HardPressboardListItem item = new HardPressboardListItem();
      item.setScopeType(scopeType);
      item.setScopeId(scopeId);
      item.setPressboardRef(String.valueOf(catalogItem.getId()));
      item.setPressboardName(catalogItem.getName());
      item.setBaselineValue(request.getBaselineValue());
      item.setCompareEnabled(!Boolean.FALSE.equals(request.getCompareEnabled()));
      item.setSortOrder(request.getSortOrder() == null ? index : request.getSortOrder());
      result.add(item);
    }
    return result;
  }

  private List<ItemResponse> responses(List<HardPressboardListItem> items) {
    return items.stream().map(ItemResponse::from).collect(Collectors.toList());
  }

  private ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  public static class ResolvedHardPressboardList {
    private final Target target;
    private final SettingListScopeType effectiveScopeType;
    private final Long effectiveScopeId;
    private final List<HardPressboardListItem> items;

    public ResolvedHardPressboardList(
        Target target, SettingListScopeType effectiveScopeType, Long effectiveScopeId,
        List<HardPressboardListItem> items) {
      this.target = target;
      this.effectiveScopeType = effectiveScopeType;
      this.effectiveScopeId = effectiveScopeId;
      this.items = items;
    }

    public Target getTarget() { return target; }
    public SettingListScopeType getEffectiveScopeType() { return effectiveScopeType; }
    public Long getEffectiveScopeId() { return effectiveScopeId; }
    public List<HardPressboardListItem> getItems() { return items; }
  }
}
