package com.zeta.business.entities.drawinglearning.dto;

import com.zeta.business.entities.drawinglearning.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DrawingGroupAdminResponse {
  private Long id;
  private Long cabinetId;
  private String cabinetName;
  private DrawingType drawingType;
  private String name;
  private Integer sortOrder;
  private Boolean enabled;
  private long pageCount;
  private long cognitionItemCount;
  private Instant createdAt;
}
