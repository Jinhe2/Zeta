package com.zeta.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class CabinetAdminResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private int sortOrder;
    private boolean enabled;
    private int deviceCount;
    private Instant createdAt;
}
