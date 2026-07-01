package com.zeta.screen.logicdiagram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigDto {

    private String version;
    private String name;
    private String description;
    private List<SettingDto> settings;
    private List<InputDto> inputs;
    private List<GateDto> gates;
    private List<TimerDto> timers;
    private List<OutputDto> outputs;
    private List<SectionDto> sections;

    /** nodeId → 初始显示值（语义类型，非泛型 Map） */
    private Map<String, String> displayState;
}
