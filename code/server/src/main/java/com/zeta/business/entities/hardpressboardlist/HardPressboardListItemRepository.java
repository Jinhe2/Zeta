package com.zeta.business.entities.hardpressboardlist;

import com.zeta.business.entities.settinglist.SettingListScopeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HardPressboardListItemRepository
    extends JpaRepository<HardPressboardListItem, Long> {
  List<HardPressboardListItem> findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(
      SettingListScopeType scopeType, Long scopeId);

  List<HardPressboardListItem> findByScopeTypeAndScopeIdIn(
      SettingListScopeType scopeType, Collection<Long> scopeIds);

  void deleteByScopeTypeAndScopeId(SettingListScopeType scopeType, Long scopeId);

  void deleteByScopeTypeAndScopeIdIn(SettingListScopeType scopeType, Collection<Long> scopeIds);
}
