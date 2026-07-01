package com.zeta.screen.monitor;

import com.zeta.screen.ieddevice.Device;
import com.zeta.screen.logicdiagram.ProtectionLogic;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

/**
 * 实验监控任务，对应 ct-screen.monitor_task。
 * 由乙方监控系统写入，本系统只读。
 */
@Entity
@Table(name = "monitor_task")
@Getter
@Setter
@NoArgsConstructor
public class MonitorTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ied_device_id", nullable = false)
    private Device iedDevice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "logic_diagram_id", nullable = false)
    private ProtectionLogic logicDiagram;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskState state;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Lob
    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "total_transitions")
    private Integer totalTransitions;

    @Lob
    @Column(name = "snapshot_json")
    private String snapshotJson;

    @Lob
    @Column(name = "comtrade_cfg")
    private byte[] comtradeCfg;

    @Lob
    @Column(name = "comtrade_dat")
    private byte[] comtradeDat;

    @Lob
    @Column(name = "comtrade_mid")
    private byte[] comtradeMid;

    @Lob
    @Column(name = "comtrade_des")
    private byte[] comtradeDes;

    @Lob
    @Column(name = "comtrade_hdr")
    private byte[] comtradeHdr;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum TaskState {
        PENDING,
        INITIALIZING,
        SUMMONING_SETTINGS,
        WAITING_RCD_MADE,
        SUMMONING_FILES,
        PARSING_FILES,
        GENERATING_SNAPSHOT,
        COMPLETED,
        FAILED,
        TIMEOUT,
        CANCELLED
    }
}
