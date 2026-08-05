package com.zeta.business.entities.logicnodecognition.dto;

import com.zeta.business.entities.logicnodecognition.*;
import com.zeta.business.media.CognitionMediaType;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LogicNodeCognitionItemAdminResponse {

  private Long id;
  private Long logicDiagramId;
  private String nodeId;
  private String nodeType;
  private String nodeName;
  private String title;
  private String imageUrl;
  private boolean hasImage;
  private CognitionMediaType mediaType;
  private String videoPath;
  private Double leftPercent;
  private Double topPercent;
  private Double widthPercent;
  private Double heightPercent;
  private String content;
  private int sortOrder;
  private boolean enabled;
  private Instant createdAt;
}
