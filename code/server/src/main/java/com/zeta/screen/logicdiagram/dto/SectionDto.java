package com.zeta.screen.logicdiagram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SectionDto {

    private String id;
    private String label;
    private Double time;

    /** 新版格式 */
    private Map<String, Boolean> states;

    /** 旧版格式（兼容） */
    private Map<String, Boolean> nodeStates;
}
