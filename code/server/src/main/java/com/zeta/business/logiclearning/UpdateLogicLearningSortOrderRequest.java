package com.zeta.business.logiclearning;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class UpdateLogicLearningSortOrderRequest {

    @NotNull(message = "请输入排序序号")
    private Integer sortOrder;
}
