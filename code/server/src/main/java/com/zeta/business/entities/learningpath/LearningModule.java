package com.zeta.business.entities.learningpath;

import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "learning_modules")
@Getter @Setter @NoArgsConstructor
public class LearningModule {
  @Id
  @Column(length = 32)
  private String code;

  @Column(nullable = false, length = 64)
  private String name;

  @Column(nullable = false, length = 256)
  private String description;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(nullable = false)
  private boolean enabled = true;

  @Enumerated(EnumType.STRING)
  @Column(name = "completion_mode", nullable = false, length = 32)
  private LearningCompletionMode completionMode = LearningCompletionMode.MANUAL_CONFIRM;
}
