package com.zeta.business.entities.pressboardchange;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 功能硬压板变位历史，对应 TestMonitor 在业务历史库中维护的表。 */
@Entity
@Table(name = "hard_pressboard_change")
@Getter
@Setter
@NoArgsConstructor
public class PressboardChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cabinet_id", nullable = false)
    private Long cabinetId;

    @Column(name = "pressboard_id", nullable = false)
    private Long pressboardId;

    @Column(name = "pressboard_name", nullable = false, length = 128)
    private String pressboardName;

    @Column(nullable = false, length = 3, columnDefinition = "ENUM('ON','OFF')")
    private String state;

    @Column(name = "change_time_ms", nullable = false)
    private Long changeTimeMs;
}
