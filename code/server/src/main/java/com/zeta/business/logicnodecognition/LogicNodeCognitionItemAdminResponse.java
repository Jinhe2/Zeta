package com.zeta.business.logicnodecognition;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class LogicNodeCognitionItemAdminResponse {

    private Long id;
    private Long logicDiagramId;
    private String nodeId;
    private String nodeType;
    private String nodeName;
    private String title;
    private String imageUrl;
    private boolean hasImage;
    private Double leftPercent;
    private Double topPercent;
    private Double widthPercent;
    private Double heightPercent;
    private String content;
    private int sortOrder;
    private boolean enabled;
    private Instant createdAt;
}
