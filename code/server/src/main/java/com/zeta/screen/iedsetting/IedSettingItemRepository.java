package com.zeta.screen.iedsetting;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IedSettingItemRepository extends JpaRepository<IedSettingItem, Long> {
  List<IedSettingItem> findByIedDeviceIdOrderByIdAsc(Long iedDeviceId);

  Optional<IedSettingItem> findByIedDeviceIdAndSettingRef(Long iedDeviceId, String settingRef);
}
