package com.zeta.screen.hardpressboard;

import com.zeta.screen.cabinet.Cabinet;
import com.zeta.screen.ieddevice.Device;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

/** 硬压板，对应 ct-screen.hard_pressboard */
@Entity
@Table(name = "hard_pressboard")
@Getter
@Setter
@NoArgsConstructor
public class HardPressboard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cabinet_id", nullable = false)
    private Cabinet cabinet;

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "pressboard_type", nullable = false)
    private PressboardType pressboardType;

    @Column(name = "row_no", nullable = false)
    private Integer rowNo;

    @Column(name = "col_no", nullable = false)
    private Integer colNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ied_device_id")
    private Device iedDevice;

    @Column(name = "ied_signal_ref", length = 512)
    private String iedSignalRef;

    @Column(name = "wiring_detection_device_id")
    private Long wiringDetectionDeviceId;

    @Column(name = "input_channel_no")
    private Integer inputChannelNo;

    @Column(name = "output_channel_no")
    private Integer outputChannelNo;

    @Lob
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum PressboardType {
        FUNCTION, EXPORT, SPARE
    }
}
