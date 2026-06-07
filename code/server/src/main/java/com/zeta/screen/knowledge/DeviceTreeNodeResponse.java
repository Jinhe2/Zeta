package com.zeta.screen.knowledge;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DeviceTreeNodeResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private int sortOrder;
    private List<ProtectionLogicBriefResponse> protectionLogics;
}
