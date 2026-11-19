package com.zeta.business.entities.learningpath;

import java.time.Instant;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "student_learning_progress", uniqueConstraints =
    @UniqueConstraint(name = "uk_student_learning_progress", columnNames = {"user_id", "module_code"}),
    indexes = @Index(name = "idx_student_learning_progress_user", columnList = "user_id,status"))
@Getter @Setter @NoArgsConstructor
public class StudentLearningProgress {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "module_code", nullable = false, length = 32)
  private String moduleCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private StudentLearningStatus status;

  @Column(name = "unlocked_at", nullable = false)
  private Instant unlockedAt;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "last_activity_at")
  private Instant lastActivityAt;

  @Column(name = "visit_count", nullable = false)
  private int visitCount;
}
