package com.zeta.screen.logicdiagram;

import java.util.Collections;
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
    /** 按节点 ID 索引的当前断面有效值；旧快照不包含该数据时为空映射。 */
    private Map<String, EffectiveValue> effectiveValues;

    public SectionSnapshotResponse(String id, String label, double time, String timestamp,
                                   Map<String, Integer> states) {
        this(id, label, time, timestamp, states, Collections.emptyMap());
    }

    public SectionSnapshotResponse(String id, String label, double time, String timestamp,
                                   Map<String, Integer> states,
                                   Map<String, EffectiveValue> effectiveValues) {
        this.id = id;
        this.label = label;
        this.time = time;
        this.timestamp = timestamp;
        this.states = states;
        this.effectiveValues = effectiveValues;
    }

    @Getter
    public static class EffectiveValue {
        private final Double value;
        private final String effectiveValueType;
        private final String unit;

        public EffectiveValue(Double value, String effectiveValueType, String unit) {
            this.value = value;
            this.effectiveValueType = effectiveValueType;
            this.unit = unit;
        }
    }
}
