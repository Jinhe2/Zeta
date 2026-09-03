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
  private final LogicSettingSelectionRepository selectionRepository;

  public SettingListService(
      SettingListItemRepository repository,
      SettingListTargetService targetService,
      SettingCatalogService catalogService,
      LogicSettingSelectionRepository selectionRepository) {
    this.repository = repository;
    this.targetService = targetService;
    this.catalogService = catalogService;
    this.selectionRepository = selectionRepository;
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public SettingListResponse get(SettingListScopeType scopeType, Long scopeId) {
    Target target = targetService.require(scopeType, scopeId);
    boolean device = scopeType == SettingListScopeType.IED_DEVICE;
    ResolvedSettingList resolved = device ? resolveForDevice(scopeId) : resolveForLogicScope(scopeType, scopeId);
    return new SettingListResponse(scopeType, scopeId, target.getScopeName(),
        target.getIedDeviceId(), target.getIedName(), resolved.getEffectiveScopeType(),
        resolved.getEffectiveScopeId(), false,
        device ? responses(resolved.getItems()) : Collections.emptyList(),
        responses(resolved.getItems()));
  }

  @Transactional("businessTransactionManager")
  public SettingListResponse replace(
      SettingListScopeType scopeType, Long scopeId, List<SettingListSaveItemRequest> requests) {
    requireDeviceScope(scopeType);
    Target target = targetService.require(scopeType, scopeId);
    Map<String, IedSettingItem> catalog = catalogService.mapByReference(target.getIedDeviceId());
    List<SettingListItem> replacements = validateAndBuild(scopeType, scopeId, requests, catalog);
    repository.deleteByScopeTypeAndScopeId(scopeType, scopeId);
    repository.flush();
    repository.saveAll(replacements);
    removeMissingSelections(scopeId, replacements);
    repository.flush();
    return get(scopeType, scopeId);
  }

  @Transactional("businessTransactionManager")
  public SettingListResponse clear(SettingListScopeType scopeType, Long scopeId) {
    requireDeviceScope(scopeType);
    targetService.require(scopeType, scopeId);
    removeMissingSelections(scopeId, Collections.emptyList());
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
    return resolveForLogicScope(SettingListScopeType.LOGIC_DIAGRAM, logicDiagramId);
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public ResolvedSettingList resolveForGroup(Long groupId) {
    return resolveForLogicScope(SettingListScopeType.LOGIC_GROUP, groupId);
  }

  private ResolvedSettingList resolveForLogicScope(SettingListScopeType type, Long id) {
    Target target = targetService.require(type, id);
    Set<String> selected = selectionRepository.findByScopeTypeAndScopeId(type, id).stream()
        .map(LogicSettingSelection::getSettingRef).collect(Collectors.toSet());
    // 复制为非托管对象，避免逻辑勾选污染装置实体。
    List<SettingListItem> items = find(SettingListScopeType.IED_DEVICE, target.getIedDeviceId()).stream()
        .map(source -> {
          SettingListItem item = new SettingListItem();
          item.setSettingRef(source.getSettingRef());
          item.setSettingFc(source.getSettingFc());
          item.setSettingName(source.getSettingName());
          item.setValueType(source.getValueType());
          item.setBaselineValue(source.getBaselineValue());
          item.setSortOrder(source.getSortOrder());
          item.setCompareEnabled(selected.contains(source.getSettingRef()));
          return item;
        }).collect(Collectors.toList());
    return new ResolvedSettingList(target, SettingListScopeType.IED_DEVICE, target.getIedDeviceId(), items);
  }

  @Transactional("businessTransactionManager")
  public SettingListResponse saveSelection(SettingListScopeType type, Long id, List<String> refs) {
    if (type != SettingListScopeType.LOGIC_DIAGRAM && type != SettingListScopeType.LOGIC_GROUP) {
      throw badRequest("校验项目选择仅支持基础逻辑和组合逻辑");
    }
    Target target = targetService.require(type, id);
    Set<String> available = find(SettingListScopeType.IED_DEVICE, target.getIedDeviceId()).stream()
        .map(SettingListItem::getSettingRef).collect(Collectors.toSet());
    if (refs == null || refs.stream().anyMatch(ref -> !available.contains(ref))) {
      throw badRequest("校验项目必须属于当前装置定值清单，请刷新后重试");
    }
    List<LogicSettingSelection> selections = refs.stream().distinct().map(ref -> {
      LogicSettingSelection item = new LogicSettingSelection();
      item.setScopeType(type);
      item.setScopeId(id);
      item.setSettingRef(ref);
      return item;
    }).collect(Collectors.toList());
    selectionRepository.deleteByScopeTypeAndScopeId(type, id);
    selectionRepository.flush();
    selectionRepository.saveAll(selections);
    selectionRepository.flush();
    return get(type, id);
  }

  private void removeMissingSelections(Long deviceId, List<SettingListItem> items) {
    Set<String> refs = items.stream().map(SettingListItem::getSettingRef).collect(Collectors.toSet());
    targetService.logicScopeIdsForDevice(deviceId).forEach((type, ids) -> {
      if (!ids.isEmpty()) {
        List<LogicSettingSelection> removed = selectionRepository.findByScopeTypeAndScopeIdIn(type, ids)
            .stream().filter(item -> !refs.contains(item.getSettingRef())).collect(Collectors.toList());
        selectionRepository.deleteAll(removed);
      }
    });
  }

  public static void requireDeviceScope(SettingListScopeType type) {
    if (type != SettingListScopeType.IED_DEVICE) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "逻辑层仅支持查看装置定值和勾选校验项目，请在装置层维护定值清单");
    }
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
