package com.zeta.business.devicedisplay;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceDisplayItemRepository extends JpaRepository<DeviceDisplayItem, Long> {

    Optional<DeviceDisplayItem> findByScreenDeviceIdAndTitle(Long screenDeviceId, String title);

    List<DeviceDisplayItem> findByScreenDeviceIdOrderBySortOrderAscIdAsc(Long screenDeviceId);
}
