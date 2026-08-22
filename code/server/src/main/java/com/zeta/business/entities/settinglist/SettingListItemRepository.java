package com.zeta.business.entities.settinglist;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingListItemRepository extends JpaRepository<SettingListItem, Long> {
  List<SettingListItem> findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(
      SettingListScopeType scopeType, Long scopeId);

  List<SettingListItem> findByScopeTypeAndScopeIdIn(
      SettingListScopeType scopeType, Collection<Long> scopeIds);

  long countByScopeTypeAndScopeId(SettingListScopeType scopeType, Long scopeId);

  void deleteByScopeTypeAndScopeId(SettingListScopeType scopeType, Long scopeId);

  void deleteByScopeTypeAndScopeIdIn(SettingListScopeType scopeType, Collection<Long> scopeIds);
}
