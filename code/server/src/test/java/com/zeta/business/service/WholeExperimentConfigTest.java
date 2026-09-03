package com.zeta.business.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import com.zeta.business.entities.cabinetdisplay.TemporaryImageRepository;
import com.zeta.business.entities.experimentguide.*;
import com.zeta.business.entities.experimentguide.dto.*;
import com.zeta.business.entities.logiclearning.*;
import com.zeta.business.entities.logiclearning.dto.UpdateWholeExperimentSequenceRequest;
import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.entities.settinglist.dto.*;
import com.zeta.screen.logicdiagram.*;
import com.zeta.screen.ieddevice.Device;
import java.util.*;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class WholeExperimentConfigTest {
  @Test
  void 引导默认显示并允许关闭且旧更新请求保留配置() {
    ExperimentGuideItemRepository repository = mock(ExperimentGuideItemRepository.class);
    ExperimentGuideService service = new ExperimentGuideService(repository,
        mock(SettingListTargetService.class), mock(SettingListService.class),
        mock(TemporaryImageRepository.class), mock(SharedMediaCleanupService.class));
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    CreateExperimentGuideItemRequest create = new CreateExperimentGuideItemRequest();
    create.setType(ExperimentGuideType.SETTING_LIST);
    create.setTitle("整定引导");
    assertTrue(service.create(SettingListScopeType.LOGIC_DIAGRAM, 10L, create).isShowInWholeExperiment());

    ExperimentGuideItem item = new ExperimentGuideItem();
    item.setId(1L); item.setScopeType(SettingListScopeType.LOGIC_DIAGRAM); item.setScopeId(10L);
    item.setType(ExperimentGuideType.SETTING_LIST); item.setTitle("整定引导");
    when(repository.findById(1L)).thenReturn(Optional.of(item));
    when(repository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.LOGIC_DIAGRAM, 10L))
        .thenReturn(Collections.singletonList(item));
    UpdateExperimentGuideItemRequest update = new UpdateExperimentGuideItemRequest();
    update.setType(ExperimentGuideType.SETTING_LIST); update.setTitle("整定引导");
    update.setShowInWholeExperiment(false);
    assertFalse(service.update(1L, update).isShowInWholeExperiment());
    update.setShowInWholeExperiment(null);
    assertFalse(service.update(1L, update).isShowInWholeExperiment());
  }

  @Test
  void 整组开关不影响普通引导且定值整定返回完整清单() {
    ExperimentGuideItemRepository repository = mock(ExperimentGuideItemRepository.class);
    SettingListService settings = mock(SettingListService.class);
    ExperimentGuideService service = new ExperimentGuideService(repository,
        mock(SettingListTargetService.class), settings, mock(TemporaryImageRepository.class), mock(SharedMediaCleanupService.class));
    ExperimentGuideItem item = new ExperimentGuideItem();
    item.setType(ExperimentGuideType.SETTING_LIST); item.setShowInWholeExperiment(false);
    when(repository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.LOGIC_DIAGRAM, 10L))
        .thenReturn(Collections.singletonList(item));
    List<SettingListItemResponse> rows = Arrays.asList(
        new SettingListItemResponse("A", "SG", "定值A", "FLOAT", true, "1", 0),
        new SettingListItemResponse("B", "SG", "定值B", "FLOAT", false, "2", 1));
    when(settings.get(SettingListScopeType.LOGIC_DIAGRAM, 10L)).thenReturn(new SettingListResponse(
        SettingListScopeType.LOGIC_DIAGRAM, 10L, "逻辑", 12L, "装置", SettingListScopeType.IED_DEVICE,
        12L, false, Collections.emptyList(), rows));
    List<ExperimentGuideItemStudentResponse> guides = service.listEnabledByScope(SettingListScopeType.LOGIC_DIAGRAM, 10L);
    assertEquals(1, guides.size());
    assertFalse(guides.get(0).isShowInWholeExperiment());
    assertEquals(2, guides.get(0).getSettingItems().size());
  }

  @Test
  void 序列默认一且与普通排序互不覆盖() {
    LogicLearningConfigRepository repository = mock(LogicLearningConfigRepository.class);
    ProtectionLogicRepository logics = mock(ProtectionLogicRepository.class);
    LogicLearningConfigService service = new LogicLearningConfigService(repository, logics);
    assertEquals(1, service.getWholeExperimentSequences(Collections.singleton(10L)).getOrDefault(10L, 1).intValue());
    when(logics.existsById(10L)).thenReturn(true);
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    LogicLearningConfig config = new LogicLearningConfig();
    config.setLogicDiagramId(10L); config.setSortOrder(8);
    when(repository.findByLogicDiagramId(10L)).thenReturn(Optional.of(config));
    for (int sequence = 1; sequence <= 3; sequence++) {
      assertEquals(sequence, service.updateWholeExperimentSequence(10L, sequence));
      assertEquals(8, config.getSortOrder().intValue());
    }
    Device device = new Device(); device.setId(12L);
    ProtectionLogic logic = new ProtectionLogic(); logic.setId(10L); logic.setDevice(device);
    when(logics.findById(10L)).thenReturn(Optional.of(logic));
    when(logics.findByDeviceIdOrderByIdAsc(12L)).thenReturn(Collections.singletonList(logic));
    when(repository.findByLogicDiagramIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(config));
    service.updateSortOrder(10L, 6);
    assertEquals(3, config.getWholeExperimentSequence().intValue());
    assertThrows(ResponseStatusException.class, () -> service.updateWholeExperimentSequence(10L, 0));
    assertThrows(ResponseStatusException.class, () -> service.updateWholeExperimentSequence(10L, 4));
    assertThrows(ResponseStatusException.class, () -> service.updateWholeExperimentSequence(99L, 1));
  }

  @Test
  void 序列请求拒绝缺失和越界值() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      UpdateWholeExperimentSequenceRequest request = new UpdateWholeExperimentSequenceRequest();
      assertFalse(factory.getValidator().validate(request).isEmpty());
      request.setWholeExperimentSequence(4);
      assertFalse(factory.getValidator().validate(request).isEmpty());
      request.setWholeExperimentSequence(2);
      assertTrue(factory.getValidator().validate(request).isEmpty());
    }
  }
}
