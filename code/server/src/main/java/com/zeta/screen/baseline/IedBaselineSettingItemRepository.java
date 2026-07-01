package com.zeta.screen.baseline;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IedBaselineSettingItemRepository extends JpaRepository<IedBaselineSettingItem, Long> {

    List<IedBaselineSettingItem> findByIedDeviceIdOrderBySortOrderAsc(Long iedDeviceId);
}
