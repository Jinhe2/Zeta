package com.zeta.screen.logicdiagram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class InputDto {

    private String id;
    private String name;
    private String channelRef;
    private String channelType;
    private String unit;
    private String thresholdRef;
    private Object thresholdValue;
    private Object baseValue;
    private String comparison;
}
