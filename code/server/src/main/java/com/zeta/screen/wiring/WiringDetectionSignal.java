package com.zeta.screen.wiring;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

/** 连线检测信号，对应 ct-screen.wiring_detection_signal */
@Entity
@Table(name = "wiring_detection_signal")
@Getter
@Setter
@NoArgsConstructor
public class WiringDetectionSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private WiringDetectionDevice device;

    @Column(name = "signal_name", nullable = false, length = 128)
    private String signalName;

    @Column(name = "signal_index", nullable = false)
    private Integer signalIndex;

    @Lob
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
