package com.zeta.business.entities.devicedisplay;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceDisplayItemRepository extends JpaRepository<DeviceDisplayItem, Long> {
  boolean existsByVideoPath(String videoPath);

  Optional<DeviceDisplayItem> findByCognitionDeviceIdAndTitle(Long cognitionDeviceId, String title);

  List<DeviceDisplayItem> findByCognitionDeviceIdOrderBySortOrderAscIdAsc(Long cognitionDeviceId);

  boolean existsByCognitionDeviceIdAndMediaType(
      Long cognitionDeviceId, com.zeta.business.media.CognitionMediaType mediaType);

  boolean existsByCognitionDeviceIdAndMediaTypeAndIdNot(
      Long cognitionDeviceId, com.zeta.business.media.CognitionMediaType mediaType, Long id);
}
