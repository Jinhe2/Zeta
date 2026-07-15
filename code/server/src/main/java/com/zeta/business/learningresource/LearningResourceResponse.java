package com.zeta.business.learningresource;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

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
