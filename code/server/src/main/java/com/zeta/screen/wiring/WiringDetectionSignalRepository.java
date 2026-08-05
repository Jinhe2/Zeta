package com.zeta.screen.wiring;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WiringDetectionSignalRepository extends JpaRepository<WiringDetectionSignal, Long> {

    List<WiringDetectionSignal> findByDeviceIdOrderBySignalIndexAsc(Long deviceId);
}
