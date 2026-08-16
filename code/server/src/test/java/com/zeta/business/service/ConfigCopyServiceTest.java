package com.zeta.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeta.business.entities.cabinetdisplay.CabinetDisplayItemRepository;
import com.zeta.business.entities.cognitiondevice.CognitionDeviceRepository;
import com.zeta.business.entities.devicedisplay.*;
import com.zeta.business.entities.drawinglearning.*;
import com.zeta.business.entities.learningresource.LearningResourceRepository;
import com.zeta.business.entities.logiclearning.LogicLearningConfigRepository;
import com.zeta.business.entities.logicnodecognition.LogicNodeCognitionItemRepository;
import com.zeta.business.entities.samplingtest.*;
import com.zeta.screen.baseline.IedBaselineSettingItemRepository;
import com.zeta.screen.cabinet.CabinetRepository;
import com.zeta.screen.ieddevice.DeviceRepository;
import com.zeta.screen.ieddevice.Device;
import com.zeta.screen.logicdiagram.ProtectionLogic;
import com.zeta.screen.logicdiagram.ProtectionLogicRepository;
import com.zeta.screen.terminal.TerminalRepository;
import com.zeta.screen.terminal.TerminalStripRepository;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class ConfigCopyServiceTest {
  @Test
  void logicComparisonIgnoresObjectKeyOrder() throws Exception {
    ConfigCopyService service = service();
    ProtectionLogic source = logic("{\"inputs\":[{\"id\":\"a\",\"name\":\"A\"}],\"version\":\"1\"}");
    ProtectionLogic target = logic("{\"version\":\"1\",\"inputs\":[{\"name\":\"A\",\"id\":\"a\"}]}");
    assertThat(compare(service, source, target, device(1L, "IED_A"), device(2L, "IED_A"))).isTrue();
  }

  @Test
  void logicComparisonPreservesArrayOrder() throws Exception {
    ConfigCopyService service = service();
    ProtectionLogic source = logic("{\"inputs\":[{\"id\":\"a\"},{\"id\":\"b\"}]}");
    ProtectionLogic target = logic("{\"inputs\":[{\"id\":\"b\"},{\"id\":\"a\"}]}");
    assertThat(compare(service, source, target, device(1L, "IED_A"), device(2L, "IED_A"))).isFalse();
  }

  @Test
  void logicComparisonNormalizesMappedIedNames() throws Exception {
    ConfigCopyService service = service();
    ProtectionLogic source = logic("{\"input\":\"IED_A$MX$PhV$phsA\",\"label\":\"IED_A 电压\"}");
    ProtectionLogic target = logic("{\"label\":\"IED_B 电压\",\"input\":\"IED_B$MX$PhV$phsA\"}");
    assertThat(compare(service, source, target, device(1L, "IED_A"), device(2L, "IED_B"))).isTrue();
  }

  @Test
  void logicComparisonStillRejectsDifferentRelativeSignalPaths() throws Exception {
    ConfigCopyService service = service();
    ProtectionLogic source = logic("{\"input\":\"IED_A$MX$PhV$phsA\"}");
    ProtectionLogic target = logic("{\"input\":\"IED_B$MX$PhV$phsB\"}");
    assertThat(compare(service, source, target, device(1L, "IED_A"), device(2L, "IED_B"))).isFalse();
  }

  @Test
  void instanceReferenceNormalizationKeepsRelativePathSignificant() throws Exception {
    ConfigCopyService service = service();
    assertThat(normalizeReference(service, "IED_A$MX$PhV$phsA", "IED_A", 1L))
        .isEqualTo(normalizeReference(service, "IED_B$MX$PhV$phsA", "IED_B", 1L));
    assertThat(normalizeReference(service, "IED_A$MX$PhV$phsA", "IED_A", 1L))
        .isNotEqualTo(normalizeReference(service, "IED_B$MX$PhV$phsB", "IED_B", 1L));
  }

  private boolean compare(ConfigCopyService service, ProtectionLogic source, ProtectionLogic target,
                          Device sourceDevice, Device targetDevice) throws Exception {
    Method method = ConfigCopyService.class.getDeclaredMethod("sameLogic", ProtectionLogic.class,
        ProtectionLogic.class, Device.class, Device.class);
    method.setAccessible(true);
    return (Boolean) method.invoke(service, source, target, sourceDevice, targetDevice);
  }

  private String normalizeReference(ConfigCopyService service, String value, String iedName,
                                    Long sourceDeviceId) throws Exception {
    Method method = ConfigCopyService.class.getDeclaredMethod(
        "normalizeInstanceText", String.class, String.class, Long.class);
    method.setAccessible(true);
    return (String) method.invoke(service, value, iedName, sourceDeviceId);
  }

  private Device device(Long id, String iedName) {
    Device device = new Device();
    device.setId(id);
    device.setIedName(iedName);
    return device;
  }

  private ProtectionLogic logic(String json) {
    ProtectionLogic logic = new ProtectionLogic();
    logic.setVersion("1.0");
    logic.setProtectType("保护");
    logic.setConfigJson(json);
    return logic;
  }

  private ConfigCopyService service() {
    return new ConfigCopyService(
        mock(CabinetRepository.class), mock(DeviceRepository.class), mock(ProtectionLogicRepository.class),
        mock(TerminalStripRepository.class), mock(TerminalRepository.class),
        mock(IedBaselineSettingItemRepository.class), mock(CabinetDisplayItemRepository.class),
        mock(CognitionDeviceRepository.class), mock(DeviceDisplayItemRepository.class),
        mock(TerminalOperationRepository.class), mock(TerminalOperationTerminalRepository.class),
        mock(DrawingGroupRepository.class), mock(DrawingPageRepository.class),
        mock(DrawingCognitionItemRepository.class), mock(LogicLearningConfigRepository.class),
        mock(LogicNodeCognitionItemRepository.class), mock(SamplingTestItemRepository.class),
        mock(SamplingTestChannelRepository.class), mock(LearningResourceRepository.class),
        mock(SharedMediaCleanupService.class), new ObjectMapper());
  }
}
