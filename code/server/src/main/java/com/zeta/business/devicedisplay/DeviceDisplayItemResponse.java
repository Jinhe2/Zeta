package com.zeta.business.devicedisplay;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeviceDisplayItemResponse {

    private Long id;
    private String title;
    private String content;
    private int sortOrder;
}
