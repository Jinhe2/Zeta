package com.zeta.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CabinetDetailResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private int sortOrder;
    private List<DeviceSummaryResponse> devices;
}
