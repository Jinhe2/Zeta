package com.zeta.screen.logicdiagram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GateDto {

    private String id;
    private String type;
    private Boolean inverted;
    private List<GateInputDto> inputs;
}
