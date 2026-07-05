package com.zeta.business.cabinetdisplay;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
public class CreateCabinetDisplayItemRequest {

    @NotBlank(message = "请输入条目名称")
    @Size(max = 128, message = "名称不能超过 128 个字符")
    private String title;

    /** 临时图片 ID（新方式：从临时表获取图片字节） */
    private Long imageId;

    /** 图片 URL（旧方式：兼容文件系统存储） */
    @Size(max = 512, message = "图片地址过长")
    private String imageUrl;

    @NotBlank(message = "请输入文字描述")
    private String content;

    @NotNull(message = "请指定排序值")
    private Integer sortOrder;

    private Boolean enabled = true;
}
