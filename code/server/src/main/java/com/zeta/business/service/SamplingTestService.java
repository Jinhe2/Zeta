package com.zeta.business.service;

import com.zeta.business.entities.cabinetdisplay.TemporaryImage;
import com.zeta.business.entities.cabinetdisplay.TemporaryImageRepository;
import com.zeta.business.entities.samplingtest.*;
import com.zeta.business.entities.samplingtest.dto.*;
import com.zeta.business.storage.CognitionVideoStorage;
import com.zeta.screen.cabinet.CabinetRepository;
import com.zeta.screen.terminal.Terminal;
import com.zeta.screen.terminal.TerminalRepository;
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
  private final TemporaryImageRepository temporaryImageRepository;
  private final CognitionVideoStorage videoStorage;
  private final CabinetRepository cabinetRepository;
  private final TerminalRepository terminalRepository;
  private final SharedMediaCleanupService mediaCleanupService;

  public SamplingTestService(
      SamplingTestItemRepository itemRepository,
      SamplingTestChannelRepository channelRepository,
      TemporaryImageRepository temporaryImageRepository,
      CognitionVideoStorage videoStorage,
      CabinetRepository cabinetRepository,
      TerminalRepository terminalRepository,
      SharedMediaCleanupService mediaCleanupService) {
    this.itemRepository = itemRepository;
    this.channelRepository = channelRepository;
    this.temporaryImageRepository = temporaryImageRepository;
    this.videoStorage = videoStorage;
    this.cabinetRepository = cabinetRepository;
    this.terminalRepository = terminalRepository;
    this.mediaCleanupService = mediaCleanupService;
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
    return toResponse(saved);
  }

  @Transactional("businessTransactionManager")
  public SamplingTestItemResponse update(Long id, SamplingTestItemRequest request) {
    SamplingTestItem item = requireItem(id);
    String previousVideoPath = item.getVideoPath();
    apply(item, request);
    SamplingTestItem saved = itemRepository.save(item);
    replaceChannels(saved, request.getChannels());
    if (!Objects.equals(previousVideoPath, saved.getVideoPath())) {
      mediaCleanupService.scheduleCognitionVideoDeletion(previousVideoPath);
    }
    return toResponse(saved);
  }

  @Transactional("businessTransactionManager")
  public void delete(Long id) {
    SamplingTestItem item = requireItem(id);
    channelRepository.deleteBySamplingTestItemId(id);
    mediaCleanupService.scheduleCognitionVideoDeletion(item.getVideoPath());
    itemRepository.delete(item);
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public SamplingTestItem requireItem(Long id) {
    return itemRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "采样测试条目不存在"));
  }

  private void apply(SamplingTestItem item, SamplingTestItemRequest request) {
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
      validateChannels(item.getScreenCabinetId(), request.getChannels());
      return;
    }

    if (request.getChannels() != null && !request.getChannels().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "普通媒体条目不能配置采样通道");
    }
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
        item.getCreatedAt(), channels);
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
