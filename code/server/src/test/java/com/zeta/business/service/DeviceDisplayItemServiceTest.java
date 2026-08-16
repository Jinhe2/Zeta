package com.zeta.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zeta.business.entities.cabinetdisplay.CabinetDisplayItem;
import com.zeta.business.entities.cabinetdisplay.CabinetDisplayItemRepository;
import com.zeta.business.entities.cabinetdisplay.TemporaryImageRepository;
import com.zeta.business.entities.cognitiondevice.CognitionDevice;
import com.zeta.business.entities.cognitiondevice.CognitionDeviceType;
import com.zeta.business.entities.devicedisplay.DeviceDisplayItem;
import com.zeta.business.entities.devicedisplay.DeviceDisplayItemRepository;
import com.zeta.business.entities.devicedisplay.TerminalOperation;
import com.zeta.business.entities.devicedisplay.TerminalOperationRepository;
import com.zeta.business.entities.devicedisplay.TerminalOperationTerminal;
import com.zeta.business.entities.devicedisplay.TerminalOperationTerminalRepository;
import com.zeta.business.entities.devicedisplay.dto.TerminalOperationRequest;
import com.zeta.business.entities.devicedisplay.dto.TerminalOperationTerminalRequest;
import com.zeta.business.entities.devicedisplay.dto.UpdateDeviceDisplayItemRequest;
import com.zeta.business.media.CognitionMediaType;
import com.zeta.business.storage.CognitionVideoStorage;
import com.zeta.business.storage.DeviceDisplayImageStorage;
import com.zeta.screen.cabinet.Cabinet;
import com.zeta.screen.terminal.Terminal;
import com.zeta.screen.terminal.TerminalRepository;
import com.zeta.screen.terminal.TerminalStrip;
import com.zeta.screen.terminal.TerminalStripRepository;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DeviceDisplayItemServiceTest {

  @Test
  void updateTerminalOperationReusesExistingOperationRow() {
    CognitionDeviceService cognitionDeviceService = mock(CognitionDeviceService.class);
    DeviceDisplayItemRepository displayItemRepository = mock(DeviceDisplayItemRepository.class);
    TerminalOperationRepository terminalOperationRepository = mock(TerminalOperationRepository.class);
    TerminalOperationTerminalRepository terminalOperationTerminalRepository =
        mock(TerminalOperationTerminalRepository.class);
    TerminalStripRepository terminalStripRepository = mock(TerminalStripRepository.class);
    TerminalRepository terminalRepository = mock(TerminalRepository.class);
    CabinetDisplayItemRepository cabinetDisplayItemRepository = mock(CabinetDisplayItemRepository.class);
    DeviceDisplayItemService service =
        new DeviceDisplayItemService(
            cognitionDeviceService,
            displayItemRepository,
            mock(DeviceDisplayImageStorage.class),
            mock(TemporaryImageRepository.class),
            mock(CognitionVideoStorage.class),
            terminalOperationRepository,
            terminalOperationTerminalRepository,
            terminalStripRepository,
            terminalRepository,
            cabinetDisplayItemRepository,
            mock(SharedMediaCleanupService.class));

    DeviceDisplayItem item = new DeviceDisplayItem();
    item.setId(50L);
    item.setCognitionDeviceId(7L);
    item.setTitle("旧标题");
    item.setMediaType(CognitionMediaType.TERMINAL_OPERATION);
    item.setContent("旧内容");
    item.setSortOrder(0);
    item.setEnabled(true);
    when(displayItemRepository.findById(50L)).thenReturn(Optional.of(item));
    when(displayItemRepository.save(any(DeviceDisplayItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

    CognitionDevice device = new CognitionDevice();
    device.setId(7L);
    device.setDeviceType(CognitionDeviceType.TERMINAL_GROUP);
    device.setCabinetDisplayItemId(9L);
    device.setTitle("端子组");
    when(cognitionDeviceService.requireDevice(7L)).thenReturn(device);

    CabinetDisplayItem cabinetItem = new CabinetDisplayItem();
    cabinetItem.setId(9L);
    cabinetItem.setScreenCabinetId(3L);
    when(cabinetDisplayItemRepository.findById(9L)).thenReturn(Optional.of(cabinetItem));

    Cabinet cabinet = new Cabinet();
    cabinet.setId(3L);
    TerminalStrip strip = new TerminalStrip();
    strip.setId(11L);
    strip.setCabinet(cabinet);
    strip.setName("1-4QD");
    strip.setLabelPrefix("QD");
    when(terminalStripRepository.findById(11L)).thenReturn(Optional.of(strip));

    Terminal terminal = new Terminal();
    terminal.setId(99L);
    terminal.setTerminalStrip(strip);
    terminal.setTerminalLabel("31");
    when(terminalRepository.findById(99L)).thenReturn(Optional.of(terminal));

    TerminalOperation existingOperation = new TerminalOperation();
    existingOperation.setId(123L);
    existingOperation.setDeviceDisplayItemId(50L);
    existingOperation.setTerminalStripId(10L);
    when(terminalOperationRepository.findByDeviceDisplayItemId(50L))
        .thenReturn(Optional.of(existingOperation));
    when(terminalOperationTerminalRepository.findByTerminalOperationIdOrderBySortOrderAscIdAsc(123L))
        .thenReturn(Collections.emptyList());

    service.update(50L, updateRequest());

    assertThat(existingOperation.getId()).isEqualTo(123L);
    verify(terminalOperationRepository).upsertByDeviceDisplayItemId(50L, 11L);
    verify(terminalOperationRepository, never()).save(any(TerminalOperation.class));
    verify(terminalOperationRepository, never()).delete(any(TerminalOperation.class));
    verify(terminalOperationTerminalRepository).deleteByTerminalOperationId(123L);
  }

  private UpdateDeviceDisplayItemRequest updateRequest() {
    TerminalOperationTerminalRequest terminalRequest = new TerminalOperationTerminalRequest();
    terminalRequest.setTerminalId(99L);
    terminalRequest.setExpectedOutputCode("Ua");

    TerminalOperationRequest terminalOperationRequest = new TerminalOperationRequest();
    terminalOperationRequest.setTerminalStripId(11L);
    terminalOperationRequest.setTerminals(Collections.singletonList(terminalRequest));

    UpdateDeviceDisplayItemRequest request = new UpdateDeviceDisplayItemRequest();
    request.setTitle("端子操作");
    request.setMediaType(CognitionMediaType.TERMINAL_OPERATION);
    request.setContent("内容");
    request.setSortOrder(1);
    request.setEnabled(true);
    request.setTerminalOperation(terminalOperationRequest);
    return request;
  }
}
