package com.zeta.business.entities.samplingtest.dto;

import java.math.BigDecimal;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SamplingTestChannelRequest {
  @NotBlank(message = "采样通道不能为空")
  private String outputCode;

  @NotNull(message = "请选择关联端子")
  private Long terminalId;

  private BigDecimal baselineMagnitude;
  private BigDecimal baselineAngle;
}
