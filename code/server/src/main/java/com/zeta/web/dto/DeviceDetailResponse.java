package com.zeta.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DeviceDetailResponse {

    private Long id;
    private Long cabinetId;
    private String cabinetCode;
    private String cabinetName;
    private String code;
    private String name;
    private String description;
    private int sortOrder;
    private List<ProtectionLogicBriefResponse> protectionLogics;
}
