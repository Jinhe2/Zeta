package com.zeta.business.monitor;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

/**
 * 实验监控任务，对应 ct-screen-monitor.monitor_task。
 * 由乙方研判程序写入，本系统读取。
 * 注意：ied_device_id 和 logic_diagram_id 是底层库 (ct-screen) 的外键，
 * 但本实体在业务库 (ct-screen-monitor) 中，不能跨库 @ManyToOne，仅存 ID。
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

    @Column(name = "ied_device_id", nullable = false)
    private Long iedDeviceId;

    @Column(name = "logic_diagram_id", nullable = false)
    private Long logicDiagramId;

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
