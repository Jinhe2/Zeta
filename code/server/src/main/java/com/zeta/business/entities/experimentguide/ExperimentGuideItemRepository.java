package com.zeta.business.entities.experimentguide;

import com.zeta.business.entities.settinglist.SettingListScopeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperimentGuideItemRepository
    extends JpaRepository<ExperimentGuideItem, Long> {

  List<ExperimentGuideItem> findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(
      SettingListScopeType scopeType, Long scopeId);

  List<ExperimentGuideItem> findByScopeTypeAndScopeIdIn(
      SettingListScopeType scopeType, Collection<Long> scopeIds);

  void deleteByScopeTypeAndScopeId(SettingListScopeType scopeType, Long scopeId);

  void deleteByScopeTypeAndScopeIdIn(SettingListScopeType scopeType, Collection<Long> scopeIds);
}
