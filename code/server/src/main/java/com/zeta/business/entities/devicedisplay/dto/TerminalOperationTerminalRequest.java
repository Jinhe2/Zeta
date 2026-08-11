package com.zeta.business.entities.devicedisplay.dto;

import com.zeta.business.entities.devicedisplay.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TerminalOperationTerminalRequest {
  private Long terminalId;
  private String expectedOutputCode;
}
