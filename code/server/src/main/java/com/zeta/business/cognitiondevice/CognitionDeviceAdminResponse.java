package com.zeta.business.cognitiondevice;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class CognitionDeviceAdminResponse {

    private Long id;
    private Long cabinetDisplayItemId;
    private String cabinetDisplayItemTitle;
    private CognitionDeviceType deviceType;
    private Long screenDeviceId;
    private String screenDeviceName;
    private String title;
    private double leftPercent;
    private double topPercent;
    private double widthPercent;
    private double heightPercent;
    private int sortOrder;
    private boolean enabled;
    private Instant createdAt;
}
