package com.zeta.business.entities.drawinglearning.dto;

import com.zeta.business.entities.drawinglearning.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DrawingCognitionItemResponse {
  private Long id;
  private Long pageId;
  private String title;
  private String content;
  private Double leftPercent;
  private Double topPercent;
  private Double widthPercent;
  private Double heightPercent;
  private Integer sortOrder;
  private Boolean enabled;
  private Instant createdAt;
}
