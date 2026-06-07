package com.zeta.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProtectionLogicBriefResponse {

    private Long id;
    private Long deviceId;
    private String code;
    private String title;
    private String description;
    private String category;
    private int sortOrder;
}
