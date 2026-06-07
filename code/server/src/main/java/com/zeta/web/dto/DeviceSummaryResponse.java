package com.zeta.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeviceSummaryResponse {

    private Long id;
    private Long cabinetId;
    private String cabinetName;
    private String code;
    private String name;
    private String description;
    private int sortOrder;
    private int protectionLogicCount;
}
