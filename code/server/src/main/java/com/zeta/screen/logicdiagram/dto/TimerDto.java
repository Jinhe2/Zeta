package com.zeta.screen.logicdiagram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimerDto {

    private String id;
    private String name;
    private String input;
    private String delayRef;
    private Object delayValue;
    private String delayType;
}
