package com.zeta.business.logicnodecognition;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import com.zeta.business.media.CognitionMediaType;

@Getter
@Setter
public class CreateLogicNodeCognitionItemRequest {

    @NotBlank(message = "请输入条目标题")
    @Size(max = 128, message = "标题不能超过 128 个字符")
    private String title;

    private Long imageId;

    @Size(max = 512, message = "图片地址过长")
    private String imageUrl;

    @NotNull(message = "请选择媒体类型")
    private CognitionMediaType mediaType = CognitionMediaType.IMAGE;

    @Size(max = 512, message = "视频路径过长")
    private String videoPath;

    private Double leftPercent;

    private Double topPercent;

    private Double widthPercent;

    private Double heightPercent;

    private String content;

    @NotNull(message = "请指定排序值")
    private Integer sortOrder;

    private Boolean enabled = true;
}
