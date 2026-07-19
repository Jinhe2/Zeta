package com.zeta.business.devicedisplay;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceDisplayItemRepository extends JpaRepository<DeviceDisplayItem, Long> {
    boolean existsByVideoPath(String videoPath);

    Optional<DeviceDisplayItem> findByCognitionDeviceIdAndTitle(Long cognitionDeviceId, String title);

    List<DeviceDisplayItem> findByCognitionDeviceIdOrderBySortOrderAscIdAsc(Long cognitionDeviceId);

    boolean existsByCognitionDeviceIdAndMediaType(Long cognitionDeviceId, com.zeta.business.media.CognitionMediaType mediaType);

    boolean existsByCognitionDeviceIdAndMediaTypeAndIdNot(Long cognitionDeviceId, com.zeta.business.media.CognitionMediaType mediaType, Long id);
}
