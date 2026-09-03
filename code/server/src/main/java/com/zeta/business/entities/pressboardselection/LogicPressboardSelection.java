package com.zeta.business.entities.pressboardselection;

import com.zeta.business.entities.settinglist.SettingListScopeType;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 逻辑只保存压板引用，基准状态统一读取装置清单。 */
@Entity
@Table(name = "logic_pressboard_selection", uniqueConstraints = @UniqueConstraint(
    columnNames = {"pressboard_kind", "scope_type", "scope_id", "pressboard_ref"}))
@Getter
@Setter
public class LogicPressboardSelection {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Enumerated(EnumType.STRING)
  @Column(name = "pressboard_kind", nullable = false, length = 16)
  private PressboardKind pressboardKind;
  @Enumerated(EnumType.STRING)
  @Column(name = "scope_type", nullable = false, length = 32)
  private SettingListScopeType scopeType;
  @Column(name = "scope_id", nullable = false)
  private Long scopeId;
  @Column(name = "pressboard_ref", nullable = false, length = 512)
  private String pressboardRef;
}
