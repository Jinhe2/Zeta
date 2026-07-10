package com.zeta.business.devicedisplay;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.zeta.business.media.CognitionMediaType;

@Getter
@AllArgsConstructor
public class DeviceDisplayItemResponse {

    private Long id;
    private String title;
    private String imageUrl;
    private CognitionMediaType mediaType;
    private Double leftPercent;
    private Double topPercent;
    private Double widthPercent;
    private Double heightPercent;
    private String content;
    private int sortOrder;
}
