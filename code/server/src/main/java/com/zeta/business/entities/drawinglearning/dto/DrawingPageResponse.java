package com.zeta.business.entities.drawinglearning.dto;

import com.zeta.business.entities.drawinglearning.*;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DrawingPageResponse {
  private Long id;
  private Long groupId;
  private String title;
  private Integer sortOrder;
  private List<DrawingCognitionItemResponse> items;
}
