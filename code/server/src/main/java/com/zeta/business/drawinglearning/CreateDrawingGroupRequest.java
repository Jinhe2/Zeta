package com.zeta.business.drawinglearning;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
public class CreateDrawingGroupRequest {

    @NotNull(message = "请选择图纸类型")
    private DrawingType drawingType;

    @NotBlank(message = "请输入分组名称")
    @Size(max = 128, message = "分组名称不能超过 128 个字符")
    private String name;

    @NotNull(message = "请指定排序值")
    private Integer sortOrder;

    private Boolean enabled = true;
}
