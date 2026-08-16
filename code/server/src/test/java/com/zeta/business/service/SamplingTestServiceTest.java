package com.zeta.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zeta.business.entities.cabinetdisplay.TemporaryImageRepository;
import com.zeta.business.entities.samplingtest.*;
import com.zeta.business.entities.samplingtest.dto.*;
import com.zeta.business.storage.CognitionVideoStorage;
import com.zeta.screen.cabinet.Cabinet;
import com.zeta.screen.cabinet.CabinetRepository;
import com.zeta.screen.terminal.Terminal;
import com.zeta.screen.terminal.TerminalRepository;
import com.zeta.screen.terminal.TerminalStrip;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class SamplingTestServiceTest {
  private SamplingTestItemRepository itemRepository;
  private SamplingTestChannelRepository channelRepository;
  private TerminalRepository terminalRepository;
  private Map<Long, Terminal> terminalsById;
  private SamplingTestService service;

  @BeforeEach
  void setUp() {
    itemRepository = mock(SamplingTestItemRepository.class);
    channelRepository = mock(SamplingTestChannelRepository.class);
    terminalRepository = mock(TerminalRepository.class);
    terminalsById = new HashMap<>();
    CabinetRepository cabinetRepository = mock(CabinetRepository.class);
    when(cabinetRepository.existsById(1L)).thenReturn(true);
    when(itemRepository.save(any(SamplingTestItem.class))).thenAnswer(invocation -> {
      SamplingTestItem item = invocation.getArgument(0);
      item.setId(10L);
      return item;
    });
    when(channelRepository.findBySamplingTestItemIdOrderBySortOrderAscIdAsc(10L))
        .thenReturn(Collections.emptyList());

    Cabinet cabinet = new Cabinet();
    cabinet.setId(1L);
    TerminalStrip strip = new TerminalStrip();
    strip.setId(20L);
    strip.setCabinet(cabinet);
    strip.setName("1X");
    strip.setLabelPrefix("1X");
    for (long id = 1; id <= 8; id++) {
      Terminal terminal = new Terminal();
      terminal.setId(id);
      terminal.setCabinet(cabinet);
      terminal.setTerminalStrip(strip);
      terminal.setTerminalLabel(String.valueOf(id));
      boolean commonEnd = id == 4 || id == 8;
      terminal.setSignalType(commonEnd ? Terminal.SignalType.END : Terminal.SignalType.ANALOG);
      terminal.setIedSignalRef(commonEnd ? null : "IED$MX$A" + id + "$cVal$mag$f");
      terminalsById.put(id, terminal);
    }
    when(terminalRepository.findAllWithCabinetAndStripByIdIn(any())).thenAnswer(invocation -> {
      Collection<Long> ids = invocation.getArgument(0);
      List<Terminal> result = new ArrayList<>();
      for (Long id : ids) if (terminalsById.containsKey(id)) result.add(terminalsById.get(id));
      return result;
    });

    service = new SamplingTestService(
        itemRepository, channelRepository, mock(TemporaryImageRepository.class),
        mock(CognitionVideoStorage.class), cabinetRepository, terminalRepository,
        mock(SharedMediaCleanupService.class));
  }

  @Test
  void requiresAllEightChannels() {
    SamplingTestItemRequest request = validRequest();
    request.setChannels(request.getChannels().subList(0, 7));
    assertThatThrownBy(() -> service.create(1L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("必须全部关联端子");
  }

  @Test
  void unMustNotHaveBaselineValues() {
    SamplingTestItemRequest request = validRequest();
    request.getChannels().stream().filter(channel -> "Un".equals(channel.getOutputCode())).findFirst()
        .orElseThrow(AssertionError::new).setBaselineMagnitude(BigDecimal.ZERO);
    assertThatThrownBy(() -> service.create(1L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Un 仅配置公共端接线");
  }

  @Test
  void acceptsCompleteConfigurationWithWiringOnlyUnAndIn() {
    SamplingTestItemResponse response = service.create(1L, validRequest());
    assertThat(response.getMediaType()).isEqualTo(SamplingTestMediaType.SAMPLING_CONFIGURATION);
  }

  @Test
  void inMustUseCommonEndTerminal() {
    Terminal inTerminal = terminalsById.get(8L);
    inTerminal.setSignalType(Terminal.SignalType.ANALOG);
    inTerminal.setIedSignalRef("IED$MX$In$cVal$mag$f");
    assertThatThrownBy(() -> service.create(1L, validRequest()))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("In 必须关联公共端");
  }

  private SamplingTestItemRequest validRequest() {
    SamplingTestItemRequest request = new SamplingTestItemRequest();
    request.setTitle("三相采样");
    request.setMediaType(SamplingTestMediaType.SAMPLING_CONFIGURATION);
    request.setContent("按要求接线并加量");
    request.setSortOrder(0);
    request.setEnabled(true);
    List<String> codes = Arrays.asList("Ua", "Ub", "Uc", "Un", "Ia", "Ib", "Ic", "In");
    List<SamplingTestChannelRequest> channels = new ArrayList<>();
    for (int index = 0; index < codes.size(); index++) {
      SamplingTestChannelRequest channel = new SamplingTestChannelRequest();
      channel.setOutputCode(codes.get(index));
      channel.setTerminalId((long) index + 1);
      if (!"Un".equals(channel.getOutputCode()) && !"In".equals(channel.getOutputCode())) {
        channel.setBaselineMagnitude(BigDecimal.valueOf(100));
        channel.setBaselineAngle(BigDecimal.valueOf(index * 30L));
      }
      channels.add(channel);
    }
    request.setChannels(channels);
    return request;
  }
}
