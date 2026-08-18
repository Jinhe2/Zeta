package com.zeta.business.entities.wiringrequirement;

import java.time.Instant;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "wiring_requirement_config",
    uniqueConstraints = @UniqueConstraint(columnNames = {"logic_diagram_id", "category"}))
@Getter
@Setter
@NoArgsConstructor
public class WiringRequirementConfig {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "logic_diagram_id", nullable = false)
  private Long logicDiagramId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private WiringCategory category;

  @Column(nullable = false)
  private Boolean required = false;

  @Enumerated(EnumType.STRING)
  @Column(name = "phase_mode", nullable = false, length = 16)
  private PhaseMode phaseMode = PhaseMode.THREE_PHASE;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void prePersist() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = Instant.now();
  }
}
