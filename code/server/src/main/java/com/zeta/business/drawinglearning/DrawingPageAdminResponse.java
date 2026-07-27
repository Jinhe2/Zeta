package com.zeta.business.drawinglearning;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

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
