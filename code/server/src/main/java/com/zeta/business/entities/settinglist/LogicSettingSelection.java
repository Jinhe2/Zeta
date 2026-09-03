package com.zeta.business.entities.settinglist;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 逻辑仅保存参与校验的定值引用，数值统一来自装置。 */
@Entity
@Table(name = "logic_setting_selection", uniqueConstraints =
    @UniqueConstraint(columnNames = {"scope_type", "scope_id", "setting_ref"}))
@Getter
@Setter
public class LogicSettingSelection {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Enumerated(EnumType.STRING)
  @Column(name = "scope_type", nullable = false, length = 32)
  private SettingListScopeType scopeType;
  @Column(name = "scope_id", nullable = false)
  private Long scopeId;
  @Column(name = "setting_ref", nullable = false, length = 512)
  private String settingRef;
}
