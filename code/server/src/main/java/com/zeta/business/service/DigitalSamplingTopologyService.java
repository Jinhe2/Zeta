package com.zeta.business.service;

import com.zeta.business.entities.samplingtest.*;
import com.zeta.screen.sampling.SamplingSignalCatalogService;
import com.zeta.screen.sampling.SamplingSignalCatalogService.Snapshot;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 为学员数字化采样测试构建仅包含已选 SV 关联的精简拓扑。 */
@Service
public class DigitalSamplingTopologyService {
  private final SamplingTestItemRepository itemRepository;
  private final DigitalSamplingTestChannelRepository channelRepository;
  private final SamplingSignalCatalogService catalog;

  public DigitalSamplingTopologyService(
      SamplingTestItemRepository itemRepository,
      DigitalSamplingTestChannelRepository channelRepository,
      SamplingSignalCatalogService catalog) {
    this.itemRepository = itemRepository;
    this.channelRepository = channelRepository;
    this.catalog = catalog;
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public Map<String, Object> getTopology(Long itemId) {
    SamplingTestItem item = itemRepository.findById(itemId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "采样测试条目不存在"));
    if (item.getMediaType() != SamplingTestMediaType.DIGITAL_SAMPLING_CONFIGURATION
        || !Boolean.TRUE.equals(item.getEnabled())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "数字化采样测试条目不存在或未启用");
    }
    Map<String, Object> center = catalog.requireDevice(item.getScreenCabinetId(), item.getIedDeviceId());
    List<DigitalSamplingTestChannel> channels = channelRepository
        .findBySamplingTestItemIdOrderBySortOrderAscIdAsc(itemId);
    Set<Long> channelIds = channels.stream().map(DigitalSamplingTestChannel::getSamplingSignalChannelId)
        .collect(Collectors.toSet());
    Map<Long, Snapshot> current = catalog.loadCurrentChannels(channelIds);
    Map<String, String> configuredDescriptions =
        catalog.listConfiguredDescriptions(item.getScreenCabinetId());

    Map<Long, Map<String, Object>> groupedEdges = new LinkedHashMap<>();
    Set<String> peerNames = new LinkedHashSet<>();
    Map<String, String> virtualDescriptions = new LinkedHashMap<>();
    for (DigitalSamplingTestChannel channel : channels) {
      Snapshot live = current.get(channel.getSamplingSignalChannelId());
      if (live != null) {
        rememberDescription(
            virtualDescriptions, live.getSourceIedName(), live.getSourceIedDescription());
        rememberDescription(
            virtualDescriptions, String.valueOf(center.get("iedName")),
            live.getReceiverIedDescription());
      }
      boolean valid = matches(channel, live, item.getIedDeviceId());
      long associationId = channel.getSamplingSignalAssociationId();
      Map<String, Object> edge = groupedEdges.get(associationId);
      if (edge == null) {
        edge = new LinkedHashMap<>();
        edge.put("id", "sampling-association-" + associationId);
        edge.put("associationId", associationId);
        edge.put("sourceIedName", channel.getSourceIedName());
        edge.put("targetIedName", center.get("iedName"));
        edge.put("controlIdentityKey", channel.getControlIdentityKey());
        edge.put("controlName", channel.getControlName());
        edge.put("controlReference", channel.getControlReference());
        edge.put("valid", valid);
        edge.put("channelIds", new ArrayList<Long>());
        groupedEdges.put(associationId, edge);
        if (channel.getSourceIedName() != null && !channel.getSourceIedName().trim().isEmpty()) {
          peerNames.add(channel.getSourceIedName());
        }
      } else if (!valid) {
        edge.put("valid", false);
      }
      @SuppressWarnings("unchecked")
      List<Long> ids = (List<Long>) edge.get("channelIds");
      ids.add(channel.getSamplingSignalChannelId());
    }

    List<Map<String, Object>> peers = new ArrayList<>();
    for (String peerName : peerNames) {
      Map<String, Object> peer = new LinkedHashMap<>();
      peer.put("iedName", peerName);
      peer.put("displayName", displayName(
          peerName, configuredDescriptions.get(peerName), virtualDescriptions.get(peerName)));
      peers.add(peer);
    }
    String centerName = String.valueOf(center.get("iedName"));
    center.put("displayName", displayName(
        centerName, configuredDescriptions.get(centerName), virtualDescriptions.get(centerName)));
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("itemId", itemId);
    result.put("center", center);
    result.put("peers", peers);
    result.put("edges", new ArrayList<>(groupedEdges.values()));
    return result;
  }

  String displayName(String iedName, String configuredDescription, String virtualDescription) {
    if (configuredDescription != null && !configuredDescription.trim().isEmpty()) {
      return configuredDescription;
    }
    if (virtualDescription != null && !virtualDescription.trim().isEmpty()) {
      return virtualDescription;
    }
    return iedName;
  }

  void rememberDescription(Map<String, String> descriptions, String iedName, String description) {
    if (iedName == null || iedName.trim().isEmpty()
        || description == null || description.trim().isEmpty()) return;
    descriptions.putIfAbsent(iedName, description);
  }

  private boolean matches(DigitalSamplingTestChannel saved, Snapshot current, Long itemDeviceId) {
    return current != null && current.isControlBlockMatched()
        && Objects.equals(saved.getIedDeviceId(), itemDeviceId)
        && current.getIedDeviceId() == saved.getIedDeviceId()
        && current.getAssociationId() == saved.getSamplingSignalAssociationId()
        && Objects.equals(current.getCategory(), saved.getCategory())
        && Objects.equals(current.getSourceIedName(), saved.getSourceIedName())
        && Objects.equals(current.getControlName(), saved.getControlName())
        && Objects.equals(current.getControlReference(), saved.getControlReference())
        && Objects.equals(current.getTelemetryKey(), saved.getTelemetryKey())
        && Objects.equals(current.getTelemetryReference(), saved.getTelemetryReference())
        && Objects.equals(current.getTelemetryDescription(), saved.getTelemetryDescription())
        && Objects.equals(current.getDatasetName(), saved.getDatasetName())
        && Objects.equals(current.getValueType(), saved.getValueType())
        && Objects.equals(current.getControlIdentityKey(), saved.getControlIdentityKey());
  }
}
