package com.zeta.business.logicnodecognition;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LogicNodeCognitionItemResponse {

    private Long id;
    private String title;
    private String imageUrl;
    private Double leftPercent;
    private Double topPercent;
    private Double widthPercent;
    private Double heightPercent;
    private String content;
    private int sortOrder;
}
