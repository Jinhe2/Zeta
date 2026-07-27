package com.zeta.business.drawinglearning;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

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
