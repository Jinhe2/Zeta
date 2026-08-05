package com.zeta.business.entities.devicedisplay.dto;

import com.zeta.business.entities.devicedisplay.*;
import com.zeta.business.media.CognitionMediaType;
import lombok.AllArgsConstructor;
import lombok.Getter;

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
    private TerminalOperationResponse terminalOperation;
}
