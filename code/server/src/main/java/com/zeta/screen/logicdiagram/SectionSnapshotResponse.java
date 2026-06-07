package com.zeta.screen.logicdiagram;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class SectionSnapshotResponse {

    private String id;
    private String label;
    private double time;
    private Map<String, Boolean> states;
}
