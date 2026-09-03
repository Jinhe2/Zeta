package com.zeta.business.entities.logiclearning.dto;

import javax.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWholeExperimentSequenceRequest {
  @NotNull(message = "请选择整组试验序列")
  @Min(value = 1, message = "整组试验序列只能为 1、2、3")
  @Max(value = 3, message = "整组试验序列只能为 1、2、3")
  private Integer wholeExperimentSequence;
}
