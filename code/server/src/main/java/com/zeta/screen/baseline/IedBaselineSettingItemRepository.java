package com.zeta.screen.baseline;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IedBaselineSettingItemRepository extends JpaRepository<IedBaselineSettingItem, Long> {

    List<IedBaselineSettingItem> findByIedDeviceIdOrderBySortOrderAsc(Long iedDeviceId);
}
