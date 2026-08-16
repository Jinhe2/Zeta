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
import com.zeta.business.entities.learningresource.LearningResource;
import com.zeta.business.entities.learningresource.LearningResourceRepository;
import com.zeta.business.entities.logiclearning.LogicLearningConfig;
import com.zeta.business.entities.logiclearning.LogicLearningConfigRepository;
import com.zeta.business.entities.logicnodecognition.LogicNodeCognitionItem;
import com.zeta.business.entities.logicnodecognition.LogicNodeCognitionItemRepository;
import com.zeta.business.entities.samplingtest.*;
import com.zeta.business.media.CognitionMediaType;
import com.zeta.screen.baseline.IedBaselineSettingItem;
import com.zeta.screen.baseline.IedBaselineSettingItemRepository;
import com.zeta.screen.cabinet.Cabinet;
import com.zeta.screen.cabinet.CabinetRepository;
import com.zeta.screen.ieddevice.Device;
import com.zeta.screen.ieddevice.DeviceRepository;
import com.zeta.screen.logicdiagram.ProtectionLogic;
import com.zeta.screen.logicdiagram.ProtectionLogicRepository;
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
  private final SamplingTestItemRepository samplingItemRepository;
  private final SamplingTestChannelRepository samplingChannelRepository;
  private final LearningResourceRepository resourceRepository;
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
      SamplingTestItemRepository samplingItemRepository,
      SamplingTestChannelRepository samplingChannelRepository,
      LearningResourceRepository resourceRepository,
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
    this.samplingItemRepository = samplingItemRepository;
    this.samplingChannelRepository = samplingChannelRepository;
    this.resourceRepository = resourceRepository;
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
        deleteLogicLearning(logicIdsForDevice(target.targetId));
        copied.put(ConfigCopyModule.LOGIC_LEARNING, copyLogicLearning(target.logicMap));
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
    if (request.getScope() == ConfigCopyScope.DEVICE
        && (!request.getModules().equals(EnumSet.of(ConfigCopyModule.LOGIC_LEARNING)))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "装置级复制仅支持逻辑学习");
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
      if (request.getModules().contains(ConfigCopyModule.CABINET_LEARNING)
          || request.getModules().contains(ConfigCopyModule.SAMPLING_TEST)) {
        validateTerminals(request.getSourceId(), targetCabinet.getId(), request.getModules(), result);
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
      validateLogics(result);
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
    if (modules.contains(ConfigCopyModule.CABINET_LEARNING)
        || modules.contains(ConfigCopyModule.SAMPLING_TEST)) {
      Set<Long> terminalIds = referencedTerminalIds(cabinetId, modules);
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

  private List<ProtectionLogic> configuredLogics(List<ProtectionLogic> candidates) {
    if (candidates.isEmpty()) return Collections.emptyList();
    Set<Long> ids = candidates.stream().map(ProtectionLogic::getId).collect(Collectors.toSet());
    Set<Long> configured = logicConfigRepository.findByLogicDiagramIdIn(ids).stream()
        .map(LogicLearningConfig::getLogicDiagramId).collect(Collectors.toSet());
    logicItemRepository.findByLogicDiagramIdIn(ids).stream()
        .map(LogicNodeCognitionItem::getLogicDiagramId).forEach(configured::add);
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

  private void validateTerminals(Long sourceCabinetId, Long targetCabinetId,
                                 Set<ConfigCopyModule> modules, TargetAnalysis result) {
    Set<Long> terminalIds = referencedTerminalIds(sourceCabinetId, modules);
    if (terminalIds.isEmpty()) return;
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

  private Set<Long> referencedTerminalIds(Long sourceCabinetId, Set<ConfigCopyModule> modules) {
    Set<Long> terminalIds = new LinkedHashSet<>();
    if (modules.contains(ConfigCopyModule.CABINET_LEARNING)) {
      List<CabinetDisplayItem> parents = cabinetItemRepository
          .findByScreenCabinetIdOrderBySortOrderAscIdAsc(sourceCabinetId);
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
          .findByScreenCabinetIdOrderBySortOrderAscIdAsc(sourceCabinetId)) {
        samplingChannelRepository.findBySamplingTestItemIdOrderBySortOrderAscIdAsc(item.getId()).stream()
            .map(SamplingTestChannel::getTerminalId).forEach(terminalIds::add);
      }
    }
    return terminalIds;
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
      copied++;
    }
    return copied;
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

  private int countModule(ConfigCopyScope scope, Long id, ConfigCopyModule module) {
    if (scope == ConfigCopyScope.DEVICE) {
      return configuredLogics(logicRepository.findByDeviceIdOrderByIdAsc(id)).size();
    }
    switch (module) {
      case CABINET_LEARNING:
        return cabinetItemRepository.findByScreenCabinetIdOrderBySortOrderAscIdAsc(id).size();
      case DRAWING_LEARNING:
        return drawingGroupRepository.findByScreenCabinetIdOrderByDrawingTypeAscSortOrderAscIdAsc(id).size();
      case LOGIC_LEARNING:
        return configuredLogics(logicRepository.findByDeviceCabinetIdOrderByIdAsc(id)).size();
      case SAMPLING_TEST:
        return samplingItemRepository.findByScreenCabinetIdOrderBySortOrderAscIdAsc(id).size();
      case LEARNING_RESOURCE:
        return resourceRepository.findByScreenCabinetId(id).size();
      default:
        return 0;
    }
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
