package com.zeta.business.service;

import com.zeta.business.entities.cabinetdisplay.TemporaryImage;
import com.zeta.business.entities.cabinetdisplay.TemporaryImageRepository;
import com.zeta.business.entities.samplingtest.*;
import com.zeta.business.entities.samplingtest.dto.*;
import com.zeta.business.storage.CognitionVideoStorage;
import com.zeta.screen.cabinet.CabinetRepository;
import com.zeta.screen.terminal.Terminal;
import com.zeta.screen.terminal.TerminalRepository;
import com.zeta.screen.sampling.SamplingSignalCatalogService;
import com.zeta.screen.sampling.SamplingSignalCatalogService.Snapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SamplingTestService {
  private static final List<String> CHANNEL_CODES =
      Collections.unmodifiableList(Arrays.asList("Ua", "Ub", "Uc", "Un", "Ia", "Ib", "Ic", "In"));
  private static final Set<String> WIRING_ONLY_CODES =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList("Un", "In")));

  private final SamplingTestItemRepository itemRepository;
  private final SamplingTestChannelRepository channelRepository;
  private final DigitalSamplingTestChannelRepository digitalChannelRepository;
  private final TemporaryImageRepository temporaryImageRepository;
  private final CognitionVideoStorage videoStorage;
  private final CabinetRepository cabinetRepository;
  private final TerminalRepository terminalRepository;
  private final SharedMediaCleanupService mediaCleanupService;
  private final SamplingSignalCatalogService samplingSignalCatalogService;

  public SamplingTestService(
      SamplingTestItemRepository itemRepository,
      SamplingTestChannelRepository channelRepository,
      DigitalSamplingTestChannelRepository digitalChannelRepository,
      TemporaryImageRepository temporaryImageRepository,
      CognitionVideoStorage videoStorage,
      CabinetRepository cabinetRepository,
      TerminalRepository terminalRepository,
      SharedMediaCleanupService mediaCleanupService,
      SamplingSignalCatalogService samplingSignalCatalogService) {
    this.itemRepository = itemRepository;
    this.channelRepository = channelRepository;
    this.digitalChannelRepository = digitalChannelRepository;
    this.temporaryImageRepository = temporaryImageRepository;
    this.videoStorage = videoStorage;
    this.cabinetRepository = cabinetRepository;
    this.terminalRepository = terminalRepository;
    this.mediaCleanupService = mediaCleanupService;
    this.samplingSignalCatalogService = samplingSignalCatalogService;
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public List<SamplingTestItemResponse> listAdmin(Long cabinetId) {
    requireCabinet(cabinetId);
    return itemRepository.findByScreenCabinetIdOrderBySortOrderAscIdAsc(cabinetId).stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public List<SamplingTestItemResponse> listEnabled(Long cabinetId) {
    requireCabinet(cabinetId);
    return itemRepository.findByScreenCabinetIdOrderBySortOrderAscIdAsc(cabinetId).stream()
        .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional("businessTransactionManager")
  public SamplingTestItemResponse create(Long cabinetId, SamplingTestItemRequest request) {
    requireCabinet(cabinetId);
    SamplingTestItem item = new SamplingTestItem();
    item.setScreenCabinetId(cabinetId);
    item.setCreatedAt(Instant.now());
    apply(item, request);
    SamplingTestItem saved = itemRepository.save(item);
    replaceChannels(saved, request.getChannels());
    replaceDigitalChannels(saved, request.getDigitalConfig());
    return toResponse(saved);
  }

  @Transactional("businessTransactionManager")
  public SamplingTestItemResponse update(Long id, SamplingTestItemRequest request) {
    SamplingTestItem item = requireItem(id);
    String previousVideoPath = item.getVideoPath();
    apply(item, request);
    SamplingTestItem saved = itemRepository.save(item);
    replaceChannels(saved, request.getChannels());
    replaceDigitalChannels(saved, request.getDigitalConfig());
    if (!Objects.equals(previousVideoPath, saved.getVideoPath())) {
      mediaCleanupService.scheduleCognitionVideoDeletion(previousVideoPath);
    }
    return toResponse(saved);
  }

  @Transactional("businessTransactionManager")
  public void delete(Long id) {
    SamplingTestItem item = requireItem(id);
    channelRepository.deleteBySamplingTestItemId(id);
    digitalChannelRepository.deleteBySamplingTestItemId(id);
    mediaCleanupService.scheduleCognitionVideoDeletion(item.getVideoPath());
    itemRepository.delete(item);
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public SamplingTestItem requireItem(Long id) {
    return itemRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "采样测试条目不存在"));
  }

  private void apply(SamplingTestItem item, SamplingTestItemRequest request) {
    validateConfigurationType(item, request.getMediaType());
    item.setTitle(request.getTitle().trim());
    item.setContent(request.getContent().trim());
    if (item.getId() == null) {
      item.setSortOrder(SortOrderHelper.resolveForCreate(
          request.getSortOrder(),
          itemRepository.findByScreenCabinetIdOrderBySortOrderAscIdAsc(item.getScreenCabinetId()),
          SamplingTestItem::getSortOrder));
    } else {
      item.setSortOrder(SortOrderHelper.resolveForUpdate(
          request.getSortOrder(),
          item.getSortOrder(),
          itemRepository.findByScreenCabinetIdOrderBySortOrderAscIdAsc(item.getScreenCabinetId()),
          SamplingTestItem::getSortOrder,
          SamplingTestItem::getId,
          item.getId()));
    }
    item.setEnabled(request.getEnabled() == null || request.getEnabled());
    item.setMediaType(request.getMediaType());

    if (request.getMediaType() == SamplingTestMediaType.SAMPLING_CONFIGURATION) {
      item.setImageUrl(null);
      item.setImageData(null);
      item.setImageContentType(null);
      item.setVideoPath(null);
      item.setIedDeviceId(null);
      validateChannels(item.getScreenCabinetId(), request.getChannels());
      if (request.getDigitalConfig() != null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "普通采样配置不能包含数字化通道");
      }
      return;
    }

    if (request.getMediaType() == SamplingTestMediaType.DIGITAL_SAMPLING_CONFIGURATION) {
      item.setImageUrl(null);
      item.setImageData(null);
      item.setImageContentType(null);
      item.setVideoPath(null);
      if (request.getChannels() != null && !request.getChannels().isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "数字化采样配置不能包含端子通道");
      }
      validateDigitalConfig(item.getScreenCabinetId(), request.getDigitalConfig());
      item.setIedDeviceId(request.getDigitalConfig().getIedDeviceId());
      return;
    }

    if (request.getChannels() != null && !request.getChannels().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "普通媒体条目不能配置采样通道");
    }
    if (request.getDigitalConfig() != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "普通媒体条目不能配置数字化采样通道");
    }
    item.setIedDeviceId(null);
    if (request.getMediaType() == SamplingTestMediaType.VIDEO) {
      String path = videoStorage.normalizeManagedPath(request.getVideoPath());
      if (!videoStorage.exists(path)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "认知视频不存在，请重新上传");
      }
      item.setVideoPath(path);
      item.setImageUrl(null);
      item.setImageData(null);
      item.setImageContentType(null);
      return;
    }

    item.setVideoPath(null);
    if (request.getImageId() != null) {
      TemporaryImage image = temporaryImageRepository.findById(request.getImageId())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "临时图片不存在或已过期"));
      item.setImageData(image.getImageData());
      item.setImageContentType(image.getContentType());
      item.setImageUrl(null);
      temporaryImageRepository.delete(image);
    } else if (!hasImage(item)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传认知图片");
    }
  }

  private void validateChannels(Long cabinetId, List<SamplingTestChannelRequest> channels) {
    if (channels == null || channels.size() != CHANNEL_CODES.size()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ua、Ub、Uc、Un、Ia、Ib、Ic、In 必须全部关联端子");
    }
    Set<String> codes = new HashSet<>();
    Set<Long> terminalIds = new HashSet<>();
    for (SamplingTestChannelRequest channel : channels) {
      if (channel == null || !CHANNEL_CODES.contains(channel.getOutputCode()) || !codes.add(channel.getOutputCode())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "采样通道缺失或重复");
      }
      if (channel.getTerminalId() == null || !terminalIds.add(channel.getTerminalId())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "八个采样通道必须关联不同端子");
      }
    }
    Map<Long, Terminal> terminalsById = terminalRepository.findAllWithCabinetAndStripByIdIn(terminalIds).stream()
        .collect(Collectors.toMap(Terminal::getId, terminal -> terminal));
    for (SamplingTestChannelRequest channel : channels) {
      Terminal terminal = terminalsById.get(channel.getTerminalId());
      if (terminal == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "关联端子不存在");
      }
      if (terminal.getCabinet() == null || !Objects.equals(cabinetId, terminal.getCabinet().getId())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "关联端子不属于当前屏柜");
      }
      if (terminal.getTerminalStrip() == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "关联端子必须属于端子排");
      }
      if (!WIRING_ONLY_CODES.contains(channel.getOutputCode())) {
        if (terminal.getSignalType() != Terminal.SignalType.ANALOG || !StringUtils.hasText(terminal.getIedSignalRef())) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, channel.getOutputCode() + " 必须关联具备实时信号引用的模拟量端子");
        }
        if (channel.getBaselineMagnitude() == null || channel.getBaselineMagnitude().compareTo(BigDecimal.ZERO) < 0
            || channel.getBaselineAngle() == null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, channel.getOutputCode() + " 必须配置合法的基准幅值和角度");
        }
      } else {
        if (terminal.getSignalType() != Terminal.SignalType.END) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, channel.getOutputCode() + " 必须关联公共端");
        }
        if (channel.getBaselineMagnitude() != null || channel.getBaselineAngle() != null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, channel.getOutputCode() + " 仅配置公共端接线，不配置基准值和角度");
        }
      }
    }
    if (!codes.equals(new HashSet<>(CHANNEL_CODES))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ua、Ub、Uc、Un、Ia、Ib、Ic、In 必须全部配置且不能重复");
    }
  }

  private void replaceChannels(SamplingTestItem item, List<SamplingTestChannelRequest> requested) {
    channelRepository.deleteBySamplingTestItemId(item.getId());
    digitalChannelRepository.deleteBySamplingTestItemId(item.getId());
    if (item.getMediaType() == SamplingTestMediaType.DIGITAL_SAMPLING_CONFIGURATION) return;
    if (item.getMediaType() != SamplingTestMediaType.SAMPLING_CONFIGURATION) return;
    Map<String, SamplingTestChannelRequest> byCode = requested.stream()
        .collect(Collectors.toMap(SamplingTestChannelRequest::getOutputCode, entry -> entry));
    for (int index = 0; index < CHANNEL_CODES.size(); index++) {
      String code = CHANNEL_CODES.get(index);
      SamplingTestChannelRequest source = byCode.get(code);
      SamplingTestChannel channel = new SamplingTestChannel();
      channel.setSamplingTestItemId(item.getId());
      channel.setOutputCode(code);
      channel.setTerminalId(source.getTerminalId());
      channel.setBaselineMagnitude(WIRING_ONLY_CODES.contains(code) ? null : source.getBaselineMagnitude());
      channel.setBaselineAngle(WIRING_ONLY_CODES.contains(code) ? null : normalizeAngle(source.getBaselineAngle()));
      channel.setSortOrder(index);
      channelRepository.save(channel);
    }
  }

  private SamplingTestItemResponse toResponse(SamplingTestItem item) {
    List<SamplingTestChannel> configuredChannels =
        channelRepository.findBySamplingTestItemIdOrderBySortOrderAscIdAsc(item.getId());
    Set<Long> terminalIds = configuredChannels.stream().map(SamplingTestChannel::getTerminalId).collect(Collectors.toSet());
    Map<Long, Terminal> terminalsById = terminalIds.isEmpty()
        ? Collections.emptyMap()
        : terminalRepository.findAllWithCabinetAndStripByIdIn(terminalIds).stream()
            .collect(Collectors.toMap(Terminal::getId, terminal -> terminal));
    List<SamplingTestChannelResponse> channels = configuredChannels.stream()
        .map(channel -> {
          Terminal terminal = terminalsById.get(channel.getTerminalId());
          return new SamplingTestChannelResponse(
              channel.getOutputCode(), channel.getTerminalId(),
              terminal == null ? null : terminal.getTerminalLabel(),
              terminal == null || terminal.getTerminalStrip() == null ? null : terminal.getTerminalStrip().getId(),
              terminal == null || terminal.getTerminalStrip() == null ? null : terminal.getTerminalStrip().getName(),
              terminal == null || terminal.getTerminalStrip() == null ? null : terminal.getTerminalStrip().getLabelPrefix(),
              channel.getBaselineMagnitude(), channel.getBaselineAngle());
        }).collect(Collectors.toList());
    return new SamplingTestItemResponse(
        item.getId(), item.getScreenCabinetId(), item.getTitle(), item.getMediaType(), item.getImageUrl(),
        item.getVideoPath(), item.getContent(), item.getSortOrder(), Boolean.TRUE.equals(item.getEnabled()),
        item.getCreatedAt(), channels, toDigitalResponse(item));
  }

  private void validateConfigurationType(SamplingTestItem item, SamplingTestMediaType requestedType) {
    if (!isConfigurationType(requestedType)) return;
    for (SamplingTestItem existing : itemRepository
        .findByScreenCabinetIdOrderBySortOrderAscIdAsc(item.getScreenCabinetId())) {
      if (Objects.equals(existing.getId(), item.getId()) || !isConfigurationType(existing.getMediaType())) continue;
      if (existing.getMediaType() != requestedType) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "同一屏柜的采样测试只能使用普通或数字化配置中的一种");
      }
    }
  }

  private boolean isConfigurationType(SamplingTestMediaType type) {
    return type == SamplingTestMediaType.SAMPLING_CONFIGURATION
        || type == SamplingTestMediaType.DIGITAL_SAMPLING_CONFIGURATION;
  }

  private void validateDigitalConfig(
      Long cabinetId, DigitalSamplingTestDtos.ConfigRequest config) {
    if (config == null || config.getIedDeviceId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择数字化采样装置");
    }
    samplingSignalCatalogService.requireDevice(cabinetId, config.getIedDeviceId());
    List<DigitalSamplingTestDtos.ChannelRequest> requested = config.getChannels();
    if (requested == null || requested.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请至少选择一个数字化采样通道");
    }
    Set<Long> channelIds = new LinkedHashSet<>();
    for (DigitalSamplingTestDtos.ChannelRequest channel : requested) {
      if (channel == null || channel.getSamplingSignalChannelId() == null
          || !channelIds.add(channel.getSamplingSignalChannelId())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "数字化采样通道缺失或重复");
      }
      if (channel.getBaselineMagnitude() == null
          || channel.getBaselineMagnitude().compareTo(BigDecimal.ZERO) < 0
          || channel.getBaselineAngle() == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "数字化采样通道必须配置合法的基准幅值和相角");
      }
    }
    Map<Long, Snapshot> current = samplingSignalCatalogService.loadCurrentChannels(channelIds);
    for (Long channelId : channelIds) {
      Snapshot snapshot = current.get(channelId);
      if (snapshot == null || snapshot.getIedDeviceId() != config.getIedDeviceId()
          || !snapshot.isControlBlockMatched()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "所选数字化采样通道不存在、已失效或不属于当前装置");
      }
    }
  }

  private void replaceDigitalChannels(
      SamplingTestItem item, DigitalSamplingTestDtos.ConfigRequest config) {
    if (item.getMediaType() != SamplingTestMediaType.DIGITAL_SAMPLING_CONFIGURATION) return;
    List<DigitalSamplingTestDtos.ChannelRequest> requested = config.getChannels();
    Set<Long> ids = requested.stream().map(DigitalSamplingTestDtos.ChannelRequest::getSamplingSignalChannelId)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    Map<Long, Snapshot> snapshots = samplingSignalCatalogService.loadCurrentChannels(ids);
    for (int index = 0; index < requested.size(); index++) {
      DigitalSamplingTestDtos.ChannelRequest source = requested.get(index);
      Snapshot snapshot = snapshots.get(source.getSamplingSignalChannelId());
      if (snapshot == null || !snapshot.isControlBlockMatched()
          || snapshot.getIedDeviceId() != item.getIedDeviceId()) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "数字化采样配置已发生变化，请重新选择通道");
      }
      DigitalSamplingTestChannel channel = new DigitalSamplingTestChannel();
      channel.setSamplingTestItemId(item.getId());
      channel.setSamplingSignalAssociationId(snapshot.getAssociationId());
      channel.setSamplingSignalChannelId(snapshot.getChannelId());
      channel.setIedDeviceId(snapshot.getIedDeviceId());
      channel.setCategory(snapshot.getCategory());
      channel.setControlIdentityKey(snapshot.getControlIdentityKey());
      channel.setSourceIedName(snapshot.getSourceIedName());
      channel.setControlName(snapshot.getControlName());
      channel.setControlReference(snapshot.getControlReference());
      channel.setTelemetryReference(snapshot.getTelemetryReference());
      channel.setTelemetryKey(snapshot.getTelemetryKey());
      channel.setTelemetryDescription(snapshot.getTelemetryDescription());
      channel.setDatasetName(snapshot.getDatasetName());
      channel.setValueType(snapshot.getValueType());
      channel.setBaselineMagnitude(source.getBaselineMagnitude());
      channel.setBaselineAngle(normalizeAngle(source.getBaselineAngle()));
      channel.setSortOrder(index);
      digitalChannelRepository.save(channel);
    }
  }

  private DigitalSamplingTestDtos.ConfigResponse toDigitalResponse(SamplingTestItem item) {
    if (item.getMediaType() != SamplingTestMediaType.DIGITAL_SAMPLING_CONFIGURATION) return null;
    List<DigitalSamplingTestChannel> configured = digitalChannelRepository
        .findBySamplingTestItemIdOrderBySortOrderAscIdAsc(item.getId());
    Set<Long> ids = configured.stream().map(DigitalSamplingTestChannel::getSamplingSignalChannelId)
        .collect(Collectors.toSet());
    Map<Long, Snapshot> current = samplingSignalCatalogService.loadCurrentChannels(ids);
    List<DigitalSamplingTestDtos.ChannelResponse> channels = new ArrayList<>();
    boolean allValid = !configured.isEmpty();
    for (DigitalSamplingTestChannel channel : configured) {
      Snapshot live = current.get(channel.getSamplingSignalChannelId());
      String invalidReason = digitalChannelInvalidReason(channel, live, item.getIedDeviceId());
      boolean valid = invalidReason == null;
      allValid &= valid;
      channels.add(new DigitalSamplingTestDtos.ChannelResponse(
          channel.getSamplingSignalAssociationId(), channel.getSamplingSignalChannelId(),
          channel.getCategory(), channel.getControlIdentityKey(), channel.getSourceIedName(),
          channel.getControlName(), channel.getControlReference(), channel.getTelemetryReference(),
          channel.getTelemetryKey(), channel.getTelemetryDescription(), channel.getDatasetName(),
          channel.getValueType(), channel.getBaselineMagnitude(), channel.getBaselineAngle(),
          channel.getSortOrder(), valid, invalidReason));
    }
    String iedName = null;
    String deviceName = null;
    String configReason = null;
    try {
      Map<String, Object> device = samplingSignalCatalogService
          .requireDevice(item.getScreenCabinetId(), item.getIedDeviceId());
      iedName = String.valueOf(device.get("iedName"));
      deviceName = String.valueOf(device.get("displayName"));
    } catch (ResponseStatusException ex) {
      allValid = false;
      configReason = "关联装置不存在或已不属于当前屏柜";
    }
    if (configReason == null && configured.isEmpty()) configReason = "未配置数字化采样通道";
    if (configReason == null && !allValid) configReason = "存在已失效的数字化采样通道，请重新选择";
    return new DigitalSamplingTestDtos.ConfigResponse(
        item.getIedDeviceId(), iedName, deviceName, allValid, configReason, channels);
  }

  private String digitalChannelInvalidReason(
      DigitalSamplingTestChannel saved, Snapshot current, Long itemDeviceId) {
    if (current == null) return "采样通道已被删除或重建";
    if (!Objects.equals(saved.getIedDeviceId(), itemDeviceId)
        || current.getIedDeviceId() != saved.getIedDeviceId()
        || current.getAssociationId() != saved.getSamplingSignalAssociationId()
        || !Objects.equals(current.getCategory(), saved.getCategory())
        || !Objects.equals(current.getSourceIedName(), saved.getSourceIedName())
        || !Objects.equals(current.getControlName(), saved.getControlName())
        || !Objects.equals(current.getControlReference(), saved.getControlReference())
        || !Objects.equals(current.getTelemetryKey(), saved.getTelemetryKey())
        || !Objects.equals(current.getTelemetryReference(), saved.getTelemetryReference())
        || !Objects.equals(current.getTelemetryDescription(), saved.getTelemetryDescription())
        || !Objects.equals(current.getDatasetName(), saved.getDatasetName())
        || !Objects.equals(current.getValueType(), saved.getValueType())
        || !Objects.equals(current.getControlIdentityKey(), saved.getControlIdentityKey())) {
      return "采样通道配置已发生变化";
    }
    if (!current.isControlBlockMatched()) return "关联的 SV 控制块已失效";
    return null;
  }

  private BigDecimal normalizeAngle(BigDecimal angle) {
    BigDecimal normalized = angle.remainder(BigDecimal.valueOf(360));
    return normalized.signum() < 0 ? normalized.add(BigDecimal.valueOf(360)) : normalized;
  }

  private boolean hasImage(SamplingTestItem item) {
    return StringUtils.hasText(item.getImageUrl()) || (item.getImageData() != null && item.getImageData().length > 0);
  }

  private void requireCabinet(Long cabinetId) {
    if (!cabinetRepository.existsById(cabinetId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "屏柜不存在");
    }
  }
}
