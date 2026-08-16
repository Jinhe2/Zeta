package com.zeta.business.entities.samplingtest.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SamplingTestChannelResponse {
  private String outputCode;
  private Long terminalId;
  private String terminalLabel;
  private Long terminalStripId;
  private String terminalStripName;
  private String terminalStripLabelPrefix;
  private BigDecimal baselineMagnitude;
  private BigDecimal baselineAngle;
}
