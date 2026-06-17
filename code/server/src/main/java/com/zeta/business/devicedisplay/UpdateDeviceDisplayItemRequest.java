package com.zeta.business.devicedisplay;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
public class UpdateDeviceDisplayItemRequest {

    @NotBlank(message = "请输入条目标题")
    @Size(max = 128, message = "标题不能超过 128 个字符")
    private String title;

    @NotBlank(message = "请上传认知图片")
    @Size(max = 512, message = "图片地址过长")
    private String imageUrl;

    @NotBlank(message = "请输入展示内容")
    private String content;

    @NotNull(message = "请指定排序值")
    private Integer sortOrder;

    @NotNull(message = "请指定启用状态")
    private Boolean enabled;
}
