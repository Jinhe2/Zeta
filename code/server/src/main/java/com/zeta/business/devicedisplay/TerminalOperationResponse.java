package com.zeta.business.devicedisplay;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class TerminalOperationResponse {
    private Long terminalStripId;
    private String terminalStripName;
    private String terminalStripLabelPrefix;
    private List<TerminalOperationTerminalResponse> terminals;
}
