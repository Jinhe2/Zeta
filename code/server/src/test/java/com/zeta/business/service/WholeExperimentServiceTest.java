package com.zeta.business.service;

import com.zeta.business.entities.wholeexperiment.*;
import com.zeta.business.entities.wholeexperiment.WholeExperimentDtos.*;
import com.zeta.screen.ieddevice.*;
import com.zeta.screen.logicdiagram.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WholeExperimentServiceTest {
  WholeExperimentRepository repository = mock(WholeExperimentRepository.class);
  WholeExperimentMemberRepository members = mock(WholeExperimentMemberRepository.class);
  ProtectionLogicRepository logics = mock(ProtectionLogicRepository.class);
  LogicLearningConfigService configs = mock(LogicLearningConfigService.class);
  WholeExperimentService service = new WholeExperimentService(repository, members, logics,
      mock(DeviceRepository.class), configs);

  @BeforeEach void 配置() {
    when(configs.getWholeExperimentSequences(anyList())).thenReturn(new HashMap<Long, Integer>() {{
      put(11L, 1); put(22L, 2); put(33L, 3); put(44L, 1);
    }});
    for (long id : new long[] {11, 22, 33, 44}) {
      Device device = new Device(); device.setId(7L);
      ProtectionLogic logic = new ProtectionLogic();
      logic.setId(id); logic.setDevice(device); logic.setLogicId("逻辑" + id); logic.setLogicName("逻辑" + id);
      when(logics.findById(id)).thenReturn(Optional.of(logic));
    }
  }

  @Test void 后端自动排序并接受两个或三个() {
    assertEquals(Arrays.asList(11L, 22L), WholeExperimentService.ids(service.validate(7L, Arrays.asList(22L, 11L))));
    assertEquals(Arrays.asList(11L, 22L, 33L),
        WholeExperimentService.ids(service.validate(7L, Arrays.asList(33L, 11L, 22L))));
  }

  @Test void 拒绝单个过多空值和重复() {
    for (List<Long> ids : Arrays.asList(Collections.singletonList(11L), Arrays.asList(11L, 22L, 33L, 44L),
        Arrays.asList(11L, 11L), Arrays.asList(11L, null))) {
      assertThrows(ResponseStatusException.class, () -> service.validate(7L, ids));
    }
  }

  @Test void 拒绝跳序列或同序列() {
    assertThrows(ResponseStatusException.class, () -> service.validate(7L, Arrays.asList(11L, 33L)));
    assertThrows(ResponseStatusException.class, () -> service.validate(7L, Arrays.asList(22L, 33L)));
    assertThrows(ResponseStatusException.class, () -> service.validate(7L, Arrays.asList(11L, 44L)));
  }

  @Test void 拒绝跨装置和逻辑不存在() {
    assertThrows(ResponseStatusException.class, () -> service.validate(9L, Arrays.asList(11L, 22L)));
    assertThrows(ResponseStatusException.class, () -> service.validate(7L, Arrays.asList(11L, 99L)));
  }

  @Test void 拒绝其他学员组合() {
    assertThrows(ResponseStatusException.class, () -> service.require(2L, 100L));
    verify(repository).findByIdAndUserId(100L, 2L);
  }

  @Test void 历史序列交换必须重新选择() {
    WholeExperiment experiment = new WholeExperiment();
    experiment.setId(100L); experiment.setDeviceId(7L);
    when(repository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(experiment));
    WholeExperimentMember one = new WholeExperimentMember(); one.setLogicDiagramId(11L); one.setSequenceNo(1);
    WholeExperimentMember two = new WholeExperimentMember(); two.setLogicDiagramId(22L); two.setSequenceNo(2);
    when(members.findByExperimentIdOrderBySequenceNoAsc(100L)).thenReturn(Arrays.asList(one, two));
    when(configs.getWholeExperimentSequences(anyList())).thenReturn(new HashMap<Long, Integer>() {{
      put(11L, 2); put(22L, 1);
    }});
    assertThrows(ResponseStatusException.class, () -> service.validatedMembers(1L, 100L));
  }

  @Test void 最近列表按学员装置限定五种() {
    service.recent(1L, 7L);
    verify(repository).findTop5ByUserIdAndDeviceIdAndLastStartedAtIsNotNullOrderByLastStartedAtDescIdDesc(1L, 7L);
  }
}
