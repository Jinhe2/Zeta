package com.zeta.business.entities.logicnodecognition.dto;

import com.zeta.business.entities.logicnodecognition.*;
import com.zeta.business.media.CognitionMediaType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LogicNodeCognitionItemResponse {

  private Long id;
  private String title;
  private String imageUrl;
  private boolean hasImage;
  private CognitionMediaType mediaType;
  private Double leftPercent;
  private Double topPercent;
  private Double widthPercent;
  private Double heightPercent;
  private String content;
  private int sortOrder;
}
