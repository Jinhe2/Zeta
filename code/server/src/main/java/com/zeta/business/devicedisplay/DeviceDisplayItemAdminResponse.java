package com.zeta.business.devicedisplay;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class DeviceDisplayItemAdminResponse {

    private Long id;
    private Long deviceId;
    private String deviceName;
    private String title;
    private String content;
    private int sortOrder;
    private boolean enabled;
    private Instant createdAt;
}
