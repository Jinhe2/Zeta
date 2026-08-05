package com.zeta.business.entities.drawinglearning.dto;

import com.zeta.business.entities.drawinglearning.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateDrawingCognitionItemRequest {

  @NotBlank(message = "请输入认知标题")
  @Size(max = 128, message = "标题不能超过 128 个字符")
  private String title;

  @NotBlank(message = "请输入文字说明")
  private String content;

  private Double leftPercent;

  private Double topPercent;

  private Double widthPercent;

  private Double heightPercent;

  @NotNull(message = "请指定排序值")
  private Integer sortOrder;

  @NotNull(message = "请选择启用状态")
  private Boolean enabled;
}
