package com.zeta.business.drawinglearning;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DrawingPageResponse {
    private Long id;
    private Long groupId;
    private String title;
    private Integer sortOrder;
    private List<DrawingCognitionItemResponse> items;
}
