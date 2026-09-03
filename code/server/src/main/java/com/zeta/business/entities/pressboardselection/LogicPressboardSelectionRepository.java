package com.zeta.business.entities.pressboardselection;

import com.zeta.business.entities.settinglist.SettingListScopeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogicPressboardSelectionRepository extends JpaRepository<LogicPressboardSelection, Long> {
  List<LogicPressboardSelection> findByPressboardKindAndScopeTypeAndScopeId(
      PressboardKind kind, SettingListScopeType type, Long id);
  List<LogicPressboardSelection> findByPressboardKindAndScopeTypeAndScopeIdIn(
      PressboardKind kind, SettingListScopeType type, Collection<Long> ids);
  void deleteByPressboardKindAndScopeTypeAndScopeId(PressboardKind kind, SettingListScopeType type, Long id);
  void deleteByPressboardKindAndScopeTypeAndScopeIdIn(PressboardKind kind, SettingListScopeType type, Collection<Long> ids);
}
