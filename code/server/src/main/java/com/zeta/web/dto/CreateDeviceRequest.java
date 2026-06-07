package com.zeta.web.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
public class CreateDeviceRequest {

    @NotBlank(message = "请输入设备编码")
    @Size(max = 64, message = "设备编码不能超过 64 个字符")
    private String code;

    @NotBlank(message = "请输入设备名称")
    @Size(max = 128, message = "设备名称不能超过 128 个字符")
    private String name;

    @Size(max = 512, message = "描述不能超过 512 个字符")
    private String description;

    @NotNull(message = "请指定排序值")
    private Integer sortOrder;

    private Boolean enabled = true;
}
