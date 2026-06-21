package com.zeta.business.snapshot;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

/**
 * 断面数据 — 每次实验产生的 v2.3 快照，归属学员，关联保护逻辑。
 * 存储在 ct-screen-monitor（业务库，ddl-auto:update）。
 */
@Entity
@Table(name = "logic_snapshot", indexes = {
        @Index(name = "idx_ls_user", columnList = "user_id"),
        @Index(name = "idx_ls_logic", columnList = "logic_id"),
        @Index(name = "idx_ls_user_logic", columnList = "user_id,logic_id")
})
@Getter
@Setter
@NoArgsConstructor
public class LogicSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 关联 ct-screen.logic_diagram.id（跨库，无外键） */
    @Column(name = "logic_id", nullable = false)
    private Long logicId;

    /** 冗余 logic_diagram.logic_id，便于跨库查询 */
    @Column(name = "logic_code", length = 128)
    private String logicCode;

    /** 冗余逻辑名称，列表展示用 */
    @Column(name = "logic_name", length = 256)
    private String logicName;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "JSON")
    private String snapshotJson;

    @Column(name = "total_transitions")
    private Integer totalTransitions = 0;

    /** COMPLETED / GENERATING / FAILED */
    @Column(length = 16, nullable = false)
    private String status = "COMPLETED";

    /** SIMULATED / MANUAL / TESTMONITOR — 数据来源标记 */
    @Column(length = 16, nullable = false)
    private String source = "SIMULATED";

    @Lob
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant createdAt;

    @Column(name = "completed_at", columnDefinition = "TIMESTAMP(6)")
    private Instant completedAt;
}
