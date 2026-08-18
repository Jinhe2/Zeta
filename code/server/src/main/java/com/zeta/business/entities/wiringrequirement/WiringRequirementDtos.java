package com.zeta.business.entities.wiringrequirement;

import com.zeta.business.entities.settinglist.SettingListScopeType;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public final class WiringRequirementDtos {
  private WiringRequirementDtos() {}

  @Getter
  @AllArgsConstructor
  public static class TerminalRef {
    private Long terminalId;
    private String terminalLabel;
    private Long terminalStripId;
    private String terminalStripName;
  }

  @Getter
  @AllArgsConstructor
  public static class GroupResponse {
    private Integer groupNo;
    private TerminalRef a;
    private TerminalRef b;
    private TerminalRef c;
    private TerminalRef n;
  }

  @Getter
  @AllArgsConstructor
  public static class CategoryResponse {
    private WiringCategory category;
    private boolean required;
    private PhaseMode phaseMode;
    private List<GroupResponse> groups;
  }

  @Getter
  @AllArgsConstructor
  public static class GetResponse {
    private SettingListScopeType scopeType;
    private Long scopeId;
    private String scopeName;
    private Long cabinetId;
    private List<CategoryResponse> categories;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  public static class GroupRequest {
    private Long terminalAId;
    private Long terminalBId;
    private Long terminalCId;
    private Long terminalNId;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  public static class CategoryRequest {
    @NotNull(message = "接线类别不能为空")
    private WiringCategory category;
    private Boolean required = false;
    @NotNull(message = "接线方式不能为空")
    private PhaseMode phaseMode = PhaseMode.THREE_PHASE;
    @Valid
    private List<GroupRequest> groups = new ArrayList<>();
  }

  @Getter
  @Setter
  @NoArgsConstructor
  public static class SaveRequest {
    @Valid
    private List<CategoryRequest> categories = new ArrayList<>();
  }

  // ── 校验响应 ──

  @Getter
  @AllArgsConstructor
  public static class PhaseCheckResponse {
    private String position;
    private Long terminalId;
    private String terminalLabel;
    private String expectedOutput;
    private String actualOutput;
    private boolean wired;
  }

  @Getter
  @AllArgsConstructor
  public static class GroupCheckResponse {
    private Integer groupNo;
    private boolean passed;
    private String message;
    private List<PhaseCheckResponse> phases;
  }

  @Getter
  @AllArgsConstructor
  public static class CategoryCheckResponse {
    private WiringCategory category;
    private PhaseMode phaseMode;
    private boolean passed;
    private List<GroupCheckResponse> groups;
  }

  @Getter
  @AllArgsConstructor
  public static class CheckResponse {
    private String status;
    private List<CategoryCheckResponse> categories;
  }
}
