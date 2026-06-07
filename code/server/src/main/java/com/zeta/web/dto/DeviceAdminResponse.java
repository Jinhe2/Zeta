package com.zeta.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class DeviceAdminResponse {

    private Long id;
    private Long cabinetId;
    private String cabinetName;
    private String code;
    private String name;
    private String description;
    private int sortOrder;
    private boolean enabled;
    private int protectionLogicCount;
    private int cognitionItemCount;
    private Instant createdAt;
}
