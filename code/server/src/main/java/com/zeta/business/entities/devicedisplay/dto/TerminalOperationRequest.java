package com.zeta.business.entities.devicedisplay.dto;

import com.zeta.business.entities.devicedisplay.*;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TerminalOperationRequest {
    private Long terminalStripId;
    private List<TerminalOperationTerminalRequest> terminals;
}
