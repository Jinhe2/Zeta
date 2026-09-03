package com.zeta.business.entities.wholeexperiment;

import java.time.Instant;
import java.util.List;
import javax.validation.constraints.*;
import lombok.*;

public final class WholeExperimentDtos {
  private WholeExperimentDtos() {}

  @Getter @Setter
  public static class CreateRequest {
    @NotNull(message = "请选择装置") @Positive(message = "装置编号无效") private Long deviceId;
    @NotNull(message = "请选择基础逻辑") @Size(min = 2, max = 3, message = "请选择两个或三个基础逻辑")
    private List<@NotNull(message = "基础逻辑不能为空") @Positive(message = "逻辑编号无效") Long> logicDiagramIds;
  }

  @Getter @Setter @NoArgsConstructor @AllArgsConstructor
  public static class Member {
    private Long logicDiagramId;
    private String code;
    private String title;
    private int sortOrder;
  }

  @Getter @AllArgsConstructor
  public static class Detail {
    private Long id;
    private Long iedDeviceId;
    private String iedName;
    private String name;
    private List<Member> members;
    private Instant lastStartedAt;
    private boolean valid;
    private String invalidReason;
  }

  @Getter @Setter
  public static class MonitorRequest {
    @NotNull(message = "请选择监测操作") private String action = "start";
    private String taskUuid;
  }
}
