package com.zeta.business.entities.cognitiondevice;

import com.zeta.business.entities.cognitiondevice.dto.*;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CognitionDeviceRepository extends JpaRepository<CognitionDevice, Long> {

  List<CognitionDevice> findByCabinetDisplayItemIdOrderBySortOrderAscIdAsc(
      Long cabinetDisplayItemId);

  Optional<CognitionDevice> findByScreenDeviceIdAndCabinetDisplayItemId(
      Long screenDeviceId, Long cabinetDisplayItemId);

  List<CognitionDevice> findByScreenDeviceId(Long screenDeviceId);
}
