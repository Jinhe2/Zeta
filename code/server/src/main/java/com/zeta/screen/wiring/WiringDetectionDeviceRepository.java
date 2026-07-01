package com.zeta.screen.wiring;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WiringDetectionDeviceRepository extends JpaRepository<WiringDetectionDevice, Long> {

    List<WiringDetectionDevice> findByCabinetIdOrderByIdAsc(Long cabinetId);
}
