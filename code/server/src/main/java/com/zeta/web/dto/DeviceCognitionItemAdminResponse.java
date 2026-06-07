package com.zeta.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class DeviceCognitionItemAdminResponse {

    private Long id;
    private Long deviceId;
    private String deviceName;
    private String title;
    private String content;
    private int sortOrder;
    private boolean enabled;
    private Instant createdAt;
}
