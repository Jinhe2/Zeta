package com.zeta.screen.logicdiagram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GateInputDto {

    private String node;
    private Boolean inverted;
}
