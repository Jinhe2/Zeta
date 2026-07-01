package com.zeta.screen.wiring;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WiringDetectionSignalRepository extends JpaRepository<WiringDetectionSignal, Long> {

    List<WiringDetectionSignal> findByDeviceIdOrderBySignalIndexAsc(Long deviceId);
}
