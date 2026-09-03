package com.zeta.business.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.zeta.business.entities.logiclearning.*;
import com.zeta.business.entities.logiclearning.dto.UpdateLogicLearningConfigsRequest.Item;
import com.zeta.screen.logicdiagram.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class LogicLearningBatchSaveTest {
  private LogicLearningConfigRepository repository;
  private LogicLearningConfigService service;
  private LogicLearningConfig first;
  private LogicLearningConfig second;

  @BeforeEach
  void 初始化() {
    repository = mock(LogicLearningConfigRepository.class);
    ProtectionLogicRepository logics = mock(ProtectionLogicRepository.class);
    service = new LogicLearningConfigService(repository, logics);
    ProtectionLogic a = new ProtectionLogic(); a.setId(10L);
    ProtectionLogic b = new ProtectionLogic(); b.setId(11L);
    when(logics.findByDeviceIdOrderByIdAsc(12L)).thenReturn(Arrays.asList(a, b));
    first = config(10L, 10, 1); second = config(11L, 20, 2);
    when(repository.findByLogicDiagramIdIn(Arrays.asList(10L, 11L))).thenReturn(Arrays.asList(first, second));
  }

  @Test
  void 一次保存两条逻辑的排序互换和序列() {
    List<Item> result = service.updateConfigs(12L, Arrays.asList(item(10L, 20, 3), item(11L, 10, 1)));
    assertEquals(2, result.size());
    assertEquals(20, first.getSortOrder().intValue());
    assertEquals(3, first.getWholeExperimentSequence().intValue());
    assertEquals(10, second.getSortOrder().intValue());
    assertEquals(1, second.getWholeExperimentSequence().intValue());
    verify(repository, times(1)).saveAll(Arrays.asList(first, second));
  }

  @Test
  void 后续条目非法时前面的配置也不修改() {
    assertThrows(ResponseStatusException.class, () -> service.updateConfigs(12L,
        Arrays.asList(item(10L, 30, 3), item(11L, 40, 4))));
    assertEquals(10, first.getSortOrder().intValue());
    assertEquals(1, first.getWholeExperimentSequence().intValue());
    verify(repository, never()).saveAll(any());
  }

  @Test
  void 最终排序与未提交的逻辑冲突则拒绝整个保存() {
    assertThrows(ResponseStatusException.class, () -> service.updateConfigs(12L, Collections.singletonList(item(10L, 20, 3))));
    verify(repository, never()).saveAll(any());
    assertEquals(10, first.getSortOrder().intValue());
  }

  @Test
  void 拒绝跨装置和重复逻辑编号() {
    assertThrows(ResponseStatusException.class, () -> service.updateConfigs(12L, Collections.singletonList(item(99L, 1, 1))));
    assertThrows(ResponseStatusException.class, () -> service.updateConfigs(12L, Arrays.asList(item(10L, 1, 1), item(10L, 2, 2))));
    verify(repository, never()).saveAll(any());
  }

  @Test
  void 旧默认排序重复时仍可只修改序列() {
    first.setSortOrder(0); second.setSortOrder(0);
    service.updateConfigs(12L, Collections.singletonList(item(10L, 0, 3)));
    assertEquals(3, first.getWholeExperimentSequence().intValue());
    assertEquals(2, second.getWholeExperimentSequence().intValue());
    verify(repository).saveAll(Collections.singletonList(first));
  }

  private LogicLearningConfig config(Long id, int order, int sequence) {
    LogicLearningConfig config = new LogicLearningConfig();
    config.setLogicDiagramId(id); config.setSortOrder(order); config.setWholeExperimentSequence(sequence);
    return config;
  }

  private Item item(Long id, int order, int sequence) {
    Item item = new Item(); item.setLogicDiagramId(id); item.setSortOrder(order); item.setWholeExperimentSequence(sequence);
    return item;
  }
}
