package com.zeta.business.logicnodecognition;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LogicNodeSummaryResponse {

    private String nodeId;
    private String nodeName;
    private String nodeType;
    private long itemCount;
}
