package com.zeta.business.entities.learningpath;

import java.time.Instant;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "learning_progress_events", indexes =
    @Index(name = "idx_learning_progress_event_user", columnList = "user_id,occurred_at"))
@Getter @Setter @NoArgsConstructor
public class LearningProgressEvent {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "user_id", nullable = false) private Long userId;
  @Column(name = "module_code", nullable = false, length = 32) private String moduleCode;
  @Column(name = "event_type", nullable = false, length = 32) private String eventType;
  @Column(name = "operator_user_id") private Long operatorUserId;
  @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
  @Column(length = 256) private String note;
}
