package com.zeta.business.entities.wiringrequirement;

import java.time.Instant;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 试验仪接线要求端子分组（ABCN 四端子一组），跨库引用 ct-screen.terminal.id。 */
@Entity
@Table(name = "wiring_requirement_group")
@Getter
@Setter
@NoArgsConstructor
public class WiringRequirementGroup {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "config_id", nullable = false)
  private Long configId;

  @Column(name = "group_no", nullable = false)
  private Integer groupNo;

  @Column(name = "terminal_a_id")
  private Long terminalAId;

  @Column(name = "terminal_b_id")
  private Long terminalBId;

  @Column(name = "terminal_c_id")
  private Long terminalCId;

  @Column(name = "terminal_n_id")
  private Long terminalNId;

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
