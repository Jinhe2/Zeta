package com.zeta.business.entities.devicedisplay.dto;

import com.zeta.business.entities.devicedisplay.*;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TerminalOperationResponse {
    private Long terminalStripId;
    private String terminalStripName;
    private String terminalStripLabelPrefix;
    private List<TerminalOperationTerminalResponse> terminals;
}
