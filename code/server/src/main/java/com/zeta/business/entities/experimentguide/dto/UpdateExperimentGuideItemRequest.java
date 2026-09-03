package com.zeta.business.entities.experimentguide.dto;

import com.zeta.business.entities.experimentguide.ExperimentGuideType;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateExperimentGuideItemRequest {

  @NotNull(message = "请选择引导类型")
  private ExperimentGuideType type;

  @NotBlank(message = "请输入引导标题")
  @Size(max = 128, message = "标题不能超过 128 个字符")
  private String title;

  private Long imageId;

  @Size(max = 512, message = "图片地址过长")
  private String imageUrl;

  private String content;
  private Integer sortOrder;

  private Boolean enabled = true;

  private Boolean showInWholeExperiment;
}
