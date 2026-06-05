package com.zeta.web.dto;

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
    private Object config;
}
