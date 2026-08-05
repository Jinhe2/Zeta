package com.zeta.screen.logicdiagram;

import java.util.Map;
import lombok.Getter;

@Getter
public class SectionSnapshotResponse {

    private String id;
    private String label;
    private double time;
    private String timestamp;
    private Map<String, Boolean> states;

    public SectionSnapshotResponse(String id, String label, double time, String timestamp,
                                   Map<String, Boolean> states) {
        this.id = id;
        this.label = label;
        this.time = time;
        this.timestamp = timestamp;
        this.states = states;
    }
}
