package com.zeta.business.entities.samplingtest.dto;

import com.zeta.business.entities.samplingtest.SamplingTestMediaType;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SamplingTestItemRequest {
  @NotBlank(message = "请输入条目标题")
  @Size(max = 128, message = "标题不能超过 128 个字符")
  private String title;

  @NotNull(message = "请选择媒体类型")
  private SamplingTestMediaType mediaType;

  private Long imageId;
  private String imageUrl;
  private String videoPath;

  @NotBlank(message = "请输入说明文字")
  private String content;

  @NotNull(message = "请指定排序值")
  private Integer sortOrder;

  private Boolean enabled = true;

  @Valid
  private List<SamplingTestChannelRequest> channels;
}
