package com.zeta.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class ProtectionLogicAdminResponse {

    private Long id;
    private Long deviceId;
    private String deviceName;
    private String code;
    private String title;
    private String description;
    private String category;
    private int sortOrder;
    private boolean enabled;
    private Instant createdAt;
}
