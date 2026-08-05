package com.zeta.business.entities.drawinglearning.dto;

import com.zeta.business.entities.drawinglearning.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateDrawingPageRequest {

  @NotBlank(message = "请输入图纸标题")
  @Size(max = 128, message = "标题不能超过 128 个字符")
  private String title;

  private Long imageId;

  @Size(max = 512, message = "图片地址过长")
  private String imageUrl;

  @NotNull(message = "请指定排序值")
  private Integer sortOrder;

  @NotNull(message = "请选择启用状态")
  private Boolean enabled;
}
