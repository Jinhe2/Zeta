package com.zeta.screen.hardpressboard;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HardPressboardRepository extends JpaRepository<HardPressboard, Long> {

    List<HardPressboard> findByCabinetIdOrderByIdAsc(Long cabinetId);

    List<HardPressboard> findByIedDeviceIdOrderByIdAsc(Long iedDeviceId);
}
