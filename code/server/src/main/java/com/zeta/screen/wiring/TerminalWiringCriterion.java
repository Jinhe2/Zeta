package com.zeta.screen.wiring;

import com.zeta.screen.terminal.Terminal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

/** 端子连线判据，对应 ct-screen.terminal_wiring_criterion */
@Entity
@Table(name = "terminal_wiring_criterion")
@Getter
@Setter
@NoArgsConstructor
public class TerminalWiringCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "terminal_id", nullable = false)
    private Terminal terminal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wiring_detection_device_id", nullable = false)
    private WiringDetectionDevice wiringDetectionDevice;

    @Enumerated(EnumType.STRING)
    @Column(name = "criterion_type", nullable = false)
    private CriterionType criterionType;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "abnormal_message", length = 256)
    private String abnormalMessage;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum CriterionType {
        NORMAL, ABNORMAL
    }
}
