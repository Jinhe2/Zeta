package com.zeta.business.drawinglearning;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

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
