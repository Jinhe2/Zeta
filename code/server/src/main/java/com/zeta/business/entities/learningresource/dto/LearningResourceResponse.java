package com.zeta.business.entities.learningresource.dto;

import com.zeta.business.entities.learningresource.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LearningResourceResponse {
  private Long id;
  private String name;
  private String description;
  private LearningResourceType resourceType;
  private Long cabinetId;
  private String cabinetName;
  private String originalFilename;
  private String contentType;
  private Long fileSize;
  private Instant createdAt;
  private Instant updatedAt;
}
