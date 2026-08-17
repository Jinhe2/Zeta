package com.zeta.business.entities.settinglist;

import java.time.Instant;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "setting_list_item",
    uniqueConstraints = @UniqueConstraint(columnNames = {"scope_type", "scope_id", "setting_ref"}))
@Getter
@Setter
@NoArgsConstructor
public class SettingListItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "scope_type", nullable = false, length = 32)
  private SettingListScopeType scopeType;

  @Column(name = "scope_id", nullable = false)
  private Long scopeId;

  @Column(name = "setting_ref", nullable = false, length = 512)
  private String settingRef;

  @Column(name = "setting_fc", nullable = false, length = 16)
  private String settingFc;

  @Column(name = "setting_name", nullable = false, length = 256)
  private String settingName;

  @Column(name = "value_type", nullable = false, length = 16)
  private String valueType;

  @Column(name = "compare_enabled", nullable = false)
  private Boolean compareEnabled = true;

  @Column(name = "baseline_value", nullable = false, length = 64)
  private String baselineValue;

  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder;

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
