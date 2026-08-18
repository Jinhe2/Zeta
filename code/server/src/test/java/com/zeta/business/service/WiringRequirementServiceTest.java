package com.zeta.business.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.entities.wiringrequirement.*;
import com.zeta.business.entities.wiringrequirement.WiringRequirementDtos.*;
import com.zeta.business.service.SettingListTargetService.Target;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class WiringRequirementServiceTest {
  private WiringRequirementConfigRepository configRepository;
  private WiringRequirementGroupRepository groupRepository;
  private SettingListTargetService targetService;
  private TerminalCatalogService terminalCatalogService;
  private WiringRequirementService service;

  @BeforeEach
  void setUp() {
    configRepository = mock(WiringRequirementConfigRepository.class);
    groupRepository = mock(WiringRequirementGroupRepository.class);
    targetService = mock(SettingListTargetService.class);
    terminalCatalogService = mock(TerminalCatalogService.class);
    service = new WiringRequirementService(
        configRepository, groupRepository, targetService, terminalCatalogService);
    Target target = new Target(SettingListScopeType.LOGIC_DIAGRAM, 8L, "过流保护", 2L, "IED_A", 3L);
    when(targetService.require(SettingListScopeType.LOGIC_DIAGRAM, 8L)).thenReturn(target);
    when(terminalCatalogService.byId(anyCollection())).thenReturn(Collections.emptyMap());
  }

  @Test
  void 需要接入但无分组时拒绝() {
    SaveRequest save = request(WiringCategory.VOLTAGE, true, PhaseMode.THREE_PHASE,
        Collections.emptyList());
    assertThrows(ResponseStatusException.class, () -> service.replace(8L, save));
  }

  @Test
  void 三相缺列时拒绝() {
    SaveRequest save = request(WiringCategory.VOLTAGE, true, PhaseMode.THREE_PHASE,
        Collections.singletonList(groupReq(1L, 2L, 3L, null)));
    assertThrows(ResponseStatusException.class, () -> service.replace(8L, save));
  }

  @Test
  void 单相缺N时拒绝() {
    SaveRequest save = request(WiringCategory.CURRENT, true, PhaseMode.SINGLE_PHASE,
        Collections.singletonList(groupReq(1L, null, null, null)));
    assertThrows(ResponseStatusException.class, () -> service.replace(8L, save));
  }

  @Test
  void 组内端子重复时拒绝() {
    SaveRequest save = request(WiringCategory.VOLTAGE, true, PhaseMode.THREE_PHASE,
        Collections.singletonList(groupReq(1L, 1L, 3L, 4L)));
    assertThrows(ResponseStatusException.class, () -> service.replace(8L, save));
  }

  @Test
  void 端子不存在时拒绝() {
    SaveRequest save = request(WiringCategory.VOLTAGE, true, PhaseMode.THREE_PHASE,
        Collections.singletonList(groupReq(1L, 2L, 3L, 4L)));
    assertThrows(ResponseStatusException.class, () -> service.replace(8L, save));
  }

  private SaveRequest request(
      WiringCategory category, boolean required, PhaseMode mode, List<GroupRequest> groups) {
    CategoryRequest categoryRequest = new CategoryRequest();
    categoryRequest.setCategory(category);
    categoryRequest.setRequired(required);
    categoryRequest.setPhaseMode(mode);
    categoryRequest.setGroups(groups);
    SaveRequest save = new SaveRequest();
    save.setCategories(Collections.singletonList(categoryRequest));
    return save;
  }

  private GroupRequest groupReq(Long a, Long b, Long c, Long n) {
    GroupRequest group = new GroupRequest();
    group.setTerminalAId(a);
    group.setTerminalBId(b);
    group.setTerminalCId(c);
    group.setTerminalNId(n);
    return group;
  }
}
