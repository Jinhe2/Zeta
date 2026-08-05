package com.zeta.screen.knowledge;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

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
