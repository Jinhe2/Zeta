package com.zeta.business.entities.learningpath;

import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "learning_module_prerequisites", uniqueConstraints =
    @UniqueConstraint(name = "uk_learning_module_prerequisite", columnNames = {"module_code", "prerequisite_code"}))
@Getter @Setter @NoArgsConstructor
public class LearningModulePrerequisite {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "module_code", nullable = false, length = 32)
  private String moduleCode;

  @Column(name = "prerequisite_code", nullable = false, length = 32)
  private String prerequisiteCode;
}
