package com.zeta.web.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
public class UpdateCabinetRequest {

    @NotBlank(message = "请输入屏柜名称")
    @Size(max = 128, message = "屏柜名称不能超过 128 个字符")
    private String name;

    @Size(max = 512, message = "描述不能超过 512 个字符")
    private String description;

    @NotNull(message = "请指定排序值")
    private Integer sortOrder;

    @NotNull(message = "请指定启用状态")
    private Boolean enabled;
}
