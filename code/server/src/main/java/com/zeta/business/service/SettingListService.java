package com.zeta.business.service;

import com.zeta.business.entities.settinglist.*;
import com.zeta.business.entities.settinglist.dto.*;
import com.zeta.business.service.SettingListTargetService.Target;
import com.zeta.screen.iedsetting.IedSettingItem;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SettingListService {
  private final SettingListItemRepository repository;
  private final SettingListTargetService targetService;
  private final SettingCatalogService catalogService;

  public SettingListService(
      SettingListItemRepository repository,
      SettingListTargetService targetService,
      SettingCatalogService catalogService) {
    this.repository = repository;
    this.targetService = targetService;
    this.catalogService = catalogService;
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public SettingListResponse get(SettingListScopeType scopeType, Long scopeId) {
    Target target = targetService.require(scopeType, scopeId);
    List<SettingListItem> configured = find(scopeType, scopeId);
    List<SettingListItem> effective = configured;
    SettingListScopeType effectiveType = configured.isEmpty() ? null : scopeType;
    Long effectiveId = configured.isEmpty() ? null : scopeId;
    boolean fallback = false;
    if ((scopeType == SettingListScopeType.LOGIC_DIAGRAM
            || scopeType == SettingListScopeType.LOGIC_GROUP)
        && configured.isEmpty()) {
      effective = find(SettingListScopeType.IED_DEVICE, target.getIedDeviceId());
      if (!effective.isEmpty()) {
        effectiveType = SettingListScopeType.IED_DEVICE;
        effectiveId = target.getIedDeviceId();
        fallback = true;
      }
    }
    return new SettingListResponse(
        scopeType,
        scopeId,
        target.getScopeName(),
        target.getIedDeviceId(),
        target.getIedName(),
        effectiveType,
        effectiveId,
        fallback,
        responses(configured),
        responses(effective));
  }

  @Transactional("businessTransactionManager")
  public SettingListResponse replace(
      SettingListScopeType scopeType, Long scopeId, List<SettingListSaveItemRequest> requests) {
    Target target = targetService.require(scopeType, scopeId);
    Map<String, IedSettingItem> catalog = catalogService.mapByReference(target.getIedDeviceId());
    List<SettingListItem> replacements = validateAndBuild(scopeType, scopeId, requests, catalog);
    repository.deleteByScopeTypeAndScopeId(scopeType, scopeId);
    repository.flush();
    repository.saveAll(replacements);
    repository.flush();
    return get(scopeType, scopeId);
  }

  @Transactional("businessTransactionManager")
  public SettingListResponse clear(SettingListScopeType scopeType, Long scopeId) {
    targetService.require(scopeType, scopeId);
    repository.deleteByScopeTypeAndScopeId(scopeType, scopeId);
    repository.flush();
    return get(scopeType, scopeId);
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public ResolvedSettingList resolveForDevice(Long iedDeviceId) {
    Target target = targetService.require(SettingListScopeType.IED_DEVICE, iedDeviceId);
    List<SettingListItem> items = find(SettingListScopeType.IED_DEVICE, iedDeviceId);
    return new ResolvedSettingList(
        target,
        items.isEmpty() ? null : SettingListScopeType.IED_DEVICE,
        items.isEmpty() ? null : iedDeviceId,
        items);
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public ResolvedSettingList resolveForLogic(Long logicDiagramId) {
    Target target = targetService.require(SettingListScopeType.LOGIC_DIAGRAM, logicDiagramId);
    List<SettingListItem> items = find(SettingListScopeType.LOGIC_DIAGRAM, logicDiagramId);
    SettingListScopeType type = SettingListScopeType.LOGIC_DIAGRAM;
    Long id = logicDiagramId;
    if (items.isEmpty()) {
      items = find(SettingListScopeType.IED_DEVICE, target.getIedDeviceId());
      type = items.isEmpty() ? null : SettingListScopeType.IED_DEVICE;
      id = items.isEmpty() ? null : target.getIedDeviceId();
    }
    return new ResolvedSettingList(target, type, id, items);
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public ResolvedSettingList resolveForGroup(Long groupId) {
    Target target = targetService.require(SettingListScopeType.LOGIC_GROUP, groupId);
    List<SettingListItem> items = find(SettingListScopeType.LOGIC_GROUP, groupId);
    SettingListScopeType type = SettingListScopeType.LOGIC_GROUP;
    Long id = groupId;
    if (items.isEmpty()) {
      items = find(SettingListScopeType.IED_DEVICE, target.getIedDeviceId());
      type = items.isEmpty() ? null : SettingListScopeType.IED_DEVICE;
      id = items.isEmpty() ? null : target.getIedDeviceId();
    }
    return new ResolvedSettingList(target, type, id, items);
  }

  private List<SettingListItem> find(SettingListScopeType type, Long id) {
    return repository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(type, id);
  }

  private List<SettingListItem> validateAndBuild(
      SettingListScopeType scopeType,
      Long scopeId,
      List<SettingListSaveItemRequest> requests,
      Map<String, IedSettingItem> catalog) {
    if (requests == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "定值清单不能为空");
    }
    Set<String> seen = new HashSet<>();
    List<SettingListItem> result = new ArrayList<>();
    for (int index = 0; index < requests.size(); index++) {
      SettingListSaveItemRequest request = requests.get(index);
      String ref = request.getSettingRef() == null ? "" : request.getSettingRef().trim();
      if (!seen.add(ref)) {
        throw badRequest("第 " + (index + 1) + " 项定值引用重复：" + ref);
      }
      IedSettingItem catalogItem = catalog.get(ref);
      if (catalogItem == null) {
        throw badRequest("第 " + (index + 1) + " 项不属于当前装置：" + ref);
      }
      String valueType =
          SettingValueTypePolicy.effectiveType(
              catalogItem.getSettingRef(), catalogItem.getValueType());
      String value = validateValue(request.getBaselineValue(), valueType, index + 1);
      SettingListItem item = new SettingListItem();
      item.setScopeType(scopeType);
      item.setScopeId(scopeId);
      item.setSettingRef(catalogItem.getSettingRef());
      item.setSettingFc("SG");
      item.setSettingName(catalogItem.getSettingName());
      item.setValueType(valueType);
      item.setCompareEnabled(!Boolean.FALSE.equals(request.getCompareEnabled()));
      item.setBaselineValue(value);
      item.setSortOrder(request.getSortOrder() == null ? index : request.getSortOrder());
      result.add(item);
    }
    return result;
  }

  private String validateValue(String raw, String valueType, int row) {
    String value = raw == null ? "" : raw.trim();
    if (value.isEmpty() || value.length() > 64) {
      throw badRequest("第 " + row + " 项定值为空或长度超过 64 个字符");
    }
    try {
      BigDecimal number = new BigDecimal(value);
      if (!Double.isFinite(number.doubleValue())) {
        throw new NumberFormatException();
      }
      if ("INTEGER".equalsIgnoreCase(valueType) && number.stripTrailingZeros().scale() > 0) {
        throw badRequest("第 " + row + " 项为整数定值，不能包含小数");
      }
      return "INTEGER".equalsIgnoreCase(valueType)
          ? number.toBigIntegerExact().toString()
          : number.stripTrailingZeros().toPlainString();
    } catch (ArithmeticException | NumberFormatException ex) {
      throw badRequest("第 " + row + " 项定值不是合法数值");
    }
  }

  private List<SettingListItemResponse> responses(List<SettingListItem> items) {
    return items.stream().map(SettingListItemResponse::from).collect(Collectors.toList());
  }

  private ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  public static class ResolvedSettingList {
    private final Target target;
    private final SettingListScopeType effectiveScopeType;
    private final Long effectiveScopeId;
    private final List<SettingListItem> items;

    public ResolvedSettingList(
        Target target,
        SettingListScopeType effectiveScopeType,
        Long effectiveScopeId,
        List<SettingListItem> items) {
      this.target = target;
      this.effectiveScopeType = effectiveScopeType;
      this.effectiveScopeId = effectiveScopeId;
      this.items = items;
    }

    public Target getTarget() { return target; }
    public SettingListScopeType getEffectiveScopeType() { return effectiveScopeType; }
    public Long getEffectiveScopeId() { return effectiveScopeId; }
    public List<SettingListItem> getItems() { return items; }
  }
}
