package com.zeta.business.entities.drawinglearning.dto;

import com.zeta.business.entities.drawinglearning.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DrawingPageAdminResponse {
  private Long id;
  private Long groupId;
  private String groupName;
  private DrawingType drawingType;
  private String title;
  private String imageUrl;
  private Integer sortOrder;
  private Boolean enabled;
  private long cognitionItemCount;
  private Instant createdAt;
}
