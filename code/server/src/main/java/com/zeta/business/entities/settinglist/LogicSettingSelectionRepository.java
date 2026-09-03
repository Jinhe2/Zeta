package com.zeta.business.entities.settinglist;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogicSettingSelectionRepository extends JpaRepository<LogicSettingSelection, Long> {
  List<LogicSettingSelection> findByScopeTypeAndScopeId(SettingListScopeType type, Long id);
  List<LogicSettingSelection> findByScopeTypeAndScopeIdIn(SettingListScopeType type, Collection<Long> ids);
  void deleteByScopeTypeAndScopeId(SettingListScopeType type, Long id);
  void deleteByScopeTypeAndScopeIdIn(SettingListScopeType type, Collection<Long> ids);
}
