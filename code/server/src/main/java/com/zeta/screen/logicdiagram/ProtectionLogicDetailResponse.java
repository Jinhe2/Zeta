package com.zeta.screen.logicdiagram;

import com.zeta.screen.logicdiagram.dto.ConfigDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProtectionLogicDetailResponse {

    private Long id;
    private String code;
    private String title;
    private String description;
    private String category;
    private ConfigDto config;
    private Long deviceId;
    private String iedName;
}
