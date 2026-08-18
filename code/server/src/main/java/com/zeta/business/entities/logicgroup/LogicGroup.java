package com.zeta.business.entities.logicgroup;

import java.time.Instant;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 组合逻辑：把同一装置的若干基础逻辑框图按序拼接为一个整体。 */
@Entity
@Table(name = "logic_group", indexes = {@Index(name = "idx_lg_device", columnList = "ied_device_id")})
@Getter
@Setter
@NoArgsConstructor
public class LogicGroup {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 屏柜库 ied_device.id（跨库，无外键） */
  @Column(name = "ied_device_id", nullable = false)
  private Long iedDeviceId;

  @Column(name = "name", nullable = false, length = 256)
  private String name;

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
