package com.zeta.repository;

import com.zeta.domain.DeviceCognitionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceCognitionItemRepository extends JpaRepository<DeviceCognitionItem, Long> {

    Optional<DeviceCognitionItem> findByDeviceIdAndTitle(Long deviceId, String title);

    List<DeviceCognitionItem> findByDeviceIdOrderBySortOrderAscIdAsc(Long deviceId);

    List<DeviceCognitionItem> findByDeviceIdAndEnabledTrueOrderBySortOrderAscIdAsc(Long deviceId);

    long countByDeviceId(Long deviceId);
}
