package com.zeta.screen.terminal;

import com.zeta.screen.cabinet.Cabinet;
import com.zeta.screen.ieddevice.Device;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

/** 端子，对应 ct-screen.terminal */
@Entity
@Table(name = "terminal")
@Getter
@Setter
@NoArgsConstructor
public class Terminal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cabinet_id", nullable = false)
    private Cabinet cabinet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminal_strip_id")
    private TerminalStrip terminalStrip;

    @Column(name = "terminal_label", nullable = false, length = 64)
    private String terminalLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_type", nullable = false)
    private SignalType signalType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ied_device_id")
    private Device iedDevice;

    @Column(name = "ied_signal_ref", length = 512)
    private String iedSignalRef;

    @Column(name = "common_end_terminal_id")
    private Long commonEndTerminalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "common_end_group_id")
    private CommonEndGroup commonEndGroup;

    @Lob
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum SignalType {
        DIGITAL, ANALOG, END, DO
    }
}
