package com.zeta.business.entities.drawinglearning.dto;

import com.zeta.business.entities.drawinglearning.*;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DrawingGroupDetailResponse {
  private Long id;
  private Long cabinetId;
  private DrawingType drawingType;
  private String name;
  private Integer sortOrder;
  private List<DrawingPageResponse> pages;
}
