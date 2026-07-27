package com.zeta.business.drawinglearning;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DrawingGroupSummaryResponse {
    private Long id;
    private Long cabinetId;
    private DrawingType drawingType;
    private String name;
    private Integer sortOrder;
    private long pageCount;
    private long cognitionItemCount;
}
