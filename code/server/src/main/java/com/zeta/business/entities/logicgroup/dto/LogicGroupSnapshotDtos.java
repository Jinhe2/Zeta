package com.zeta.business.entities.logicgroup.dto;

import com.zeta.screen.logicdiagram.SectionSnapshotResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

public final class LogicGroupSnapshotDtos {
  private LogicGroupSnapshotDtos() {}

  @Getter
  @AllArgsConstructor
  public static class MemberSummaryResponse {
    private Long logicDiagramId;
    private String logicId;
    private Integer order;
    private String status;
    private Boolean experimentPassed;
    private Integer totalTransitions;
    private String errorCode;
    private String errorMessage;
  }

  @Getter
  @AllArgsConstructor
  public static class MemberDetailResponse {
    private Long groupSnapshotId;
    private Long groupId;
    private Long logicDiagramId;
    private String logicId;
    private Integer order;
    private String status;
    private Boolean experimentPassed;
    private Integer totalTransitions;
    private String errorCode;
    private String errorMessage;
    private List<SectionSnapshotResponse> sections;
  }
}
