package com.zeta.screen.baseline;

import com.zeta.screen.logicdiagram.ProtectionLogic;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

/** 逻辑框图基准定值项，对应 ct-screen.logic_diagram_baseline_setting_item */
@Entity
@Table(name = "logic_diagram_baseline_setting_item")
@Getter
@Setter
@NoArgsConstructor
public class LogicDiagramBaselineSettingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "logic_diagram_id", nullable = false)
    private ProtectionLogic logicDiagram;

    @Column(name = "setting_ref", nullable = false, length = 512)
    private String settingRef;

    @Column(name = "setting_fc", length = 16)
    private String settingFc;

    @Column(name = "setting_description", length = 256)
    private String settingDescription;

    @Column(name = "baseline_value", nullable = false, length = 64)
    private String baselineValue;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
