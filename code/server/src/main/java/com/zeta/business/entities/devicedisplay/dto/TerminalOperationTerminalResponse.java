package com.zeta.business.entities.devicedisplay.dto;

import com.zeta.business.entities.devicedisplay.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TerminalOperationTerminalResponse {
  private Long terminalId;
  private String terminalLabel;
  private String meaning;
}
