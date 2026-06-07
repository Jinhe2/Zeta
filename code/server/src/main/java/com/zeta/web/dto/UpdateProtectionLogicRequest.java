package com.zeta.web.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
public class UpdateProtectionLogicRequest {

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

    @NotNull(message = "请指定启用状态")
    private Boolean enabled;
}
