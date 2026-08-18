package com.zeta.business.entities.logicgroup;

import java.time.Instant;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 组合逻辑成员：一个基础逻辑框图在组合中的顺序。 */
@Entity
@Table(
    name = "logic_group_member",
    uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "logic_diagram_id"}),
    indexes = @Index(name = "idx_lgm_group_order", columnList = "group_id, sort_order"))
@Getter
@Setter
@NoArgsConstructor
public class LogicGroupMember {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "group_id", nullable = false)
  private Long groupId;

  /** 屏柜库 logic_diagram.id（跨库，无外键） */
  @Column(name = "logic_diagram_id", nullable = false)
  private Long logicDiagramId;

  /** 成员顺序，决定下发 monitord logic_ids 的顺序 */
  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder = 0;

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
