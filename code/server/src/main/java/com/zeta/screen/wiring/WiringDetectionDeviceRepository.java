package com.zeta.screen.wiring;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WiringDetectionDeviceRepository extends JpaRepository<WiringDetectionDevice, Long> {

    List<WiringDetectionDevice> findByCabinetIdOrderByIdAsc(Long cabinetId);
}
