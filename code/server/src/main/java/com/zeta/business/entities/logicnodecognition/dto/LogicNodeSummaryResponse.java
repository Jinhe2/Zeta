package com.zeta.business.entities.logicnodecognition.dto;

import com.zeta.business.entities.logicnodecognition.*;
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
