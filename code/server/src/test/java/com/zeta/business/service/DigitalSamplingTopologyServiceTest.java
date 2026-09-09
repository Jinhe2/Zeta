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
    when(catalog.listDevices(1L)).thenReturn(Arrays.asList(
        device(11L, "IED01", "保护装置"), device(12L, "MU01", "合并单元"), device(13L, "OTHER", "无关装置")));

    Map<String, Object> topology = service.getTopology(5L);

    assertThat((List<?>) topology.get("peers")).hasSize(1);
    @SuppressWarnings("unchecked")
    Map<String, Object> edge = ((List<Map<String, Object>>) topology.get("edges")).get(0);
    assertThat(edge).containsEntry("associationId", 31L).containsEntry("valid", false);
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
