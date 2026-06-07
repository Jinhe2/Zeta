package com.zeta.web.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
public class CreateProtectionLogicRequest {

    @NotBlank(message = "请输入逻辑编码")
    @Size(max = 64, message = "逻辑编码不能超过 64 个字符")
    private String code;

    @NotBlank(message = "请输入逻辑名称")
    @Size(max = 128, message = "逻辑名称不能超过 128 个字符")
    private String title;

    @Size(max = 512, message = "描述不能超过 512 个字符")
    private String description;

    @NotBlank(message = "请输入保护类别")
    @Size(max = 32, message = "保护类别不能超过 32 个字符")
    private String category;

    @NotNull(message = "请指定排序值")
    private Integer sortOrder;

    private Boolean enabled = true;
}
