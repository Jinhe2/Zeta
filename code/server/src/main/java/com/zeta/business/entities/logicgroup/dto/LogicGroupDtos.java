package com.zeta.business.entities.logicgroup.dto;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public final class LogicGroupDtos {
  private LogicGroupDtos() {}

  @Getter
  @Setter
  @NoArgsConstructor
  public static class MemberRequest {
    private Long logicDiagramId;
    private Integer sortOrder;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  public static class CreateLogicGroupRequest {
    @NotBlank(message = "组合逻辑名称不能为空")
    private String name;
    private Integer sortOrder;
    @Valid private List<MemberRequest> members = new ArrayList<>();
  }

  @Getter
  @Setter
  @NoArgsConstructor
  public static class UpdateLogicGroupRequest {
    @NotBlank(message = "组合逻辑名称不能为空")
    private String name;
    private Integer sortOrder;
    @Valid private List<MemberRequest> members = new ArrayList<>();
  }

  @Getter
  @AllArgsConstructor
  public static class MemberResponse {
    private Long logicDiagramId;
    private String code;
    private String title;
    private Integer sortOrder;
  }

  @Getter
  @AllArgsConstructor
  public static class LogicGroupResponse {
    private Long id;
    private Long iedDeviceId;
    private String name;
    private Integer sortOrder;
    private int memberCount;
  }

  @Getter
  @AllArgsConstructor
  public static class LogicGroupDetailResponse {
    private Long id;
    private Long iedDeviceId;
    private String iedName;
    private String name;
    private Integer sortOrder;
    private List<MemberResponse> members;
  }
}
