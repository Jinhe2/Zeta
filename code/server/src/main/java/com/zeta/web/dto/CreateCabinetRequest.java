package com.zeta.web.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
public class CreateCabinetRequest {

    @NotBlank(message = "请输入屏柜编码")
    @Size(max = 64, message = "屏柜编码不能超过 64 个字符")
    private String code;

    @NotBlank(message = "请输入屏柜名称")
    @Size(max = 128, message = "屏柜名称不能超过 128 个字符")
    private String name;

    @Size(max = 512, message = "描述不能超过 512 个字符")
    private String description;

    @NotNull(message = "请指定排序值")
    private Integer sortOrder;

    private Boolean enabled = true;
}
