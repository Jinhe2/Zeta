package com.zeta.business.entities.hardpressboardlist;

import com.zeta.business.entities.settinglist.SettingListScopeType;
import java.time.Instant;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "hard_pressboard_list_item",
    uniqueConstraints = @UniqueConstraint(columnNames = {"scope_type", "scope_id", "pressboard_ref"}))
@Getter
@Setter
@NoArgsConstructor
public class HardPressboardListItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "scope_type", nullable = false, length = 32)
  private SettingListScopeType scopeType;

  @Column(name = "scope_id", nullable = false)
  private Long scopeId;

  @Column(name = "pressboard_ref", nullable = false, length = 512)
  private String pressboardRef;

  @Column(name = "pressboard_name", nullable = false, length = 256)
  private String pressboardName;

  @Column(name = "baseline_value", nullable = false)
  private Boolean baselineValue;

  @Column(name = "compare_enabled", nullable = false)
  private Boolean compareEnabled = true;

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
