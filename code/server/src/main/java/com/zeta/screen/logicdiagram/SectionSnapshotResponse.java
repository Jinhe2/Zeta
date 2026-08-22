package com.zeta.screen.logicdiagram;

import java.util.Map;
import lombok.Getter;

@Getter
public class SectionSnapshotResponse {

    private String id;
    private String label;
    private double time;
    private String timestamp;
    /** 节点断面值：1 表示满足，0 表示不满足，-1 表示节点无实际数据。 */
    private Map<String, Integer> states;

    public SectionSnapshotResponse(String id, String label, double time, String timestamp,
                                   Map<String, Integer> states) {
        this.id = id;
        this.label = label;
        this.time = time;
        this.timestamp = timestamp;
        this.states = states;
    }
}
