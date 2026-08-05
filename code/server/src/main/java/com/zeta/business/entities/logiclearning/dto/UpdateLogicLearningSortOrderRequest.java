package com.zeta.business.entities.logiclearning.dto;

import com.zeta.business.entities.logiclearning.*;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateLogicLearningSortOrderRequest {

  @NotNull(message = "请输入排序序号")
  private Integer sortOrder;
}
