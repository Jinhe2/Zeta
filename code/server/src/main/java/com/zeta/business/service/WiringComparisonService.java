package com.zeta.business.service;

import com.zeta.business.entities.wiringrequirement.*;
import com.zeta.business.entities.wiringrequirement.WiringRequirementDtos.*;
import com.zeta.business.service.WiringRequirementService.ResolvedWiringRequirement;
import com.zeta.integration.monitor.TerminalStatusClient;
import com.zeta.integration.monitor.TerminalStatusClient.TerminalWiringState;
import com.zeta.screen.terminal.Terminal;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class WiringComparisonService {
  private final TerminalStatusClient statusClient;

  public WiringComparisonService(TerminalStatusClient statusClient) {
    this.statusClient = statusClient;
  }

  public boolean hasEnabledConfigs(ResolvedWiringRequirement resolved) {
    return !resolved.getRequiredConfigs().isEmpty();
  }

  public Map<Long, TerminalWiringState> summonCurrentStates(Long cabinetId, List<Long> terminalIds) {
    return statusClient.summon(cabinetId, terminalIds);
  }

  public CheckResponse skipped(ResolvedWiringRequirement resolved) {
    return new CheckResponse("SKIPPED", Collections.emptyList());
  }

  public CheckResponse compareResolved(
      ResolvedWiringRequirement resolved, Map<Long, TerminalWiringState> states) {
    List<CategoryCheckResponse> categories = new ArrayList<>();
    boolean anyFail = false;
    for (WiringRequirementConfig config : resolved.getRequiredConfigs()) {
      List<WiringRequirementGroup> groups =
          resolved.getGroupsByConfigId().getOrDefault(config.getId(), Collections.emptyList());
      List<GroupCheckResponse> groupChecks = new ArrayList<>();
      boolean categoryPassed = true;
      for (WiringRequirementGroup group : groups) {
        GroupCheckResponse check = evaluateGroup(
            config.getCategory(), config.getPhaseMode(), group,
            resolved.getTerminalsById(), states);
        groupChecks.add(check);
        if (!check.isPassed()) {
          categoryPassed = false;
          anyFail = true;
        }
      }
      categories.add(new CategoryCheckResponse(
          config.getCategory(), config.getPhaseMode(), categoryPassed, groupChecks));
    }
    if (categories.isEmpty()) return skipped(resolved);
    return new CheckResponse(anyFail ? "MISMATCH" : "MATCHED", categories);
  }

  public GroupCheckResponse evaluateGroup(
      WiringCategory category, PhaseMode mode, WiringRequirementGroup group,
      Map<Long, Terminal> terminalsById, Map<Long, TerminalWiringState> states) {
    PhaseInfo a = phaseInfo("A", group.getTerminalAId(), terminalsById, states);
    PhaseInfo b = phaseInfo("B", group.getTerminalBId(), terminalsById, states);
    PhaseInfo c = phaseInfo("C", group.getTerminalCId(), terminalsById, states);
    PhaseInfo n = phaseInfo("N", group.getTerminalNId(), terminalsById, states);
    PhaseInfo[] phases = {a, b, c, n};

    boolean passed;
    String message;
    Map<String, String> family = null;

    if (mode == PhaseMode.SINGLE_PHASE) {
      boolean anyPhaseWired = a.wired || b.wired || c.wired;
      passed = anyPhaseWired && n.wired;
      message = passed ? "接线正确" : "缺少线路";
    } else {
      boolean allWired = a.wired && b.wired && c.wired && n.wired;
      if (!allWired) {
        passed = false;
        message = "缺少线路";
      } else {
        family = resolveFamily(category, a.actualOutput);
        if (family == null) {
          passed = false;
          message = "相序错误";
        } else {
          boolean ordered = Objects.equals(b.actualOutput, family.get("B"))
              && Objects.equals(c.actualOutput, family.get("C"))
              && Objects.equals(n.actualOutput, family.get("N"));
          passed = ordered;
          message = ordered ? "接线正确" : "相序错误";
        }
      }
    }

    List<PhaseCheckResponse> phaseResponses = new ArrayList<>();
    for (PhaseInfo info : phases) {
      String expected = family == null ? null : family.get(info.position);
      phaseResponses.add(new PhaseCheckResponse(
          info.position, info.terminalId, info.terminalLabel, expected, info.actualOutput, info.wired));
    }
    return new GroupCheckResponse(group.getGroupNo(), passed, message, phaseResponses);
  }

  private PhaseInfo phaseInfo(
      String position, Long terminalId, Map<Long, Terminal> terminalsById,
      Map<Long, TerminalWiringState> states) {
    Terminal terminal = terminalId == null ? null : terminalsById.get(terminalId);
    String label = terminal == null ? null : terminal.getTerminalLabel();
    TerminalWiringState state = terminalId == null ? null : states.get(terminalId);
    boolean wired = state != null && state.isConnected();
    String actual = state == null ? null : state.firstOutputCode();
    return new PhaseInfo(position, terminalId, label, actual, wired);
  }

  private Map<String, String> resolveFamily(WiringCategory category, String aCode) {
    if (aCode == null) return null;
    Map<String, String> primary = primaryFamily(category);
    if (aCode.equals(primary.get("A"))) return primary;
    Map<String, String> secondary = secondaryFamily(category);
    if (aCode.equals(secondary.get("A"))) return secondary;
    return null;
  }

  private static Map<String, String> primaryFamily(WiringCategory category) {
    return category == WiringCategory.VOLTAGE
        ? familyMap("Ua", "Ub", "Uc", "Un")
        : familyMap("Ia", "Ib", "Ic", "In");
  }

  private static Map<String, String> secondaryFamily(WiringCategory category) {
    return category == WiringCategory.VOLTAGE
        ? familyMap("Ux", "Uy", "Uz", "Un2")
        : familyMap("Ix", "Iy", "Iz", "In2");
  }

  private static Map<String, String> familyMap(String a, String b, String c, String n) {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("A", a);
    map.put("B", b);
    map.put("C", c);
    map.put("N", n);
    return map;
  }

  private static class PhaseInfo {
    final String position;
    final Long terminalId;
    final String terminalLabel;
    final String actualOutput;
    final boolean wired;

    PhaseInfo(String position, Long terminalId, String terminalLabel, String actualOutput, boolean wired) {
      this.position = position;
      this.terminalId = terminalId;
      this.terminalLabel = terminalLabel;
      this.actualOutput = actualOutput;
      this.wired = wired;
    }
  }
}
