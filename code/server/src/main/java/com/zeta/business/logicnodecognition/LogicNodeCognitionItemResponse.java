package com.zeta.business.logicnodecognition;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.zeta.business.media.CognitionMediaType;

@Getter
@AllArgsConstructor
public class LogicNodeCognitionItemResponse {

    private Long id;
    private String title;
    private String imageUrl;
    private boolean hasImage;
    private CognitionMediaType mediaType;
    private Double leftPercent;
    private Double topPercent;
    private Double widthPercent;
    private Double heightPercent;
    private String content;
    private int sortOrder;
}
