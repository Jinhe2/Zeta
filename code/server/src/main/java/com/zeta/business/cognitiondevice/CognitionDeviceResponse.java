package com.zeta.business.cognitiondevice;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CognitionDeviceResponse {

    private Long id;
    private CognitionDeviceType deviceType;
    private String title;
    private double leftPercent;
    private double topPercent;
    private double widthPercent;
    private double heightPercent;
    private int sortOrder;
}
