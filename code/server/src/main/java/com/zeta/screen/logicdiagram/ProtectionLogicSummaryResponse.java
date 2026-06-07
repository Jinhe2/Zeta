package com.zeta.screen.logicdiagram;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProtectionLogicSummaryResponse {

    private Long id;
    private String code;
    private String title;
    private String description;
    private String category;
    private int inputCount;
    private int gateCount;
    private int outputCount;
}
