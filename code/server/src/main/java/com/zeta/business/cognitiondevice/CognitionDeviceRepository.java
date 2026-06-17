package com.zeta.business.cognitiondevice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CognitionDeviceRepository extends JpaRepository<CognitionDevice, Long> {

    List<CognitionDevice> findByCabinetDisplayItemIdOrderBySortOrderAscIdAsc(Long cabinetDisplayItemId);

    Optional<CognitionDevice> findByScreenDeviceIdAndCabinetDisplayItemId(Long screenDeviceId, Long cabinetDisplayItemId);

    List<CognitionDevice> findByScreenDeviceId(Long screenDeviceId);
}
