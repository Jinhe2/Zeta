package com.zeta.business.entities.softpressboardlist;

import com.zeta.business.entities.settinglist.SettingListScopeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SoftPressboardListItemRepository
    extends JpaRepository<SoftPressboardListItem, Long> {
  List<SoftPressboardListItem> findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(
      SettingListScopeType scopeType, Long scopeId);

  List<SoftPressboardListItem> findByScopeTypeAndScopeIdIn(
      SettingListScopeType scopeType, Collection<Long> scopeIds);

  void deleteByScopeTypeAndScopeId(SettingListScopeType scopeType, Long scopeId);

  void deleteByScopeTypeAndScopeIdIn(SettingListScopeType scopeType, Collection<Long> scopeIds);
}
