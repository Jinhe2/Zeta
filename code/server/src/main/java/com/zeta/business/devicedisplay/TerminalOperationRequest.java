package com.zeta.business.devicedisplay;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TerminalOperationRequest {
    private Long terminalStripId;
    private List<TerminalOperationTerminalRequest> terminals;
}
