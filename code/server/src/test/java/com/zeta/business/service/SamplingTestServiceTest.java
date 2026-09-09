package com.zeta.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.zeta.business.entities.cabinetdisplay.TemporaryImageRepository;
import com.zeta.business.entities.samplingtest.*;
import com.zeta.business.entities.samplingtest.dto.DigitalSamplingTestDtos;
import com.zeta.business.entities.samplingtest.dto.SamplingTestItemRequest;
import com.zeta.business.storage.CognitionVideoStorage;
import com.zeta.screen.cabinet.CabinetRepository;
import com.zeta.screen.sampling.SamplingSignalCatalogService;
import com.zeta.screen.sampling.SamplingSignalCatalogService.Snapshot;
import com.zeta.screen.terminal.TerminalRepository;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

class SamplingTestServiceTest {
  private SamplingTestItemRepository items;
  private SamplingTestChannelRepository channels;
  private DigitalSamplingTestChannelRepository digitalChannels;
  private CabinetRepository cabinets;
  private SamplingSignalCatalogService catalog;
  private SamplingTestService service;

  @BeforeEach
  void setUp() {
    items = mock(SamplingTestItemRepository.class);
    channels = mock(SamplingTestChannelRepository.class);
    digitalChannels = mock(DigitalSamplingTestChannelRepository.class);
    cabinets = mock(CabinetRepository.class);
    catalog = mock(SamplingSignalCatalogService.class);
    service = new SamplingTestService(
        items, channels, digitalChannels, mock(TemporaryImageRepository.class),
        mock(CognitionVideoStorage.class), cabinets, mock(TerminalRepository.class),
        mock(SharedMediaCleanupService.class), catalog);
    when(cabinets.existsById(1L)).thenReturn(true);
    when(items.findByScreenCabinetIdOrderBySortOrderAscIdAsc(1L)).thenReturn(Collections.emptyList());
    when(items.save(any(SamplingTestItem.class))).thenAnswer(invocation -> {
      SamplingTestItem item = invocation.getArgument(0);
      item.setId(100L);
      return item;
    });
    when(catalog.requireDevice(1L, 11L)).thenReturn(device());
  }

  @Test
  void createsDigitalConfigurationAndNormalizesAngle() {
    Snapshot snapshot = snapshot();
    when(catalog.loadCurrentChannels(anyCollection()))
        .thenReturn(Collections.singletonMap(41L, snapshot));

    service.create(1L, digitalRequest(new BigDecimal("370")));

    ArgumentCaptor<DigitalSamplingTestChannel> captor =
        ArgumentCaptor.forClass(DigitalSamplingTestChannel.class);
    verify(digitalChannels).save(captor.capture());
    assertThat(captor.getValue().getSamplingSignalAssociationId()).isEqualTo(31L);
    assertThat(captor.getValue().getBaselineAngle()).isEqualByComparingTo("10");
  }

  @Test
  void rejectsMixedNormalAndDigitalConfigurationTypes() {
    SamplingTestItem normal = new SamplingTestItem();
    normal.setId(9L);
    normal.setScreenCabinetId(1L);
    normal.setMediaType(SamplingTestMediaType.SAMPLING_CONFIGURATION);
    when(items.findByScreenCabinetIdOrderBySortOrderAscIdAsc(1L))
        .thenReturn(Collections.singletonList(normal));

    assertThatThrownBy(() -> service.create(1L, digitalRequest(BigDecimal.ZERO)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("只能使用普通或数字化配置中的一种");
  }

  @Test
  void rejectsDeletedOrRecreatedChannel() {
    when(catalog.loadCurrentChannels(anyCollection())).thenReturn(Collections.emptyMap());

    assertThatThrownBy(() -> service.create(1L, digitalRequest(BigDecimal.ZERO)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("不存在、已失效或不属于当前装置");
  }

  private SamplingTestItemRequest digitalRequest(BigDecimal angle) {
    DigitalSamplingTestDtos.ChannelRequest channel = new DigitalSamplingTestDtos.ChannelRequest();
    channel.setSamplingSignalChannelId(41L);
    channel.setBaselineMagnitude(new BigDecimal("57.735"));
    channel.setBaselineAngle(angle);
    DigitalSamplingTestDtos.ConfigRequest config = new DigitalSamplingTestDtos.ConfigRequest();
    config.setIedDeviceId(11L);
    config.setChannels(Collections.singletonList(channel));
    SamplingTestItemRequest request = new SamplingTestItemRequest();
    request.setTitle("数字化采样");
    request.setContent("按基准值加量");
    request.setMediaType(SamplingTestMediaType.DIGITAL_SAMPLING_CONFIGURATION);
    request.setDigitalConfig(config);
    request.setChannels(Collections.emptyList());
    return request;
  }

  private Snapshot snapshot() {
    return new Snapshot(31L, 41L, 11L, "VOLTAGE", "identity", "MU01", null, null, "MSVCB01",
        "MU01/LLN0$MS$MSVCB01", "IED/LD/MMXU$MX$A$phsA$cVal$mag$f", "telemetry-key",
        "A相电压", "dsAin", "FLOAT32", 0, 0, true);
  }

  private Map<String, Object> device() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", 11L);
    result.put("cabinetId", 1L);
    result.put("iedName", "IED01");
    result.put("displayName", "保护装置");
    return result;
  }
}
