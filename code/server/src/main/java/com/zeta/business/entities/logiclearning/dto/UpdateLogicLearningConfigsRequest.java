package com.zeta.business.entities.logiclearning.dto;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateLogicLearningConfigsRequest {
  @NotEmpty(message = "请提交需要保存的逻辑配置")
  @Valid
  private List<@NotNull(message = "逻辑配置条目不能为空") Item> items;

  @Getter
  @Setter
  public static class Item {
    @NotNull(message = "缺少逻辑编号")
    private Long logicDiagramId;
    @NotNull(message = "排序序号不能为空")
    private Integer sortOrder;
    @NotNull(message = "请选择整组试验序列")
    @Min(value = 1, message = "整组试验序列只能为 1、2、3")
    @Max(value = 3, message = "整组试验序列只能为 1、2、3")
    private Integer wholeExperimentSequence;
  }
}
