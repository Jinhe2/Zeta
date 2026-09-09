package com.zeta.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.zeta.business.entities.samplingtest.*;
import com.zeta.screen.sampling.SamplingSignalCatalogService;
import java.util.*;
import org.junit.jupiter.api.Test;

class DigitalSamplingTopologyServiceTest {
  @Test
  void containsOnlySelectedSamplingAssociationAndMarksMissingConfigurationInvalid() {
    SamplingTestItemRepository items = mock(SamplingTestItemRepository.class);
    DigitalSamplingTestChannelRepository channels = mock(DigitalSamplingTestChannelRepository.class);
    SamplingSignalCatalogService catalog = mock(SamplingSignalCatalogService.class);
    DigitalSamplingTopologyService service = new DigitalSamplingTopologyService(items, channels, catalog);
    SamplingTestItem item = new SamplingTestItem();
    item.setId(5L);
    item.setScreenCabinetId(1L);
    item.setIedDeviceId(11L);
    item.setEnabled(true);
    item.setMediaType(SamplingTestMediaType.DIGITAL_SAMPLING_CONFIGURATION);
    DigitalSamplingTestChannel channel = new DigitalSamplingTestChannel();
    channel.setSamplingSignalAssociationId(31L);
    channel.setSamplingSignalChannelId(41L);
    channel.setIedDeviceId(11L);
    channel.setSourceIedName("MU01");
    channel.setControlIdentityKey("identity");
    channel.setControlName("MSVCB01");
    channel.setTelemetryKey("telemetry-key");
    channel.setTelemetryReference("IED/LD/MMXU$MX$A$phsA$cVal$mag$f");
    when(items.findById(5L)).thenReturn(Optional.of(item));
    when(channels.findBySamplingTestItemIdOrderBySortOrderAscIdAsc(5L))
        .thenReturn(Collections.singletonList(channel));
    when(catalog.loadCurrentChannels(anyCollection())).thenReturn(Collections.emptyMap());
    when(catalog.requireDevice(1L, 11L)).thenReturn(device(11L, "IED01", "保护装置"));
    Map<String, String> configuredDescriptions = new LinkedHashMap<>();
    configuredDescriptions.put("IED01", "保护装置");
    configuredDescriptions.put("MU01", "合并单元");
    when(catalog.listConfiguredDescriptions(1L)).thenReturn(configuredDescriptions);

    Map<String, Object> topology = service.getTopology(5L);

    assertThat((List<?>) topology.get("peers")).hasSize(1);
    @SuppressWarnings("unchecked")
    Map<String, Object> edge = ((List<Map<String, Object>>) topology.get("edges")).get(0);
    assertThat(edge).containsEntry("associationId", 31L).containsEntry("valid", false);
    @SuppressWarnings("unchecked")
    Map<String, Object> peer = ((List<Map<String, Object>>) topology.get("peers")).get(0);
    assertThat(peer).containsEntry("displayName", "合并单元");
  }

  @Test
  @SuppressWarnings("unchecked")
  void usesVirtualDescriptionsForDevicesMissingConfiguredDescriptions() {
    SamplingTestItemRepository items = mock(SamplingTestItemRepository.class);
    DigitalSamplingTestChannelRepository channels = mock(DigitalSamplingTestChannelRepository.class);
    SamplingSignalCatalogService catalog = mock(SamplingSignalCatalogService.class);
    DigitalSamplingTopologyService service = new DigitalSamplingTopologyService(items, channels, catalog);
    SamplingTestItem item = item();
    DigitalSamplingTestChannel channel = channel();
    SamplingSignalCatalogService.Snapshot live = mock(SamplingSignalCatalogService.Snapshot.class);
    when(live.getSourceIedName()).thenReturn("MU01");
    when(live.getSourceIedDescription()).thenReturn("虚端子合并单元");
    when(live.getReceiverIedDescription()).thenReturn("虚端子保护装置");
    when(items.findById(5L)).thenReturn(Optional.of(item));
    when(channels.findBySamplingTestItemIdOrderBySortOrderAscIdAsc(5L))
        .thenReturn(Collections.singletonList(channel));
    when(catalog.loadCurrentChannels(anyCollection())).thenReturn(Collections.singletonMap(41L, live));
    when(catalog.requireDevice(1L, 11L)).thenReturn(device(11L, "IED01", "IED01"));
    when(catalog.listConfiguredDescriptions(1L)).thenReturn(Collections.emptyMap());

    Map<String, Object> topology = service.getTopology(5L);

    Map<String, Object> peer = ((List<Map<String, Object>>) topology.get("peers")).get(0);
    assertThat(peer).containsEntry("displayName", "虚端子合并单元");
    assertThat((Map<String, Object>) topology.get("center"))
        .containsEntry("displayName", "虚端子保护装置");
  }

  @Test
  void resolvesDescriptionsInConfiguredVirtualAndIedNameOrder() {
    DigitalSamplingTopologyService service = new DigitalSamplingTopologyService(
        mock(SamplingTestItemRepository.class), mock(DigitalSamplingTestChannelRepository.class),
        mock(SamplingSignalCatalogService.class));

    assertThat(service.displayName("IED-A", "装置表描述", "虚端子表描述")).isEqualTo("装置表描述");
    assertThat(service.displayName("IED-B", "", "虚端子表描述")).isEqualTo("虚端子表描述");
    assertThat(service.displayName("IED-C", null, null)).isEqualTo("IED-C");
  }

  private SamplingTestItem item() {
    SamplingTestItem item = new SamplingTestItem();
    item.setId(5L);
    item.setScreenCabinetId(1L);
    item.setIedDeviceId(11L);
    item.setEnabled(true);
    item.setMediaType(SamplingTestMediaType.DIGITAL_SAMPLING_CONFIGURATION);
    return item;
  }

  private DigitalSamplingTestChannel channel() {
    DigitalSamplingTestChannel channel = new DigitalSamplingTestChannel();
    channel.setSamplingSignalAssociationId(31L);
    channel.setSamplingSignalChannelId(41L);
    channel.setIedDeviceId(11L);
    channel.setSourceIedName("MU01");
    channel.setControlIdentityKey("identity");
    channel.setControlName("MSVCB01");
    channel.setTelemetryKey("telemetry-key");
    channel.setTelemetryReference("IED/LD/MMXU$MX$A$phsA$cVal$mag$f");
    return channel;
  }

  private Map<String, Object> device(Long id, String iedName, String displayName) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", id);
    result.put("cabinetId", 1L);
    result.put("iedName", iedName);
    result.put("displayName", displayName);
    return result;
  }
}
