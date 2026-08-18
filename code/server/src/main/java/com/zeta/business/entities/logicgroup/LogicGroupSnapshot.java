package com.zeta.business.entities.logicgroup;

import java.time.Instant;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 组合逻辑实验结果快照，存储 monitord 返回的 v3.0 组合 JSON。 */
@Entity
@Table(
    name = "logic_group_snapshot",
    indexes = {
      @Index(name = "idx_lgs_user", columnList = "user_id"),
      @Index(name = "idx_lgs_group", columnList = "group_id")
    })
@Getter
@Setter
@NoArgsConstructor
public class LogicGroupSnapshot {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "group_id", nullable = false)
  private Long groupId;

  @Column(name = "group_name", length = 256)
  private String groupName;

  @Column(name = "snapshot_json", nullable = false, columnDefinition = "JSON")
  private String snapshotJson;

  @Column(name = "total_transitions")
  private Integer totalTransitions = 0;

  @Column(name = "experiment_passed")
  private Boolean experimentPassed;

  /** COMPLETED / FAILED */
  @Column(length = 16, nullable = false)
  private String status = "COMPLETED";

  /** MONITOR — 数据来源标记 */
  @Column(length = 16, nullable = false)
  private String source = "MONITOR";

  @Lob private String errorMessage;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @PrePersist
  void prePersist() {
    createdAt = Instant.now();
  }
}
