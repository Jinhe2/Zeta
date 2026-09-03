package com.zeta.business.entities.wholeexperiment;

import java.time.Instant;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "whole_experiment_run",
    indexes = @Index(name = "idx_whole_run_history", columnList = "experiment_id,user_id,created_at"))
@Getter
@Setter
public class WholeExperimentRun {
  @Id @Column(name = "task_uuid", length = 36) private String taskUuid;
  @Column(name = "experiment_id", nullable = false) private Long experimentId;
  @Column(name = "user_id", nullable = false) private Long userId;
  @Column(name = "members_json", nullable = false, columnDefinition = "LONGTEXT") private String membersJson;
  @Column(name = "ied_name", nullable = false, length = 256) private String iedName;
  @Column(nullable = false, length = 32) private String status = "STARTING";
  @Column(name = "result_json", columnDefinition = "LONGTEXT") private String resultJson;
  @Column(name = "snapshot_json", columnDefinition = "LONGTEXT") private String snapshotJson;
  @Column(name = "total_transitions") private Integer totalTransitions = 0;
  @Column(name = "experiment_passed") private Boolean experimentPassed;
  @Column(name = "error_message", columnDefinition = "LONGTEXT") private String errorMessage;
  @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
  @Column(name = "accepted_at") private Instant acceptedAt;
  @Column(name = "completed_at") private Instant completedAt;
  @Transient public boolean isTerminal() {
    return "COMPLETED".equals(status) || "FAILED".equals(status) || "START_FAILED".equals(status);
  }
}
