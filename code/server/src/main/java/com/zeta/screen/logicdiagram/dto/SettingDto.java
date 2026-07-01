package com.zeta.screen.logicdiagram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SettingDto {

    private String id;
    private String name;
    private Object defaultValue;
    private String sourceType;
    private String sourceRef;
    private String dataPoint;
    private String dataType;
    private String unit;
}
