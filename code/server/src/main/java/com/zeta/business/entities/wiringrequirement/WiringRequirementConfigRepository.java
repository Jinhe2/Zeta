package com.zeta.business.entities.wiringrequirement;

import com.zeta.business.entities.settinglist.SettingListScopeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WiringRequirementConfigRepository
    extends JpaRepository<WiringRequirementConfig, Long> {
  List<WiringRequirementConfig> findByScopeTypeAndScopeIdOrderByIdAsc(
      SettingListScopeType scopeType, Long scopeId);

  void deleteByScopeTypeAndScopeId(SettingListScopeType scopeType, Long scopeId);
}
