package com.zeta.business.service;

import static org.junit.jupiter.api.Assertions.*;

import com.zeta.business.entities.wiringrequirement.PhaseMode;
import com.zeta.business.entities.wiringrequirement.WiringCategory;
import com.zeta.business.entities.wiringrequirement.WiringRequirementDtos.GroupCheckResponse;
import com.zeta.business.entities.wiringrequirement.WiringRequirementGroup;
import com.zeta.integration.monitor.TerminalStatusClient.TerminalWiringState;
import com.zeta.screen.terminal.Terminal;
import java.util.*;
import org.junit.jupiter.api.Test;

class WiringComparisonServiceTest {
  private final WiringComparisonService service = new WiringComparisonService(null);

  @Test
  void 单相任一相与N有接线即通过() {
    WiringRequirementGroup group = group(1L, null, null, 2L);
    Map<Long, Terminal> terminals = terminals(1L, 2L);
    Map<Long, TerminalWiringState> states = new HashMap<>();
    states.put(1L, connected("Ua"));
    states.put(2L, connected("Un"));

    GroupCheckResponse result = service.evaluateGroup(
        WiringCategory.VOLTAGE, PhaseMode.SINGLE_PHASE, group, terminals, states);
    assertTrue(result.isPassed());
    assertEquals("接线正确", result.getMessage());
  }

  @Test
  void 单相缺N不通过() {
    WiringRequirementGroup group = group(1L, null, null, 2L);
    Map<Long, TerminalWiringState> states = new HashMap<>();
    states.put(1L, connected("Ua"));

    GroupCheckResponse result = service.evaluateGroup(
        WiringCategory.VOLTAGE, PhaseMode.SINGLE_PHASE, group, terminals(1L, 2L), states);
    assertFalse(result.isPassed());
    assertEquals("缺少线路", result.getMessage());
  }

  @Test
  void 三相主家族相序正确通过() {
    WiringRequirementGroup group = group(1L, 2L, 3L, 4L);
    Map<Long, TerminalWiringState> states = new HashMap<>();
    states.put(1L, connected("Ua"));
    states.put(2L, connected("Ub"));
    states.put(3L, connected("Uc"));
    states.put(4L, connected("Un"));

    GroupCheckResponse result = service.evaluateGroup(
        WiringCategory.VOLTAGE, PhaseMode.THREE_PHASE, group, terminals(1L, 2L, 3L, 4L), states);
    assertTrue(result.isPassed());
    assertEquals("接线正确", result.getMessage());
  }

  @Test
  void 三相副家族相序正确通过() {
    WiringRequirementGroup group = group(1L, 2L, 3L, 4L);
    Map<Long, TerminalWiringState> states = new HashMap<>();
    states.put(1L, connected("Ux"));
    states.put(2L, connected("Uy"));
    states.put(3L, connected("Uz"));
    states.put(4L, connected("Un2"));

    GroupCheckResponse result = service.evaluateGroup(
        WiringCategory.VOLTAGE, PhaseMode.THREE_PHASE, group, terminals(1L, 2L, 3L, 4L), states);
    assertTrue(result.isPassed());
  }

  @Test
  void 三相缺相提示缺少线路() {
    WiringRequirementGroup group = group(1L, 2L, 3L, 4L);
    Map<Long, TerminalWiringState> states = new HashMap<>();
    states.put(1L, connected("Ua"));
    states.put(2L, connected("Ub"));
    states.put(3L, connected("Uc"));

    GroupCheckResponse result = service.evaluateGroup(
        WiringCategory.VOLTAGE, PhaseMode.THREE_PHASE, group, terminals(1L, 2L, 3L, 4L), states);
    assertFalse(result.isPassed());
    assertEquals("缺少线路", result.getMessage());
  }

  @Test
  void 三相相序错乱提示相序错误() {
    WiringRequirementGroup group = group(1L, 2L, 3L, 4L);
    Map<Long, TerminalWiringState> states = new HashMap<>();
    states.put(1L, connected("Ua"));
    states.put(2L, connected("Uc"));
    states.put(3L, connected("Ub"));
    states.put(4L, connected("Un"));

    GroupCheckResponse result = service.evaluateGroup(
        WiringCategory.VOLTAGE, PhaseMode.THREE_PHASE, group, terminals(1L, 2L, 3L, 4L), states);
    assertFalse(result.isPassed());
    assertEquals("相序错误", result.getMessage());
  }

  @Test
  void 三相A相非法提示相序错误() {
    WiringRequirementGroup group = group(1L, 2L, 3L, 4L);
    Map<Long, TerminalWiringState> states = new HashMap<>();
    states.put(1L, connected("Ub"));
    states.put(2L, connected("Ub"));
    states.put(3L, connected("Uc"));
    states.put(4L, connected("Un"));

    GroupCheckResponse result = service.evaluateGroup(
        WiringCategory.VOLTAGE, PhaseMode.THREE_PHASE, group, terminals(1L, 2L, 3L, 4L), states);
    assertFalse(result.isPassed());
    assertEquals("相序错误", result.getMessage());
  }

  @Test
  void 电流副家族相序正确通过() {
    WiringRequirementGroup group = group(1L, 2L, 3L, 4L);
    Map<Long, TerminalWiringState> states = new HashMap<>();
    states.put(1L, connected("Ix"));
    states.put(2L, connected("Iy"));
    states.put(3L, connected("Iz"));
    states.put(4L, connected("In2"));

    GroupCheckResponse result = service.evaluateGroup(
        WiringCategory.CURRENT, PhaseMode.THREE_PHASE, group, terminals(1L, 2L, 3L, 4L), states);
    assertTrue(result.isPassed());
  }

  private WiringRequirementGroup group(Long a, Long b, Long c, Long n) {
    WiringRequirementGroup group = new WiringRequirementGroup();
    group.setGroupNo(0);
    group.setTerminalAId(a);
    group.setTerminalBId(b);
    group.setTerminalCId(c);
    group.setTerminalNId(n);
    return group;
  }

  private Map<Long, Terminal> terminals(Long... ids) {
    Map<Long, Terminal> map = new HashMap<>();
    for (Long id : ids) {
      Terminal terminal = new Terminal();
      terminal.setId(id);
      terminal.setTerminalLabel("端子" + id);
      map.put(id, terminal);
    }
    return map;
  }

  private TerminalWiringState connected(String code) {
    return new TerminalWiringState(true, "CONNECTED", Collections.singletonList(code));
  }
}
