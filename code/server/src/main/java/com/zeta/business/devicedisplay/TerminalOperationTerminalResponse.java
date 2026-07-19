package com.zeta.business.devicedisplay;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TerminalOperationTerminalResponse {
    private Long terminalId;
    private String terminalLabel;
    private String meaning;
}
