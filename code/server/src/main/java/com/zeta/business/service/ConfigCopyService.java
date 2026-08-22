package com.zeta.business.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.zeta.business.entities.cabinetdisplay.CabinetDisplayItem;
import com.zeta.business.entities.cabinetdisplay.CabinetDisplayItemRepository;
import com.zeta.business.entities.cognitiondevice.CognitionDevice;
import com.zeta.business.entities.cognitiondevice.CognitionDeviceRepository;
import com.zeta.business.entities.configcopy.ConfigCopyModule;
import com.zeta.business.entities.configcopy.ConfigCopyScope;
import com.zeta.business.entities.configcopy.ConfigCopyStatus;
import com.zeta.business.entities.configcopy.dto.*;
import com.zeta.business.entities.devicedisplay.*;
import com.zeta.business.entities.drawinglearning.*;
import com.zeta.business.entities.experimentguide.*;
import com.zeta.business.entities.hardpressboardlist.HardPressboardListItem;
import com.zeta.business.entities.hardpressboardlist.HardPressboardListItemRepository;
import com.zeta.business.entities.learningresource.LearningResource;
import com.zeta.business.entities.learningresource.LearningResourceRepository;
import com.zeta.business.entities.logiclearning.LogicLearningConfig;
import com.zeta.business.entities.logiclearning.LogicLearningConfigRepository;
import com.zeta.business.entities.logicnodecognition.LogicNodeCognitionItem;
import com.zeta.business.entities.logicnodecognition.LogicNodeCognitionItemRepository;
import com.zeta.business.entities.logicgroup.LogicGroup;
import com.zeta.business.entities.logicgroup.LogicGroupMember;
import com.zeta.business.entities.logicgroup.LogicGroupMemberRepository;
import com.zeta.business.entities.logicgroup.LogicGroupRepository;
import com.zeta.business.entities.samplingtest.*;
import com.zeta.business.entities.settinglist.SettingListItem;
import com.zeta.business.entities.settinglist.SettingListItemRepository;
import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.entities.softpressboardlist.SoftPressboardListItem;
import com.zeta.business.entities.softpressboardlist.SoftPressboardListItemRepository;
import com.zeta.business.entities.wiringrequirement.WiringRequirementConfig;
import com.zeta.business.entities.wiringrequirement.WiringRequirementConfigRepository;
import com.zeta.business.entities.wiringrequirement.WiringRequirementGroup;
import com.zeta.business.entities.wiringrequirement.WiringRequirementGroupRepository;
import com.zeta.business.media.CognitionMediaType;
import com.zeta.screen.baseline.IedBaselineSettingItem;
import com.zeta.screen.baseline.IedBaselineSettingItemRepository;
import com.zeta.screen.cabinet.Cabinet;
import com.zeta.screen.cabinet.CabinetRepository;
import com.zeta.screen.hardpressboard.HardPressboard;
import com.zeta.screen.ieddevice.Device;
import com.zeta.screen.ieddevice.DeviceRepository;
import com.zeta.screen.iedsetting.IedSettingItem;
import com.zeta.screen.logicdiagram.ProtectionLogic;
import com.zeta.screen.logicdiagram.ProtectionLogicRepository;
import com.zeta.screen.softpressboard.IedSoftPressboardItem;
import com.zeta.screen.terminal.Terminal;
import com.zeta.screen.terminal.TerminalRepository;
import com.zeta.screen.terminal.TerminalStrip;
import com.zeta.screen.terminal.TerminalStripRepository;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConfigCopyService {
  private final CabinetRepository cabinetRepository;
  private final DeviceRepository deviceRepository;
  private final ProtectionLogicRepository logicRepository;
  private final TerminalStripRepository stripRepository;
  private final TerminalRepository terminalRepository;
  private final IedBaselineSettingItemRepository baselineRepository;
  private final CabinetDisplayItemRepository cabinetItemRepository;
  private final CognitionDeviceRepository cognitionDeviceRepository;
  private final DeviceDisplayItemRepository deviceItemRepository;
  private final TerminalOperationRepository operationRepository;
  private final TerminalOperationTerminalRepository operationTerminalRepository;
  private final DrawingGroupRepository drawingGroupRepository;
  private final DrawingPageRepository drawingPageRepository;
  private final DrawingCognitionItemRepository drawingItemRepository;
  private final LogicLearningConfigRepository logicConfigRepository;
  private final LogicNodeCognitionItemRepository logicItemRepository;
  private final ExperimentGuideItemRepository experimentGuideItemRepository;
  private final SamplingTestItemRepository samplingItemRepository;
  private final SamplingTestChannelRepository samplingChannelRepository;
  private final LearningResourceRepository resourceRepository;
  private final SettingListItemRepository settingItemRepository;
  private final SoftPressboardListItemRepository softPressboardItemRepository;
  private final HardPressboardListItemRepository hardPressboardItemRepository;
  private final WiringRequirementConfigRepository wiringConfigRepository;
  private final WiringRequirementGroupRepository wiringGroupRepository;
  private final LogicGroupRepository logicGroupRepository;
  private final LogicGroupMemberRepository logicGroupMemberRepository;
  private final SettingCatalogService settingCatalogService;
  private final SoftPressboardCatalogService softPressboardCatalogService;
  private final HardPressboardCatalogService hardPressboardCatalogService;
  private final SettingListTargetService targetService;
  private final SharedMediaCleanupService mediaCleanupService;
  private final ObjectMapper objectMapper;

  public ConfigCopyService(
      CabinetRepository cabinetRepository,
      DeviceRepository deviceRepository,
      ProtectionLogicRepository logicRepository,
      TerminalStripRepository stripRepository,
      TerminalRepository terminalRepository,
      IedBaselineSettingItemRepository baselineRepository,
      CabinetDisplayItemRepository cabinetItemRepository,
      CognitionDeviceRepository cognitionDeviceRepository,
      DeviceDisplayItemRepository deviceItemRepository,
      TerminalOperationRepository operationRepository,
      TerminalOperationTerminalRepository operationTerminalRepository,
      DrawingGroupRepository drawingGroupRepository,
      DrawingPageRepository drawingPageRepository,
      DrawingCognitionItemRepository drawingItemRepository,
      LogicLearningConfigRepository logicConfigRepository,
      LogicNodeCognitionItemRepository logicItemRepository,
      ExperimentGuideItemRepository experimentGuideItemRepository,
      SamplingTestItemRepository samplingItemRepository,
      SamplingTestChannelRepository samplingChannelRepository,
      LearningResourceRepository resourceRepository,
      SettingListItemRepository settingItemRepository,
      SoftPressboardListItemRepository softPressboardItemRepository,
      HardPressboardListItemRepository hardPressboardItemRepository,
      WiringRequirementConfigRepository wiringConfigRepository,
      WiringRequirementGroupRepository wiringGroupRepository,
      LogicGroupRepository logicGroupRepository,
      LogicGroupMemberRepository logicGroupMemberRepository,
      SettingCatalogService settingCatalogService,
      SoftPressboardCatalogService softPressboardCatalogService,
      HardPressboardCatalogService hardPressboardCatalogService,
      SettingListTargetService targetService,
      SharedMediaCleanupService mediaCleanupService,
      ObjectMapper objectMapper) {
    this.cabinetRepository = cabinetRepository;
    this.deviceRepository = deviceRepository;
    this.logicRepository = logicRepository;
    this.stripRepository = stripRepository;
    this.terminalRepository = terminalRepository;
    this.baselineRepository = baselineRepository;
    this.cabinetItemRepository = cabinetItemRepository;
    this.cognitionDeviceRepository = cognitionDeviceRepository;
    this.deviceItemRepository = deviceItemRepository;
    this.operationRepository = operationRepository;
    this.operationTerminalRepository = operationTerminalRepository;
    this.drawingGroupRepository = drawingGroupRepository;
    this.drawingPageRepository = drawingPageRepository;
    this.drawingItemRepository = drawingItemRepository;
    this.logicConfigRepository = logicConfigRepository;
    this.logicItemRepository = logicItemRepository;
    this.experimentGuideItemRepository = experimentGuideItemRepository;
    this.samplingItemRepository = samplingItemRepository;
    this.samplingChannelRepository = samplingChannelRepository;
    this.resourceRepository = resourceRepository;
    this.settingItemRepository = settingItemRepository;
    this.softPressboardItemRepository = softPressboardItemRepository;
    this.hardPressboardItemRepository = hardPressboardItemRepository;
    this.wiringConfigRepository = wiringConfigRepository;
    this.wiringGroupRepository = wiringGroupRepository;
    this.logicGroupRepository = logicGroupRepository;
    this.logicGroupMemberRepository = logicGroupMemberRepository;
    this.settingCatalogService = settingCatalogService;
    this.softPressboardCatalogService = softPressboardCatalogService;
    this.hardPressboardCatalogService = hardPressboardCatalogService;
    this.targetService = targetService;
    this.mediaCleanupService = mediaCleanupService;
    this.objectMapper = objectMapper;
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public ConfigCopyPrecheckResponse precheck(ConfigCopyRequest request) {
    return analyze(request).response;
  }

  @Transactional("businessTransactionManager")
  public ConfigCopyExecuteResponse execute(ConfigCopyRequest request) {
    Analysis analysis = analyze(request);
    if (!analysis.response.isReady()) {
      return new ConfigCopyExecuteResponse(false, analysis.response, Collections.emptyList());
    }
    List<ConfigCopyExecuteResponse.TargetExecutionResponse> results = new ArrayList<>();
    for (TargetAnalysis target : analysis.targets) {
      EnumMap<ConfigCopyModule, Integer> copied = new EnumMap<>(ConfigCopyModule.class);
      if (request.getScope() == ConfigCopyScope.CABINET) {
        Long sourceCabinetId = request.getSourceId();
        Long targetCabinetId = target.targetId;
        if (request.getModules().contains(ConfigCopyModule.CABINET_LEARNING)) {
          deleteCabinetLearning(targetCabinetId);
          copied.put(ConfigCopyModule.CABINET_LEARNING,
              copyCabinetLearning(sourceCabinetId, targetCabinetId, target.deviceMap, target.terminalMap));
        }
        if (request.getModules().contains(ConfigCopyModule.DRAWING_LEARNING)) {
          deleteDrawingLearning(targetCabinetId);
          copied.put(ConfigCopyModule.DRAWING_LEARNING, copyDrawingLearning(sourceCabinetId, targetCabinetId));
        }
        if (request.getModules().contains(ConfigCopyModule.LOGIC_LEARNING)) {
          deleteLogicLearning(logicIdsForCabinet(targetCabinetId));
          copied.put(ConfigCopyModule.LOGIC_LEARNING, copyLogicLearning(target.logicMap));
        }
        if (request.getModules().contains(ConfigCopyModule.LOGIC_GROUP)) {
          deleteLogicGroups(target.deviceMap.values());
          copied.put(ConfigCopyModule.LOGIC_GROUP, copyLogicGroups(target));
        }
        if (request.getModules().contains(ConfigCopyModule.BASELINE_CONFIG)) {
          deleteBaselineConfig(request.getScope(), target.targetId);
          copied.put(ConfigCopyModule.BASELINE_CONFIG,
              copyBaselineConfig(request.getScope(), request.getSourceId(), target));
        }
        if (request.getModules().contains(ConfigCopyModule.SAMPLING_TEST)) {
          deleteSamplingTests(targetCabinetId);
          copied.put(ConfigCopyModule.SAMPLING_TEST,
              copySamplingTests(sourceCabinetId, targetCabinetId, target.terminalMap));
        }
        if (request.getModules().contains(ConfigCopyModule.LEARNING_RESOURCE)) {
          deleteLearningResources(targetCabinetId);
          copied.put(ConfigCopyModule.LEARNING_RESOURCE, copyLearningResources(sourceCabinetId, targetCabinetId));
        }
      } else {
        if (request.getModules().contains(ConfigCopyModule.LOGIC_LEARNING)) {
          deleteLogicLearning(logicIdsForDevice(target.targetId));
          copied.put(ConfigCopyModule.LOGIC_LEARNING, copyLogicLearning(target.logicMap));
        }
        if (request.getModules().contains(ConfigCopyModule.LOGIC_GROUP)) {
          deleteLogicGroups(target.deviceMap.values());
          copied.put(ConfigCopyModule.LOGIC_GROUP, copyLogicGroups(target));
        }
        if (request.getModules().contains(ConfigCopyModule.BASELINE_CONFIG)) {
          deleteBaselineConfig(request.getScope(), target.targetId);
          copied.put(ConfigCopyModule.BASELINE_CONFIG,
              copyBaselineConfig(request.getScope(), request.getSourceId(), target));
        }
      }
      results.add(new ConfigCopyExecuteResponse.TargetExecutionResponse(
          target.targetId, target.targetName, copied));
    }
    return new ConfigCopyExecuteResponse(true, analysis.response, results);
  }

  private Analysis analyze(ConfigCopyRequest request) {
    validateRequest(request);
    List<TargetAnalysis> analyses = new ArrayList<>();
    List<TargetPrecheckResponse> responses = new ArrayList<>();
    Set<Long> seenTargets = new HashSet<>();
    for (TargetCopyRequest requestedTarget : request.getTargets()) {
      if (!seenTargets.add(requestedTarget.getTargetId())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "目标不能重复");
      }
      TargetAnalysis target = request.getScope() == ConfigCopyScope.CABINET
          ? analyzeCabinetTarget(request, requestedTarget)
          : analyzeDeviceTarget(request, requestedTarget);
      analyses.add(target);
      responses.add(target.toResponse());
    }
    boolean ready = responses.stream().allMatch(item -> item.getStatus() == ConfigCopyStatus.READY);
    return new Analysis(new ConfigCopyPrecheckResponse(ready, responses), analyses);
  }

  private void validateRequest(ConfigCopyRequest request) {
    if (request.getScope() == ConfigCopyScope.DEVICE) {
      Set<ConfigCopyModule> allowed = EnumSet.of(
          ConfigCopyModule.LOGIC_LEARNING, ConfigCopyModule.BASELINE_CONFIG, ConfigCopyModule.LOGIC_GROUP);
      for (ConfigCopyModule module : request.getModules()) {
        if (!allowed.contains(module)) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
              "装置级复制仅支持逻辑学习、基准配置和组合逻辑");
        }
      }
    }
    if (request.getScope() == ConfigCopyScope.CABINET) {
      requireCabinet(request.getSourceId());
    } else {
      requireDevice(request.getSourceId());
    }
  }

  private TargetAnalysis analyzeCabinetTarget(ConfigCopyRequest request, TargetCopyRequest requestedTarget) {
    Cabinet targetCabinet = requireCabinet(requestedTarget.getTargetId());
    TargetAnalysis result = new TargetAnalysis(targetCabinet.getId(), targetCabinet.getName());
    if (Objects.equals(request.getSourceId(), targetCabinet.getId())) {
      result.incompatible("SOURCE_EQUALS_TARGET", "源屏柜不能作为目标屏柜", request.getSourceId());
      return finishCounts(request, result);
    }

    Set<Long> requiredDeviceIds = requiredDeviceIds(request.getSourceId(), request.getModules());
    resolveDeviceMappings(request.getSourceId(), targetCabinet.getId(), requiredDeviceIds,
        requestedTarget.getDeviceMappings(), result);
    if (!result.needsMapping && !result.incompatible) {
      if (request.getModules().contains(ConfigCopyModule.CABINET_LEARNING)) {
        validateBaselines(request.getSourceId(), result);
      }
      if (request.getModules().contains(ConfigCopyModule.LOGIC_LEARNING)) validateLogics(result);
      if (request.getModules().contains(ConfigCopyModule.LOGIC_GROUP)) validateLogicGroups(result);
      if (request.getModules().contains(ConfigCopyModule.BASELINE_CONFIG)) {
        resolveBaselineLogicMappings(result);
        validateSettingPressboards(result);
        validateHardPressboards(result);
      }
      if (request.getModules().contains(ConfigCopyModule.CABINET_LEARNING)
          || request.getModules().contains(ConfigCopyModule.SAMPLING_TEST)
          || request.getModules().contains(ConfigCopyModule.BASELINE_CONFIG)) {
        validateTerminals(ConfigCopyScope.CABINET, request.getSourceId(), targetCabinet.getId(), request.getModules(), result);
      }
    }
    return finishCounts(request, result);
  }

  private TargetAnalysis analyzeDeviceTarget(ConfigCopyRequest request, TargetCopyRequest requestedTarget) {
    Device source = requireDevice(request.getSourceId());
    Device target = requireDevice(requestedTarget.getTargetId());
    TargetAnalysis result = new TargetAnalysis(target.getId(), target.getName());
    if (Objects.equals(source.getId(), target.getId())) {
      result.incompatible("SOURCE_EQUALS_TARGET", "源装置不能作为目标装置", source.getId());
    } else if (!StringUtils.hasText(source.getDeviceType()) || !StringUtils.hasText(target.getDeviceType())) {
      result.incompatible("DEVICE_TYPE_MISSING", "源装置或目标装置缺少装置类型，不能复制", source.getId());
    } else if (!Objects.equals(source.getDeviceType(), target.getDeviceType())) {
      result.incompatible("DEVICE_TYPE_MISMATCH", "源装置与目标装置类型不一致", source.getId());
    } else {
      result.deviceMap.put(source.getId(), target.getId());
      result.mappingResponses.add(mappingResponse(source, target, false, Collections.emptyList()));
      if (request.getModules().contains(ConfigCopyModule.LOGIC_LEARNING)) validateLogics(result);
      if (request.getModules().contains(ConfigCopyModule.LOGIC_GROUP)) validateLogicGroups(result);
      if (request.getModules().contains(ConfigCopyModule.BASELINE_CONFIG)) {
        resolveBaselineLogicMappings(result);
        validateSettingPressboards(result);
        validateHardPressboards(result);
        validateTerminals(ConfigCopyScope.DEVICE, source.getId(), target.getId(), request.getModules(), result);
      }
    }
    return finishCounts(request, result);
  }

  private TargetAnalysis finishCounts(ConfigCopyRequest request, TargetAnalysis target) {
    for (ConfigCopyModule module : request.getModules()) {
      target.sourceCounts.put(module, countModule(request.getScope(), request.getSourceId(), module));
      target.overwriteCounts.put(module, countModule(request.getScope(), target.targetId, module));
    }
    return target;
  }

  private Set<Long> requiredDeviceIds(Long cabinetId, Set<ConfigCopyModule> modules) {
    Set<Long> ids = new LinkedHashSet<>();
    if (modules.contains(ConfigCopyModule.CABINET_LEARNING)) {
      List<CabinetDisplayItem> cabinetItems = cabinetItemRepository
          .findByScreenCabinetIdOrderBySortOrderAscIdAsc(cabinetId);
      if (!cabinetItems.isEmpty()) {
        List<Long> parentIds = cabinetItems.stream().map(CabinetDisplayItem::getId).collect(Collectors.toList());
        cognitionDeviceRepository.findByCabinetDisplayItemIdIn(parentIds).stream()
            .map(CognitionDevice::getScreenDeviceId).filter(Objects::nonNull).forEach(ids::add);
      }
    }
    if (modules.contains(ConfigCopyModule.LOGIC_LEARNING)) {
      for (Device device : deviceRepository.findByCabinetIdOrderByIdAsc(cabinetId)) {
        if (!configuredLogics(logicRepository.findByDeviceIdOrderByIdAsc(device.getId())).isEmpty()) {
          ids.add(device.getId());
        }
      }
    }
    if (modules.contains(ConfigCopyModule.BASELINE_CONFIG)) {
      for (Device device : deviceRepository.findByCabinetIdOrderByIdAsc(cabinetId)) {
        if (hasBaselineConfigAtDevice(device.getId())) ids.add(device.getId());
      }
    }
    if (modules.contains(ConfigCopyModule.LOGIC_GROUP)) {
      List<Long> allDeviceIds = deviceRepository.findByCabinetIdOrderByIdAsc(cabinetId).stream()
          .map(Device::getId).collect(Collectors.toList());
      if (!allDeviceIds.isEmpty()) {
        logicGroupRepository.findByIedDeviceIdIn(allDeviceIds).stream()
            .map(LogicGroup::getIedDeviceId).forEach(ids::add);
      }
    }
    if (modules.contains(ConfigCopyModule.CABINET_LEARNING)
        || modules.contains(ConfigCopyModule.SAMPLING_TEST)
        || modules.contains(ConfigCopyModule.BASELINE_CONFIG)) {
      Set<Long> terminalIds = referencedTerminalIds(ConfigCopyScope.CABINET, cabinetId, modules);
      if (!terminalIds.isEmpty()) {
        terminalRepository.findAllWithCabinetAndStripByIdIn(terminalIds).stream()
            .map(Terminal::getIedDevice).filter(Objects::nonNull).map(Device::getId).forEach(ids::add);
      }
    }
    return ids;
  }

  private void resolveDeviceMappings(Long sourceCabinetId, Long targetCabinetId, Set<Long> requiredIds,
                                     List<DeviceMappingRequest> manualMappings, TargetAnalysis result) {
    Map<Long, Long> manual = manualMappings == null ? Collections.emptyMap() : manualMappings.stream()
        .collect(Collectors.toMap(DeviceMappingRequest::getSourceDeviceId,
            DeviceMappingRequest::getTargetDeviceId, (a, b) -> b));
    Map<Long, Device> sources = deviceRepository.findByCabinetIdOrderByIdAsc(sourceCabinetId).stream()
        .collect(Collectors.toMap(Device::getId, Function.identity()));
    List<Device> targets = deviceRepository.findByCabinetIdOrderByIdAsc(targetCabinetId);
    Set<Long> usedTargetIds = new HashSet<>();
    for (Long sourceId : requiredIds) {
      Device source = sources.get(sourceId);
      if (source == null) {
        result.incompatible("SOURCE_DEVICE_MISSING", "源装置不存在或已不属于源屏柜", sourceId);
        continue;
      }
      if (!StringUtils.hasText(source.getDeviceType())) {
        result.incompatible("DEVICE_TYPE_MISSING", "源装置缺少装置类型：" + source.getName(), sourceId);
        result.mappingResponses.add(mappingResponse(source, null, false, Collections.emptyList()));
        continue;
      }
      List<Device> candidates = targets.stream()
          .filter(item -> Objects.equals(source.getDeviceType(), item.getDeviceType()))
          .collect(Collectors.toList());
      Device resolved = null;
      boolean automatic = false;
      Long manualTargetId = manual.get(sourceId);
      if (manualTargetId != null) {
        resolved = candidates.stream().filter(item -> item.getId().equals(manualTargetId)).findFirst().orElse(null);
        if (resolved == null) {
          result.incompatible("DEVICE_MAPPING_INVALID", "人工指定的目标装置不存在、类型不一致或不属于目标屏柜", sourceId);
        }
      } else {
        List<Device> exact = candidates.stream()
            .filter(item -> Objects.equals(source.getIedName(), item.getIedName()))
            .collect(Collectors.toList());
        if (exact.size() == 1) {
          resolved = exact.get(0);
          automatic = true;
        } else if (exact.isEmpty() && candidates.size() == 1) {
          resolved = candidates.get(0);
          automatic = true;
        } else {
          if (candidates.isEmpty()) {
            result.incompatible("DEVICE_MISSING", "目标屏柜没有同类型装置：" + source.getName(), sourceId);
          } else {
            result.needsMapping = true;
            result.issues.add(new ConfigCopyIssueResponse("DEVICE_MAPPING_REQUIRED",
                "装置无法唯一自动匹配，请选择目标装置：" + source.getName(), sourceId));
          }
        }
      }
      if (resolved != null && !usedTargetIds.add(resolved.getId())) {
        result.incompatible("DEVICE_MAPPING_DUPLICATE", "多个源装置不能映射到同一目标装置", sourceId);
        resolved = null;
      }
      result.mappingResponses.add(mappingResponse(source, resolved, automatic,
          candidates.stream().map(this::candidateResponse).collect(Collectors.toList())));
      if (resolved != null) result.deviceMap.put(sourceId, resolved.getId());
    }
  }

  private void validateBaselines(Long sourceCabinetId, TargetAnalysis result) {
    if (result.incompatible) return;
    Set<Long> baselineDevices = new HashSet<>();
    List<CabinetDisplayItem> parents = cabinetItemRepository
        .findByScreenCabinetIdOrderBySortOrderAscIdAsc(sourceCabinetId);
    if (!parents.isEmpty()) {
      List<CognitionDevice> devices = cognitionDeviceRepository.findByCabinetDisplayItemIdIn(
          parents.stream().map(CabinetDisplayItem::getId).collect(Collectors.toList()));
      if (!devices.isEmpty()) {
        Map<Long, CognitionDevice> byId = devices.stream().collect(Collectors.toMap(CognitionDevice::getId, d -> d));
        deviceItemRepository.findByCognitionDeviceIdIn(byId.keySet()).stream()
            .filter(item -> item.getMediaType() == CognitionMediaType.IED_BASELINE_SETTING)
            .map(item -> byId.get(item.getCognitionDeviceId()))
            .filter(Objects::nonNull).map(CognitionDevice::getScreenDeviceId).filter(Objects::nonNull)
            .forEach(baselineDevices::add);
      }
    }
    for (Long sourceDeviceId : baselineDevices) {
      Long targetDeviceId = result.deviceMap.get(sourceDeviceId);
      if (targetDeviceId == null) continue;
      Device sourceDevice = requireDevice(sourceDeviceId);
      Device targetDevice = requireDevice(targetDeviceId);
      List<IedBaselineSettingItem> source = baselineRepository
          .findByIedDeviceIdOrderBySortOrderAsc(sourceDeviceId);
      List<IedBaselineSettingItem> target = baselineRepository
          .findByIedDeviceIdOrderBySortOrderAsc(targetDeviceId);
      if (source.size() != target.size()) {
        result.incompatible("BASELINE_STRUCTURE_MISMATCH", "IED 定值项数量不一致", sourceDeviceId);
        continue;
      }
      for (int i = 0; i < source.size(); i++) {
        IedBaselineSettingItem a = source.get(i);
        IedBaselineSettingItem b = target.get(i);
        if (!Objects.equals(normalizeInstanceText(a.getSettingRef(), sourceDevice.getIedName(), sourceDeviceId),
                normalizeInstanceText(b.getSettingRef(), targetDevice.getIedName(), sourceDeviceId))
            || !Objects.equals(a.getSettingFc(), b.getSettingFc())
            || !Objects.equals(
                normalizeInstanceText(a.getSettingDescription(), sourceDevice.getIedName(), sourceDeviceId),
                normalizeInstanceText(b.getSettingDescription(), targetDevice.getIedName(), sourceDeviceId))
            || !Objects.equals(a.getSortOrder(), b.getSortOrder())) {
          result.incompatible("BASELINE_STRUCTURE_MISMATCH", "IED 定值项结构不一致", sourceDeviceId);
          break;
        }
      }
    }
  }

  private void validateLogics(TargetAnalysis result) {
    for (Map.Entry<Long, Long> mapping : result.deviceMap.entrySet()) {
      Device sourceDevice = requireDevice(mapping.getKey());
      Device targetDevice = requireDevice(mapping.getValue());
      List<ProtectionLogic> sourceLogics = configuredLogics(
          logicRepository.findByDeviceIdOrderByIdAsc(mapping.getKey()));
      List<ProtectionLogic> targetLogics = logicRepository.findByDeviceIdOrderByIdAsc(mapping.getValue());
      for (ProtectionLogic source : sourceLogics) {
        List<ProtectionLogic> matched = targetLogics.stream()
            .filter(item -> Objects.equals(source.getLogicId(), item.getLogicId()))
            .collect(Collectors.toList());
        if (matched.size() != 1) {
          result.incompatible(matched.isEmpty() ? "LOGIC_MISSING" : "LOGIC_AMBIGUOUS",
              "目标装置无法唯一匹配逻辑图：" + source.getLogicName(), source.getId());
          continue;
        }
        ProtectionLogic target = matched.get(0);
        if (!sameLogic(source, target, sourceDevice, targetDevice)) {
          result.incompatible("LOGIC_STRUCTURE_MISMATCH",
              "逻辑图结构不一致：" + source.getLogicName(), source.getId());
          continue;
        }
        result.logicMap.put(source.getId(), target.getId());
      }
    }
  }

  private void validateLogicGroups(TargetAnalysis result) {
    for (Map.Entry<Long, Long> mapping : result.deviceMap.entrySet()) {
      Long sourceDeviceId = mapping.getKey();
      Long targetDeviceId = mapping.getValue();
      List<LogicGroup> sourceGroups = logicGroupRepository
          .findByIedDeviceIdOrderBySortOrderAscIdAsc(sourceDeviceId);
      if (sourceGroups.isEmpty()) continue;
      List<ProtectionLogic> targetLogics = logicRepository.findByDeviceIdOrderByIdAsc(targetDeviceId);
      for (LogicGroup group : sourceGroups) {
        for (LogicGroupMember member : logicGroupMemberRepository
            .findByGroupIdOrderBySortOrderAscIdAsc(group.getId())) {
          Long sourceLogicId = member.getLogicDiagramId();
          if (result.groupLogicMap.containsKey(sourceLogicId)) continue;
          ProtectionLogic sourceLogic = requireLogic(sourceLogicId);
          List<ProtectionLogic> matched = targetLogics.stream()
              .filter(item -> Objects.equals(sourceLogic.getLogicName(), item.getLogicName()))
              .collect(Collectors.toList());
          if (matched.size() != 1) {
            result.incompatible(matched.isEmpty() ? "LOGIC_MISSING" : "LOGIC_AMBIGUOUS",
                "目标装置无法唯一匹配组合逻辑所需的基础逻辑：" + sourceLogic.getLogicName(), sourceDeviceId);
            continue;
          }
          result.groupLogicMap.put(sourceLogicId, matched.get(0).getId());
        }
      }
    }
  }

  private void validateSettingPressboards(TargetAnalysis result) {
    for (Map.Entry<Long, Long> mapping : result.deviceMap.entrySet()) {
      Device sourceDevice = requireDevice(mapping.getKey());
      Device targetDevice = requireDevice(mapping.getValue());
      validateSettingItems(sourceDevice, targetDevice, result);
      validateSoftPressboardItems(sourceDevice, targetDevice, result);
    }
  }

  private void validateSettingItems(Device sourceDevice, Device targetDevice, TargetAnalysis result) {
    List<SettingListItem> sourceItems = settingItemRepository
        .findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.IED_DEVICE, sourceDevice.getId());
    if (sourceItems.isEmpty()) return;
    Map<String, IedSettingItem> targetByRelRef = new LinkedHashMap<>();
    for (IedSettingItem item : settingCatalogService.list(targetDevice.getId())) {
      targetByRelRef.put(stripIedName(item.getSettingRef(), targetDevice.getIedName()), item);
    }
    for (SettingListItem item : sourceItems) {
      String relRef = stripIedName(item.getSettingRef(), sourceDevice.getIedName());
      IedSettingItem target = targetByRelRef.get(relRef);
      if (target == null || !Objects.equals(item.getSettingName(), target.getSettingName())) {
        result.incompatible("BASELINE_ITEM_MISSING",
            "目标装置缺少定值项：" + item.getSettingName(), sourceDevice.getId());
      }
    }
  }

  private void validateSoftPressboardItems(Device sourceDevice, Device targetDevice, TargetAnalysis result) {
    List<SoftPressboardListItem> sourceItems = softPressboardItemRepository
        .findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.IED_DEVICE, sourceDevice.getId());
    if (sourceItems.isEmpty()) return;
    Map<String, IedSoftPressboardItem> targetByRelRef = new LinkedHashMap<>();
    for (IedSoftPressboardItem item : softPressboardCatalogService.list(targetDevice.getId())) {
      targetByRelRef.put(stripIedName(item.getPressboardRef(), targetDevice.getIedName()), item);
    }
    for (SoftPressboardListItem item : sourceItems) {
      String relRef = stripIedName(item.getPressboardRef(), sourceDevice.getIedName());
      IedSoftPressboardItem target = targetByRelRef.get(relRef);
      if (target == null || !Objects.equals(item.getPressboardName(), target.getPressboardName())) {
        result.incompatible("BASELINE_ITEM_MISSING",
            "目标装置缺少软压板项：" + item.getPressboardName(), sourceDevice.getId());
      }
    }
  }

  private void validateHardPressboards(TargetAnalysis result) {
    for (Map.Entry<Long, Long> mapping : result.deviceMap.entrySet()) {
      Long sourceDeviceId = mapping.getKey();
      List<HardPressboardListItem> sourceItems = hardPressboardItemRepository
          .findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.IED_DEVICE, sourceDeviceId);
      if (sourceItems.isEmpty()) continue;
      Long targetCabinetId = targetService
          .require(SettingListScopeType.IED_DEVICE, mapping.getValue()).getCabinetId();
      Map<String, HardPressboard> targetByName = new LinkedHashMap<>();
      for (HardPressboard pb : hardPressboardCatalogService.list(targetCabinetId)) {
        targetByName.put(pb.getName(), pb);
      }
      for (HardPressboardListItem item : sourceItems) {
        if (!targetByName.containsKey(item.getPressboardName())) {
          result.incompatible("HARD_PRESSBOARD_MISSING",
              "目标屏柜缺少硬压板：" + item.getPressboardName(), sourceDeviceId);
        }
      }
    }
  }

  private void resolveBaselineLogicMappings(TargetAnalysis result) {
    for (Map.Entry<Long, Long> mapping : result.deviceMap.entrySet()) {
      Long sourceDeviceId = mapping.getKey();
      Long targetDeviceId = mapping.getValue();
      List<ProtectionLogic> sourceLogics = logicRepository.findByDeviceIdOrderByIdAsc(sourceDeviceId);
      List<ProtectionLogic> targetLogics = logicRepository.findByDeviceIdOrderByIdAsc(targetDeviceId);
      for (ProtectionLogic source : sourceLogics) {
        if (result.logicMap.containsKey(source.getId())) continue;
        if (!hasBaselineConfigAtLogic(source.getId())) continue;
        List<ProtectionLogic> matched = targetLogics.stream()
            .filter(item -> Objects.equals(source.getLogicId(), item.getLogicId()))
            .collect(Collectors.toList());
        if (matched.size() != 1) {
          result.incompatible(matched.isEmpty() ? "LOGIC_MISSING" : "LOGIC_AMBIGUOUS",
              "目标装置无法唯一匹配逻辑图：" + source.getLogicName(), source.getId());
          continue;
        }
        result.logicMap.put(source.getId(), matched.get(0).getId());
      }
    }
  }

  private boolean hasBaselineConfigAtLogic(Long logicId) {
    return !settingItemRepository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(
        SettingListScopeType.LOGIC_DIAGRAM, logicId).isEmpty()
        || !softPressboardItemRepository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(
        SettingListScopeType.LOGIC_DIAGRAM, logicId).isEmpty()
        || !hardPressboardItemRepository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(
        SettingListScopeType.LOGIC_DIAGRAM, logicId).isEmpty()
        || !wiringConfigRepository.findByScopeTypeAndScopeIdOrderByIdAsc(
        SettingListScopeType.LOGIC_DIAGRAM, logicId).isEmpty();
  }

  private ProtectionLogic requireLogic(Long id) {
    return logicRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "逻辑框图不存在"));
  }

  private List<ProtectionLogic> configuredLogics(List<ProtectionLogic> candidates) {
    if (candidates.isEmpty()) return Collections.emptyList();
    Set<Long> ids = candidates.stream().map(ProtectionLogic::getId).collect(Collectors.toSet());
    Set<Long> configured = logicConfigRepository.findByLogicDiagramIdIn(ids).stream()
        .map(LogicLearningConfig::getLogicDiagramId).collect(Collectors.toSet());
    logicItemRepository.findByLogicDiagramIdIn(ids).stream()
        .map(LogicNodeCognitionItem::getLogicDiagramId).forEach(configured::add);
    experimentGuideItemRepository.findByScopeTypeAndScopeIdIn(SettingListScopeType.LOGIC_DIAGRAM, ids).stream()
        .map(ExperimentGuideItem::getScopeId).forEach(configured::add);
    return candidates.stream().filter(item -> configured.contains(item.getId())).collect(Collectors.toList());
  }

  private boolean sameLogic(ProtectionLogic source, ProtectionLogic target,
                            Device sourceDevice, Device targetDevice) {
    if (!Objects.equals(source.getVersion(), target.getVersion())
        || !Objects.equals(source.getProtectType(), target.getProtectType())) return false;
    try {
      JsonNode sourceTree = normalizeJsonTree(objectMapper.readTree(source.getConfigJson()),
          sourceDevice.getIedName(), sourceDevice.getId());
      JsonNode targetTree = normalizeJsonTree(objectMapper.readTree(target.getConfigJson()),
          targetDevice.getIedName(), sourceDevice.getId());
      return Objects.equals(sourceTree, targetTree);
    } catch (Exception ex) {
      return false;
    }
  }

  private void validateTerminals(ConfigCopyScope scope, Long sourceId, Long targetId,
                                 Set<ConfigCopyModule> modules, TargetAnalysis result) {
    Set<Long> terminalIds = referencedTerminalIds(scope, sourceId, modules);
    if (terminalIds.isEmpty()) return;
    Long sourceCabinetId = scope == ConfigCopyScope.CABINET
        ? sourceId : targetService.require(SettingListScopeType.IED_DEVICE, sourceId).getCabinetId();
    Long targetCabinetId = scope == ConfigCopyScope.CABINET
        ? targetId : targetService.require(SettingListScopeType.IED_DEVICE, targetId).getCabinetId();
    Map<Long, Terminal> sourceTerminals = terminalRepository.findAllWithCabinetAndStripByIdIn(terminalIds).stream()
        .collect(Collectors.toMap(Terminal::getId, Function.identity()));
    List<TerminalStrip> targetStrips = stripRepository.findByCabinetIdOrderBySortOrderAsc(targetCabinetId);
    for (Long terminalId : terminalIds) {
      Terminal source = sourceTerminals.get(terminalId);
      if (source == null || source.getTerminalStrip() == null) {
        result.incompatible("TERMINAL_MISSING", "源端子不存在或不属于端子排", terminalId);
        continue;
      }
      TerminalStrip sourceStrip = source.getTerminalStrip();
      List<TerminalStrip> strips = targetStrips.stream()
          .filter(item -> Objects.equals(sourceStrip.getName(), item.getName())
              && Objects.equals(sourceStrip.getLabelPrefix(), item.getLabelPrefix()))
          .collect(Collectors.toList());
      if (strips.size() != 1) {
        result.incompatible(strips.isEmpty() ? "TERMINAL_STRIP_MISSING" : "TERMINAL_STRIP_AMBIGUOUS",
            "目标屏柜无法唯一匹配端子排：" + sourceStrip.getName(), sourceStrip.getId());
        continue;
      }
      List<Terminal> terminals = terminalRepository.findByTerminalStripIdOrderByIdAsc(strips.get(0).getId()).stream()
          .filter(item -> Objects.equals(source.getTerminalLabel(), item.getTerminalLabel()))
          .collect(Collectors.toList());
      if (terminals.size() != 1) {
        result.incompatible(terminals.isEmpty() ? "TERMINAL_MISSING" : "TERMINAL_AMBIGUOUS",
            "目标端子排无法唯一匹配端子：" + source.getTerminalLabel(), terminalId);
        continue;
      }
      Terminal target = terminals.get(0);
      if (source.getSignalType() != target.getSignalType()
          || !Objects.equals(normalizeMappedReference(source.getIedSignalRef(), true, result),
              normalizeMappedReference(target.getIedSignalRef(), false, result))) {
        result.incompatible("TERMINAL_STRUCTURE_MISMATCH",
            "端子信号类型或相对信号引用不一致：" + source.getTerminalLabel(), terminalId);
      } else {
        result.terminalMap.put(terminalId, target.getId());
      }
    }
  }

  private Set<Long> referencedTerminalIds(ConfigCopyScope scope, Long sourceId, Set<ConfigCopyModule> modules) {
    Set<Long> terminalIds = new LinkedHashSet<>();
    if (modules.contains(ConfigCopyModule.CABINET_LEARNING)) {
      List<CabinetDisplayItem> parents = cabinetItemRepository
          .findByScreenCabinetIdOrderBySortOrderAscIdAsc(sourceId);
      if (!parents.isEmpty()) {
        List<CognitionDevice> devices = cognitionDeviceRepository.findByCabinetDisplayItemIdIn(
            parents.stream().map(CabinetDisplayItem::getId).collect(Collectors.toList()));
        if (!devices.isEmpty()) {
          List<DeviceDisplayItem> items = deviceItemRepository.findByCognitionDeviceIdIn(
              devices.stream().map(CognitionDevice::getId).collect(Collectors.toList()));
          if (!items.isEmpty()) {
            List<TerminalOperation> operations = operationRepository.findByDeviceDisplayItemIdIn(
                items.stream().map(DeviceDisplayItem::getId).collect(Collectors.toList()));
            for (TerminalOperation operation : operations) {
              operationTerminalRepository.findByTerminalOperationIdOrderBySortOrderAscIdAsc(operation.getId())
                  .stream().map(TerminalOperationTerminal::getTerminalId).forEach(terminalIds::add);
            }
          }
        }
      }
    }
    if (modules.contains(ConfigCopyModule.SAMPLING_TEST)) {
      for (SamplingTestItem item : samplingItemRepository
          .findByScreenCabinetIdOrderBySortOrderAscIdAsc(sourceId)) {
        samplingChannelRepository.findBySamplingTestItemIdOrderBySortOrderAscIdAsc(item.getId()).stream()
            .map(SamplingTestChannel::getTerminalId).forEach(terminalIds::add);
      }
    }
    if (modules.contains(ConfigCopyModule.BASELINE_CONFIG)) {
      collectWiringTerminalIds(scope, sourceId, terminalIds);
    }
    return terminalIds;
  }

  private boolean hasBaselineConfigAtDevice(Long deviceId) {
    return !settingItemRepository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(
        SettingListScopeType.IED_DEVICE, deviceId).isEmpty()
        || !softPressboardItemRepository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(
        SettingListScopeType.IED_DEVICE, deviceId).isEmpty()
        || !hardPressboardItemRepository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(
        SettingListScopeType.IED_DEVICE, deviceId).isEmpty()
        || !wiringConfigRepository.findByScopeTypeAndScopeIdOrderByIdAsc(
        SettingListScopeType.IED_DEVICE, deviceId).isEmpty();
  }

  private void collectSourceScopeIds(ConfigCopyScope scope, Long sourceId,
                                     Set<Long> deviceIds, Set<Long> logicIds, Set<Long> groupIds) {
    if (scope == ConfigCopyScope.CABINET) {
      deviceIds.addAll(deviceRepository.findByCabinetIdOrderByIdAsc(sourceId).stream()
          .map(Device::getId).collect(Collectors.toList()));
      logicIds.addAll(logicRepository.findByDeviceCabinetIdOrderByIdAsc(sourceId).stream()
          .map(ProtectionLogic::getId).collect(Collectors.toList()));
    } else {
      deviceIds.add(sourceId);
      logicIds.addAll(logicRepository.findByDeviceIdOrderByIdAsc(sourceId).stream()
          .map(ProtectionLogic::getId).collect(Collectors.toList()));
    }
    if (!deviceIds.isEmpty()) {
      groupIds.addAll(logicGroupRepository.findByIedDeviceIdIn(deviceIds).stream()
          .map(LogicGroup::getId).collect(Collectors.toList()));
    }
  }

  private void collectWiringTerminalIds(ConfigCopyScope scope, Long sourceId, Set<Long> terminalIds) {
    Set<Long> deviceIds = new LinkedHashSet<>();
    Set<Long> logicIds = new LinkedHashSet<>();
    Set<Long> groupIds = new LinkedHashSet<>();
    collectSourceScopeIds(scope, sourceId, deviceIds, logicIds, groupIds);
    collectWiringConfigTerminals(SettingListScopeType.IED_DEVICE, deviceIds, terminalIds);
    collectWiringConfigTerminals(SettingListScopeType.LOGIC_DIAGRAM, logicIds, terminalIds);
    collectWiringConfigTerminals(SettingListScopeType.LOGIC_GROUP, groupIds, terminalIds);
  }

  private void collectWiringConfigTerminals(SettingListScopeType scopeType, Set<Long> scopeIds,
                                            Set<Long> terminalIds) {
    if (scopeIds.isEmpty()) return;
    for (WiringRequirementConfig config : wiringConfigRepository.findByScopeTypeAndScopeIdIn(scopeType, scopeIds)) {
      for (WiringRequirementGroup group : wiringGroupRepository
          .findByConfigIdOrderByGroupNoAscIdAsc(config.getId())) {
        addTerminalId(terminalIds, group.getTerminalAId());
        addTerminalId(terminalIds, group.getTerminalBId());
        addTerminalId(terminalIds, group.getTerminalCId());
        addTerminalId(terminalIds, group.getTerminalNId());
      }
    }
  }

  private void addTerminalId(Set<Long> ids, Long id) {
    if (id != null) ids.add(id);
  }

  private String stripIedName(String ref, String iedName) {
    if (ref == null) return null;
    String trimmed = ref.trim();
    if (!StringUtils.hasText(iedName)) return trimmed;
    String prefix = iedName + "/";
    if (trimmed.startsWith(prefix)) return trimmed.substring(prefix.length());
    if (trimmed.startsWith(iedName) && trimmed.length() > iedName.length()
        && trimmed.charAt(iedName.length()) != '/') {
      return trimmed.substring(iedName.length());
    }
    return trimmed;
  }

  private int copyCabinetLearning(Long sourceCabinetId, Long targetCabinetId,
                                  Map<Long, Long> deviceMap, Map<Long, Long> terminalMap) {
    int copied = 0;
    for (CabinetDisplayItem sourceParent : cabinetItemRepository
        .findByScreenCabinetIdOrderBySortOrderAscIdAsc(sourceCabinetId)) {
      CabinetDisplayItem targetParent = new CabinetDisplayItem();
      targetParent.setScreenCabinetId(targetCabinetId);
      targetParent.setTitle(sourceParent.getTitle());
      targetParent.setImageUrl(sourceParent.getImageUrl());
      targetParent.setImageData(copyBytes(sourceParent.getImageData()));
      targetParent.setImageContentType(sourceParent.getImageContentType());
      targetParent.setContent(sourceParent.getContent());
      targetParent.setSortOrder(sourceParent.getSortOrder());
      targetParent.setEnabled(sourceParent.getEnabled());
      targetParent.setCreatedAt(Instant.now());
      targetParent = cabinetItemRepository.save(targetParent);
      copied++;
      for (CognitionDevice sourceDevice : cognitionDeviceRepository
          .findByCabinetDisplayItemIdOrderBySortOrderAscIdAsc(sourceParent.getId())) {
        CognitionDevice targetDevice = new CognitionDevice();
        targetDevice.setCabinetDisplayItemId(targetParent.getId());
        targetDevice.setDeviceType(sourceDevice.getDeviceType());
        targetDevice.setScreenDeviceId(sourceDevice.getScreenDeviceId() == null ? null
            : deviceMap.get(sourceDevice.getScreenDeviceId()));
        targetDevice.setTitle(sourceDevice.getTitle());
        targetDevice.setLeftPercent(sourceDevice.getLeftPercent());
        targetDevice.setTopPercent(sourceDevice.getTopPercent());
        targetDevice.setWidthPercent(sourceDevice.getWidthPercent());
        targetDevice.setHeightPercent(sourceDevice.getHeightPercent());
        targetDevice.setSortOrder(sourceDevice.getSortOrder());
        targetDevice.setEnabled(sourceDevice.getEnabled());
        targetDevice.setCreatedAt(Instant.now());
        targetDevice = cognitionDeviceRepository.save(targetDevice);
        for (DeviceDisplayItem sourceItem : deviceItemRepository
            .findByCognitionDeviceIdOrderBySortOrderAscIdAsc(sourceDevice.getId())) {
          DeviceDisplayItem targetItem = copyDeviceItem(sourceItem, targetDevice.getId());
          targetItem = deviceItemRepository.save(targetItem);
          TerminalOperation sourceOperation = operationRepository.findByDeviceDisplayItemId(sourceItem.getId()).orElse(null);
          if (sourceOperation != null) copyTerminalOperation(sourceOperation, targetItem.getId(), terminalMap);
        }
      }
    }
    return copied;
  }

  private DeviceDisplayItem copyDeviceItem(DeviceDisplayItem source, Long cognitionDeviceId) {
    DeviceDisplayItem target = new DeviceDisplayItem();
    target.setCognitionDeviceId(cognitionDeviceId);
    target.setTitle(source.getTitle());
    target.setImageUrl(source.getImageUrl());
    target.setImageData(copyBytes(source.getImageData()));
    target.setImageContentType(source.getImageContentType());
    target.setMediaType(source.getMediaType());
    target.setVideoPath(source.getVideoPath());
    target.setLeftPercent(source.getLeftPercent()); target.setTopPercent(source.getTopPercent());
    target.setWidthPercent(source.getWidthPercent()); target.setHeightPercent(source.getHeightPercent());
    target.setContent(source.getContent()); target.setSortOrder(source.getSortOrder());
    target.setEnabled(source.getEnabled()); target.setCreatedAt(Instant.now());
    return target;
  }

  private void copyTerminalOperation(TerminalOperation source, Long targetItemId, Map<Long, Long> terminalMap) {
    List<TerminalOperationTerminal> sourceTerminals = operationTerminalRepository
        .findByTerminalOperationIdOrderBySortOrderAscIdAsc(source.getId());
    if (sourceTerminals.isEmpty()) return;
    Long mappedFirst = terminalMap.get(sourceTerminals.get(0).getTerminalId());
    Terminal mappedTerminal = terminalRepository.findAllWithCabinetAndStripByIdIn(Collections.singleton(mappedFirst))
        .stream().findFirst().orElseThrow(() -> new IllegalStateException("预检后的目标端子不存在"));
    TerminalOperation target = new TerminalOperation();
    target.setDeviceDisplayItemId(targetItemId);
    target.setTerminalStripId(mappedTerminal.getTerminalStrip().getId());
    target = operationRepository.save(target);
    for (TerminalOperationTerminal sourceTerminal : sourceTerminals) {
      TerminalOperationTerminal copy = new TerminalOperationTerminal();
      copy.setTerminalOperationId(target.getId());
      copy.setTerminalId(terminalMap.get(sourceTerminal.getTerminalId()));
      copy.setExpectedOutputCode(sourceTerminal.getExpectedOutputCode());
      copy.setSortOrder(sourceTerminal.getSortOrder());
      operationTerminalRepository.save(copy);
    }
  }

  private int copyDrawingLearning(Long sourceCabinetId, Long targetCabinetId) {
    int copied = 0;
    for (DrawingGroup sourceGroup : drawingGroupRepository
        .findByScreenCabinetIdOrderByDrawingTypeAscSortOrderAscIdAsc(sourceCabinetId)) {
      DrawingGroup group = new DrawingGroup();
      group.setScreenCabinetId(targetCabinetId); group.setDrawingType(sourceGroup.getDrawingType());
      group.setName(sourceGroup.getName()); group.setSortOrder(sourceGroup.getSortOrder());
      group.setEnabled(sourceGroup.getEnabled()); group.setCreatedAt(Instant.now());
      group = drawingGroupRepository.save(group); copied++;
      for (DrawingPage sourcePage : drawingPageRepository
          .findByDrawingGroupIdOrderBySortOrderAscIdAsc(sourceGroup.getId())) {
        DrawingPage page = new DrawingPage();
        page.setDrawingGroupId(group.getId()); page.setTitle(sourcePage.getTitle());
        page.setImageUrl(sourcePage.getImageUrl()); page.setImageData(copyBytes(sourcePage.getImageData()));
        page.setImageContentType(sourcePage.getImageContentType()); page.setSortOrder(sourcePage.getSortOrder());
        page.setEnabled(sourcePage.getEnabled()); page.setCreatedAt(Instant.now());
        page = drawingPageRepository.save(page);
        for (DrawingCognitionItem sourceItem : drawingItemRepository
            .findByDrawingPageIdOrderBySortOrderAscIdAsc(sourcePage.getId())) {
          DrawingCognitionItem item = new DrawingCognitionItem();
          item.setDrawingPageId(page.getId()); item.setTitle(sourceItem.getTitle()); item.setContent(sourceItem.getContent());
          item.setLeftPercent(sourceItem.getLeftPercent()); item.setTopPercent(sourceItem.getTopPercent());
          item.setWidthPercent(sourceItem.getWidthPercent()); item.setHeightPercent(sourceItem.getHeightPercent());
          item.setSortOrder(sourceItem.getSortOrder()); item.setEnabled(sourceItem.getEnabled()); item.setCreatedAt(Instant.now());
          drawingItemRepository.save(item);
        }
      }
    }
    return copied;
  }

  private int copyLogicLearning(Map<Long, Long> logicMap) {
    int copied = 0;
    for (Map.Entry<Long, Long> entry : logicMap.entrySet()) {
      LogicLearningConfig sourceConfig = logicConfigRepository.findByLogicDiagramId(entry.getKey()).orElse(null);
      if (sourceConfig != null) {
        LogicLearningConfig target = new LogicLearningConfig();
        target.setLogicDiagramId(entry.getValue()); target.setSortOrder(sourceConfig.getSortOrder());
        logicConfigRepository.save(target);
      }
      for (LogicNodeCognitionItem source : logicItemRepository
          .findByLogicDiagramIdIn(Collections.singleton(entry.getKey()))) {
        LogicNodeCognitionItem target = new LogicNodeCognitionItem();
        target.setLogicDiagramId(entry.getValue()); target.setNodeId(source.getNodeId());
        target.setNodeType(source.getNodeType()); target.setNodeName(source.getNodeName()); target.setTitle(source.getTitle());
        target.setImageUrl(source.getImageUrl()); target.setImageData(copyBytes(source.getImageData()));
        target.setImageContentType(source.getImageContentType()); target.setMediaType(source.getMediaType());
        target.setVideoPath(source.getVideoPath()); target.setLeftPercent(source.getLeftPercent());
        target.setTopPercent(source.getTopPercent()); target.setWidthPercent(source.getWidthPercent());
        target.setHeightPercent(source.getHeightPercent()); target.setContent(source.getContent());
        target.setSortOrder(source.getSortOrder()); target.setEnabled(source.getEnabled()); target.setCreatedAt(Instant.now());
        logicItemRepository.save(target);
      }
      for (ExperimentGuideItem source : experimentGuideItemRepository
          .findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.LOGIC_DIAGRAM, entry.getKey())) {
        ExperimentGuideItem target = new ExperimentGuideItem();
        target.setScopeType(SettingListScopeType.LOGIC_DIAGRAM);
        target.setScopeId(entry.getValue());
        target.setType(source.getType());
        target.setTitle(source.getTitle());
        target.setImageUrl(source.getImageUrl());
        target.setImageData(copyBytes(source.getImageData()));
        target.setImageContentType(source.getImageContentType());
        target.setContent(source.getContent());
        target.setSortOrder(source.getSortOrder());
        target.setEnabled(source.getEnabled());
        target.setCreatedAt(Instant.now());
        experimentGuideItemRepository.save(target);
      }
      copied++;
    }
    return copied;
  }

  private int copyLogicGroups(TargetAnalysis target) {
    int copied = 0;
    for (Map.Entry<Long, Long> deviceEntry : target.deviceMap.entrySet()) {
      Long sourceDeviceId = deviceEntry.getKey();
      Long targetDeviceId = deviceEntry.getValue();
      for (LogicGroup sourceGroup : logicGroupRepository
          .findByIedDeviceIdOrderBySortOrderAscIdAsc(sourceDeviceId)) {
        LogicGroup targetGroup = new LogicGroup();
        targetGroup.setIedDeviceId(targetDeviceId);
        targetGroup.setName(sourceGroup.getName());
        targetGroup.setSortOrder(sourceGroup.getSortOrder());
        targetGroup = logicGroupRepository.save(targetGroup);
        target.logicGroupMap.put(sourceGroup.getId(), targetGroup.getId());
        for (LogicGroupMember sourceMember : logicGroupMemberRepository
            .findByGroupIdOrderBySortOrderAscIdAsc(sourceGroup.getId())) {
          Long targetLogicId = target.groupLogicMap.get(sourceMember.getLogicDiagramId());
          if (targetLogicId == null) {
            throw new IllegalStateException("预检后的组合逻辑成员基础逻辑映射不存在");
          }
          LogicGroupMember member = new LogicGroupMember();
          member.setGroupId(targetGroup.getId());
          member.setLogicDiagramId(targetLogicId);
          member.setSortOrder(sourceMember.getSortOrder());
          logicGroupMemberRepository.save(member);
        }
        copyGroupGuides(sourceGroup.getId(), targetGroup.getId());
        copied++;
      }
    }
    return copied;
  }

  private void copyGroupGuides(Long sourceGroupId, Long targetGroupId) {
    for (ExperimentGuideItem source : experimentGuideItemRepository
        .findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.LOGIC_GROUP, sourceGroupId)) {
      ExperimentGuideItem guide = new ExperimentGuideItem();
      guide.setScopeType(SettingListScopeType.LOGIC_GROUP);
      guide.setScopeId(targetGroupId);
      guide.setType(source.getType());
      guide.setTitle(source.getTitle());
      guide.setImageUrl(source.getImageUrl());
      guide.setImageData(copyBytes(source.getImageData()));
      guide.setImageContentType(source.getImageContentType());
      guide.setContent(source.getContent());
      guide.setSortOrder(source.getSortOrder());
      guide.setEnabled(source.getEnabled());
      guide.setCreatedAt(Instant.now());
      experimentGuideItemRepository.save(guide);
    }
  }

  private int copyBaselineConfig(ConfigCopyScope scope, Long sourceId, TargetAnalysis target) {
    Set<Long> deviceIds = new LinkedHashSet<>();
    Set<Long> logicIds = new LinkedHashSet<>();
    Set<Long> groupIds = new LinkedHashSet<>();
    collectSourceScopeIds(scope, sourceId, deviceIds, logicIds, groupIds);
    int copied = 0;
    copied += copySettingItems(target, deviceIds, logicIds, groupIds);
    copied += copySoftPressboardItems(target, deviceIds, logicIds, groupIds);
    copied += copyHardPressboardItems(target, deviceIds, logicIds, groupIds);
    copied += copyWiringConfigs(target, deviceIds, logicIds, groupIds);
    return copied;
  }

  private int copySettingItems(TargetAnalysis target, Set<Long> deviceIds, Set<Long> logicIds, Set<Long> groupIds) {
    int copied = 0;
    copied += copySettingItemsForScope(target, SettingListScopeType.IED_DEVICE, deviceIds);
    copied += copySettingItemsForScope(target, SettingListScopeType.LOGIC_DIAGRAM, logicIds);
    copied += copySettingItemsForScope(target, SettingListScopeType.LOGIC_GROUP, groupIds);
    return copied;
  }

  private int copySettingItemsForScope(TargetAnalysis target, SettingListScopeType scopeType, Set<Long> sourceScopeIds) {
    if (sourceScopeIds.isEmpty()) return 0;
    int copied = 0;
    for (SettingListItem source : settingItemRepository.findByScopeTypeAndScopeIdIn(scopeType, sourceScopeIds)) {
      Long targetScopeId = mapScopeId(target, scopeType, source.getScopeId());
      if (targetScopeId == null) continue;
      SettingListItem item = new SettingListItem();
      item.setScopeType(scopeType);
      item.setScopeId(targetScopeId);
      item.setSettingRef(remapRef(target, scopeType, source.getScopeId(), source.getSettingRef()));
      item.setSettingFc(source.getSettingFc());
      item.setSettingName(source.getSettingName());
      item.setValueType(source.getValueType());
      item.setCompareEnabled(source.getCompareEnabled());
      item.setBaselineValue(source.getBaselineValue());
      item.setSortOrder(source.getSortOrder());
      settingItemRepository.save(item);
      copied++;
    }
    return copied;
  }

  private int copySoftPressboardItems(TargetAnalysis target, Set<Long> deviceIds, Set<Long> logicIds, Set<Long> groupIds) {
    int copied = 0;
    copied += copySoftPressboardItemsForScope(target, SettingListScopeType.IED_DEVICE, deviceIds);
    copied += copySoftPressboardItemsForScope(target, SettingListScopeType.LOGIC_DIAGRAM, logicIds);
    copied += copySoftPressboardItemsForScope(target, SettingListScopeType.LOGIC_GROUP, groupIds);
    return copied;
  }

  private int copySoftPressboardItemsForScope(TargetAnalysis target, SettingListScopeType scopeType, Set<Long> sourceScopeIds) {
    if (sourceScopeIds.isEmpty()) return 0;
    int copied = 0;
    for (SoftPressboardListItem source : softPressboardItemRepository.findByScopeTypeAndScopeIdIn(scopeType, sourceScopeIds)) {
      Long targetScopeId = mapScopeId(target, scopeType, source.getScopeId());
      if (targetScopeId == null) continue;
      SoftPressboardListItem item = new SoftPressboardListItem();
      item.setScopeType(scopeType);
      item.setScopeId(targetScopeId);
      item.setPressboardRef(remapRef(target, scopeType, source.getScopeId(), source.getPressboardRef()));
      item.setPressboardName(source.getPressboardName());
      item.setBaselineValue(source.getBaselineValue());
      item.setCompareEnabled(source.getCompareEnabled());
      item.setSortOrder(source.getSortOrder());
      softPressboardItemRepository.save(item);
      copied++;
    }
    return copied;
  }

  private int copyHardPressboardItems(TargetAnalysis target, Set<Long> deviceIds, Set<Long> logicIds, Set<Long> groupIds) {
    int copied = 0;
    copied += copyHardPressboardItemsForScope(target, SettingListScopeType.IED_DEVICE, deviceIds);
    copied += copyHardPressboardItemsForScope(target, SettingListScopeType.LOGIC_DIAGRAM, logicIds);
    copied += copyHardPressboardItemsForScope(target, SettingListScopeType.LOGIC_GROUP, groupIds);
    return copied;
  }

  private int copyHardPressboardItemsForScope(TargetAnalysis target, SettingListScopeType scopeType, Set<Long> sourceScopeIds) {
    if (sourceScopeIds.isEmpty()) return 0;
    int copied = 0;
    for (HardPressboardListItem source : hardPressboardItemRepository.findByScopeTypeAndScopeIdIn(scopeType, sourceScopeIds)) {
      Long targetScopeId = mapScopeId(target, scopeType, source.getScopeId());
      if (targetScopeId == null) continue;
      Long targetCabinetId = targetService.require(scopeType, targetScopeId).getCabinetId();
      Map<String, HardPressboard> targetByName = new LinkedHashMap<>();
      for (HardPressboard pb : hardPressboardCatalogService.list(targetCabinetId)) {
        targetByName.put(pb.getName(), pb);
      }
      HardPressboard targetPb = targetByName.get(source.getPressboardName());
      if (targetPb == null) continue;
      HardPressboardListItem item = new HardPressboardListItem();
      item.setScopeType(scopeType);
      item.setScopeId(targetScopeId);
      item.setPressboardRef(String.valueOf(targetPb.getId()));
      item.setPressboardName(targetPb.getName());
      item.setBaselineValue(source.getBaselineValue());
      item.setCompareEnabled(source.getCompareEnabled());
      item.setSortOrder(source.getSortOrder());
      hardPressboardItemRepository.save(item);
      copied++;
    }
    return copied;
  }

  private int copyWiringConfigs(TargetAnalysis target, Set<Long> deviceIds, Set<Long> logicIds, Set<Long> groupIds) {
    int copied = 0;
    copied += copyWiringConfigsForScope(target, SettingListScopeType.IED_DEVICE, deviceIds);
    copied += copyWiringConfigsForScope(target, SettingListScopeType.LOGIC_DIAGRAM, logicIds);
    copied += copyWiringConfigsForScope(target, SettingListScopeType.LOGIC_GROUP, groupIds);
    return copied;
  }

  private int copyWiringConfigsForScope(TargetAnalysis target, SettingListScopeType scopeType, Set<Long> sourceScopeIds) {
    if (sourceScopeIds.isEmpty()) return 0;
    int copied = 0;
    for (WiringRequirementConfig source : wiringConfigRepository.findByScopeTypeAndScopeIdIn(scopeType, sourceScopeIds)) {
      Long targetScopeId = mapScopeId(target, scopeType, source.getScopeId());
      if (targetScopeId == null) continue;
      WiringRequirementConfig config = new WiringRequirementConfig();
      config.setScopeType(scopeType);
      config.setScopeId(targetScopeId);
      config.setCategory(source.getCategory());
      config.setRequired(source.getRequired());
      config.setPhaseMode(source.getPhaseMode());
      config = wiringConfigRepository.save(config);
      for (WiringRequirementGroup sourceGroup : wiringGroupRepository
          .findByConfigIdOrderByGroupNoAscIdAsc(source.getId())) {
        WiringRequirementGroup group = new WiringRequirementGroup();
        group.setConfigId(config.getId());
        group.setGroupNo(sourceGroup.getGroupNo());
        group.setTerminalAId(mapTerminal(target.terminalMap, sourceGroup.getTerminalAId()));
        group.setTerminalBId(mapTerminal(target.terminalMap, sourceGroup.getTerminalBId()));
        group.setTerminalCId(mapTerminal(target.terminalMap, sourceGroup.getTerminalCId()));
        group.setTerminalNId(mapTerminal(target.terminalMap, sourceGroup.getTerminalNId()));
        wiringGroupRepository.save(group);
      }
      copied++;
    }
    return copied;
  }

  private Long mapScopeId(TargetAnalysis target, SettingListScopeType scopeType, Long sourceScopeId) {
    if (scopeType == SettingListScopeType.IED_DEVICE) return target.deviceMap.get(sourceScopeId);
    if (scopeType == SettingListScopeType.LOGIC_DIAGRAM) return target.logicMap.get(sourceScopeId);
    return target.logicGroupMap.get(sourceScopeId);
  }

  private Long mapTerminal(Map<Long, Long> terminalMap, Long sourceTerminalId) {
    return sourceTerminalId == null ? null : terminalMap.get(sourceTerminalId);
  }

  private String remapRef(TargetAnalysis target, SettingListScopeType scopeType, Long sourceScopeId, String ref) {
    Long targetScopeId = mapScopeId(target, scopeType, sourceScopeId);
    if (targetScopeId == null) return ref;
    String sourceIedName = targetService.require(scopeType, sourceScopeId).getIedName();
    String targetIedName = targetService.require(scopeType, targetScopeId).getIedName();
    return replaceIedName(ref, sourceIedName, targetIedName);
  }

  private String replaceIedName(String ref, String sourceIedName, String targetIedName) {
    if (ref == null || !StringUtils.hasText(sourceIedName) || !StringUtils.hasText(targetIedName)) {
      return ref;
    }
    String rel = stripIedName(ref, sourceIedName);
    if (Objects.equals(rel, ref.trim())) return ref;
    return targetIedName + "/" + rel;
  }

  private int copySamplingTests(Long sourceCabinetId, Long targetCabinetId, Map<Long, Long> terminalMap) {
    int copied = 0;
    for (SamplingTestItem source : samplingItemRepository
        .findByScreenCabinetIdOrderBySortOrderAscIdAsc(sourceCabinetId)) {
      SamplingTestItem target = new SamplingTestItem();
      target.setScreenCabinetId(targetCabinetId); target.setTitle(source.getTitle());
      target.setMediaType(source.getMediaType()); target.setImageUrl(source.getImageUrl());
      target.setImageData(copyBytes(source.getImageData())); target.setImageContentType(source.getImageContentType());
      target.setVideoPath(source.getVideoPath()); target.setContent(source.getContent());
      target.setSortOrder(source.getSortOrder()); target.setEnabled(source.getEnabled()); target.setCreatedAt(Instant.now());
      target = samplingItemRepository.save(target); copied++;
      for (SamplingTestChannel sourceChannel : samplingChannelRepository
          .findBySamplingTestItemIdOrderBySortOrderAscIdAsc(source.getId())) {
        SamplingTestChannel channel = new SamplingTestChannel();
        channel.setSamplingTestItemId(target.getId()); channel.setOutputCode(sourceChannel.getOutputCode());
        channel.setTerminalId(terminalMap.get(sourceChannel.getTerminalId()));
        channel.setBaselineMagnitude(sourceChannel.getBaselineMagnitude());
        channel.setBaselineAngle(sourceChannel.getBaselineAngle()); channel.setSortOrder(sourceChannel.getSortOrder());
        samplingChannelRepository.save(channel);
      }
    }
    return copied;
  }

  private int copyLearningResources(Long sourceCabinetId, Long targetCabinetId) {
    int copied = 0;
    for (LearningResource source : resourceRepository.findByScreenCabinetId(sourceCabinetId)) {
      LearningResource target = new LearningResource();
      target.setName(source.getName()); target.setDescription(source.getDescription());
      target.setResourceType(source.getResourceType()); target.setScreenCabinetId(targetCabinetId);
      target.setFilePath(source.getFilePath()); target.setOriginalFilename(source.getOriginalFilename());
      target.setContentType(source.getContentType()); target.setFileSize(source.getFileSize());
      target.setCreatedAt(Instant.now()); target.setUpdatedAt(Instant.now());
      resourceRepository.save(target); copied++;
    }
    return copied;
  }

  private void deleteCabinetLearning(Long cabinetId) {
    List<CabinetDisplayItem> parents = cabinetItemRepository.findByScreenCabinetIdOrderBySortOrderAscIdAsc(cabinetId);
    if (parents.isEmpty()) return;
    parents.stream().map(CabinetDisplayItem::getImageUrl).filter(StringUtils::hasText)
        .forEach(mediaCleanupService::scheduleCabinetImageDeletion);
    List<Long> parentIds = parents.stream().map(CabinetDisplayItem::getId).collect(Collectors.toList());
    List<CognitionDevice> devices = cognitionDeviceRepository.findByCabinetDisplayItemIdIn(parentIds);
    if (!devices.isEmpty()) {
      List<Long> deviceIds = devices.stream().map(CognitionDevice::getId).collect(Collectors.toList());
      List<DeviceDisplayItem> items = deviceItemRepository.findByCognitionDeviceIdIn(deviceIds);
      if (!items.isEmpty()) {
        items.stream().map(DeviceDisplayItem::getImageUrl).filter(StringUtils::hasText)
            .forEach(mediaCleanupService::scheduleDeviceImageDeletion);
        items.stream().map(DeviceDisplayItem::getVideoPath).filter(StringUtils::hasText)
            .forEach(mediaCleanupService::scheduleCognitionVideoDeletion);
        List<Long> itemIds = items.stream().map(DeviceDisplayItem::getId).collect(Collectors.toList());
        List<TerminalOperation> operations = operationRepository.findByDeviceDisplayItemIdIn(itemIds);
        if (!operations.isEmpty()) operationTerminalRepository.deleteByTerminalOperationIdIn(
            operations.stream().map(TerminalOperation::getId).collect(Collectors.toList()));
        operationRepository.deleteByDeviceDisplayItemIdIn(itemIds);
        deviceItemRepository.deleteByCognitionDeviceIdIn(deviceIds);
      }
      cognitionDeviceRepository.deleteByCabinetDisplayItemIdIn(parentIds);
    }
    cabinetItemRepository.deleteByScreenCabinetId(cabinetId);
  }

  private void deleteDrawingLearning(Long cabinetId) {
    List<DrawingGroup> groups = drawingGroupRepository
        .findByScreenCabinetIdOrderByDrawingTypeAscSortOrderAscIdAsc(cabinetId);
    if (groups.isEmpty()) return;
    List<Long> groupIds = groups.stream().map(DrawingGroup::getId).collect(Collectors.toList());
    List<DrawingPage> pages = drawingPageRepository.findByDrawingGroupIdIn(groupIds);
    pages.stream().map(DrawingPage::getImageUrl).filter(StringUtils::hasText)
        .forEach(mediaCleanupService::scheduleCabinetImageDeletion);
    if (!pages.isEmpty()) drawingItemRepository.deleteByDrawingPageIdIn(
        pages.stream().map(DrawingPage::getId).collect(Collectors.toList()));
    for (Long groupId : groupIds) drawingPageRepository.deleteByDrawingGroupId(groupId);
    drawingGroupRepository.deleteAll(groups);
  }

  private void deleteLogicLearning(Collection<Long> logicIds) {
    if (logicIds.isEmpty()) return;
    List<LogicNodeCognitionItem> items = logicItemRepository.findByLogicDiagramIdIn(logicIds);
    items.stream()
        .map(LogicNodeCognitionItem::getVideoPath).filter(StringUtils::hasText)
        .forEach(mediaCleanupService::scheduleCognitionVideoDeletion);
    items.stream().map(LogicNodeCognitionItem::getImageUrl).filter(StringUtils::hasText)
        .forEach(mediaCleanupService::scheduleDeviceImageDeletion);
    logicItemRepository.deleteByLogicDiagramIdIn(logicIds);
    logicConfigRepository.deleteByLogicDiagramIdIn(logicIds);

    List<ExperimentGuideItem> guides = experimentGuideItemRepository
        .findByScopeTypeAndScopeIdIn(SettingListScopeType.LOGIC_DIAGRAM, logicIds);
    guides.stream().map(ExperimentGuideItem::getImageUrl).filter(StringUtils::hasText)
        .forEach(mediaCleanupService::scheduleDeviceImageDeletion);
    experimentGuideItemRepository.deleteByScopeTypeAndScopeIdIn(SettingListScopeType.LOGIC_DIAGRAM, logicIds);
  }

  private void deleteSamplingTests(Long cabinetId) {
    List<SamplingTestItem> items = samplingItemRepository
        .findByScreenCabinetIdOrderBySortOrderAscIdAsc(cabinetId);
    if (items.isEmpty()) return;
    items.stream().map(SamplingTestItem::getVideoPath).filter(StringUtils::hasText)
        .forEach(mediaCleanupService::scheduleCognitionVideoDeletion);
    samplingChannelRepository.deleteBySamplingTestItemIdIn(
        items.stream().map(SamplingTestItem::getId).collect(Collectors.toList()));
    samplingItemRepository.deleteByScreenCabinetId(cabinetId);
  }

  private void deleteLearningResources(Long cabinetId) {
    List<LearningResource> resources = resourceRepository.findByScreenCabinetId(cabinetId);
    resources.stream().map(LearningResource::getFilePath).filter(StringUtils::hasText)
        .forEach(mediaCleanupService::scheduleLearningResourceDeletion);
    resourceRepository.deleteByScreenCabinetId(cabinetId);
  }

  private void deleteLogicGroups(Collection<Long> targetDeviceIds) {
    if (targetDeviceIds == null || targetDeviceIds.isEmpty()) return;
    List<LogicGroup> groups = logicGroupRepository.findByIedDeviceIdIn(targetDeviceIds);
    if (groups.isEmpty()) return;
    List<Long> groupIds = groups.stream().map(LogicGroup::getId).collect(Collectors.toList());
    experimentGuideItemRepository.deleteByScopeTypeAndScopeIdIn(SettingListScopeType.LOGIC_GROUP, groupIds);
    for (Long groupId : groupIds) {
      logicGroupMemberRepository.deleteByGroupId(groupId);
    }
    logicGroupRepository.deleteByIedDeviceIdIn(targetDeviceIds);
  }

  private void deleteBaselineConfig(ConfigCopyScope scope, Long targetId) {
    Set<Long> deviceIds = new LinkedHashSet<>();
    Set<Long> logicIds = new LinkedHashSet<>();
    Set<Long> groupIds = new LinkedHashSet<>();
    collectSourceScopeIds(scope, targetId, deviceIds, logicIds, groupIds);
    deleteSettingItems(deviceIds, logicIds, groupIds);
    deleteSoftPressboardItems(deviceIds, logicIds, groupIds);
    deleteHardPressboardItems(deviceIds, logicIds, groupIds);
    deleteWiringConfigs(deviceIds, logicIds, groupIds);
    settingItemRepository.flush();
    softPressboardItemRepository.flush();
    hardPressboardItemRepository.flush();
    wiringConfigRepository.flush();
  }

  private void deleteSettingItems(Set<Long> deviceIds, Set<Long> logicIds, Set<Long> groupIds) {
    if (!deviceIds.isEmpty()) settingItemRepository.deleteByScopeTypeAndScopeIdIn(SettingListScopeType.IED_DEVICE, deviceIds);
    if (!logicIds.isEmpty()) settingItemRepository.deleteByScopeTypeAndScopeIdIn(SettingListScopeType.LOGIC_DIAGRAM, logicIds);
    if (!groupIds.isEmpty()) settingItemRepository.deleteByScopeTypeAndScopeIdIn(SettingListScopeType.LOGIC_GROUP, groupIds);
  }

  private void deleteSoftPressboardItems(Set<Long> deviceIds, Set<Long> logicIds, Set<Long> groupIds) {
    if (!deviceIds.isEmpty()) softPressboardItemRepository.deleteByScopeTypeAndScopeIdIn(SettingListScopeType.IED_DEVICE, deviceIds);
    if (!logicIds.isEmpty()) softPressboardItemRepository.deleteByScopeTypeAndScopeIdIn(SettingListScopeType.LOGIC_DIAGRAM, logicIds);
    if (!groupIds.isEmpty()) softPressboardItemRepository.deleteByScopeTypeAndScopeIdIn(SettingListScopeType.LOGIC_GROUP, groupIds);
  }

  private void deleteHardPressboardItems(Set<Long> deviceIds, Set<Long> logicIds, Set<Long> groupIds) {
    if (!deviceIds.isEmpty()) hardPressboardItemRepository.deleteByScopeTypeAndScopeIdIn(SettingListScopeType.IED_DEVICE, deviceIds);
    if (!logicIds.isEmpty()) hardPressboardItemRepository.deleteByScopeTypeAndScopeIdIn(SettingListScopeType.LOGIC_DIAGRAM, logicIds);
    if (!groupIds.isEmpty()) hardPressboardItemRepository.deleteByScopeTypeAndScopeIdIn(SettingListScopeType.LOGIC_GROUP, groupIds);
  }

  private void deleteWiringConfigs(Set<Long> deviceIds, Set<Long> logicIds, Set<Long> groupIds) {
    deleteWiringConfigsForScope(SettingListScopeType.IED_DEVICE, deviceIds);
    deleteWiringConfigsForScope(SettingListScopeType.LOGIC_DIAGRAM, logicIds);
    deleteWiringConfigsForScope(SettingListScopeType.LOGIC_GROUP, groupIds);
  }

  private void deleteWiringConfigsForScope(SettingListScopeType scopeType, Set<Long> scopeIds) {
    if (scopeIds.isEmpty()) return;
    List<WiringRequirementConfig> configs = wiringConfigRepository.findByScopeTypeAndScopeIdIn(scopeType, scopeIds);
    if (configs.isEmpty()) return;
    List<Long> configIds = configs.stream().map(WiringRequirementConfig::getId).collect(Collectors.toList());
    wiringGroupRepository.deleteByConfigIdIn(configIds);
    wiringConfigRepository.deleteByScopeTypeAndScopeIdIn(scopeType, scopeIds);
  }

  private int countModule(ConfigCopyScope scope, Long id, ConfigCopyModule module) {
    if (scope == ConfigCopyScope.DEVICE) {
      switch (module) {
        case LOGIC_LEARNING:
          return configuredLogics(logicRepository.findByDeviceIdOrderByIdAsc(id)).size();
        case LOGIC_GROUP:
          return logicGroupRepository.findByIedDeviceIdOrderBySortOrderAscIdAsc(id).size();
        case BASELINE_CONFIG:
          return countBaselineConfigAtDevice(id);
        default:
          return 0;
      }
    }
    switch (module) {
      case CABINET_LEARNING:
        return cabinetItemRepository.findByScreenCabinetIdOrderBySortOrderAscIdAsc(id).size();
      case DRAWING_LEARNING:
        return drawingGroupRepository.findByScreenCabinetIdOrderByDrawingTypeAscSortOrderAscIdAsc(id).size();
      case LOGIC_LEARNING:
        return configuredLogics(logicRepository.findByDeviceCabinetIdOrderByIdAsc(id)).size();
      case LOGIC_GROUP:
        return countLogicGroups(id);
      case BASELINE_CONFIG:
        return countBaselineConfig(id);
      case SAMPLING_TEST:
        return samplingItemRepository.findByScreenCabinetIdOrderBySortOrderAscIdAsc(id).size();
      case LEARNING_RESOURCE:
        return resourceRepository.findByScreenCabinetId(id).size();
      default:
        return 0;
    }
  }

  private int countLogicGroups(Long cabinetId) {
    List<Long> deviceIds = deviceRepository.findByCabinetIdOrderByIdAsc(cabinetId).stream()
        .map(Device::getId).collect(Collectors.toList());
    if (deviceIds.isEmpty()) return 0;
    return logicGroupRepository.findByIedDeviceIdIn(deviceIds).size();
  }

  private int countBaselineConfig(Long cabinetId) {
    Set<Long> deviceIds = new LinkedHashSet<>();
    Set<Long> logicIds = new LinkedHashSet<>();
    Set<Long> groupIds = new LinkedHashSet<>();
    collectSourceScopeIds(ConfigCopyScope.CABINET, cabinetId, deviceIds, logicIds, groupIds);
    return countBaselineItems(deviceIds, logicIds, groupIds);
  }

  private int countBaselineConfigAtDevice(Long deviceId) {
    Set<Long> deviceIds = new LinkedHashSet<>();
    Set<Long> logicIds = new LinkedHashSet<>();
    Set<Long> groupIds = new LinkedHashSet<>();
    collectSourceScopeIds(ConfigCopyScope.DEVICE, deviceId, deviceIds, logicIds, groupIds);
    return countBaselineItems(deviceIds, logicIds, groupIds);
  }

  private int countBaselineItems(Set<Long> deviceIds, Set<Long> logicIds, Set<Long> groupIds) {
    int count = 0;
    count += countBaselineScope(SettingListScopeType.IED_DEVICE, deviceIds);
    count += countBaselineScope(SettingListScopeType.LOGIC_DIAGRAM, logicIds);
    count += countBaselineScope(SettingListScopeType.LOGIC_GROUP, groupIds);
    return count;
  }

  private int countBaselineScope(SettingListScopeType scopeType, Set<Long> scopeIds) {
    if (scopeIds.isEmpty()) return 0;
    return settingItemRepository.findByScopeTypeAndScopeIdIn(scopeType, scopeIds).size()
        + softPressboardItemRepository.findByScopeTypeAndScopeIdIn(scopeType, scopeIds).size()
        + hardPressboardItemRepository.findByScopeTypeAndScopeIdIn(scopeType, scopeIds).size()
        + wiringConfigRepository.findByScopeTypeAndScopeIdIn(scopeType, scopeIds).size();
  }

  private List<Long> logicIdsForCabinet(Long cabinetId) {
    return logicRepository.findByDeviceCabinetIdOrderByIdAsc(cabinetId).stream()
        .map(ProtectionLogic::getId).collect(Collectors.toList());
  }

  private List<Long> logicIdsForDevice(Long deviceId) {
    return logicRepository.findByDeviceIdOrderByIdAsc(deviceId).stream()
        .map(ProtectionLogic::getId).collect(Collectors.toList());
  }

  private Cabinet requireCabinet(Long id) {
    return cabinetRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "屏柜不存在"));
  }

  private Device requireDevice(Long id) {
    return deviceRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "装置不存在"));
  }

  private DeviceCandidateResponse candidateResponse(Device device) {
    return new DeviceCandidateResponse(device.getId(), device.getIedName(), device.getName(), device.getDeviceType());
  }

  private ResolvedDeviceMappingResponse mappingResponse(Device source, Device target, boolean automatic,
                                                         List<DeviceCandidateResponse> candidates) {
    return new ResolvedDeviceMappingResponse(source.getId(), source.getIedName(), source.getName(),
        target == null ? null : target.getId(), target == null ? null : target.getIedName(),
        target == null ? null : target.getName(), automatic, candidates);
  }

  private JsonNode normalizeJsonTree(JsonNode node, String iedName, Long sourceDeviceId) {
    if (node == null || node.isNull()) return node;
    if (node.isTextual()) {
      return TextNode.valueOf(normalizeInstanceText(node.textValue(), iedName, sourceDeviceId));
    }
    if (node.isArray()) {
      ArrayNode normalized = objectMapper.createArrayNode();
      node.forEach(child -> normalized.add(normalizeJsonTree(child, iedName, sourceDeviceId)));
      return normalized;
    }
    if (node.isObject()) {
      ObjectNode normalized = objectMapper.createObjectNode();
      node.fields().forEachRemaining(field -> normalized.set(
          normalizeInstanceText(field.getKey(), iedName, sourceDeviceId),
          normalizeJsonTree(field.getValue(), iedName, sourceDeviceId)));
      return normalized;
    }
    return node.deepCopy();
  }

  private String normalizeMappedReference(String value, boolean sourceSide, TargetAnalysis result) {
    if (!StringUtils.hasText(value)) return null;
    String normalized = value.trim();
    List<Map.Entry<Long, Long>> mappings = new ArrayList<>(result.deviceMap.entrySet());
    mappings.sort((left, right) -> {
      Device leftDevice = requireDevice(sourceSide ? left.getKey() : left.getValue());
      Device rightDevice = requireDevice(sourceSide ? right.getKey() : right.getValue());
      return Integer.compare(rightDevice.getIedName().length(), leftDevice.getIedName().length());
    });
    for (Map.Entry<Long, Long> mapping : mappings) {
      Device device = requireDevice(sourceSide ? mapping.getKey() : mapping.getValue());
      normalized = normalizeInstanceText(normalized, device.getIedName(), mapping.getKey());
    }
    return normalized;
  }

  private String normalizeInstanceText(String value, String iedName, Long sourceDeviceId) {
    if (value == null) return null;
    String normalized = value.trim();
    if (!StringUtils.hasText(iedName)) return normalized;
    return normalized.replace(iedName, "\uE000" + sourceDeviceId + "\uE001");
  }

  private byte[] copyBytes(byte[] value) {
    return value == null ? null : Arrays.copyOf(value, value.length);
  }

  private static class Analysis {
    final ConfigCopyPrecheckResponse response;
    final List<TargetAnalysis> targets;
    Analysis(ConfigCopyPrecheckResponse response, List<TargetAnalysis> targets) {
      this.response = response; this.targets = targets;
    }
  }

  private static class TargetAnalysis {
    final Long targetId;
    final String targetName;
    final List<ConfigCopyIssueResponse> issues = new ArrayList<>();
    final List<ResolvedDeviceMappingResponse> mappingResponses = new ArrayList<>();
    final Map<Long, Long> deviceMap = new LinkedHashMap<>();
    final Map<Long, Long> logicMap = new LinkedHashMap<>();
    final Map<Long, Long> terminalMap = new LinkedHashMap<>();
    final Map<Long, Long> groupLogicMap = new LinkedHashMap<>();
    final Map<Long, Long> logicGroupMap = new LinkedHashMap<>();
    final EnumMap<ConfigCopyModule, Integer> sourceCounts = new EnumMap<>(ConfigCopyModule.class);
    final EnumMap<ConfigCopyModule, Integer> overwriteCounts = new EnumMap<>(ConfigCopyModule.class);
    boolean needsMapping;
    boolean incompatible;
    TargetAnalysis(Long targetId, String targetName) { this.targetId = targetId; this.targetName = targetName; }
    void incompatible(String code, String message, Long sourceId) {
      incompatible = true; issues.add(new ConfigCopyIssueResponse(code, message, sourceId));
    }
    TargetPrecheckResponse toResponse() {
      ConfigCopyStatus status = incompatible ? ConfigCopyStatus.INCOMPATIBLE
          : needsMapping ? ConfigCopyStatus.NEEDS_MAPPING : ConfigCopyStatus.READY;
      return new TargetPrecheckResponse(targetId, targetName, status, issues, mappingResponses,
          sourceCounts, overwriteCounts);
    }
  }
}
