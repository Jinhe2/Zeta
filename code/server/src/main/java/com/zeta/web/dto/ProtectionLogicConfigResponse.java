package com.zeta.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProtectionLogicConfigResponse {

    private Long id;
    private Long deviceId;
    private String code;
    private String title;
    private String configJson;
}
