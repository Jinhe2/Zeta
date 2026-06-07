package com.zeta.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CabinetSummaryResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private int sortOrder;
    private int deviceCount;
}
